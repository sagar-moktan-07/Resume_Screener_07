"""Read-only access to the candidates table that Spring Boot fills."""
from sqlalchemy import create_engine, text
from sqlalchemy.engine import URL

import config

_engine = None


def get_engine():
    global _engine
    if _engine is None:
        url = URL.create(
            "postgresql+psycopg2",
            username=config.DB_USER,
            password=config.DB_PASSWORD,
            host=config.DB_HOST,
            port=config.DB_PORT,
            database=config.DB_NAME,
        )
        _engine = create_engine(
            url,
            pool_pre_ping=True,
            # Second safety net on top of the read-only database account
            connect_args={"options": "-c default_transaction_read_only=on"},
        )
    return _engine


def fetch_candidates() -> list[dict]:
    query = text("""
        SELECT id, file_name, full_name, email, phone,
               qualifications, skills, experience, raw_text
        FROM candidates
        ORDER BY id
    """)
    with get_engine().connect() as conn:
        return [dict(row._mapping) for row in conn.execute(query)]


def count_candidates() -> int:
    with get_engine().connect() as conn:
        return conn.execute(text("SELECT COUNT(*) FROM candidates")).scalar_one()