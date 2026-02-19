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
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class StockMovementControllerTest {

    private static final String TENANT = "tenant-stock-mvmt";

    private static String productId;
    private static String warehouseId;
    private static String areaId;
    private static String locationId;
    private static String batchId;
    private static String userId;
    private static String movementId;

    @Test
    @Order(1)
    void setup_createProduct() {
        productId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"sku": "MVMT-PROD-001", "name": "Movement Test Product"}
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
                        {"name": "Movement Test Warehouse"}
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
                        {"name": "Movement Test Area"}
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
                        {"name": "Movement Test Location"}
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
                            "batchCode": "MVMT-BATCH-001",
                            "quantity": 200
                        }
                        """.formatted(productId, locationId))
                .when()
                .post("/api/v1/stock-batches")
                .then()
                .statusCode(201)
                .body("quantityCurrent", equalTo(200))
                .extract().path("id");
    }

    @Test
    @Order(6)
    void setup_createUser() {
        userId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {"email": "mvmt-test@example.com", "name": "Movement Test User"}
                        """)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Test
    @Order(7)
    void createEntradaMovement_shouldReturn201() {
        movementId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "movementType": "MANUAL_INIT",
                            "quantity": 50,
                            "isInbound": true,
                            "userId": "%s",
                            "sourceType": "MANUAL",
                            "reasonDescription": "Initial stock entry",
                            "batchQuantities": [
                                {
                                    "batchId": "%s",
                                    "quantity": 50
                                }
                            ]
                        }
                        """.formatted(userId, batchId))
                .when()
                .post("/api/v1/stock-movements")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("movementType", equalTo("MANUAL_INIT"))
                .body("quantity", equalTo(50))
                .extract().path("id");
    }

    @Test
    @Order(8)
    void getMovement_shouldReturn200() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-movements/" + movementId)
                .then()
                .statusCode(200)
                .body("id", equalTo(movementId))
                .body("movementType", equalTo("MANUAL_INIT"));
    }

    @Test
    @Order(9)
    void listMovements_shouldReturnPaged() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/v1/stock-movements")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("page", equalTo(0))
                .body("size", equalTo(20));
    }

    @Test
    @Order(10)
    void getByType_shouldReturn() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-movements/by-type/MANUAL_INIT")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(11)
    void getByDateRange_shouldReturn() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("from", "2020-01-01T00:00:00Z")
                .queryParam("to", "2030-01-01T00:00:00Z")
                .when()
                .get("/api/v1/stock-movements/by-date-range")
                .then()
                .statusCode(200);
    }

    @Test
    @Order(12)
    void getByReference_shouldReturnEmpty() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/stock-movements/by-reference/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @Order(13)
    void createMovement_missingRequired_shouldReturn400() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "reasonDescription": "Missing required fields"
                        }
                        """)
                .when()
                .post("/api/v1/stock-movements")
                .then()
                .statusCode(400);
    }
}
