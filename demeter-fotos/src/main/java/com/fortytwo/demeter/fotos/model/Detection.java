package com.fortytwo.demeter.fotos.model;

import com.fortytwo.demeter.common.model.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.Map;

/**
 * ML detection result representing a single detected object/plant in an image.
 * Links to session for grouping and optionally to classification for type identification.
 */
@Entity
@Table(name = "detections")
public class Detection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PhotoProcessingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classification_id")
    private Classification classification;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bounding_box", columnDefinition = "jsonb")
    private Map<String, Object> boundingBox;

    // Geometry fields for precise positioning
    @Column(name = "center_x_px")
    private Integer centerXPx;

    @Column(name = "center_y_px")
    private Integer centerYPx;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    @Column(name = "is_alive")
    private Boolean isAlive = true;

    // Legacy field (deprecated - use session instead)
    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    // Getters
    public PhotoProcessingSession getSession() { return session; }
    public Classification getClassification() { return classification; }
    public String getLabel() { return label; }
    public BigDecimal getConfidence() { return confidence; }
    public Map<String, Object> getBoundingBox() { return boundingBox; }
    public Integer getCenterXPx() { return centerXPx; }
    public Integer getCenterYPx() { return centerYPx; }
    public Integer getWidthPx() { return widthPx; }
    public Integer getHeightPx() { return heightPx; }
    public Boolean getIsAlive() { return isAlive; }
    @Deprecated public Image getImage() { return image; }

    // Setters
    public void setSession(PhotoProcessingSession session) { this.session = session; }
    public void setClassification(Classification classification) { this.classification = classification; }
    public void setLabel(String label) { this.label = label; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public void setBoundingBox(Map<String, Object> boundingBox) { this.boundingBox = boundingBox; }
    public void setCenterXPx(Integer centerXPx) { this.centerXPx = centerXPx; }
    public void setCenterYPx(Integer centerYPx) { this.centerYPx = centerYPx; }
    public void setWidthPx(Integer widthPx) { this.widthPx = widthPx; }
    public void setHeightPx(Integer heightPx) { this.heightPx = heightPx; }
    public void setIsAlive(Boolean isAlive) { this.isAlive = isAlive; }
    @Deprecated public void setImage(Image image) { this.image = image; }
}
