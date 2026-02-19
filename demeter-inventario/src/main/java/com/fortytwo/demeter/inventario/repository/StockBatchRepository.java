package com.fortytwo.demeter.inventario.repository;

import com.fortytwo.demeter.inventario.model.BatchStatus;
import com.fortytwo.demeter.inventario.model.StockBatch;
import com.fortytwo.demeter.productos.model.ProductState;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class StockBatchRepository implements PanacheRepositoryBase<StockBatch, UUID> {

    public Optional<StockBatch> findByBatchCode(String code) {
        return find("batchCode", code).firstResultOptional();
    }

    public List<StockBatch> findByProductId(UUID productId) {
        return find("product.id", productId).list();
    }

    public List<StockBatch> findByStorageLocationId(UUID storageLocationId) {
        return find("currentStorageLocation.id", storageLocationId).list();
    }

    public List<StockBatch> findByStatus(BatchStatus status) {
        return find("status", status).list();
    }

    /**
     * Find the active batch for a specific location + product + state + size + packaging combination.
     * Active batches have cycleEndDate = null.
     */
    public Optional<StockBatch> findActiveBatch(
            UUID storageLocationId, UUID productId, ProductState productState,
            UUID productSizeId, UUID packagingCatalogId) {
        StringBuilder query = new StringBuilder(
            "currentStorageLocation.id = :locId AND product.id = :prodId " +
            "AND productState = :state AND cycleEndDate IS NULL"
        );
        Parameters params = Parameters
            .with("locId", storageLocationId)
            .and("prodId", productId)
            .and("state", productState);

        if (productSizeId != null) {
            query.append(" AND productSize.id = :sizeId");
            params.and("sizeId", productSizeId);
        } else {
            query.append(" AND productSize IS NULL");
        }

        if (packagingCatalogId != null) {
            query.append(" AND packagingCatalog.id = :pkgId");
            params.and("pkgId", packagingCatalogId);
        } else {
            query.append(" AND packagingCatalog IS NULL");
        }

        return find(query.toString(), params).firstResultOptional();
    }

    /**
     * Find all active batches for a storage location.
     */
    public List<StockBatch> findActiveByStorageLocation(UUID storageLocationId) {
        return find("currentStorageLocation.id = ?1 AND cycleEndDate IS NULL", storageLocationId).list();
    }

    /**
     * Get the latest cycle number for a given combination.
     */
    public Integer getLatestCycleNumber(
            UUID storageLocationId, UUID productId, ProductState productState,
            UUID productSizeId, UUID packagingCatalogId) {
        StringBuilder query = new StringBuilder(
            "SELECT MAX(b.cycleNumber) FROM StockBatch b WHERE " +
            "b.currentStorageLocation.id = :locId AND b.product.id = :prodId AND b.productState = :state"
        );

        if (productSizeId != null) {
            query.append(" AND b.productSize.id = :sizeId");
        } else {
            query.append(" AND b.productSize IS NULL");
        }

        if (packagingCatalogId != null) {
            query.append(" AND b.packagingCatalog.id = :pkgId");
        } else {
            query.append(" AND b.packagingCatalog IS NULL");
        }

        var typedQuery = getEntityManager()
            .createQuery(query.toString(), Integer.class)
            .setParameter("locId", storageLocationId)
            .setParameter("prodId", productId)
            .setParameter("state", productState);

        if (productSizeId != null) {
            typedQuery.setParameter("sizeId", productSizeId);
        }
        if (packagingCatalogId != null) {
            typedQuery.setParameter("pkgId", packagingCatalogId);
        }

        Integer result = typedQuery.getSingleResult();
        return result != null ? result : 0;
    }
}
