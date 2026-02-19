package com.fortytwo.demeter.fotos.dto;

import com.fortytwo.demeter.fotos.model.Classification;
import java.time.Instant;
import java.util.UUID;

public record ClassificationDTO(
    UUID id,
    UUID sessionId,
    UUID productId,
    String productName,
    UUID productSizeId,
    String productSizeLabel,
    String productState,
    UUID packagingCatalogId,
    String packagingCatalogName,
    Integer productConf,
    Integer productSizeConf,
    Integer productStateConf,
    Integer packagingConf,
    String modelVersion,
    String name,
    String description,
    Instant createdAt
) {
    public static ClassificationDTO from(Classification c) {
        return new ClassificationDTO(
            c.getId(),
            c.getSession() != null ? c.getSession().getId() : null,
            c.getProduct() != null ? c.getProduct().getId() : null,
            c.getProduct() != null ? c.getProduct().getName() : null,
            c.getProductSize() != null ? c.getProductSize().getId() : null,
            c.getProductSize() != null ? c.getProductSize().getLabel() : null,
            c.getProductState() != null ? c.getProductState().name() : null,
            c.getPackagingCatalog() != null ? c.getPackagingCatalog().getId() : null,
            c.getPackagingCatalog() != null ? c.getPackagingCatalog().getName() : null,
            c.getProductConf(),
            c.getProductSizeConf(),
            c.getProductStateConf(),
            c.getPackagingConf(),
            c.getModelVersion(),
            c.getName(),
            c.getDescription(),
            c.getCreatedAt()
        );
    }
}
