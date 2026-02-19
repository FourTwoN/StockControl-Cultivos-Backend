package com.fortytwo.demeter.app.map.dto;

import java.time.Instant;

/**
 * Summary statistics for location history.
 */
public record HistorySummary(
        int totalPeriods,
        Instant earliestDate,
        Instant latestDate
) {}
