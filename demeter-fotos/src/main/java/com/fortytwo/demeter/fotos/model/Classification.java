package com.fortytwo.demeter.fotos.model;

import com.fortytwo.demeter.common.model.BaseEntity;
import com.fortytwo.demeter.empaquetado.model.PackagingCatalog;
import com.fortytwo.demeter.productos.model.Product;
import com.fortytwo.demeter.productos.model.ProductSize;
import com.fortytwo.demeter.productos.model.ProductState;
import jakarta.persistence.*;

/**
 * ML classification result linking a session to identified product/size/state/packaging.
 * One session can have multiple classifications (one per detected product type).
 */
@Entity
@Table(name = "classifications")
public class Classification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PhotoProcessingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_size_id")
    private ProductSize productSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_state", length = 50)
    private ProductState productState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaging_catalog_id")
    private PackagingCatalog packagingCatalog;

    // Confidence scores (0-100 or 0-1000 depending on ML model)
    @Column(name = "product_conf")
    private Integer productConf;

    @Column(name = "product_size_conf")
    private Integer productSizeConf;

    @Column(name = "product_state_conf")
    private Integer productStateConf;

    @Column(name = "packaging_conf")
    private Integer packagingConf;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Legacy fields (deprecated)
    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    @Deprecated
    private String category;

    // Getters
    public PhotoProcessingSession getSession() { return session; }
    public Product getProduct() { return product; }
    public ProductSize getProductSize() { return productSize; }
    public ProductState getProductState() { return productState; }
    public PackagingCatalog getPackagingCatalog() { return packagingCatalog; }
    public Integer getProductConf() { return productConf; }
    public Integer getProductSizeConf() { return productSizeConf; }
    public Integer getProductStateConf() { return productStateConf; }
    public Integer getPackagingConf() { return packagingConf; }
    public String getModelVersion() { return modelVersion; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    @Deprecated public Image getImage() { return image; }
    @Deprecated public String getCategory() { return category; }

    // Setters
    public void setSession(PhotoProcessingSession session) { this.session = session; }
    public void setProduct(Product product) { this.product = product; }
    public void setProductSize(ProductSize productSize) { this.productSize = productSize; }
    public void setProductState(ProductState productState) { this.productState = productState; }
    public void setPackagingCatalog(PackagingCatalog packagingCatalog) { this.packagingCatalog = packagingCatalog; }
    public void setProductConf(Integer productConf) { this.productConf = productConf; }
    public void setProductSizeConf(Integer productSizeConf) { this.productSizeConf = productSizeConf; }
    public void setProductStateConf(Integer productStateConf) { this.productStateConf = productStateConf; }
    public void setPackagingConf(Integer packagingConf) { this.packagingConf = packagingConf; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    @Deprecated public void setImage(Image image) { this.image = image; }
    @Deprecated public void setCategory(String category) { this.category = category; }
}
