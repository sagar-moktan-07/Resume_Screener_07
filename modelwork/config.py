"""All settings come from environment variables (loaded from .env)."""
import os

from dotenv import load_dotenv

load_dotenv()


def _bool(name: str, default: str) -> bool:
    return os.getenv(name, default).strip().lower() == "true"


def _optional_bool(name: str):
    value = os.getenv(name, "").strip().lower()
    return None if value == "" else value == "true"


# --- Security: shared secret between Spring Boot and this server
SERVICE_API_KEY = os.getenv("SERVICE_API_KEY", "").strip()

# --- Database (read-only account)
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "5432"))
DB_NAME = os.getenv("DB_NAME", "resumedb")
DB_USER = os.getenv("DB_USER", "fastapi_reader")
DB_PASSWORD = os.getenv("DB_PASSWORD", "")

# --- Ollama
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434").rstrip("/")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:3b")
OLLAMA_TIMEOUT = int(os.getenv("OLLAMA_TIMEOUT", "300"))
OLLAMA_NUM_CTX = int(os.getenv("OLLAMA_NUM_CTX", "4096"))
OLLAMA_THINK = _optional_bool("OLLAMA_THINK")

# --- Ranking
TOP_N = int(os.getenv("TOP_N", "7"))
WEIGHT_KEYWORD = float(os.getenv("WEIGHT_KEYWORD", "0.4"))
WEIGHT_LLM = float(os.getenv("WEIGHT_LLM", "0.6"))
MAX_RESUME_CHARS = int(os.getenv("MAX_RESUME_CHARS", "3500"))
AUTO_RANK = _bool("AUTO_RANK_ON_DATA_READY", "true")


def validate() -> None:
    """Refuse to start with a missing or weak configuration."""
    problems = []
    if len(SERVICE_API_KEY) < 16 or SERVICE_API_KEY.lower().startswith("change"):
        problems.append(
            "SERVICE_API_KEY is missing or too weak (16+ characters). Generate one with:\n"
            '     python -c "import secrets; print(secrets.token_urlsafe(32))"')
    if not DB_PASSWORD:
        problems.append("DB_PASSWORD is empty")
    if problems:
        raise RuntimeError("Configuration problems:\n - " + "\n - ".join(problems))