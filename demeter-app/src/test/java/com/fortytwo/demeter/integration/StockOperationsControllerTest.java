package com.fortytwo.demeter.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for StockOperationsController.
 * Tests muerte, plantado, desplazamiento, and ajuste operations.
 *
 * Uses lazy initialization to ensure tests can run independently or in any order.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class StockOperationsControllerTest {

    private static final String TENANT = "tenant-stock-ops-v4";
    private static final String BASE_PATH = "/api/v1/stock/movements";

    // Test data IDs - static for sharing between tests
    private static String userId;
    private static String productId;
    private static String warehouseId;
    private static String areaId;
    private static String locationId;
    private static String locationId2;
    private static String batchId;
    private static String batchId2;
    private static boolean setupComplete = false;

    // ═══════════════════════════════════════════════════════════════
    // SETUP: Lazy initialization helper
    // ═══════════════════════════════════════════════════════════════

    /**
     * Ensures all test data exists. Called at start of each operation test.
     * Uses static flag to avoid recreating data on each test.
     */
    private void ensureTestDataSetup() {
        if (setupComplete) {
            return;
        }

        // Create user
        userId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "externalId": "test-user",
                            "email": "test-user-v4@test.local",
                            "name": "Test User V4",
                            "role": "ADMIN"
                        }
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create product
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "SO-PROD-V4", "name": "Stock Ops Product V4"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create warehouse
        warehouseId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Stock Ops Warehouse V4"}
                        """)
                .when()
                .post("/api/v1/warehouses")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create area
        areaId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Stock Ops Area V4"}
                        """)
                .when()
                .post("/api/v1/warehouses/" + warehouseId + "/areas")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create locations
        locationId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Location A V4"}
                        """)
                .when()
                .post("/api/v1/areas/" + areaId + "/locations")
                .then()
                .statusCode(201)
                .extract().path("id");

        locationId2 = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Location B V4"}
                        """)
                .when()
                .post("/api/v1/areas/" + areaId + "/locations")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Create batches
        batchId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "storageLocationId": "%s",
                            "productState": "ACTIVE",
                            "batchCode": "BATCH-V4-001",
                            "quantity": 100
                        }
                        """.formatted(productId, locationId))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .body("quantityCurrent", equalTo(100))
                .extract().path("id");

        batchId2 = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "storageLocationId": "%s",
                            "productState": "ACTIVE",
                            "batchCode": "BATCH-V4-002",
                            "quantity": 50
                        }
                        """.formatted(productId, locationId2))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .body("quantityCurrent", equalTo(50))
                .extract().path("id");

        setupComplete = true;
    }

    // ═══════════════════════════════════════════════════════════════
    // MUERTE TESTS
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    void muerte_shouldDecrementQuantity() {
        ensureTestDataSetup();

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "batchId": "%s",
                            "quantity": 10,
                            "reasonDescription": "Disease outbreak"
                        }
                        """.formatted(batchId))
                .when()
                .post(BASE_PATH + "/muerte")
                .then()
                .statusCode(201)
                .body("movement", notNullValue())
                .body("movement.movementType", equalTo("MUERTE"))
                .body("movement.quantity", equalTo(-10))
                .body("quantityRemoved", equalTo(10))
                .body("newQuantity", equalTo(90));
    }

    @Test
    @Order(11)
    void muerte_insufficientStock_shouldReturn400() {
        ensureTestDataSetup();

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "batchId": "%s",
                            "quantity": 1000
                        }
                        """.formatted(batchId))
                .when()
                .post(BASE_PATH + "/muerte")
                .then()
                .statusCode(400)
                .body("message", equalTo("INSUFFICIENT_STOCK"));
    }

    @Test
    @Order(12)
    void muerte_batchNotFound_shouldReturn404() {
        ensureTestDataSetup();

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "batchId": "00000000-0000-0000-0000-000000000000",
                            "quantity": 5
                        }
                        """)
                .when()
                .post(BASE_PATH + "/muerte")
                .then()
                .statusCode(404);
    }

    // ═══════════════════════════════════════════════════════════════
    // PLANTADO TESTS
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(20)
    void plantado_shouldIncrementQuantity() {
        ensureTestDataSetup();

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "batchId": "%s",
                            "quantity": 20,
                            "reasonDescription": "New seedlings"
                        }
                        """.formatted(batchId))
                .when()
                .post(BASE_PATH + "/plantado")
                .then()
                .statusCode(201)
                .body("movement.movementType", equalTo("PLANTADO"))
                .body("movement.quantity", equalTo(20))
                .body("newQuantity", equalTo(110));  // 90 + 20
    }

    // ═══════════════════════════════════════════════════════════════
    // AJUSTE TESTS
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(30)
    void ajuste_positive_shouldAddStock() {
        ensureTestDataSetup();

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "batchId": "%s",
                            "quantity": 5,
                            "reasonDescription": "Found extra plants"
                        }
                        """.formatted(batchId))
                .when()
                .post(BASE_PATH + "/ajuste")
                .then()
                .statusCode(201)
                .body("movement.movementType", equalTo("AJUSTE"))
                .body("quantityAdjusted", equalTo(5))
                .body("newQuantity", equalTo(115));  // 110 + 5
    }

    @Test
    @Order(31)
    void ajuste_negative_shouldSubtractStock() {
        ensureTestDataSetup();

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "batchId": "%s",
                            "quantity": -15
                        }
                        """.formatted(batchId))
                .when()
                .post(BASE_PATH + "/ajuste")
                .then()
                .statusCode(201)
                .body("quantityAdjusted", equalTo(-15))
                .body("newQuantity", equalTo(100));  // 115 - 15
    }

    @Test
    @Order(32)
    void ajuste_zeroQuantity_shouldReturn400() {
        ensureTestDataSetup();

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "batchId": "%s",
                            "quantity": 0
                        }
                        """.formatted(batchId))
                .when()
                .post(BASE_PATH + "/ajuste")
                .then()
                .statusCode(400);
    }

    // ═══════════════════════════════════════════════════════════════
    // DESPLAZAMIENTO TESTS
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(40)
    void desplazamiento_movimiento_shouldTransferStock() {
        ensureTestDataSetup();

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "sourceBatchId": "%s",
                            "destinationBatchId": "%s",
                            "quantity": 25
                        }
                        """.formatted(batchId, batchId2))
                .when()
                .post(BASE_PATH + "/desplazamiento")
                .then()
                .statusCode(201)
                .body("operationType", equalTo("movimiento"))
                .body("quantity", equalTo(25))
                .body("sourceBatch.newQuantity", equalTo(75))    // 100 - 25
                .body("destinationBatch.newQuantity", equalTo(75)); // 50 + 25
    }

    @Test
    @Order(41)
    void desplazamiento_insufficientStock_shouldReturn400() {
        ensureTestDataSetup();

        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "sourceBatchId": "%s",
                            "destinationBatchId": "%s",
                            "quantity": 1000
                        }
                        """.formatted(batchId, batchId2))
                .when()
                .post(BASE_PATH + "/desplazamiento")
                .then()
                .statusCode(400)
                .body("message", equalTo("INSUFFICIENT_STOCK"));
    }

    // ═══════════════════════════════════════════════════════════════
    // FINAL VERIFICATION
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(99)
    void verifyFinalBatchQuantities() {
        ensureTestDataSetup();

        // Batch 1: 100 - 10 (muerte) + 20 (plantado) + 5 (ajuste) - 15 (ajuste) - 25 (desp) = 75
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-batches/" + batchId)
                .then()
                .statusCode(200)
                .body("quantityCurrent", equalTo(75));

        // Batch 2: 50 + 25 (desplazamiento) = 75
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-batches/" + batchId2)
                .then()
                .statusCode(200)
                .body("quantityCurrent", equalTo(75));
    }
}
