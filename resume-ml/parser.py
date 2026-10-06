import re


def parse_resume(resume):

    text = resume["text"]
    text_lower = text.lower()
    skills = extract_skills(text_lower)
    experience = extract_experience(text_lower)
    education = extract_education(text_lower)
    return {
        "skills": skills,
        "experience": experience,
        "education": education,
        "text": text
    }

def extract_skills(text):

    known_skills = [
        "python",
        "java",
        "javascript",
        "typescript",
        "c++",
        "sql",
        "postgresql",
        "mysql",
        "fastapi",
        "django",
        "flask",
        "spring boot",
        "docker",
        "aws",
        "machine learning",
        "deep learning",
        "tensorflow",
        "pytorch",
        "react",
        "node.js"
    ]
    return [
        skill
        for skill in known_skills
        if skill in text
    ]


def extract_experience(text):
    match = re.search(
        r"(\d+(?:\.\d+)?)\+?\s*(?:years|yrs)",
        text
    )
    if match:
        return float(match.group(1))

    return 0.0


def extract_education(text):

    education_keywords = [
        "bachelor",
        "master",
        "bsc",
        "bca",
        "bit",
        "btech",
        "mca",
        "mtech",
        "computer science",
        "information technology"
    ]

    return [
        keyword
        for keyword in education_keywords
        if keyword in text
    ]
