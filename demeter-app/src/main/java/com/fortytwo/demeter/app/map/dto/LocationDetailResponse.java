package com.fortytwo.demeter.app.map.dto;

/**
 * Detailed response for a single storage location.
 * Includes the latest session summary and computed metrics.
 */
public record LocationDetailResponse(
        LocationInfo location,
        SessionSummary latestSession,
        Integer daysWithoutUpdate,
        String areaPosition,
        // Category breakdown
        Integer totalCactus,
        Integer totalSuculentas,
        Integer totalInjertos,
        Integer totalEmptyContainers
) {}
