# Development Conventions

## Virtual Threads (MANDATORY)

Virtual Threads are **enabled globally** via `quarkus.virtual-threads.enabled=true`.

**ALWAYS write simple blocking code** — NO Reactive/Mutiny needed:

```java
// CORRECT - Simple blocking code on Virtual Thread
@GET
public List<Product> list() {
    return productRepository.listAll();
}

// WRONG - Don't use Reactive with Virtual Threads
@GET
public Uni<List<Product>> list() {  // Unnecessary complexity
    return productRepository.listAll();
}
```

## Panache Query Guidelines

| Complexity | Tool |
|------------|------|
| CRUD basico | Panache built-in (`findById`, `listAll`, `persist`) |
| Filtros simples | PQL: `list("price > ?1", 100)` |
| Joins simples | JPQL en `find()` |
| Joins complejos | JPQL con `createQuery()` + projection DTOs |
| PostgreSQL features | SQL Nativo (CTEs, JSONB, window functions) |

## Immutability (CRITICAL)

**NEVER mutate objects** — always create new instances:

```java
// WRONG
product.price = newPrice;

// CORRECT
return new Product(product.id, product.name, newPrice);
```

## Multi-Tenancy

Never filter by tenant manually — Hibernate `@TenantId` does it automatically:

```java
// CORRECT
productRepository.listAll();  // Hibernate adds WHERE tenant_id = ?

// WRONG
productRepository.list("tenantId = ?1", tenantId);  // Redundant
```
