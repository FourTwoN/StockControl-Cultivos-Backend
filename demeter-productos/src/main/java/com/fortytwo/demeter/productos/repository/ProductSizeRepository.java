package com.fortytwo.demeter.productos.repository;

import com.fortytwo.demeter.productos.model.ProductSize;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ProductSizeRepository implements PanacheRepositoryBase<ProductSize, UUID> {

    public List<ProductSize> findByProductId(UUID productId) {
        return find("product.id", productId).list();
    }
}
