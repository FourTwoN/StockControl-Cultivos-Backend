package com.fortytwo.demeter.app.map.dto;

import java.time.Instant;

/**
 * Preview metrics for a storage location shown in the map view.
 * Contains aggregated data from the latest photo processing sessions.
 */
public record LocationPreview(
        Integer currentQuantity,
        Integer previousQuantity,
        Instant lastPhotoDate,
        Integer daysWithoutUpdate,
        String status,
        Float qualityScore,
        // Category counts (plant-specific)
        Integer totalCactus,
        Integer totalSuculentas,
        Integer totalInjertos,
        Integer totalEmptyContainers
) {
    /**
     * Calculate the change in quantity between current and previous photo sessions.
     *
     * @return Quantity change (positive = increase, negative = decrease), or null if not calculable
     */
    public Integer quantityChange() {
        if (currentQuantity == null || previousQuantity == null) {
            return null;
        }
        return currentQuantity - previousQuantity;
    }

    /**
     * Create a preview for a location with no photo sessions.
     */
    public static LocationPreview pending() {
        return new LocationPreview(
                null, null, null, null,
                "PENDING", null,
                null, null, null, null
        );
    }
}
