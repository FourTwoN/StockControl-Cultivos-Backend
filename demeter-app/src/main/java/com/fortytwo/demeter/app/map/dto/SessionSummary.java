package com.fortytwo.demeter.app.map.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary of the latest photo processing session for a location.
 */
public record SessionSummary(
        UUID sessionId,
        String status,
        Integer totalDetected,
        Integer totalEstimated,
        Integer totalEmptyContainers,
        Float avgConfidence,
        // Category counts
        Integer totalCactus,
        Integer totalSuculentas,
        Integer totalInjertos,
        Instant createdAt
) {}
