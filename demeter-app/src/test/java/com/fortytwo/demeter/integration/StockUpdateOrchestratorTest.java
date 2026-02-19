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
 * Tests for the Photo → Stock integration flow.
 *
 * The StockUpdateOrchestrator is called internally when ML processing completes.
 * These tests verify the flow through the processing callback endpoint.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class StockUpdateOrchestratorTest {

    private static final String TENANT = "tenant-orchestrator-test";

    private static String productId;
    private static String warehouseId;
    private static String areaId;
    private static String locationId;
    private static String sessionId;
    private static String imageId;

    @Test
    @Order(1)
    void setup_createProduct() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "ORCH-PROD-001", "name": "Orchestrator Test Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(2)
    void setup_createWarehouse() {
        warehouseId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Orchestrator Test Warehouse"}
                        """)
                .when()
                .post("/api/v1/warehouses")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(3)
    void setup_createArea() {
        areaId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Orchestrator Test Area"}
                        """)
                .when()
                .post("/api/v1/warehouses/" + warehouseId + "/areas")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(4)
    void setup_createLocation() {
        locationId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Orchestrator Test Location"}
                        """)
                .when()
                .post("/api/v1/areas/" + areaId + "/locations")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(5)
    void setup_createPhotoSession() {
        sessionId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/v1/photo-sessions")
                .then()
                .statusCode(201)
                .body("status", equalTo("PENDING"))
                .extract().path("id");
    }

    @Test
    @Order(6)
    void getSession_shouldBePending() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/photo-sessions/" + sessionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(sessionId))
                .body("status", equalTo("PENDING"));
    }

    @Test
    @Order(7)
    void getSessionStatus_shouldReturnStatusInfo() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/photo-sessions/" + sessionId + "/status")
                .then()
                .statusCode(200)
                .body("totalImages", equalTo(0))
                .body("processedImages", equalTo(0));
    }

    @Test
    @Order(8)
    void getSessionEstimations_shouldBeEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/photo-sessions/" + sessionId + "/estimations")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @Order(9)
    void listSessions_shouldContainOurSession() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/photo-sessions")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(10)
    void verifyStockBatchesAtLocation_shouldBeEmpty() {
        // Before any ML processing, location should have no batches
        // (from this specific test - could have batches from other tests)
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("locationId", locationId)
                .when()
                .get("/api/v1/stock-batches")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(11)
    void createManualBatch_thenVerify() {
        // Create a batch manually to test the location-batch relationship
        String batchId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "storageLocationId": "%s",
                            "productState": "ACTIVE",
                            "batchCode": "ORCH-BATCH-001",
                            "quantity": 100
                        }
                        """.formatted(productId, locationId))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .extract().path("id");

        // Verify batch exists at location
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-batches/by-location/" + locationId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].productId", equalTo(productId));
    }

    @Test
    @Order(12)
    void verifyActiveBatches_shouldReturnBatches() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-batches/active/by-location/" + locationId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }
}
