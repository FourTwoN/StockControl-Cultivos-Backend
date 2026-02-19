package com.fortytwo.demeter.fotos.service;

import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.fotos.dto.EstimationDTO;
import com.fortytwo.demeter.fotos.model.Classification;
import com.fortytwo.demeter.fotos.model.Estimation;
import com.fortytwo.demeter.fotos.model.PhotoProcessingSession;
import com.fortytwo.demeter.fotos.repository.ClassificationRepository;
import com.fortytwo.demeter.fotos.repository.EstimationRepository;
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
 * Service for managing ML estimation results.
 */
@ApplicationScoped
public class EstimationService {

    private static final Logger log = Logger.getLogger(EstimationService.class);

    // Common estimation types
    public static final String TYPE_COUNT = "COUNT";
    public static final String TYPE_AREA = "AREA";
    public static final String TYPE_COVERAGE = "COVERAGE";
    public static final String TYPE_DENSITY = "DENSITY";

    @Inject
    EstimationRepository estimationRepository;

    @Inject
    PhotoProcessingSessionRepository sessionRepository;

    @Inject
    ClassificationRepository classificationRepository;

    public List<EstimationDTO> findBySessionId(UUID sessionId) {
        return estimationRepository.findBySessionId(sessionId)
                .stream().map(EstimationDTO::from).toList();
    }

    public List<EstimationDTO> findBySessionIdAndType(UUID sessionId, String estimationType) {
        return estimationRepository.findBySessionIdAndType(sessionId, estimationType)
                .stream().map(EstimationDTO::from).toList();
    }

    public EstimationDTO findById(UUID id) {
        Estimation estimation = estimationRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Estimation", id));
        return EstimationDTO.from(estimation);
    }

    /**
     * Create an estimation from ML results.
     *
     * @param sessionId Processing session ID
     * @param classificationId Classification this estimation applies to (nullable)
     * @param estimationType Type of estimation (COUNT, AREA, etc.)
     * @param value Estimated value
     * @param unit Unit of measurement
     * @param confidence Confidence score (0.0 - 1.0)
     * @param vegetationPolygon GeoJSON polygon for area estimation
     * @param detectedAreaCm2 Detected area in cm²
     * @param estimatedCount Estimated count of items
     * @param calculationMethod Method used for calculation
     * @return Created estimation
     */
    @Transactional
    public Estimation create(
            UUID sessionId,
            UUID classificationId,
            String estimationType,
            BigDecimal value,
            String unit,
            BigDecimal confidence,
            Map<String, Object> vegetationPolygon,
            BigDecimal detectedAreaCm2,
            Integer estimatedCount,
            String calculationMethod
    ) {
        PhotoProcessingSession session = sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("PhotoProcessingSession", sessionId));

        Estimation estimation = new Estimation();
        estimation.setSession(session);

        if (classificationId != null) {
            Classification classification = classificationRepository.findByIdOptional(classificationId)
                    .orElseThrow(() -> new EntityNotFoundException("Classification", classificationId));
            estimation.setClassification(classification);
        }

        estimation.setEstimationType(estimationType);
        estimation.setValue(value);
        estimation.setUnit(unit);
        estimation.setConfidence(confidence);
        estimation.setVegetationPolygon(vegetationPolygon);
        estimation.setDetectedAreaCm2(detectedAreaCm2);
        estimation.setEstimatedCount(estimatedCount);
        estimation.setCalculationMethod(calculationMethod);

        estimationRepository.persist(estimation);
        log.infof("Created estimation for session %s: type=%s, value=%s %s",
                sessionId, estimationType, value, unit);

        return estimation;
    }

    /**
     * Create a count estimation.
     */
    @Transactional
    public Estimation createCountEstimation(
            UUID sessionId,
            UUID classificationId,
            int count,
            BigDecimal confidence,
            String calculationMethod
    ) {
        return create(
                sessionId,
                classificationId,
                TYPE_COUNT,
                BigDecimal.valueOf(count),
                "units",
                confidence,
                null,
                null,
                count,
                calculationMethod
        );
    }

    /**
     * Link an estimation to a classification.
     */
    @Transactional
    public void linkToClassification(UUID estimationId, UUID classificationId) {
        Estimation estimation = estimationRepository.findByIdOptional(estimationId)
                .orElseThrow(() -> new EntityNotFoundException("Estimation", estimationId));
        Classification classification = classificationRepository.findByIdOptional(classificationId)
                .orElseThrow(() -> new EntityNotFoundException("Classification", classificationId));
        estimation.setClassification(classification);
    }

    @Transactional
    public void delete(UUID id) {
        Estimation estimation = estimationRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Estimation", id));
        estimationRepository.delete(estimation);
    }
}
