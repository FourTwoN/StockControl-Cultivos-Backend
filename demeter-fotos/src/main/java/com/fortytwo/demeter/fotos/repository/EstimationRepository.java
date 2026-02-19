package com.fortytwo.demeter.fotos.repository;

import com.fortytwo.demeter.fotos.model.Estimation;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EstimationRepository implements PanacheRepositoryBase<Estimation, UUID> {

    public List<Estimation> findBySessionId(UUID sessionId) {
        return find("session.id", sessionId).list();
    }

    public List<Estimation> findBySessionIdAndType(UUID sessionId, String estimationType) {
        return find("session.id = ?1 AND estimationType = ?2", sessionId, estimationType).list();
    }

    public List<Estimation> findByClassificationId(UUID classificationId) {
        return find("classification.id", classificationId).list();
    }
}
