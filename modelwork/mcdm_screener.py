import numpy as np
import pandas as pd
from typing import List, Dict, Any

class CandidateMCDMRanker:
    def __init__(self, criteria_weights: Dict[str, float], criteria_types: Dict[str, str]):
        """
        :param criteria_weights: Dictionary of feature name -> weight (sum should equal 1.0)
        :param criteria_types: Dictionary of feature name -> '+' (benefit) or '-' (cost)
        """
        self.criteria_keys = list(criteria_weights.keys())
        self.weights = np.array([criteria_weights[k] for k in self.criteria_keys])
        self.types = [criteria_types[k] for k in self.criteria_keys]
        
        # Normalize weights so they sum to 1
        self.weights = self.weights / np.sum(self.weights)

    def rank_candidates(self, candidates: List[Dict[str, Any]]) -> pd.DataFrame:
        """
        Processes candidate metrics using TOPSIS vector normalization.
        """
        if not candidates:
            return pd.DataFrame()

        df = pd.DataFrame(candidates)
        candidate_ids = df['id']
        candidate_names = df['name']
        
        # Extract matrix of evaluation criteria
        matrix = df[self.criteria_keys].values.astype(float)

        # 1. Vector Normalization
        # R_ij = x_ij / sqrt(sum(x_ij^2))
        norm_factors = np.sqrt(np.sum(matrix ** 2, axis=0))
        # Prevent division by zero
        norm_factors[norm_factors == 0] = 1e-9
        normalized_matrix = matrix / norm_factors

        # 2. Weighted Normalized Matrix
        weighted_matrix = normalized_matrix * self.weights

        # 3. Determine Ideal Best (A*) and Ideal Worst (A-) solutions
        ideal_best = np.zeros(len(self.criteria_keys))
        ideal_worst = np.zeros(len(self.criteria_keys))

        for j, c_type in enumerate(self.types):
            if c_type == '+':
                ideal_best[j] = np.max(weighted_matrix[:, j])
                ideal_worst[j] = np.min(weighted_matrix[:, j])
            else:
                ideal_best[j] = np.min(weighted_matrix[:, j])
                ideal_worst[j] = np.max(weighted_matrix[:, j])

        # 4. Calculate Euclidean Distances to Ideal Best (S*) and Ideal Worst (S-)
        distance_best = np.sqrt(np.sum((weighted_matrix - ideal_best) ** 2, axis=1))
        distance_worst = np.sqrt(np.sum((weighted_matrix - ideal_worst) ** 2, axis=1))

        # 5. Calculate TOPSIS Relative Closeness Score C_i = S- / (S* + S-)
        denom = distance_best + distance_worst
        denom[denom == 0] = 1e-9
        topsis_scores = distance_worst / denom

        # 6. Build Final Ranked Output
        df['mcdm_score'] = np.round(topsis_scores * 100, 2)
        df['rank'] = df['mcdm_score'].rank(ascending=False, method='min').astype(int)

        # Sort by rank
        result_df = df.sort_values(by='rank').reset_index(drop=True)
        return result_df


# ==========================================
# Example Usage in Your FastAPI / Backend Project
# ==========================================
if __name__ == "__main__":
    # 1. Define Criteria Weights (Adjust per job vacancy requirements)
    weights = {
        'keyword_match_pct': 0.35,  # Benefit (+): Required tech stack keywords (0 - 100)
        'years_experience': 0.30,   # Benefit (+): Total relevant years (e.g., 0 - 15)
        'education_level': 0.15,    # Benefit (+): 1 = High School, 2 = BS, 3 = MS, 4 = PhD
        'assessment_score': 0.20,   # Benefit (+): Coding/Screening test result (0 - 100)
    }

    criteria_types = {
        'keyword_match_pct': '+',
        'years_experience': '+',
        'education_level': '+',
        'assessment_score': '+',
    }

    # 2. Simulated parsed data from 5 candidates out of 100
    mock_candidates = [
        {
            "id": "CAND-001", "name": "Alice Smith",
            "keyword_match_pct": 85.0, "years_experience": 6, "education_level": 3, "assessment_score": 92.0
        },
        {
            "id": "CAND-002", "name": "Bob Jones",
            "keyword_match_pct": 95.0, "years_experience": 2, "education_level": 2, "assessment_score": 78.0
        },
        {
            "id": "CAND-003", "name": "Charlie Brown",
            "keyword_match_pct": 60.0, "years_experience": 10, "education_level": 2, "assessment_score": 88.0
        },
        {
            "id": "CAND-004", "name": "Diana Prince",
            "keyword_match_pct": 90.0, "years_experience": 8, "education_level": 4, "assessment_score": 95.0
        },
        {
            "id": "CAND-005", "name": "Evan Wright",
            "keyword_match_pct": 40.0, "years_experience": 1, "education_level": 1, "assessment_score": 50.0
        },
    ]

    # 3. Instantiate and Run Ranker
    ranker = CandidateMCDMRanker(criteria_weights=weights, criteria_types=criteria_types)
    ranked_results = ranker.rank_candidates(mock_candidates)

    # Display Top Candidates
    print(ranked_results[['rank', 'id', 'name', 'mcdm_score', 'years_experience', 'keyword_match_pct']])