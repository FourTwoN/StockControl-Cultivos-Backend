package com.fortytwo.demeter.fotos.service;

import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.empaquetado.repository.PackagingCatalogRepository;
import com.fortytwo.demeter.fotos.dto.ClassificationDTO;
import com.fortytwo.demeter.fotos.model.Classification;
import com.fortytwo.demeter.fotos.model.PhotoProcessingSession;
import com.fortytwo.demeter.fotos.repository.ClassificationRepository;
import com.fortytwo.demeter.fotos.repository.PhotoProcessingSessionRepository;
import com.fortytwo.demeter.productos.model.ProductState;
import com.fortytwo.demeter.productos.repository.ProductRepository;
import com.fortytwo.demeter.productos.repository.ProductSizeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing ML classification results.
 */
@ApplicationScoped
public class ClassificationService {

    private static final Logger log = Logger.getLogger(ClassificationService.class);

    @Inject
    ClassificationRepository classificationRepository;

    @Inject
    PhotoProcessingSessionRepository sessionRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    ProductSizeRepository productSizeRepository;

    @Inject
    PackagingCatalogRepository packagingCatalogRepository;

    public List<ClassificationDTO> findBySessionId(UUID sessionId) {
        return classificationRepository.findBySessionId(sessionId)
                .stream().map(ClassificationDTO::from).toList();
    }

    public ClassificationDTO findById(UUID id) {
        Classification classification = classificationRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Classification", id));
        return ClassificationDTO.from(classification);
    }

    /**
     * Create a classification from ML results.
     *
     * @param sessionId Processing session ID
     * @param productId Detected product ID (nullable)
     * @param productSizeId Detected product size ID (nullable)
     * @param productState Detected product state (nullable)
     * @param packagingCatalogId Detected packaging ID (nullable)
     * @param productConf Confidence for product (0-100 or 0-1000)
     * @param productSizeConf Confidence for size
     * @param productStateConf Confidence for state
     * @param packagingConf Confidence for packaging
     * @param modelVersion ML model version
     * @param name Optional name/label
     * @param description Optional description
     * @return Created classification
     */
    @Transactional
    public Classification create(
            UUID sessionId,
            UUID productId,
            UUID productSizeId,
            ProductState productState,
            UUID packagingCatalogId,
            Integer productConf,
            Integer productSizeConf,
            Integer productStateConf,
            Integer packagingConf,
            String modelVersion,
            String name,
            String description
    ) {
        PhotoProcessingSession session = sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("PhotoProcessingSession", sessionId));

        Classification classification = new Classification();
        classification.setSession(session);

        if (productId != null) {
            classification.setProduct(productRepository.findByIdOptional(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Product", productId)));
        }

        if (productSizeId != null) {
            classification.setProductSize(productSizeRepository.findByIdOptional(productSizeId)
                    .orElseThrow(() -> new EntityNotFoundException("ProductSize", productSizeId)));
        }

        classification.setProductState(productState);

        if (packagingCatalogId != null) {
            classification.setPackagingCatalog(packagingCatalogRepository.findByIdOptional(packagingCatalogId)
                    .orElseThrow(() -> new EntityNotFoundException("PackagingCatalog", packagingCatalogId)));
        }

        classification.setProductConf(productConf);
        classification.setProductSizeConf(productSizeConf);
        classification.setProductStateConf(productStateConf);
        classification.setPackagingConf(packagingConf);
        classification.setModelVersion(modelVersion);
        classification.setName(name);
        classification.setDescription(description);

        classificationRepository.persist(classification);
        log.infof("Created classification for session %s: product=%s, state=%s",
                sessionId, productId, productState);

        return classification;
    }

    @Transactional
    public void delete(UUID id) {
        Classification classification = classificationRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Classification", id));
        classificationRepository.delete(classification);
    }
}
