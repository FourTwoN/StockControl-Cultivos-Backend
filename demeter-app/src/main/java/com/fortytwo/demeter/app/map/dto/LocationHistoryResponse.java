package com.fortytwo.demeter.app.map.dto;

import java.util.List;

/**
 * Paginated history response for a storage location.
 */
public record LocationHistoryResponse(
        LocationInfo location,
        List<LocationHistoryItem> periods,
        HistorySummary summary,
        Pagination pagination
) {}
