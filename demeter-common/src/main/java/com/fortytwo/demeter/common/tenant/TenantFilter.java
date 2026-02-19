package com.fortytwo.demeter.common.tenant;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * JAX-RS filter that extracts tenant ID and populates TenantContext.
 *
 * <p>This filter runs early in the request pipeline (AUTHENTICATION + 1) to ensure
 * TenantContext is populated before any service method accesses it.
 *
 * <p>Priority order:
 * <ol>
 *   <li>JWT claim "tenant_id" (production)</li>
 *   <li>X-Tenant-ID header (dev/testing)</li>
 *   <li>Default tenant "default" (fallback)</li>
 * </ol>
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 1)
public class TenantFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(TenantFilter.class);

    private static final String DEFAULT_TENANT = "default";
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT_CLAIM = "tenant_id";

    @Inject
    JsonWebToken jwt;

    @Inject
    TenantContext tenantContext;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String tenantId = null;

        // Priority 1: JWT claim
        try {
            if (jwt != null && jwt.containsClaim(TENANT_CLAIM)) {
                tenantId = jwt.getClaim(TENANT_CLAIM);
            }
        } catch (IllegalStateException ignored) {
            // Principal is not a JWT — fall through to header
        }

        // Priority 2: Header fallback
        if (tenantId == null) {
            tenantId = requestContext.getHeaderString(TENANT_HEADER);
        }

        // Priority 3: Default
        if (tenantId == null) {
            tenantId = DEFAULT_TENANT;
        }

        tenantContext.setCurrentTenantId(tenantId);
        log.debugf("TenantFilter: Set tenant to '%s'", tenantId);
    }
}
