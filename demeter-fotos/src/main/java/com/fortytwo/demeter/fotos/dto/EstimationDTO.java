package com.fortytwo.demeter.fotos.dto;

import com.fortytwo.demeter.fotos.model.Estimation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EstimationDTO(
    UUID id,
    UUID sessionId,
    UUID classificationId,
    String estimationType,
    BigDecimal value,
    String unit,
    BigDecimal confidence,
    Map<String, Object> vegetationPolygon,
    BigDecimal detectedAreaCm2,
    Integer estimatedCount,
    String calculationMethod,
    Instant createdAt
) {
    public static EstimationDTO from(Estimation e) {
        return new EstimationDTO(
            e.getId(),
            e.getSession() != null ? e.getSession().getId() : null,
            e.getClassification() != null ? e.getClassification().getId() : null,
            e.getEstimationType(),
            e.getValue(),
            e.getUnit(),
            e.getConfidence(),
            e.getVegetationPolygon(),
            e.getDetectedAreaCm2(),
            e.getEstimatedCount(),
            e.getCalculationMethod(),
            e.getCreatedAt()
        );
    }
}
