package com.example.resumescreener.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FastApiClient {
    private static final Logger log = LoggerFactory.getLogger(FastApiClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public FastApiClient(RestTemplateBuilder builder,
                        @Value("${fastapi.base-url}") String baseUrl) {
        this.restTemplate = builder.build();
        this.baseUrl = baseUrl;
    }

    public boolean notifyBatch(List<Long> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return false;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "resumes_stored");
        payload.put("batch_id", "batch-" + System.currentTimeMillis());
        payload.put("candidate_ids", candidateIds);
        payload.put("count", candidateIds.size());

        try {
            restTemplate.postForEntity(baseUrl + "/data-ready", payload, String.class);
            log.info("Notified FastAPI for {} candidates.", candidateIds.size());
            return true;
        } catch (Exception e) {
            log.warn("FastAPI notification failed: {}", e.getMessage());
            return false;
        }
    }
}
