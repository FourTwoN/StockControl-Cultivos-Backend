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
 * Multi-tenant isolation tests for Map API.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>Tenant A cannot see Tenant B's warehouse hierarchy in bulk load</li>
 *   <li>Location detail returns 404 for cross-tenant access</li>
 *   <li>Location history returns 404 for cross-tenant access</li>
 *   <li>Cache is isolated per tenant</li>
 * </ul>
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class MapMultiTenantTest {

    // Unique tenant IDs for isolation testing
    private static final String TENANT_A = "tenant-map-iso-a";
    private static final String TENANT_B = "tenant-map-iso-b";

    // Tenant A resources
    private static String warehouseAId;
    private static String areaAId;
    private static String locationAId;

    // Tenant B resources
    private static String warehouseBId;
    private static String areaBId;
    private static String locationBId;

    // ========================================
    // SETUP: Create warehouse for Tenant A (Order 1-3)
    // ========================================

    @Test
    @Order(1)
    void setupA_createWarehouse() {
        warehouseAId = given()
                .header("X-Tenant-ID", TENANT_A)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Alpha Warehouse",
                            "address": "Alpha Street 1"
                        }
                        """)
                .when()
                .post("/api/v1/warehouses")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(2)
    void setupA_createArea() {
        areaAId = given()
                .header("X-Tenant-ID", TENANT_A)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Alpha Zone",
                            "position": "N"
                        }
                        """)
                .when()
                .post("/api/v1/warehouses/" + warehouseAId + "/areas")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(3)
    void setupA_createLocation() {
        locationAId = given()
                .header("X-Tenant-ID", TENANT_A)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Alpha Rack 1"
                        }
                        """)
                .when()
                .post("/api/v1/areas/" + areaAId + "/locations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    // ========================================
    // SETUP: Create warehouse for Tenant B (Order 4-6)
    // ========================================

    @Test
    @Order(4)
    void setupB_createWarehouse() {
        warehouseBId = given()
                .header("X-Tenant-ID", TENANT_B)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Beta Warehouse",
                            "address": "Beta Street 1"
                        }
                        """)
                .when()
                .post("/api/v1/warehouses")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(5)
    void setupB_createArea() {
        areaBId = given()
                .header("X-Tenant-ID", TENANT_B)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Beta Zone",
                            "position": "S"
                        }
                        """)
                .when()
                .post("/api/v1/warehouses/" + warehouseBId + "/areas")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(6)
    void setupB_createLocation() {
        locationBId = given()
                .header("X-Tenant-ID", TENANT_B)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Beta Rack 1"
                        }
                        """)
                .when()
                .post("/api/v1/areas/" + areaBId + "/locations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    // ========================================
    // BULK LOAD ISOLATION TESTS (Order 10-12)
    // ========================================

    @Test
    @Order(10)
    void bulkLoad_tenantASeesOnlyAlphaWarehouse() {
        given()
                .header("X-Tenant-ID", TENANT_A)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200)
                .body("warehouses.size()", equalTo(1))
                .body("warehouses[0].name", equalTo("Alpha Warehouse"))
                .body("warehouses.findAll { it.name == 'Beta Warehouse' }.size()", equalTo(0));
    }

    @Test
    @Order(11)
    void bulkLoad_tenantBSeesOnlyBetaWarehouse() {
        given()
                .header("X-Tenant-ID", TENANT_B)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200)
                .body("warehouses.size()", equalTo(1))
                .body("warehouses[0].name", equalTo("Beta Warehouse"))
                .body("warehouses.findAll { it.name == 'Alpha Warehouse' }.size()", equalTo(0));
    }

    @Test
    @Order(12)
    void bulkLoad_noCrossContamination() {
        // Verify counts are exactly 1 for each tenant
        int countA = given()
                .header("X-Tenant-ID", TENANT_A)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200)
                .extract().path("warehouses.size()");

        int countB = given()
                .header("X-Tenant-ID", TENANT_B)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200)
                .extract().path("warehouses.size()");

        // Each tenant should see exactly 1 warehouse
        org.junit.jupiter.api.Assertions.assertEquals(1, countA, "Tenant A should see exactly 1 warehouse");
        org.junit.jupiter.api.Assertions.assertEquals(1, countB, "Tenant B should see exactly 1 warehouse");
    }

    // ========================================
    // LOCATION DETAIL ISOLATION TESTS (Order 20-22)
    // ========================================

    @Test
    @Order(20)
    void locationDetail_tenantACanAccessOwnLocation() {
        given()
                .header("X-Tenant-ID", TENANT_A)
                .when()
                .get("/api/v1/map/locations/" + locationAId + "/detail")
                .then()
                .statusCode(200)
                .body("location.name", equalTo("Alpha Rack 1"));
    }

    @Test
    @Order(21)
    void locationDetail_tenantBCannotAccessTenantALocation() {
        // Cross-tenant access should return 404
        given()
                .header("X-Tenant-ID", TENANT_B)
                .when()
                .get("/api/v1/map/locations/" + locationAId + "/detail")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(22)
    void locationDetail_tenantACannotAccessTenantBLocation() {
        // Cross-tenant access should return 404
        given()
                .header("X-Tenant-ID", TENANT_A)
                .when()
                .get("/api/v1/map/locations/" + locationBId + "/detail")
                .then()
                .statusCode(404);
    }

    // ========================================
    // LOCATION HISTORY ISOLATION TESTS (Order 30-32)
    // ========================================

    @Test
    @Order(30)
    void locationHistory_tenantACanAccessOwnLocation() {
        given()
                .header("X-Tenant-ID", TENANT_A)
                .when()
                .get("/api/v1/map/locations/" + locationAId + "/history")
                .then()
                .statusCode(200)
                .body("location.name", equalTo("Alpha Rack 1"));
    }

    @Test
    @Order(31)
    void locationHistory_tenantBCannotAccessTenantALocation() {
        // Cross-tenant access should return 404
        given()
                .header("X-Tenant-ID", TENANT_B)
                .when()
                .get("/api/v1/map/locations/" + locationAId + "/history")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(32)
    void locationHistory_tenantACannotAccessTenantBLocation() {
        // Cross-tenant access should return 404
        given()
                .header("X-Tenant-ID", TENANT_A)
                .when()
                .get("/api/v1/map/locations/" + locationBId + "/history")
                .then()
                .statusCode(404);
    }

    // ========================================
    // CACHE ISOLATION TESTS (Order 40)
    // ========================================

    @Test
    @Order(40)
    void cacheIsolation_invalidateDoesNotAffectOtherTenant() {
        // Load data for both tenants
        given()
                .header("X-Tenant-ID", TENANT_A)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200);

        given()
                .header("X-Tenant-ID", TENANT_B)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200);

        // Invalidate cache for Tenant A
        given()
                .header("X-Tenant-ID", TENANT_A)
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v1/map/cache/invalidate")
                .then()
                .statusCode(204);

        // Tenant B should still see their data
        given()
                .header("X-Tenant-ID", TENANT_B)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200)
                .body("warehouses.size()", equalTo(1))
                .body("warehouses[0].name", equalTo("Beta Warehouse"));
    }

    // ========================================
    // CLEANUP (Order 99)
    // ========================================

    @Test
    @Order(99)
    void cleanup_deleteWarehouses() {
        // Delete Tenant A warehouse
        given()
                .header("X-Tenant-ID", TENANT_A)
                .when()
                .delete("/api/v1/warehouses/" + warehouseAId)
                .then()
                .statusCode(anyOf(equalTo(204), equalTo(200)));

        // Delete Tenant B warehouse
        given()
                .header("X-Tenant-ID", TENANT_B)
                .when()
                .delete("/api/v1/warehouses/" + warehouseBId)
                .then()
                .statusCode(anyOf(equalTo(204), equalTo(200)));
    }
}
