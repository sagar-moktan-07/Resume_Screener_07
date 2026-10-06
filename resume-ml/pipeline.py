from database import get_resumes
from parser import parse_resume
from mcdm import calculate_mcdm_score
from reranker import rerank_resumes
from qwen import evaluate_with_qwen


def run_screening(job_description: str, limit: int = 100):

    # ---------------------------------------------------------
    # 1. GET RESUMES FROM DATABASE
    # ---------------------------------------------------------

    resumes = get_resumes(limit=limit)

    print(f"Loaded {len(resumes)} resumes from database.")

    if not resumes:
        return []

    # ---------------------------------------------------------
    # 2. FAST PARSER
    # ---------------------------------------------------------

    parsed_resumes = []

    for resume in resumes:

        parsed = parse_resume(resume)

        parsed_resumes.append({
            "id": resume["id"],
            "raw_text": resume["text"],
            "parsed": parsed
        })

    print(f"Parsed {len(parsed_resumes)} resumes.")

    # ---------------------------------------------------------
    # 3. MCDM / FAST FILTER
    # ---------------------------------------------------------

    scored_resumes = []

    for resume in parsed_resumes:

        score = calculate_mcdm_score(
            resume["parsed"],
            job_description
        )

        scored_resumes.append({
            **resume,
            "mcdm_score": score
        })

    # Highest MCDM score first
    scored_resumes.sort(
        key=lambda x: x["mcdm_score"],
        reverse=True
    )

    print("MCDM ranking completed.")

    # We don't need to send all 100 resumes
    # to the expensive reranker.
    candidates = scored_resumes[:20]

    print(f"Sending top {len(candidates)} candidates to reranker.")

    # ---------------------------------------------------------
    # 4. BGE RERANKER
    # ---------------------------------------------------------

    reranked = rerank_resumes(
        candidates,
        job_description
    )

    # ---------------------------------------------------------
    # 5. TOP 5
    # ---------------------------------------------------------

    top_5 = reranked[:5]

    print("\nTop 5 candidates:")

    for rank, resume in enumerate(top_5, start=1):
        print(
            f"{rank}. Resume ID: {resume['id']} "
            f"| Score: {resume['reranker_score']:.4f}"
        )

    # ---------------------------------------------------------
    # 6. QWEN FINAL EVALUATION
    # ---------------------------------------------------------

    final_results = []

    for resume in top_5:

        evaluation = evaluate_with_qwen(
            resume=resume,
            job_description=job_description
        )

        final_results.append({
            "resume_id": resume["id"],
            "mcdm_score": resume["mcdm_score"],
            "reranker_score": resume["reranker_score"],
            "evaluation": evaluation
        })

    return final_results
