# API Endpoints

All endpoints require `X-Tenant-ID` header (except tenant config GET).
Authentication via Bearer JWT token in production.

## Productos Module
| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/api/v1/products?page=0&size=20` | ALL | List products (paged) |
| GET | `/api/v1/products/{id}` | ALL | Get product by ID |
| POST | `/api/v1/products` | ADMIN, SUPERVISOR | Create product |
| PUT | `/api/v1/products/{id}` | ADMIN, SUPERVISOR | Update product |
| DELETE | `/api/v1/products/{id}` | ADMIN | Delete product |
| * | `/api/v1/categories` | (same pattern) | Category CRUD |
| * | `/api/v1/families` | (same pattern) | Family CRUD |

## Inventario Module
| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| * | `/api/v1/stock-batches` | (same CRUD pattern) | Batch CRUD + by-product, by-status filters |
| * | `/api/v1/stock-movements` | (same pattern) | Movement CRUD + batch associations |

## Ventas Module
| * | `/api/v1/sales` | (CRUD pattern) | Sales CRUD |
| POST | `/api/v1/sales/{id}/complete` | ADMIN, SUPERVISOR | Complete sale (creates stock movements) |
| POST | `/api/v1/sales/{id}/cancel` | ADMIN, SUPERVISOR | Cancel sale |

## Costos Module
| * | `/api/v1/costs` | (CRUD pattern) | Cost entries + by-product, by-batch filters |

## Precios Module
| * | `/api/v1/price-lists` | (CRUD pattern) | Price list CRUD + activate/deactivate |
| * | `/api/v1/price-lists/{id}/entries` | (CRUD pattern) | Price entries + bulk add |

## Ubicaciones Module
| * | `/api/v1/warehouses` | (CRUD pattern) | Warehouse CRUD + soft delete |
| * | `/api/v1/storage-areas` | (CRUD) | Storage area CRUD |
| * | `/api/v1/storage-locations` | (CRUD) | Storage location CRUD |
| * | `/api/v1/storage-bins` | (CRUD) | Storage bin CRUD |
| * | `/api/v1/storage-bin-types` | (CRUD) | Bin type CRUD |

## Empaquetado Module
| * | `/api/v1/packaging-types` | (CRUD) | |
| * | `/api/v1/packaging-materials` | (CRUD) | |
| * | `/api/v1/packaging-colors` | (CRUD) | |
| * | `/api/v1/packaging-catalogs` | (CRUD) | |

## Usuarios Module
| * | `/api/v1/users` | (CRUD) | User CRUD + roles |

## Analytics Module
| GET | `/api/v1/analytics/*` | ALL | 7 read-only endpoints (stock summary, movements, inventory valuation, top products, location occupancy, dashboard, movement history) |

## Fotos Module (DLC)
| * | `/api/v1/photo-sessions` | (CRUD) | Photo processing sessions + status polling |
| GET | `/api/v1/images` | ALL | Images with detections/classifications |

## Chatbot Module (DLC)
| * | `/api/v1/chat/sessions` | (CRUD) | Chat sessions + messages |
| GET | `/api/v1/chat/messages` | ALL | Messages + tool executions |

## Tenant Config
| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/api/v1/tenants/{tenantId}/config` | **@PermitAll** | Public config (theme, modules) — called before login |
| GET | `/api/v1/tenants` | ADMIN | List all tenants |
| POST | `/api/v1/tenants` | ADMIN | Create tenant |
| PUT | `/api/v1/tenants/{tenantId}` | ADMIN | Update tenant |
| DELETE | `/api/v1/tenants/{tenantId}` | ADMIN | Delete tenant |

## Health Endpoints
- `/q/health/live` — Liveness
- `/q/health/ready` — Readiness
- `/q/health` — Overall
