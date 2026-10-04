package com.example.resumescreener.dto;

import java.util.List;

public record UploadResult(
        String batchId,
        int stored,
        int failed,
        boolean fastApiNotified,
        List<FileResult> files) {

    public record FileResult(String fileName, String status, String message, Long candidateId) {}
}
