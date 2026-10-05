import logging
import secrets
from contextlib import asynccontextmanager

from fastapi import BackgroundTasks, Depends, FastAPI, HTTPException, Security
from fastapi.security import APIKeyHeader
from pydantic import BaseModel, Field

import config
import db
import llm
import ranker

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("main")


@asynccontextmanager
async def lifespan(app: FastAPI):
    config.validate()          # stops the server right here if the key or password is missing
    ranker.load_cache()
    log.info("Ready. Model: %s", config.OLLAMA_MODEL)
    yield


app = FastAPI(title="Resume Ranker", lifespan=lifespan)

# ------------------------------------------------------------------ security
api_key_header = APIKeyHeader(name="X-API-Key", auto_error=False)


def require_api_key(key: str | None = Security(api_key_header)) -> None:
    if not key or not secrets.compare_digest(key.encode(), config.SERVICE_API_KEY.encode()):
        raise HTTPException(status_code=401, detail="Invalid or missing API key")


# ----------------------------------------------------------------- endpoints
class DataReady(BaseModel):
    event: str = "resumes_stored"
    batch_id: str
    candidate_ids: list[int] = Field(default_factory=list)
    count: int = 0


@app.get("/health")
def health():
    """Public: checks the database and Ollama. Returns no secrets."""
    try:
        database = {"ok": True, "candidates": db.count_candidates()}
    except Exception as e:
        first_line = (str(e).splitlines() or [""])[0][:200]
        database = {"ok": False, "error": f"{type(e).__name__}: {first_line}"}

    ollama = llm.ollama_status()
    healthy = database["ok"] and ollama["reachable"] and ollama["model_installed"]
    return {"status": "ok" if healthy else "degraded",
            "model": config.OLLAMA_MODEL,
            "database": database,
            "ollama": ollama}


@app.post("/data-ready", dependencies=[Depends(require_api_key)])
def data_ready(payload: DataReady, background: BackgroundTasks):
    """Called by Spring Boot after a batch of resumes was saved."""
    log.info("data-ready: batch=%s, %d new candidates", payload.batch_id, payload.count)

    job_id, note = None, None
    if config.AUTO_RANK:
        vacancy = ranker.load_default_vacancy()
        if vacancy:
            job_id = ranker.create_job(vacancy, trigger=f"batch:{payload.batch_id}")
            background.add_task(ranker.run_job, job_id, vacancy)
        else:
            note = "AUTO_RANK is on but vacancy.json is missing or invalid"

    return {"received": True, "auto_rank_job_id": job_id, "note": note}


@app.post("/rank", status_code=202, dependencies=[Depends(require_api_key)])
def start_ranking(background: BackgroundTasks, vacancy: ranker.Vacancy | None = None):
    """Start a ranking job. Send a vacancy in the body, or send nothing to use vacancy.json."""
    v = vacancy or ranker.load_default_vacancy()
    if v is None:
        raise HTTPException(400, "No vacancy in the request and vacancy.json is missing or invalid")

    job_id = ranker.create_job(v, trigger="manual")
    background.add_task(ranker.run_job, job_id, v)
    return {"job_id": job_id, "status": "queued", "check": f"/rank/{job_id}"}


# "latest" must be declared before "{job_id}" or it would be treated as a job id
@app.get("/rank/latest", dependencies=[Depends(require_api_key)])
def latest_ranking():
    job = ranker.latest_job()
    if job is None:
        raise HTTPException(404, "No ranking job has run yet")
    return job


@app.get("/rank/{job_id}", dependencies=[Depends(require_api_key)])
def get_ranking(job_id: str):
    job = ranker.get_job(job_id)
    if job is None:
        raise HTTPException(404, "Unknown job id")
    return job