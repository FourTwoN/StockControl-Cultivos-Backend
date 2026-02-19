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
 * Tests for storage location hierarchy and configuration.
 *
 * Note: StorageLocationConfig service is internal and doesn't have REST endpoints.
 * These tests verify the storage hierarchy through available REST endpoints.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class StorageLocationConfigTest {

    private static final String TENANT = "tenant-slc-test";

    private static String warehouseId;
    private static String areaId;
    private static String locationId;
    private static String productId;

    @Test
    @Order(1)
    void setup_createWarehouse() {
        warehouseId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "SLC Test Warehouse", "description": "Test warehouse for config"}
                        """)
                .when()
                .post("/api/v1/warehouses")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("SLC Test Warehouse"))
                .extract().path("id");
    }

    @Test
    @Order(2)
    void setup_createArea() {
        areaId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "SLC Test Area", "description": "Test area for config"}
                        """)
                .when()
                .post("/api/v1/warehouses/" + warehouseId + "/areas")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("SLC Test Area"))
                .extract().path("id");
    }

    @Test
    @Order(3)
    void setup_createLocation() {
        locationId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "SLC Test Location", "capacity": 100}
                        """)
                .when()
                .post("/api/v1/areas/" + areaId + "/locations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("SLC Test Location"))
                .extract().path("id");
    }

    @Test
    @Order(4)
    void setup_createProduct() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "SLC-PROD-001", "name": "SLC Test Product"}
                        """)
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(5)
    void getWarehouse_shouldReturnWithAreas() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/warehouses/" + warehouseId)
                .then()
                .statusCode(200)
                .body("id", equalTo(warehouseId))
                .body("name", equalTo("SLC Test Warehouse"));
    }

    @Test
    @Order(6)
    void listAreas_shouldReturnAreaForWarehouse() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/warehouses/" + warehouseId + "/areas")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].name", equalTo("SLC Test Area"));
    }

    @Test
    @Order(7)
    void listLocations_shouldReturnLocationForArea() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/areas/" + areaId + "/locations")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].name", equalTo("SLC Test Location"));
    }

    @Test
    @Order(8)
    void getLocation_shouldReturnDetails() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/areas/" + areaId + "/locations/" + locationId)
                .then()
                .statusCode(200)
                .body("id", equalTo(locationId))
                .body("name", equalTo("SLC Test Location"));
    }

    @Test
    @Order(9)
    void createBatchAtLocation_shouldLinkToLocation() {
        String batchId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "storageLocationId": "%s",
                            "productState": "ACTIVE",
                            "batchCode": "SLC-BATCH-001",
                            "quantity": 50
                        }
                        """.formatted(productId, locationId))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");

        // Verify batch is linked to location
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-batches/by-location/" + locationId)
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].id", equalTo(batchId));
    }

    @Test
    @Order(10)
    void updateLocation_shouldModifyDetails() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "SLC Test Location Updated", "capacity": 200}
                        """)
                .when()
                .put("/api/v1/areas/" + areaId + "/locations/" + locationId)
                .then()
                .statusCode(200)
                .body("name", equalTo("SLC Test Location Updated"));
    }
}
