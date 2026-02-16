# Architecture

## 13 Gradle Subprojects

```
demeter-app          (Quarkus main — entry point, tests, config, migrations)
├── demeter-common      (BaseEntity, tenant resolver, RLS, auth, exceptions, DTOs)
├── demeter-productos   (Products, categories, families, density params)
├── demeter-inventario  (Stock batches, movements, custom JSONB attributes)
├── demeter-ventas      (Sales lifecycle: create → complete/cancel)
├── demeter-costos      (Cost tracking per product/batch)
├── demeter-precios     (Price lists with versioning and tiers)
├── demeter-ubicaciones (Warehouses, areas, locations, bins — custom hierarchy)
├── demeter-empaquetado (Packaging types, materials, colors, catalogs)
├── demeter-usuarios    (Users, roles, OIDC mapping)
├── demeter-analytics   (7 read-only dashboard endpoints)
├── demeter-fotos       (Photo processing sessions, AI detections — DLC)
└── demeter-chatbot     (Conversational AI with tool execution — DLC)
```

## Module Dependency Graph
```
demeter-analytics ── productos, inventario, ventas, costos, ubicaciones, empaquetado, precios
demeter-ventas ───── productos, inventario
demeter-costos ───── productos, inventario
demeter-precios ──── productos
demeter-inventario ─ productos
demeter-fotos ────── productos
demeter-chatbot ──── (common only)
demeter-productos ── (common only)
demeter-ubicaciones ─ (common only)
demeter-empaquetado ─ (common only)
demeter-usuarios ─── (common only)
```

## Module Selection (Build-time)
Modules can be enabled/disabled via Gradle property:
```bash
./gradlew :demeter-app:quarkusDev -Pdemeter.modules=productos,inventario,ventas
```
`common` is always included. Default is ALL modules.

## 3-Layer Tenant Isolation

| Layer | Mechanism |
|-------|-----------|
| Application | `DemeterTenantResolver` — extracts `tenant_id` from JWT claim or `X-Tenant-ID` header |
| ORM | Hibernate `@TenantId` on `BaseEntity` — auto-appends `WHERE tenant_id = ?` |
| Database | PostgreSQL RLS policies on all 40+ tables — `USING(tenant_id = current_setting('app.current_tenant'))` |

### Tenant Resolution Priority
1. JWT `tenant_id` claim (guarded with try-catch for @TestSecurity)
2. `X-Tenant-ID` header (dev/testing fallback)
3. Default `"default"` tenant

### RLS Connection Customizer
`RlsConnectionCustomizer` implements `AgroalPoolInterceptor`:
- `onConnectionAcquire`: sets `app.current_tenant` via `SELECT set_config()`
- `onConnectionReturn`: resets to empty string

## Flyway Migrations
- V1: Baseline schema (30 tables)
- V2: Tenant ID indexes
- V3: RLS policies
- V4: `tenants` master table (no tenant_id, no RLS)
- V5: Seed example tenants (go-bar, central-de-bebidas)

## RBAC Roles
- `ADMIN` — full access, can delete
- `SUPERVISOR` — create/update access
- `WORKER` — read + limited write
- `VIEWER` — read-only

## Special: Tenants Table
- **NOT** multi-tenant (no `tenant_id` column, no RLS, no `BaseEntity`)
- PK is String slug (e.g., "go-bar"), not UUID
- Contains: name, industry, theme (JSONB), enabledModules (JSONB), settings (JSONB)
- `GET /api/v1/tenants/{tenantId}/config` is `@PermitAll` (pre-login)
