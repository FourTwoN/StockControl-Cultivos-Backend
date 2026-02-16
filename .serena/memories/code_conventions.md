# Code Conventions & Patterns

## Module Internal Structure (per domain module)
Each module follows the same 5-package pattern:
```
demeter-{module}/src/main/java/com/fortytwo/demeter/{module}/
├── model/          JPA entities
├── dto/            Java records (request DTOs, response DTOs)
├── repository/     PanacheRepositoryBase implementations
├── service/        @ApplicationScoped business logic
└── controller/     JAX-RS @Path endpoints
```

## Entity Pattern
- All domain entities **extend `BaseEntity`** (UUID PK, `@TenantId tenant_id`, timestamps)
- Exception: `Tenant` entity uses String PK slug, does NOT extend BaseEntity
- Annotations: `@Entity`, `@Table(name = "...")`, field-level JPA annotations
- Explicit getters/setters (no Lombok)
- Enums: `@Enumerated(EnumType.STRING)`
- Relationships: `@ManyToOne(fetch = FetchType.LAZY)`, `@OneToMany(mappedBy, cascade = ALL, orphanRemoval = true)`
- Lists initialized as `new ArrayList<>()`

## DTO Pattern
- **Java records** for all DTOs
- Response DTOs have a static `from(Entity)` factory method for mapping
- Request DTOs use Jakarta validation: `@NotBlank`, `@Size(max = N)`, `@NotNull`
- Create DTOs: required fields validated; Update DTOs: all fields nullable (partial update)

## Repository Pattern
- Implements `PanacheRepositoryBase<Entity, UUID>` (or `<Entity, String>` for Tenant)
- `@ApplicationScoped`
- Custom queries: `find("fieldName", value).firstResultOptional()` / `.list()`

## Service Pattern
- `@ApplicationScoped`
- `@Inject` repositories (field injection)
- `@Transactional` on mutating methods
- Returns DTOs, not entities
- Throws `EntityNotFoundException("Type", id)` for 404s
- `findAll(int page, int size)` returns `PagedResponse<DTO>` via `PagedResponse.of()`
- `findById(UUID)` returns single DTO
- `create(CreateRequest)` returns DTO
- `update(UUID, UpdateRequest)` returns DTO (null-check each field for partial updates)
- `delete(UUID)` returns void

## Controller Pattern
- `@Path("/api/v1/{resource}")` + `@Produces(APPLICATION_JSON)` + `@Consumes(APPLICATION_JSON)`
- `@Inject` service (field injection)
- `@RolesAllowed({...})` per endpoint:
  - GET: all roles (ADMIN, SUPERVISOR, WORKER, VIEWER)
  - POST/PUT: ADMIN, SUPERVISOR
  - DELETE: ADMIN only
- Pagination: `@QueryParam("page") @DefaultValue("0")` + `@QueryParam("size") @DefaultValue("20")`
- POST returns `Response.status(CREATED).entity(dto).build()`
- DELETE returns `Response.noContent().build()`

## Error Handling
- `GlobalExceptionHandler` (JAX-RS ExceptionMapper) handles:
  - `EntityNotFoundException` → 404
  - `TenantMismatchException` → 403
  - `ConstraintViolationException` → 400
  - `WebApplicationException` → pass-through status
  - All others → 500 (logged)
- Response format: `ErrorResponse(int status, String error, String message)` record

## Naming Conventions
- Packages: lowercase Spanish module names (productos, ventas, costos, etc.)
- Classes: English (Product, Sale, StockBatch, etc.)
- API paths: English kebab-case (`/stock-batches`, `/price-lists`)
- Database tables: English snake_case (`stock_batches`, `price_lists`)
- Enums: UPPER_SNAKE_CASE (ACTIVE, PENDING, COMPLETED)

## JSONB Fields
- Mapped with `@JdbcTypeCode(SqlTypes.JSON)` + `@Column(columnDefinition = "jsonb")`
- Used for: `custom_attributes` (StockBatch), `theme`/`settings`/`enabledModules` (Tenant)
