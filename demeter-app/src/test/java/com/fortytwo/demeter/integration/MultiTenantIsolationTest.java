package com.fortytwo.demeter.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Verifies multi-tenant data isolation at the API level.
 * This is the most critical test in the suite: it ensures that
 * tenants cannot see or access each other's data.
 *
 * Each test method uses unique tenant IDs to prevent cross-test contamination.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class MultiTenantIsolationTest {

    // Tenant IDs for product isolation test
    private static final String TENANT_PROD_A = "tenant-iso-prod-a";
    private static final String TENANT_PROD_B = "tenant-iso-prod-b";

    // Tenant IDs for stock batch isolation test
    private static final String TENANT_BATCH_A = "tenant-iso-batch-a";
    private static final String TENANT_BATCH_B = "tenant-iso-batch-b";

    // Tenant IDs for sale isolation test
    private static final String TENANT_SALE_A = "tenant-iso-sale-a";
    private static final String TENANT_SALE_B = "tenant-iso-sale-b";

    // Storage for product isolation test
    private static String productAId;
    private static String productBId;

    // Storage for stock batch isolation test
    private static String batchProductAId;
    private static String batchProductBId;
    private static String warehouseAId;
    private static String warehouseBId;
    private static String areaAId;
    private static String areaBId;
    private static String locationAId;
    private static String locationBId;
    private static String batchAId;

    // Storage for sale isolation test
    private static String saleProductAId;
    private static String saleProductBId;
    private static String saleAId;

    // ========================================
    // PRODUCT ISOLATION TESTS (Order 1-4)
    // ========================================

    @Test
    @Order(1)
    void productIsolation_createProductForTenantA() {
        productAId = given()
                .header("X-Tenant-ID", TENANT_PROD_A)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "ISO-001", "name": "Alpha Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Alpha Product"))
                .extract().path("id");
    }

    @Test
    @Order(2)
    void productIsolation_createProductForTenantB() {
        // Same SKU is allowed across tenants
        productBId = given()
                .header("X-Tenant-ID", TENANT_PROD_B)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "ISO-001", "name": "Beta Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Beta Product"))
                .extract().path("id");
    }

    @Test
    @Order(3)
    void productIsolation_tenantASeesOnlyTheirProduct() {
        given()
                .header("X-Tenant-ID", TENANT_PROD_A)
                .when()
                .get("/api/v1/products")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].name", equalTo("Alpha Product"));
    }

    @Test
    @Order(4)
    void productIsolation_tenantBSeesOnlyTheirProduct() {
        given()
                .header("X-Tenant-ID", TENANT_PROD_B)
                .when()
                .get("/api/v1/products")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].name", equalTo("Beta Product"));
    }

    @Test
    @Order(5)
    void productIsolation_crossTenantAccessBlocked_BtoA() {
        given()
                .header("X-Tenant-ID", TENANT_PROD_B)
                .when()
                .get("/api/v1/products/" + productAId)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    void productIsolation_crossTenantAccessBlocked_AtoB() {
        given()
                .header("X-Tenant-ID", TENANT_PROD_A)
                .when()
                .get("/api/v1/products/" + productBId)
                .then()
                .statusCode(404);
    }

    // ========================================
    // STOCK BATCH ISOLATION TESTS (Order 10-25)
    // ========================================

    @Test
    @Order(10)
    void batchIsolation_createProductForTenantA() {
        batchProductAId = given()
                .header("X-Tenant-ID", TENANT_BATCH_A)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "BATCH-ISO-001", "name": "Alpha Batch Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(11)
    void batchIsolation_createProductForTenantB() {
        batchProductBId = given()
                .header("X-Tenant-ID", TENANT_BATCH_B)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "BATCH-ISO-001", "name": "Beta Batch Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(12)
    void batchIsolation_createWarehouseForTenantA() {
        warehouseAId = given()
                .header("X-Tenant-ID", TENANT_BATCH_A)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Alpha Warehouse"}
                        """)
                .when()
                .post("/api/v1/warehouses")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(13)
    void batchIsolation_createWarehouseForTenantB() {
        warehouseBId = given()
                .header("X-Tenant-ID", TENANT_BATCH_B)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Beta Warehouse"}
                        """)
                .when()
                .post("/api/v1/warehouses")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(14)
    void batchIsolation_createAreaForTenantA() {
        areaAId = given()
                .header("X-Tenant-ID", TENANT_BATCH_A)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Alpha Area"}
                        """)
                .when()
                .post("/api/v1/warehouses/" + warehouseAId + "/areas")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(15)
    void batchIsolation_createAreaForTenantB() {
        areaBId = given()
                .header("X-Tenant-ID", TENANT_BATCH_B)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Beta Area"}
                        """)
                .when()
                .post("/api/v1/warehouses/" + warehouseBId + "/areas")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(16)
    void batchIsolation_createLocationForTenantA() {
        locationAId = given()
                .header("X-Tenant-ID", TENANT_BATCH_A)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Alpha Location"}
                        """)
                .when()
                .post("/api/v1/areas/" + areaAId + "/locations")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(17)
    void batchIsolation_createLocationForTenantB() {
        locationBId = given()
                .header("X-Tenant-ID", TENANT_BATCH_B)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Beta Location"}
                        """)
                .when()
                .post("/api/v1/areas/" + areaBId + "/locations")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(18)
    void batchIsolation_createBatchForTenantA() {
        batchAId = given()
                .header("X-Tenant-ID", TENANT_BATCH_A)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "storageLocationId": "%s",
                            "productState": "ACTIVE",
                            "batchCode": "BATCH-A-001",
                            "quantity": 50
                        }
                        """.formatted(batchProductAId, locationAId))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .body("quantityCurrent", equalTo(50))
                .extract().path("id");
    }

    @Test
    @Order(19)
    void batchIsolation_createBatchForTenantB() {
        given()
                .header("X-Tenant-ID", TENANT_BATCH_B)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "storageLocationId": "%s",
                            "productState": "ACTIVE",
                            "batchCode": "BATCH-B-001",
                            "quantity": 75
                        }
                        """.formatted(batchProductBId, locationBId))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .body("quantityCurrent", equalTo(75));
    }

    @Test
    @Order(20)
    void batchIsolation_tenantASeesOnlyTheirBatch() {
        given()
                .header("X-Tenant-ID", TENANT_BATCH_A)
                .when()
                .get("/api/v1/stock-batches")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].batchCode", equalTo("BATCH-A-001"));
    }

    @Test
    @Order(21)
    void batchIsolation_tenantBSeesOnlyTheirBatch() {
        given()
                .header("X-Tenant-ID", TENANT_BATCH_B)
                .when()
                .get("/api/v1/stock-batches")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].batchCode", equalTo("BATCH-B-001"));
    }

    @Test
    @Order(22)
    void batchIsolation_crossTenantAccessBlocked() {
        given()
                .header("X-Tenant-ID", TENANT_BATCH_B)
                .when()
                .get("/api/v1/stock-batches/" + batchAId)
                .then()
                .statusCode(404);
    }

    // ========================================
    // SALE ISOLATION TESTS (Order 30-37)
    // ========================================

    @Test
    @Order(30)
    void saleIsolation_createProductForTenantA() {
        saleProductAId = given()
                .header("X-Tenant-ID", TENANT_SALE_A)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "SALE-ISO-001", "name": "Alpha Sale Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(31)
    void saleIsolation_createProductForTenantB() {
        saleProductBId = given()
                .header("X-Tenant-ID", TENANT_SALE_B)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "SALE-ISO-001", "name": "Beta Sale Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(32)
    void saleIsolation_createSaleForTenantA() {
        saleAId = given()
                .header("X-Tenant-ID", TENANT_SALE_A)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "customerName": "Alpha Customer",
                            "items": [
                                {
                                    "productId": "%s",
                                    "quantity": 5,
                                    "unitPrice": 10.00
                                }
                            ]
                        }
                        """.formatted(saleProductAId))
                .when()
                .post("/api/v1/sales")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(33)
    void saleIsolation_createSaleForTenantB() {
        given()
                .header("X-Tenant-ID", TENANT_SALE_B)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "customerName": "Beta Customer",
                            "items": [
                                {
                                    "productId": "%s",
                                    "quantity": 3,
                                    "unitPrice": 20.00
                                }
                            ]
                        }
                        """.formatted(saleProductBId))
                .when()
                .post("/api/v1/sales")
                .then()
                .statusCode(201);
    }

    @Test
    @Order(34)
    void saleIsolation_tenantACanAccessTheirSales() {
        given()
                .header("X-Tenant-ID", TENANT_SALE_A)
                .when()
                .get("/api/v1/sales")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(35)
    void saleIsolation_crossTenantAccessBlocked() {
        given()
                .header("X-Tenant-ID", TENANT_SALE_B)
                .when()
                .get("/api/v1/sales/" + saleAId)
                .then()
                .statusCode(404);
    }
}
