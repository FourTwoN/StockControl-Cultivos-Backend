package com.fortytwo.demeter.fotos.repository;

import com.fortytwo.demeter.fotos.model.Detection;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DetectionRepository implements PanacheRepositoryBase<Detection, UUID> {

    public List<Detection> findBySessionId(UUID sessionId) {
        return find("session.id", sessionId).list();
    }

    public List<Detection> findByClassificationId(UUID classificationId) {
        return find("classification.id", classificationId).list();
    }

    public List<Detection> findAliveBySessionId(UUID sessionId) {
        return find("session.id = ?1 AND isAlive = true", sessionId).list();
    }

    public long countBySessionId(UUID sessionId) {
        return count("session.id", sessionId);
    }

    public long countAliveBySessionId(UUID sessionId) {
        return count("session.id = ?1 AND isAlive = true", sessionId);
    }

    @Deprecated
    public List<Detection> findByImageId(UUID imageId) {
        return find("image.id", imageId).list();
    }
}
