import os
from typing import Any, Dict, List, Optional

from dotenv import load_dotenv
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine

load_dotenv(os.path.join(os.path.dirname(__file__), ".env"), override=False)

DATABASE_URL = os.getenv("DATABASE_URL") or (
    f"postgresql+psycopg://{os.getenv('DB_USER','resume_app')}:{os.getenv('DB_PASSWORD','')}@"
    f"{os.getenv('DB_HOST','127.0.0.1')}:{os.getenv('DB_PORT','5432')}/{os.getenv('DB_NAME','resumedb')}"
)

engine: Engine = create_engine(DATABASE_URL, pool_pre_ping=True)


def fetch_candidates_for_screening(limit: int = 100) -> List[Dict[str, Any]]:
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                """
                SELECT id, file_name, full_name, email, phone, raw_text, skills, qualifications, experience
                FROM candidate_resume
                ORDER BY id DESC
                LIMIT :limit
                """
            ),
            {"limit": limit},
        )
        return [dict(r._mapping) for r in rows]


def fetch_vacancy_by_id(vacancy_id: int) -> Optional[Dict[str, Any]]:
    with engine.begin() as conn:
        row = conn.execute(
            text(
                """
                SELECT id, job_title, department, job_description, required_skills, preferred_skills,
                       qualifications, education_requirements, experience_requirements,
                       responsibilities, other_requirements, status
                FROM vacancy_post
                WHERE id = :vacancy_id
                """
            ),
            {"vacancy_id": vacancy_id},
        ).first()
    if row is None:
        return None
    return dict(row._mapping)
