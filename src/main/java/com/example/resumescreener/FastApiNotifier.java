package com.example.resumescreener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class FastApiNotifier {

    private static final Logger log = LoggerFactory.getLogger(FastApiNotifier.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public FastApiNotifier(RestTemplateBuilder builder,
                           @Value("${fastapi.base-url}") String baseUrl) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.baseUrl = baseUrl;
    }

    /** Returns true if FastAPI acknowledged, false if it was unreachable or errored. */
    public boolean notifyDataReady(String batchId, List<Long> candidateIds) {
        Map<String, Object> payload = Map.of(
                "event", "resumes_stored",
                "batch_id", batchId,
                "candidate_ids", candidateIds,
                "count", candidateIds.size());

        try {
            restTemplate.postForEntity(baseUrl + "/data-ready", payload, String.class);
            log.info("FastAPI notified for batch {}", batchId);
            return true;
        } catch (RestClientException e) {
            // Data is already saved, so a down FastAPI must not break the upload
            log.warn("Could not notify FastAPI: {}", e.getMessage());
            return false;
        }
    }
}