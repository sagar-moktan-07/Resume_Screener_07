from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict, Any

app = FastAPI(title="Resume ML Service", version="1.0.0")


class HealthResponse(BaseModel):
    status: str
    service: str
    version: str


class DataReadyRequest(BaseModel):
    event: str
    batch_id: Optional[str] = None
    candidate_ids: Optional[List[int]] = None
    vacancy_id: Optional[int] = None
    count: Optional[int] = None


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok", service="resume-ml", version="1.0.0")


@app.get("/data-ready")
def data_ready_get() -> Dict[str, Any]:
    return {
        "status": "ok",
        "message": "Resume ML service is ready",
        "service": "resume-ml",
        "ready": True,
    }


@app.post("/data-ready")
def data_ready_post(payload: DataReadyRequest) -> Dict[str, Any]:
    if payload.event != "resume_uploaded":
        raise HTTPException(status_code=400, detail="Unsupported event")

    return {
        "status": "accepted",
        "event": payload.event,
        "batch_id": payload.batch_id,
        "candidate_ids": payload.candidate_ids or [],
        "vacancy_id": payload.vacancy_id,
        "count": payload.count or 0,
        "message": "Payload accepted by AI service",
    }
