# Task Completion Checklist

When a task is completed, verify the following:

## 1. Build Check
```bash
./gradlew build
```
Must complete with `BUILD SUCCESSFUL`.

## 2. Run Tests
```bash
./gradlew :demeter-app:test
```
All 49+ tests must pass. If new tests were added, verify they pass too.

## 3. Code Quality
- [ ] No `System.out.println()` — use `org.jboss.logging.Logger` instead
- [ ] No hardcoded secrets or magic values
- [ ] All new DTOs are Java records with Jakarta validation on create requests
- [ ] New entities extend `BaseEntity` (unless they're like `Tenant` — global, no tenant_id)
- [ ] `@Transactional` on all mutating service methods
- [ ] `@RolesAllowed` on all controller endpoints
- [ ] Error handling via `EntityNotFoundException` (not raw 404 responses)

## 4. Multi-Tenancy
- [ ] New entities include `tenant_id` via `BaseEntity` (Hibernate `@TenantId`)
- [ ] Unique constraints are scoped: `UNIQUE(tenant_id, ...)` not global
- [ ] Flyway migration adds RLS policy for new tables
- [ ] Flyway migration adds tenant_id index for new tables

## 5. Patterns
- [ ] Follow the 5-package structure: model, dto, repository, service, controller
- [ ] DTOs have `static from(Entity)` mapper methods
- [ ] Services return DTOs, not entities
- [ ] PagedResponse for list endpoints
- [ ] Partial updates via null-check pattern

## 6. API Conventions
- [ ] Path: `/api/v1/{resource}` (English kebab-case)
- [ ] POST returns 201 with entity
- [ ] DELETE returns 204
- [ ] Pagination: `?page=0&size=20`
