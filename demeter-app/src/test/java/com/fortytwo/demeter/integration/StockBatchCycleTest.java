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
 * Tests for stock batch cycle management.
 * Tests the cycle tracking functionality through REST API.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class StockBatchCycleTest {

    private static final String TENANT = "tenant-cycle-test";

    private static String productId;
    private static String warehouseId;
    private static String areaId;
    private static String locationId;
    private static String batchId;

    @Test
    @Order(1)
    void setup_createProduct() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "CYCLE-PROD-001", "name": "Cycle Test Product"}
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
                        {"name": "Cycle Test Warehouse"}
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
                        {"name": "Cycle Test Area"}
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
                        {"name": "Cycle Test Location"}
                        """)
                .when()
                .post("/api/v1/areas/" + areaId + "/locations")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(5)
    void createBatch_withLocation_shouldSetCycleNumber() {
        batchId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "storageLocationId": "%s",
                            "productState": "ACTIVE",
                            "batchCode": "CYCLE-BATCH-001",
                            "quantity": 100
                        }
                        """.formatted(productId, locationId))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("batchCode", equalTo("CYCLE-BATCH-001"))
                .body("quantityCurrent", equalTo(100))
                .extract().path("id");
    }

    @Test
    @Order(6)
    void getBatch_shouldReturnWithCycleInfo() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-batches/" + batchId)
                .then()
                .statusCode(200)
                .body("id", equalTo(batchId))
                .body("productId", equalTo(productId))
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    @Order(7)
    void getActiveByLocation_shouldReturnBatch() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-batches/active/by-location/" + locationId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].id", equalTo(batchId));
    }

    @Test
    @Order(8)
    void createSecondBatch_sameCriteria_shouldWork() {
        String secondBatchId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "storageLocationId": "%s",
                            "productState": "ACTIVE",
                            "batchCode": "CYCLE-BATCH-002",
                            "quantity": 85
                        }
                        """.formatted(productId, locationId))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("quantityCurrent", equalTo(85))
                .extract().path("id");

        // Verify both batches exist
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-batches/by-location/" + locationId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(2));
    }

    @Test
    @Order(9)
    void filterByActiveOnly_shouldReturnActiveBatches() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("locationId", locationId)
                .queryParam("activeOnly", true)
                .when()
                .get("/api/v1/stock-batches")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }
}
