package com.fortytwo.demeter.fotos.repository;

import com.fortytwo.demeter.fotos.model.Classification;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ClassificationRepository implements PanacheRepositoryBase<Classification, UUID> {

    public List<Classification> findBySessionId(UUID sessionId) {
        return find("session.id", sessionId).list();
    }

    public List<Classification> findByProductId(UUID productId) {
        return find("product.id", productId).list();
    }

    @Deprecated
    public List<Classification> findByImageId(UUID imageId) {
        return find("image.id", imageId).list();
    }
}
