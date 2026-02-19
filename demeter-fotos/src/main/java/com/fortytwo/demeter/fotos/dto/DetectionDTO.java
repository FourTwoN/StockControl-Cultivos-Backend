package com.fortytwo.demeter.fotos.dto;

import com.fortytwo.demeter.fotos.model.Detection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DetectionDTO(
    UUID id,
    UUID sessionId,
    UUID classificationId,
    String label,
    BigDecimal confidence,
    Map<String, Object> boundingBox,
    Integer centerXPx,
    Integer centerYPx,
    Integer widthPx,
    Integer heightPx,
    Boolean isAlive,
    Instant createdAt
) {
    public static DetectionDTO from(Detection d) {
        return new DetectionDTO(
            d.getId(),
            d.getSession() != null ? d.getSession().getId() : null,
            d.getClassification() != null ? d.getClassification().getId() : null,
            d.getLabel(),
            d.getConfidence(),
            d.getBoundingBox(),
            d.getCenterXPx(),
            d.getCenterYPx(),
            d.getWidthPx(),
            d.getHeightPx(),
            d.getIsAlive(),
            d.getCreatedAt()
        );
    }
}
