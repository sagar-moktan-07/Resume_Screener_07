import os
import json
import logging
import httpx
from typing import List, Optional
from pydantic import BaseModel, Field
from fastapi import FastAPI, BackgroundTasks, HTTPException, status
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy import Table, Column, Integer, String, Text, Float, MetaData, select, insert

# Setup Logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("FastAPIScreener")

# 1. Configuration & Async Database Setup
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite+aiosqlite:///./candidates.db")
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434/api/generate")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:7b")

engine = create_async_engine(DATABASE_URL, echo=False)
async_session = async_sessionmaker(engine, expire_on_commit=False, class_=AsyncSession)

# Reflect or define tables
metadata = MetaData()

candidates_info = Table(
    'candidatesinfo', metadata,
    Column('id', Integer, primary_key=True),
    Column('skills', Text),
    Column('qualifications', Text),
    Column('experience', Text),
    Column('projects', Text)
)

finalized_candidates = Table(
    'finalizedcandidates', metadata,
    Column('id', Integer, primary_key=True),
    Column('candidate_id', Integer),
    Column('batch_id', String),
    Column('skills', Text),
    Column('qualifications', Text),
    Column('experience', Text),
    Column('projects', Text),
    Column('match_score', Float),
    Column('selection_reason', Text)
)

# 2. Request & Response Schemas
class DataReadyPayload(BaseModel):
    event: str
    batch_id: str
    candidate_ids: List[int]
    count: int

class CandidateEvaluation(BaseModel):
    candidate_id: int
    score: float = Field(description="Match score between 0.0 and 100.0")
    reasoning: str = Field(description="Brief justification for ranking")

class TopSelectionOutput(BaseModel):
    evaluations: List[CandidateEvaluation]
    top_7_candidate_ids: List[int] = Field(description="Exactly top 7 candidate IDs ordered best to worst")

# 3. Core Screening Workflow
async def run_candidate_screening(batch_id: str, candidate_ids: List[int], target_job_profile: str):
    logger.info(f"Starting screening for Batch {batch_id} across {len(candidate_ids)} candidates.")

    async with async_session() as db:
        # A. Fetch Candidate Data from DB
        stmt = select(candidates_info).where(candidates_info.c.id.in_(candidate_ids))
        result = await db.execute(stmt)
        candidates_records = [dict(row._mapping) for row in result.fetchall()]

        if not candidates_records:
            logger.warning(f"No records found in DB for batch {batch_id}")
            return

        # B. Prompt Ollama for Structured Top 7 Ranking
        prompt = f"""
        You are an expert technical recruiter screening software candidate profiles.
        
        TARGET JOB PROFILE / CRITERIA:
        {target_job_profile}

        CANDIDATE DATASET:
        {json.dumps(candidates_records, default=str)}

        Evaluate all provided candidates strictly based on their skills, qualifications, experience, and projects.
        Select and rank the TOP 7 candidates best matching the job profile.

        Respond strictly in JSON matching this schema:
        {json.dumps(TopSelectionOutput.model_json_schema())}
        """

        try:
            async with httpx.AsyncClient(timeout=120.0) as client:
                res = await client.post(
                    OLLAMA_URL,
                    json={
                        "model": OLLAMA_MODEL,
                        "prompt": prompt,
                        "format": "json",
                        "stream": False
                    }
                )
                res.raise_for_status()
                ollama_resp = res.json().get("response", "{}")

            selection = TopSelectionOutput.model_validate_json(ollama_resp)
            top_ids = selection.top_7_candidate_ids[:7]
            logger.info(f"Top 7 Candidates Selected by Ollama: {top_ids}")

        except Exception as e:
            logger.error(f"Failed to screen candidates with local Ollama: {e}")
            return

        # C. Insert Top 7 into `finalizedcandidates` Table
        candidates_by_id = {c['id']: c for c in candidates_records}
        evaluations_by_id = {e.candidate_id: e for e in selection.evaluations}

        for cid in top_ids:
            if cid in candidates_by_id:
                cand = candidates_by_id[cid]
                eval_data = evaluations_by_id.get(cid)

                insert_stmt = insert(finalized_candidates).values(
                    candidate_id=cand['id'],
                    batch_id=batch_id,
                    skills=cand.get('skills'),
                    qualifications=cand.get('qualifications'),
                    experience=cand.get('experience'),
                    projects=cand.get('projects'),
                    match_score=eval_data.score if eval_data else 0.0,
                    selection_reason=eval_data.reasoning if eval_data else "Selected in top 7."
                )
                await db.execute(insert_stmt)

        await db.commit()
        logger.info(f"Batch {batch_id} screening complete. Top 7 inserted into finalizedcandidates.")

# 4. FastAPI Application Setup
app = FastAPI(title="Candidate Resume Screening Service")

@app.post("/data-ready", status_code=status.HTTP_202_ACCEPTED)
async def handle_data_ready(payload: DataReadyPayload, background_tasks: BackgroundTasks):
    if payload.event != "resumes_stored":
        raise HTTPException(status_code=400, detail="Invalid event type")

    # Default baseline hiring criteria (Can be pulled from DB or configuration)
    job_profile = "Senior Software Engineer skilled in Python, Java, SQL, Cloud Architecture, and Database Design."

    # Process heavy LLM screening asynchronously in background so Java client gets an immediate HTTP 202 acknowledgment
    background_tasks.add_task(
        run_candidate_screening,
        batch_id=payload.batch_id,
        candidate_ids=payload.candidate_ids,
        target_job_profile=job_profile
    )

    return {"status": "accepted", "message": "Screening process triggered in background.", "batch_id": payload.batch_id}