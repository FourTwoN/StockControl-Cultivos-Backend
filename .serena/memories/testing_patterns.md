# Testing Patterns

## Test Infrastructure
- Framework: JUnit 5 + REST Assured
- Container: Testcontainers with PostgreSQL 17 alpine
- Security: `quarkus-test-security` — `@TestSecurity(user = "test-user", roles = {"ADMIN"})`
- OIDC disabled in test profile: `quarkus.oidc.enabled=false`
- Flyway runs migrations automatically in tests
- Test config: `demeter-app/src/test/resources/application.properties`

## Test Location
All integration tests live in:
`demeter-app/src/test/java/com/fortytwo/demeter/integration/`

## Test Classes (49 tests total)
| Class | Tests | Purpose |
|-------|-------|---------|
| HealthCheckTest | 3 | Liveness, readiness, overall health |
| ProductControllerTest | 7 | Product CRUD + validation + 404 |
| StockBatchControllerTest | 10 | Batch CRUD + status transitions + filters |
| SaleFlowTest | 14 | Full lifecycle: product → batch → sale → complete/cancel |
| MultiTenantIsolationTest | 3 | Tenant A/B data isolation |
| OpenApiTest | 5 | OpenAPI spec + Swagger UI paths |
| TenantConfigTest | 7 | Tenant config CRUD + public endpoint |

## Test Patterns

### Standard Test Class Structure
```java
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "test-user", roles = {"ADMIN"})
class XxxControllerTest {
    private static final String TENANT = "tenant-xxx";
    private static String createdId;

    @Test @Order(1)
    void create_shouldReturn201() { ... }

    @Test @Order(2)
    void getById_shouldReturnCreated() { ... }
    // ...
}
```

### REST Assured Pattern
```java
given()
    .header("X-Tenant-ID", TENANT)
    .contentType(ContentType.JSON)
    .body("""
        { "field": "value" }
    """)
    .when()
    .post("/api/v1/resource")
    .then()
    .statusCode(201)
    .body("id", notNullValue())
    .body("field", equalTo("value"))
    .extract().path("id");
```

### Key Lesson: Tenant IDs must be unique per test class
`MultiTenantIsolationTest` uses unique tenant IDs per method to avoid cross-test contamination.

## Known Test Pitfalls
1. **@TestSecurity vs JWT**: `JsonWebToken` proxy throws `IllegalStateException` when not a real JWT.
   `DemeterTenantResolver` wraps JWT access in try-catch.
2. **BigDecimal serialization**: `BigDecimal("10")` serializes as integer `10`, not `10.0`.
   Use integer comparison or `.floatValue()` comparison.
3. **RLS in tests**: Flyway runs V3 (RLS policies), so `X-Tenant-ID` header must be set on
   every test request; otherwise the RLS policy will filter out all rows.
