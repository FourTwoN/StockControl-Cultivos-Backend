# Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 25 |
| Framework | Quarkus | 3.31.2 |
| ORM | Hibernate with Panache | (Quarkus-managed) |
| Database | PostgreSQL | 17 (alpine, via DevServices) |
| Multi-Tenancy | Hibernate DISCRIMINATOR + PostgreSQL RLS | 3-layer |
| Authentication | OIDC (Auth0) + JWT | |
| Authorization | Jakarta `@RolesAllowed` (RBAC) | |
| Migrations | Flyway (V1–V5) | |
| Build | Gradle (Kotlin DSL) | 9.3.1 |
| Concurrency | **Virtual Threads (Project Loom)** enabled globally |
| Testing | JUnit 5 + REST Assured + Testcontainers | |
| API Docs | SmallRye OpenAPI + Swagger UI | |
| Containerization | Docker multi-stage + docker-compose | |
| Cloud Deploy | Google Cloud Run + Cloud SQL + Terraform | |

## Key Dependencies (from gradle.properties)
- `quarkusVersion=3.31.2`
- `javaVersion=25`
- `testcontainersVersion=1.20.4`
- `restAssuredVersion=5.5.1`

## Common Module (`demeter-common`) Dependencies
- `quarkus-hibernate-orm-panache`
- `quarkus-jdbc-postgresql`
- `quarkus-oidc`
- `quarkus-smallrye-jwt`
- `quarkus-rest-jackson`
- `quarkus-hibernate-validator`
- `quarkus-smallrye-openapi`
- `quarkus-flyway`
