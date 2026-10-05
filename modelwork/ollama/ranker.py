"""Scoring (keywords + LLM) and the background ranking jobs."""
import hashlib
import json
import logging
import re
import threading
import uuid
from datetime import datetime, timezone
from pathlib import Path

import requests
from pydantic import BaseModel, Field, ValidationError, field_validator

import config
import db
import llm

log = logging.getLogger("ranker")

BASE_DIR = Path(__file__).parent
VACANCY_FILE = BASE_DIR / "vacancy.json"
CACHE_FILE = BASE_DIR / "score_cache.json"


# ------------------------------------------------------------------ vacancy
class Vacancy(BaseModel):
    title: str = Field(min_length=2, max_length=200)
    description: str = Field(min_length=10, max_length=4000)
    required_skills: list[str] = Field(default_factory=list, max_length=30)
    top_n: int = Field(default=config.TOP_N, ge=1, le=50)

    @field_validator("required_skills")
    @classmethod
    def clean_skills(cls, skills: list[str]) -> list[str]:
        cleaned, seen = [], set()
        for s in skills:
            s = s.strip()
            if s and s.lower() not in seen:
                seen.add(s.lower())
                cleaned.append(s)
        return cleaned


def load_default_vacancy() -> Vacancy | None:
    try:
        return Vacancy.model_validate_json(VACANCY_FILE.read_text(encoding="utf-8"))
    except (OSError, ValidationError) as e:
        log.warning("vacancy.json is missing or invalid: %s", e)
        return None


def _vacancy_hash(v: Vacancy) -> str:
    raw = json.dumps([v.title, v.description, sorted(s.lower() for s in v.required_skills)])
    return hashlib.sha256(raw.encode()).hexdigest()[:16]


# -------------------------------------------------------------------- cache
# The LLM is slow, so every (vacancy, model, resume text) score is remembered.
# New uploads only cost one model call per NEW resume.
_cache: dict[str, dict] = {}
_cache_lock = threading.Lock()


def load_cache() -> None:
    if CACHE_FILE.exists():
        try:
            _cache.update(json.loads(CACHE_FILE.read_text(encoding="utf-8")))
            log.info("Loaded %d cached scores", len(_cache))
        except (OSError, json.JSONDecodeError):
            log.warning("score_cache.json is unreadable, starting with an empty cache")


def _save_cache() -> None:
    tmp = CACHE_FILE.with_suffix(".tmp")
    tmp.write_text(json.dumps(_cache), encoding="utf-8")
    tmp.replace(CACHE_FILE)


def _cache_key(vac_hash: str, c: dict) -> str:
    content = hashlib.sha256((c.get("raw_text") or "").encode()).hexdigest()[:16]
    return f"{vac_hash}|{config.OLLAMA_MODEL}|{content}"


# ------------------------------------------------------------ text + scoring
SECTION_LIMITS = (("QUALIFICATIONS", "qualifications", 800),
                  ("SKILLS", "skills", 800),
                  ("EXPERIENCE", "experience", 1800))


def build_resume_text(c: dict) -> str:
    parts = []
    for label, key, limit in SECTION_LIMITS:
        value = (c.get(key) or "").strip()
        if value:
            parts.append(f"{label}:\n{value[:limit]}")
    if not parts:   # the Java parser found no sections, so use the raw text
        return (c.get("raw_text") or "")[:config.MAX_RESUME_CHARS]
    return "\n\n".join(parts)[:config.MAX_RESUME_CHARS]


def keyword_match(required: list[str], text: str):
    matched, missing = [], []
    for skill in required:
        pattern = r"(?<![A-Za-z0-9])" + re.escape(skill) + r"(?![A-Za-z0-9])"
        (matched if re.search(pattern, text, re.IGNORECASE) else missing).append(skill)
    pct = round(100 * len(matched) / len(required)) if required else None
    return matched, missing, pct


def _llm_score(v: Vacancy, vac_hash: str, c: dict) -> dict | None:
    key = _cache_key(vac_hash, c)
    with _cache_lock:
        if key in _cache:
            return _cache[key]

    for attempt in (1, 2):
        try:
            result = llm.ask_model(v.model_dump(), build_resume_text(c))
            with _cache_lock:
                _cache[key] = result
                _save_cache()
            return result
        except requests.Timeout:
            log.warning("Timeout while scoring candidate %s", c["id"])
            return None
        except (requests.ConnectionError, requests.HTTPError) as e:
            # Ollama is down or the model is missing: stop the whole job instead of
            # silently producing a keyword-only ranking.
            raise RuntimeError(f"Ollama request failed: {e}") from e
        except (ValueError, KeyError, TypeError) as e:
            log.warning("Bad model output for candidate %s (attempt %s): %s", c["id"], attempt, e)
    return None


def score_candidate(v: Vacancy, vac_hash: str, c: dict) -> dict:
    haystack = c.get("raw_text") or " ".join(
        filter(None, [c.get("qualifications"), c.get("skills"), c.get("experience")]))
    matched, missing, kw = keyword_match(v.required_skills, haystack)

    llm_result = _llm_score(v, vac_hash, c)
    llm_score = llm_result["score"] if llm_result else None

    if kw is None and llm_score is None:
        final = 0.0
    elif kw is None:
        final = float(llm_score)
    elif llm_score is None:
        final = float(kw)
    else:
        total = config.WEIGHT_KEYWORD + config.WEIGHT_LLM
        final = ((config.WEIGHT_KEYWORD * kw + config.WEIGHT_LLM * llm_score) / total
                 if total else float(llm_score))

    return {
        "candidate_id": c["id"],
        "name": c["full_name"],
        "email": c["email"],
        "phone": c["phone"],
        "file_name": c["file_name"],
        "final_score": round(final, 1),
        "keyword_score": kw,
        "llm_score": llm_score,
        "matched_skills": matched,
        "missing_skills": missing,
        "reason": llm_result["reason"] if llm_result
                  else "LLM scoring failed, ranked by keyword match only",
    }


# --------------------------------------------------------------------- jobs
_run_lock = threading.Lock()    # one ranking at a time: the model is the bottleneck
_jobs_lock = threading.Lock()
_jobs: dict[str, dict] = {}
_latest_job_id: str | None = None


def _now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


def create_job(v: Vacancy, trigger: str) -> str:
    global _latest_job_id
    job_id = uuid.uuid4().hex[:12]
    with _jobs_lock:
        _jobs[job_id] = {
            "job_id": job_id,
            "status": "queued",
            "trigger": trigger,
            "vacancy_title": v.title,
            "model": config.OLLAMA_MODEL,
            "created_at": _now(),
            "started_at": None,
            "finished_at": None,
            "progress": {"done": 0, "total": 0},
            "error": None,
            "result": None,
        }
        _latest_job_id = job_id
        while len(_jobs) > 20:               # keep only the 20 most recent jobs
            _jobs.pop(next(iter(_jobs)))
    return job_id


def get_job(job_id: str) -> dict | None:
    return _jobs.get(job_id)


def latest_job() -> dict | None:
    return _jobs.get(_latest_job_id) if _latest_job_id else None


def run_job(job_id: str, v: Vacancy) -> None:
    job = _jobs[job_id]
    with _run_lock:
        job["status"] = "running"
        job["started_at"] = _now()
        try:
            ollama = llm.ollama_status()
            if not ollama["reachable"]:
                raise RuntimeError(f"Ollama is not reachable at {config.OLLAMA_URL}")
            if not ollama["model_installed"]:
                raise RuntimeError(f"Model '{config.OLLAMA_MODEL}' is not installed. "
                                   f"Run: ollama pull {config.OLLAMA_MODEL}")

            candidates = db.fetch_candidates()
            job["progress"]["total"] = len(candidates)
            vac_hash = _vacancy_hash(v)

            scored = []
            for c in candidates:
                scored.append(score_candidate(v, vac_hash, c))
                job["progress"]["done"] += 1

            scored.sort(key=lambda r: (-r["final_score"], -(r["keyword_score"] or 0), r["candidate_id"]))
            top = scored[:v.top_n]
            for i, r in enumerate(top, start=1):
                r["rank"] = i

            job["result"] = {
                "total_candidates": len(scored),
                "llm_failures": sum(1 for r in scored if r["llm_score"] is None),
                "top": top,
            }
            job["status"] = "done"
        except Exception as e:
            log.exception("Ranking job %s failed", job_id)
            job["status"] = "failed"
            job["error"] = str(e)
        finally:
            job["finished_at"] = _now()