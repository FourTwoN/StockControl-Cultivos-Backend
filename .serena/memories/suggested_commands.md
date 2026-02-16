# Suggested Commands

## Development

### Run in dev mode (all modules)
```bash
./gradlew :demeter-app:quarkusDev
```

### Run with specific modules
```bash
./gradlew :demeter-app:quarkusDev -Pdemeter.modules=productos,inventario,ventas
```

### Print active modules
```bash
./gradlew :demeter-app:printDemeterModules
```

### Build project
```bash
./gradlew build
```

### Build without tests
```bash
./gradlew build -x test
```

## Testing

### Run all tests (49 integration tests)
```bash
./gradlew :demeter-app:test
```

### Run a specific test class
```bash
./gradlew :demeter-app:test --tests "com.fortytwo.demeter.integration.ProductControllerTest"
```

### Run a specific test method
```bash
./gradlew :demeter-app:test --tests "com.fortytwo.demeter.integration.ProductControllerTest.createProduct_shouldReturn201"
```

## API Documentation
- Swagger UI: `http://localhost:8080/q/swagger-ui/`
- OpenAPI spec: `http://localhost:8080/q/openapi`

## Docker

### Build and run with docker-compose
```bash
docker-compose up -d
```

### Build Docker image
```bash
docker build -t demeter-backend .
```

## Quick API Test
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "X-Tenant-ID: go-bar" \
  -H "Content-Type: application/json" \
  -d '{"sku": "TEST-001", "name": "Test Product"}'
```

## Useful Gradle
```bash
./gradlew clean                          # Clean build artifacts
./gradlew dependencies                   # Show dependency tree
./gradlew :demeter-app:quarkusBuild      # Production build
```

## Prerequisites
- Java 25: `sdk install java 25-open`
- Docker (for PostgreSQL via DevServices/Testcontainers)
- No manual DB setup needed — Quarkus DevServices auto-starts PostgreSQL 17
