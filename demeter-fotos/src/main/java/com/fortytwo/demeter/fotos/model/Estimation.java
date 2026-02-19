package com.fortytwo.demeter.fotos.model;

import com.fortytwo.demeter.common.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.Map;

/**
 * ML estimation result for aggregate measurements like count, area, or vegetation coverage.
 * Links to session and optionally to a specific classification.
 */
@Entity
@Table(name = "estimations")
public class Estimation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PhotoProcessingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id")
    private Classification classification;

    @Column(name = "estimation_type", nullable = false, length = 100)
    private String estimationType;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal value;

    @Column(length = 50)
    private String unit;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    // Area estimation fields
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vegetation_polygon", columnDefinition = "jsonb")
    private Map<String, Object> vegetationPolygon;

    @Column(name = "detected_area_cm2", precision = 10, scale = 2)
    private BigDecimal detectedAreaCm2;

    // Count estimation fields
    @Column(name = "estimated_count")
    private Integer estimatedCount;

    @Column(name = "calculation_method", length = 100)
    private String calculationMethod;

    // Legacy field (deprecated - use specific fields above)
    @Deprecated
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    // Getters
    public PhotoProcessingSession getSession() { return session; }
    public Classification getClassification() { return classification; }
    public String getEstimationType() { return estimationType; }
    public BigDecimal getValue() { return value; }
    public String getUnit() { return unit; }
    public BigDecimal getConfidence() { return confidence; }
    public Map<String, Object> getVegetationPolygon() { return vegetationPolygon; }
    public BigDecimal getDetectedAreaCm2() { return detectedAreaCm2; }
    public Integer getEstimatedCount() { return estimatedCount; }
    public String getCalculationMethod() { return calculationMethod; }
    @Deprecated public Map<String, Object> getMetadata() { return metadata; }

    // Setters
    public void setSession(PhotoProcessingSession session) { this.session = session; }
    public void setClassification(Classification classification) { this.classification = classification; }
    public void setEstimationType(String estimationType) { this.estimationType = estimationType; }
    public void setValue(BigDecimal value) { this.value = value; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public void setVegetationPolygon(Map<String, Object> vegetationPolygon) { this.vegetationPolygon = vegetationPolygon; }
    public void setDetectedAreaCm2(BigDecimal detectedAreaCm2) { this.detectedAreaCm2 = detectedAreaCm2; }
    public void setEstimatedCount(Integer estimatedCount) { this.estimatedCount = estimatedCount; }
    public void setCalculationMethod(String calculationMethod) { this.calculationMethod = calculationMethod; }
    @Deprecated public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
