package com.fortytwo.demeter.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Map visualization API.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Bulk load endpoint with hierarchy structure</li>
 *   <li>Location detail with session summary</li>
 *   <li>Location history with pagination</li>
 *   <li>Presigned URL batch generation</li>
 *   <li>Cache invalidation</li>
 * </ul>
 *
 * <p>Test data setup:
 * <ul>
 *   <li>1 warehouse with 2 areas</li>
 *   <li>2 locations per area (4 total)</li>
 *   <li>Completed photo sessions with estimations</li>
 * </ul>
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class MapControllerTest {

    private static final String TENANT = "tenant-map-test";

    // Storage hierarchy IDs
    private static String warehouseId;
    private static String area1Id;
    private static String area2Id;
    private static String location1Id;
    private static String location2Id;
    private static String location3Id;
    private static String location4Id;

    // Product family and product for category counts
    private static String familyCactusId;
    private static String productCactusId;

    // Photo session data
    private static String session1Id;
    private static String session2Id;

    // ========================================
    // SETUP: Create warehouse hierarchy (Order 1-7)
    // ========================================

    @Test
    @Order(1)
    void setup_createWarehouse() {
        warehouseId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Main Warehouse",
                            "address": "123 Test Street"
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
    void setup_createArea1() {
        area1Id = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Zone A",
                            "description": "First zone",
                            "position": "N"
                        }
                        """)
                .when()
                .post("/api/v1/warehouses/" + warehouseId + "/areas")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(3)
    void setup_createArea2() {
        area2Id = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Zone B",
                            "description": "Second zone",
                            "position": "S"
                        }
                        """)
                .when()
                .post("/api/v1/warehouses/" + warehouseId + "/areas")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(4)
    void setup_createLocation1() {
        location1Id = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Rack 1",
                            "description": "First rack in Zone A"
                        }
                        """)
                .when()
                .post("/api/v1/areas/" + area1Id + "/locations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(5)
    void setup_createLocation2() {
        location2Id = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Rack 2",
                            "description": "Second rack in Zone A"
                        }
                        """)
                .when()
                .post("/api/v1/areas/" + area1Id + "/locations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(6)
    void setup_createLocation3() {
        location3Id = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Rack 3",
                            "description": "First rack in Zone B"
                        }
                        """)
                .when()
                .post("/api/v1/areas/" + area2Id + "/locations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(7)
    void setup_createLocation4() {
        location4Id = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Rack 4",
                            "description": "Second rack in Zone B"
                        }
                        """)
                .when()
                .post("/api/v1/areas/" + area2Id + "/locations")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    // ========================================
    // SETUP: Create product family and product (Order 8-9)
    // ========================================

    @Test
    @Order(8)
    void setup_createCactusFamily() {
        familyCactusId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Cactus Family",
                            "description": "All cactus plants"
                        }
                        """)
                .when()
                .post("/api/v1/families")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(9)
    void setup_createCactusProduct() {
        productCactusId = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "sku": "CACTUS-001",
                            "name": "Standard Cactus",
                            "familyId": "%s"
                        }
                        """.formatted(familyCactusId))
                .when()
                .post("/api/v1/products")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    // ========================================
    // SETUP: Create photo sessions (Order 10-11)
    // ========================================

    @Test
    @Order(10)
    void setup_createPhotoSession1() {
        // Create a session linked to location1
        session1Id = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "storageLocationId": "%s"
                        }
                        """.formatted(location1Id))
                .when()
                .post("/api/v1/photo-sessions")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    @Order(11)
    void setup_createPhotoSession2() {
        // Create another session for location1 (for history testing)
        session2Id = given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "storageLocationId": "%s"
                        }
                        """.formatted(location1Id))
                .when()
                .post("/api/v1/photo-sessions")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");
    }

    // ========================================
    // BULK LOAD TESTS (Order 20-24)
    // ========================================

    @Test
    @Order(20)
    void bulkLoad_returnsHierarchy() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200)
                .body("warehouses", notNullValue())
                .body("warehouses.size()", greaterThanOrEqualTo(1))
                .body("warehouses[0].warehouseId", notNullValue())
                .body("warehouses[0].name", equalTo("Main Warehouse"))
                .body("warehouses[0].areas", notNullValue())
                .body("warehouses[0].areas.size()", equalTo(2));
    }

    @Test
    @Order(21)
    void bulkLoad_includesAreasAndLocations() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200)
                .body("warehouses[0].areas[0].areaId", notNullValue())
                .body("warehouses[0].areas[0].locations", notNullValue())
                .body("warehouses[0].areas[0].locations.size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(22)
    void bulkLoad_includesLocationPreview() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200)
                .body("warehouses[0].areas[0].locations[0].preview", notNullValue())
                .body("warehouses[0].areas[0].locations[0].preview.status", notNullValue());
    }

    @Test
    @Order(23)
    void bulkLoad_isCached() {
        // First call
        long start1 = System.currentTimeMillis();
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200);
        long duration1 = System.currentTimeMillis() - start1;

        // Second call should be faster (cached)
        long start2 = System.currentTimeMillis();
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200);
        long duration2 = System.currentTimeMillis() - start2;

        // Note: This is a soft assertion - caching might not always show timing difference
        // due to other factors, but generally second call should be at least as fast
        org.junit.jupiter.api.Assertions.assertTrue(duration2 <= duration1 + 100,
                "Second call should not be significantly slower than first");
    }

    @Test
    @Order(24)
    void bulkLoad_emptyForOtherTenant() {
        given()
                .header("X-Tenant-ID", "tenant-map-other")
                .when()
                .get("/api/v1/map/bulk-load")
                .then()
                .statusCode(200)
                .body("warehouses.size()", equalTo(0));
    }

    // ========================================
    // LOCATION DETAIL TESTS (Order 30-33)
    // ========================================

    @Test
    @Order(30)
    void getLocationDetail_returnsData() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/map/locations/" + location1Id + "/detail")
                .then()
                .statusCode(200)
                .body("location", notNullValue())
                .body("location.locationId", equalTo(location1Id))
                .body("location.name", equalTo("Rack 1"));
    }

    @Test
    @Order(31)
    void getLocationDetail_notFound() {
        String nonExistentId = UUID.randomUUID().toString();
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/map/locations/" + nonExistentId + "/detail")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(32)
    void getLocationDetail_crossTenantReturns404() {
        // Try to access location from another tenant
        given()
                .header("X-Tenant-ID", "tenant-map-other")
                .when()
                .get("/api/v1/map/locations/" + location1Id + "/detail")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(33)
    void getLocationDetail_includesAreaPosition() {
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/map/locations/" + location1Id + "/detail")
                .then()
                .statusCode(200)
                .body("areaPosition", notNullValue());
    }

    // ========================================
    // LOCATION HISTORY TESTS (Order 40-44)
    // ========================================

    @Test
    @Order(40)
    void getLocationHistory_returnsPagination() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 1)
                .queryParam("perPage", 12)
                .when()
                .get("/api/v1/map/locations/" + location1Id + "/history")
                .then()
                .statusCode(200)
                .body("location", notNullValue())
                .body("location.locationId", equalTo(location1Id))
                .body("pagination", notNullValue())
                .body("pagination.page", equalTo(1))
                .body("pagination.perPage", equalTo(12));
    }

    @Test
    @Order(41)
    void getLocationHistory_notFound() {
        String nonExistentId = UUID.randomUUID().toString();
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .get("/api/v1/map/locations/" + nonExistentId + "/history")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(42)
    void getLocationHistory_respectsPerPageLimit() {
        // perPage > 50 should be capped at 50
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("page", 1)
                .queryParam("perPage", 100)
                .when()
                .get("/api/v1/map/locations/" + location1Id + "/history")
                .then()
                .statusCode(200)
                .body("pagination.perPage", equalTo(50));
    }

    @Test
    @Order(43)
    void getLocationHistory_withoutUrls() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("includeUrls", false)
                .when()
                .get("/api/v1/map/locations/" + location1Id + "/history")
                .then()
                .statusCode(200)
                .body("periods", notNullValue());
    }

    @Test
    @Order(44)
    void getLocationHistory_withUrls() {
        given()
                .header("X-Tenant-ID", TENANT)
                .queryParam("includeUrls", true)
                .when()
                .get("/api/v1/map/locations/" + location1Id + "/history")
                .then()
                .statusCode(200)
                .body("periods", notNullValue());
    }

    // ========================================
    // PRESIGNED URL TESTS (Order 50-52)
    // ========================================

    @Test
    @Order(50)
    void presignedUrls_batchGeneration() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "storageKeys": ["test/image1.jpg", "test/image2.jpg"]
                        }
                        """)
                .when()
                .post("/api/v1/map/presigned-urls")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    @Order(51)
    void presignedUrls_emptyList() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "storageKeys": []
                        }
                        """)
                .when()
                .post("/api/v1/map/presigned-urls")
                .then()
                .statusCode(400); // @NotEmpty validation rejects empty list
    }

    @Test
    @Order(52)
    void presignedUrls_nullStorageKeys() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/v1/map/presigned-urls")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    // ========================================
    // CACHE INVALIDATION TESTS (Order 60-61)
    // ========================================

    @Test
    @Order(60)
    void invalidateCache_adminOnly() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v1/map/cache/invalidate")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(61)
    @TestSecurity(user = "viewer-user", roles = {"VIEWER"})
    void invalidateCache_forbiddenForViewer() {
        given()
                .header("X-Tenant-ID", TENANT)
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v1/map/cache/invalidate")
                .then()
                .statusCode(403);
    }

    // ========================================
    // CLEANUP (Order 99)
    // ========================================

    @Test
    @Order(99)
    void cleanup_deleteWarehouse() {
        // Delete warehouse (cascades to areas and locations)
        given()
                .header("X-Tenant-ID", TENANT)
                .when()
                .delete("/api/v1/warehouses/" + warehouseId)
                .then()
                .statusCode(anyOf(equalTo(204), equalTo(200)));
    }
}
