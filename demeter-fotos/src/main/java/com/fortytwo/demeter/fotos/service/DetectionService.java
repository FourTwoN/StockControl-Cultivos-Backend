package com.fortytwo.demeter.fotos.service;

import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.fotos.dto.DetectionDTO;
import com.fortytwo.demeter.fotos.model.Classification;
import com.fortytwo.demeter.fotos.model.Detection;
import com.fortytwo.demeter.fotos.model.PhotoProcessingSession;
import com.fortytwo.demeter.fotos.repository.ClassificationRepository;
import com.fortytwo.demeter.fotos.repository.DetectionRepository;
import com.fortytwo.demeter.fotos.repository.PhotoProcessingSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing ML detection results.
 */
@ApplicationScoped
public class DetectionService {

    private static final Logger log = Logger.getLogger(DetectionService.class);

    @Inject
    DetectionRepository detectionRepository;

    @Inject
    PhotoProcessingSessionRepository sessionRepository;

    @Inject
    ClassificationRepository classificationRepository;

    public List<DetectionDTO> findBySessionId(UUID sessionId) {
        return detectionRepository.findBySessionId(sessionId)
                .stream().map(DetectionDTO::from).toList();
    }

    public List<DetectionDTO> findAliveBySessionId(UUID sessionId) {
        return detectionRepository.findAliveBySessionId(sessionId)
                .stream().map(DetectionDTO::from).toList();
    }

    public DetectionDTO findById(UUID id) {
        Detection detection = detectionRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Detection", id));
        return DetectionDTO.from(detection);
    }

    public long countBySessionId(UUID sessionId) {
        return detectionRepository.countBySessionId(sessionId);
    }

    public long countAliveBySessionId(UUID sessionId) {
        return detectionRepository.countAliveBySessionId(sessionId);
    }

    /**
     * Create a detection from ML results.
     *
     * @param sessionId Processing session ID
     * @param classificationId Classification this detection belongs to (nullable)
     * @param label Detection label (e.g., "plant", "fruit")
     * @param confidence Confidence score (0.0 - 1.0)
     * @param boundingBox Bounding box coordinates {x1, y1, x2, y2}
     * @param centerXPx Center X coordinate in pixels
     * @param centerYPx Center Y coordinate in pixels
     * @param widthPx Detection width in pixels
     * @param heightPx Detection height in pixels
     * @param isAlive Whether the detected plant appears alive
     * @return Created detection
     */
    @Transactional
    public Detection create(
            UUID sessionId,
            UUID classificationId,
            String label,
            BigDecimal confidence,
            Map<String, Object> boundingBox,
            Integer centerXPx,
            Integer centerYPx,
            Integer widthPx,
            Integer heightPx,
            Boolean isAlive
    ) {
        PhotoProcessingSession session = sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("PhotoProcessingSession", sessionId));

        Detection detection = new Detection();
        detection.setSession(session);

        if (classificationId != null) {
            Classification classification = classificationRepository.findByIdOptional(classificationId)
                    .orElseThrow(() -> new EntityNotFoundException("Classification", classificationId));
            detection.setClassification(classification);
        }

        detection.setLabel(label);
        detection.setConfidence(confidence);
        detection.setBoundingBox(boundingBox);
        detection.setCenterXPx(centerXPx);
        detection.setCenterYPx(centerYPx);
        detection.setWidthPx(widthPx);
        detection.setHeightPx(heightPx);
        detection.setIsAlive(isAlive != null ? isAlive : true);

        detectionRepository.persist(detection);
        log.debugf("Created detection for session %s: label=%s, alive=%s", sessionId, label, isAlive);

        return detection;
    }

    /**
     * Link a detection to a classification.
     */
    @Transactional
    public void linkToClassification(UUID detectionId, UUID classificationId) {
        Detection detection = detectionRepository.findByIdOptional(detectionId)
                .orElseThrow(() -> new EntityNotFoundException("Detection", detectionId));
        Classification classification = classificationRepository.findByIdOptional(classificationId)
                .orElseThrow(() -> new EntityNotFoundException("Classification", classificationId));
        detection.setClassification(classification);
    }

    @Transactional
    public void delete(UUID id) {
        Detection detection = detectionRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Detection", id));
        detectionRepository.delete(detection);
    }
}
