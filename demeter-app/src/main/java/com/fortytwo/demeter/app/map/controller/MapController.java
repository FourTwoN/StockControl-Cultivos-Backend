package com.fortytwo.demeter.app.map.controller;

import com.fortytwo.demeter.app.map.dto.*;
import com.fortytwo.demeter.app.map.service.MapViewService;
import com.fortytwo.demeter.common.auth.RoleConstants;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;

/**
 * REST API for map visualization endpoints.
 *
 * <p>Provides optimized endpoints for loading warehouse map data:
 * <ul>
 *   <li>Bulk load - entire hierarchy with preview metrics in one call</li>
 *   <li>Location detail - detailed view of a single location</li>
 *   <li>Location history - paginated photo history</li>
 *   <li>Presigned URLs - batch URL generation for lazy image loading</li>
 * </ul>
 */
@Path("/api/v1/map")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Map", description = "Warehouse map visualization endpoints")
public class MapController {

    private static final Logger log = Logger.getLogger(MapController.class);

    @Inject
    MapViewService mapViewService;

    /**
     * Bulk load the complete warehouse hierarchy with location preview metrics.
     *
     * <p>This is the primary endpoint for initial map loading. Returns all
     * warehouses, areas, and locations with aggregated metrics from the
     * latest photo processing sessions.
     *
     * <p>Results are cached per tenant with a 5-minute TTL for performance.
     *
     * @return Complete map hierarchy with preview metrics
     */
    @GET
    @Path("/bulk-load")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    @Operation(
            summary = "Bulk load map data",
            description = "Load the complete warehouse hierarchy with location preview metrics for map visualization"
    )
    @APIResponse(
            responseCode = "200",
            description = "Map data loaded successfully",
            content = @Content(schema = @Schema(implementation = MapBulkLoadResponse.class))
    )
    public MapBulkLoadResponse bulkLoad() {
        log.info("Map bulk load requested");
        return mapViewService.getBulkLoad();
    }

    /**
     * Get detailed information for a specific storage location.
     *
     * <p>Returns comprehensive data about a location including the latest
     * photo session summary, category breakdowns, and computed metrics.
     *
     * @param locationId Storage location UUID
     * @return Detailed location response
     */
    @GET
    @Path("/locations/{locationId}/detail")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    @Operation(
            summary = "Get location detail",
            description = "Get detailed information for a specific storage location including latest session data"
    )
    @APIResponse(
            responseCode = "200",
            description = "Location detail retrieved",
            content = @Content(schema = @Schema(implementation = LocationDetailResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Location not found"
    )
    public LocationDetailResponse getLocationDetail(
            @Parameter(description = "Storage location UUID", required = true)
            @PathParam("locationId") UUID locationId
    ) {
        log.infof("Location detail requested: %s", locationId);
        return mapViewService.getLocationDetail(locationId);
    }

    /**
     * Get paginated photo history for a storage location.
     *
     * <p>Returns a list of photo processing sessions for the location with
     * quantity data. Supports lazy loading by default - set includeUrls=true
     * to get presigned thumbnail URLs in the response.
     *
     * @param locationId Storage location UUID
     * @param page       Page number (1-indexed, default 1)
     * @param perPage    Items per page (default 12, max 50)
     * @param includeUrls Whether to include presigned URLs (default false for lazy loading)
     * @return Paginated history response
     */
    @GET
    @Path("/locations/{locationId}/history")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    @Operation(
            summary = "Get location history",
            description = "Get paginated photo history for a storage location"
    )
    @APIResponse(
            responseCode = "200",
            description = "Location history retrieved",
            content = @Content(schema = @Schema(implementation = LocationHistoryResponse.class))
    )
    @APIResponse(
            responseCode = "404",
            description = "Location not found"
    )
    public LocationHistoryResponse getLocationHistory(
            @Parameter(description = "Storage location UUID", required = true)
            @PathParam("locationId") UUID locationId,

            @Parameter(description = "Page number (1-indexed)")
            @QueryParam("page") @DefaultValue("1") int page,

            @Parameter(description = "Items per page (max 50)")
            @QueryParam("perPage") @DefaultValue("12") int perPage,

            @Parameter(description = "Include presigned URLs (false for lazy loading)")
            @QueryParam("includeUrls") @DefaultValue("false") boolean includeUrls
    ) {
        // Validate pagination params
        if (page < 1) page = 1;
        if (perPage < 1) perPage = 12;
        if (perPage > 50) perPage = 50;

        log.infof("Location history requested: %s (page=%d, perPage=%d)", locationId, page, perPage);
        return mapViewService.getLocationHistory(locationId, page, perPage, includeUrls);
    }

    /**
     * Generate presigned URLs for a batch of storage keys.
     *
     * <p>Used by the frontend for lazy loading images. Instead of including
     * URLs in the initial response, the frontend requests URLs on-demand
     * as images come into view.
     *
     * @param request Batch request with list of storage keys
     * @return Map of storage key to presigned URL
     */
    @POST
    @Path("/presigned-urls")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    @Operation(
            summary = "Generate presigned URLs",
            description = "Generate presigned URLs for multiple storage keys (batch)"
    )
    @APIResponse(
            responseCode = "200",
            description = "URLs generated successfully"
    )
    public Map<String, String> generatePresignedUrls(
            @Valid PresignedUrlBatchRequest request
    ) {
        log.infof("Presigned URL batch request for %d keys", request.storageKeys().size());
        return mapViewService.generatePresignedUrls(request.storageKeys());
    }

    /**
     * Invalidate the map cache (admin only).
     *
     * <p>Forces a refresh of the cached map data. Normally the cache is
     * invalidated automatically when photo processing completes.
     */
    @POST
    @Path("/cache/invalidate")
    @RolesAllowed({RoleConstants.ADMIN})
    @Operation(
            summary = "Invalidate map cache",
            description = "Force refresh of cached map data (admin only)"
    )
    @APIResponse(
            responseCode = "204",
            description = "Cache invalidated"
    )
    public void invalidateCache() {
        log.info("Map cache invalidation requested");
        mapViewService.invalidateMapCache();
    }
}
