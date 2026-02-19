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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class StockMovementFilterTest {

    private static final String TENANT = "tenant-smf";

    private static String productId;
    private static String warehouseId;
    private static String areaId;
    private static String locationId;
    private static String batchId;
    private static String userId;
    private static String manualInitMovementId;
    private static String ajusteMovementId;

    @Test
    @Order(1)
    void setup_createProduct() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "SMF-PROD", "name": "Movement Filter Product"}
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
                        {"name": "Movement Filter Test Warehouse"}
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
                        {"name": "Movement Filter Test Area"}
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
                        {"name": "Movement Filter Test Location"}
                        """)
                .when()
                .post("/api/v1/areas/" + areaId + "/locations")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(5)
    void setup_createBatch() {
        batchId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "productId": "%s",
                            "storageLocationId": "%s",
                            "productState": "ACTIVE",
                            "batchCode": "SMF-BATCH",
                            "quantity": 500
                        }
                        """.formatted(productId, locationId))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .body("quantityCurrent", equalTo(500))
                .extract().path("id");
    }

    @Test
    @Order(6)
    void setup_createUser() {
        userId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "smf-test@example.com", "name": "SMF Test User"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(7)
    void setup_createMovements() {
        manualInitMovementId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "movementType": "MANUAL_INIT",
                            "quantity": 100,
                            "isInbound": true,
                            "userId": "%s",
                            "sourceType": "MANUAL",
                            "reasonDescription": "Initial entry",
                            "batchQuantities": [{"batchId": "%s", "quantity": 100}]
                        }
                        """.formatted(userId, batchId))
                .when()
                .post("/api/v1/stock-movements")
                .then()
                .statusCode(201)
                .extract().path("id");

        ajusteMovementId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "movementType": "AJUSTE",
                            "quantity": 50,
                            "isInbound": false,
                            "userId": "%s",
                            "sourceType": "MANUAL",
                            "reasonDescription": "Stock adjustment",
                            "batchQuantities": [{"batchId": "%s", "quantity": 50}]
                        }
                        """.formatted(userId, batchId))
                .when()
                .post("/api/v1/stock-movements")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(8)
    void filterByType_shouldReturnMatchingOnly() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("type", "MANUAL_INIT")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].movementType", equalTo("MANUAL_INIT"));
    }

    @Test
    @Order(9)
    void filterByTypeAjuste_shouldReturnAjusteOnly() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("type", "AJUSTE")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].movementType", equalTo("AJUSTE"));
    }

    @Test
    @Order(10)
    void filterByDateRange_shouldReturnMovementsInRange() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("startDate", "2020-01-01T00:00:00Z")
                .queryParam("endDate", "2030-12-31T23:59:59Z")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    @Order(11)
    void filterByFutureDateRange_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("startDate", "2090-01-01T00:00:00Z")
                .queryParam("endDate", "2091-01-01T00:00:00Z")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }

    @Test
    @Order(12)
    void filterByTypeAndDate_shouldCombineFilters() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("type", "MANUAL_INIT")
                .queryParam("startDate", "2020-01-01T00:00:00Z")
                .queryParam("endDate", "2030-12-31T23:59:59Z")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].movementType", equalTo("MANUAL_INIT"));
    }

    @Test
    @Order(13)
    void noFilters_shouldReturnAll() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));
    }

    @Test
    @Order(14)
    void filterByTypeNoMatch_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("type", "VENTA")
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));
    }
}
