import os
import psycopg2


def get_resumes(limit: int = 100):

    connection = psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=os.getenv("DB_PORT", "5432"),
        database=os.getenv("DB_NAME", "resume_screener"),
        user=os.getenv("DB_USER", "postgres"),
        password=os.getenv("DB_PASSWORD")
    )

    try:
        with connection.cursor() as cursor:

            cursor.execute(
                """
                SELECT id, extracted_text
                FROM resumes
                WHERE extracted_text IS NOT NULL
                ORDER BY id
                LIMIT %s
                """,
                (limit,)
            )

            rows = cursor.fetchall()

            return [
                {
                    "id": row[0],
                    "text": row[1]
                }
                for row in rows
            ]

    finally:
        connection.close()
