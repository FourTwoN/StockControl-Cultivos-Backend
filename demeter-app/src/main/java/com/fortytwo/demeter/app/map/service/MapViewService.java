package com.fortytwo.demeter.app.map.service;

import com.fortytwo.demeter.app.map.dto.*;
import com.fortytwo.demeter.app.map.repository.MapViewRepository;
import com.fortytwo.demeter.app.map.repository.MapViewRepository.BulkLoadRow;
import com.fortytwo.demeter.app.map.repository.MapViewRepository.LocationDetailRow;
import com.fortytwo.demeter.app.map.repository.MapViewRepository.LocationHistoryRow;
import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.common.tenant.TenantContext;
import com.fortytwo.demeter.fotos.storage.StorageService;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for map view operations with caching support.
 *
 * <p>Transforms flat database rows into nested hierarchy and handles
 * presigned URL generation for images.
 */
@ApplicationScoped
public class MapViewService {

    private static final Logger log = Logger.getLogger(MapViewService.class);

    @Inject
    MapViewRepository repository;

    @Inject
    StorageService storageService;

    @Inject
    TenantContext tenantContext;

    @ConfigProperty(name = "demeter.map.url-expiration-minutes", defaultValue = "15")
    int urlExpirationMinutes;

    /**
     * Get bulk load data for the map view.
     *
     * <p>Returns the complete warehouse hierarchy with location preview metrics.
     * Results are cached per tenant with a 5-minute TTL.
     *
     * @return MapBulkLoadResponse with nested warehouse/area/location hierarchy
     */
    public MapBulkLoadResponse getBulkLoad() {
        String tenantId = tenantContext.getCurrentTenantId();
        // Use a default key if tenant is null (should not happen in production)
        String cacheKey = tenantId != null ? tenantId : "__default__";
        return getBulkLoadCached(cacheKey);
    }

    /**
     * Internal cached method - tenant ID is used as cache key.
     */
    @CacheResult(cacheName = "map-bulk-load")
    @Transactional
    MapBulkLoadResponse getBulkLoadCached(String tenantId) {
        log.infof("Loading map bulk data for tenant: %s", tenantId);

        List<BulkLoadRow> rows = repository.getBulkLoadData();

        if (rows.isEmpty()) {
            log.info("No map data found");
            return MapBulkLoadResponse.empty();
        }

        MapBulkLoadResponse response = transformToHierarchy(rows);

        log.infof("Map bulk load complete: %d warehouses, %d total locations",
                response.warehouses().size(),
                rows.size());

        return response;
    }

    /**
     * Get detailed information for a specific location.
     *
     * @param locationId Storage location UUID
     * @return LocationDetailResponse
     * @throws EntityNotFoundException if location not found
     */
    @Transactional
    public LocationDetailResponse getLocationDetail(UUID locationId) {
        log.infof("Getting location detail: %s", locationId);

        LocationDetailRow row = repository.getLocationDetail(locationId);

        if (row == null) {
            throw new EntityNotFoundException("StorageLocation", locationId);
        }

        return new LocationDetailResponse(
                new LocationInfo(row.locationId(), row.locationCode(), row.locationName()),
                row.sessionId() != null ? new SessionSummary(
                        row.sessionId(),
                        row.sessionStatus(),
                        row.totalDetected(),
                        null, // totalEstimated - same as totalDetected for now
                        row.totalEmptyContainers(),
                        row.avgConfidence(),
                        row.totalCactus(),
                        row.totalSuculentas(),
                        row.totalInjertos(),
                        row.sessionCreatedAt()
                ) : null,
                row.daysWithoutUpdate(),
                row.areaPosition(),
                row.totalCactus(),
                row.totalSuculentas(),
                row.totalInjertos(),
                row.totalEmptyContainers()
        );
    }

    /**
     * Get paginated photo history for a location.
     *
     * @param locationId  Storage location UUID
     * @param page        Page number (1-indexed)
     * @param perPage     Items per page
     * @param includeUrls Whether to include presigned URLs (false for lazy loading)
     * @return LocationHistoryResponse with periods and pagination
     * @throws EntityNotFoundException if location not found
     */
    @Transactional
    public LocationHistoryResponse getLocationHistory(
            UUID locationId,
            int page,
            int perPage,
            boolean includeUrls
    ) {
        log.infof("Getting location history: %s (page=%d, perPage=%d, includeUrls=%s)",
                locationId, page, perPage, includeUrls);

        // Get location info first
        LocationDetailRow locationDetail = repository.getLocationDetail(locationId);
        if (locationDetail == null) {
            throw new EntityNotFoundException("StorageLocation", locationId);
        }

        // Get history data
        List<LocationHistoryRow> rows = repository.getLocationHistory(locationId, page, perPage);
        int totalItems = repository.countLocationSessions(locationId);

        // Build periods list
        List<LocationHistoryItem> periods = new ArrayList<>();
        List<String> storageKeys = new ArrayList<>();

        // Collect storage keys for batch URL generation
        for (LocationHistoryRow row : rows) {
            if (row.photoStorageKey() != null && !row.photoStorageKey().isEmpty()) {
                storageKeys.add(row.photoStorageKey());
            }
        }

        // Generate URLs in batch if requested
        Map<String, String> urlMap = Collections.emptyMap();
        if (includeUrls && !storageKeys.isEmpty()) {
            Duration expiration = Duration.ofMinutes(urlExpirationMinutes);
            urlMap = storageService.generateReadUrlsBatch(storageKeys, expiration);
        }

        // Calculate net change between consecutive periods
        Integer previousQuantity = null;
        for (int i = 0; i < rows.size(); i++) {
            LocationHistoryRow row = rows.get(i);

            Integer netChange = null;
            if (previousQuantity != null && row.cantidadFinal() != null) {
                netChange = row.cantidadFinal() - previousQuantity;
            }

            String thumbnailUrl = null;
            String storageKey = null;
            if (row.photoStorageKey() != null) {
                if (includeUrls) {
                    thumbnailUrl = urlMap.getOrDefault(row.photoStorageKey(), null);
                } else {
                    storageKey = row.photoStorageKey();
                }
            }

            periods.add(new LocationHistoryItem(
                    row.fecha(),
                    null, // periodEnd - could be calculated from next row
                    row.sessionId(),
                    null, // cantidadInicial
                    null, // muertes
                    null, // trasplantes
                    null, // plantados
                    null, // cantidadVendida
                    row.cantidadFinal(),
                    netChange,
                    thumbnailUrl,
                    storageKey
            ));

            previousQuantity = row.cantidadFinal();
        }

        // Build summary
        HistorySummary summary = new HistorySummary(
                totalItems,
                periods.isEmpty() ? null : periods.get(periods.size() - 1).fecha(),
                periods.isEmpty() ? null : periods.get(0).fecha()
        );

        return new LocationHistoryResponse(
                new LocationInfo(
                        locationDetail.locationId(),
                        locationDetail.locationCode(),
                        locationDetail.locationName()
                ),
                periods,
                summary,
                Pagination.of(page, perPage, totalItems)
        );
    }

    /**
     * Generate presigned URLs for a batch of storage keys.
     *
     * @param storageKeys List of storage keys/paths
     * @return Map of storage key to presigned URL
     */
    public Map<String, String> generatePresignedUrls(List<String> storageKeys) {
        if (storageKeys == null || storageKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        log.infof("Generating presigned URLs for %d keys", storageKeys.size());

        Duration expiration = Duration.ofMinutes(urlExpirationMinutes);
        return storageService.generateReadUrlsBatch(storageKeys, expiration);
    }

    /**
     * Invalidate the map cache for the current tenant.
     *
     * <p>Should be called after photo processing completes or when
     * location data changes.
     */
    public void invalidateMapCache() {
        String tenantId = tenantContext.getCurrentTenantId();
        String cacheKey = tenantId != null ? tenantId : "__default__";
        invalidateMapCacheForTenant(cacheKey);
    }

    /**
     * Internal method to invalidate cache for specific tenant.
     */
    @CacheInvalidate(cacheName = "map-bulk-load")
    void invalidateMapCacheForTenant(String tenantId) {
        log.infof("Invalidating map cache for tenant: %s", tenantId);
    }

    // =========================================================================
    // Hierarchy transformation
    // =========================================================================

    /**
     * Transform flat database rows into nested warehouse/area/location hierarchy.
     */
    private MapBulkLoadResponse transformToHierarchy(List<BulkLoadRow> rows) {
        // Group by warehouse
        Map<UUID, List<BulkLoadRow>> byWarehouse = rows.stream()
                .collect(Collectors.groupingBy(BulkLoadRow::warehouseId, LinkedHashMap::new, Collectors.toList()));

        List<WarehouseNode> warehouses = new ArrayList<>();

        for (Map.Entry<UUID, List<BulkLoadRow>> warehouseEntry : byWarehouse.entrySet()) {
            List<BulkLoadRow> warehouseRows = warehouseEntry.getValue();
            BulkLoadRow firstRow = warehouseRows.get(0);

            // Group by area within this warehouse
            Map<UUID, List<BulkLoadRow>> byArea = warehouseRows.stream()
                    .collect(Collectors.groupingBy(BulkLoadRow::areaId, LinkedHashMap::new, Collectors.toList()));

            List<AreaNode> areas = new ArrayList<>();

            for (Map.Entry<UUID, List<BulkLoadRow>> areaEntry : byArea.entrySet()) {
                List<BulkLoadRow> areaRows = areaEntry.getValue();
                BulkLoadRow firstAreaRow = areaRows.get(0);

                // Build location nodes
                List<LocationNode> locations = areaRows.stream()
                        .map(this::buildLocationNode)
                        .toList();

                areas.add(new AreaNode(
                        firstAreaRow.areaId(),
                        firstAreaRow.areaCode(),
                        firstAreaRow.areaName(),
                        firstAreaRow.areaPosition(),
                        locations
                ));
            }

            warehouses.add(new WarehouseNode(
                    firstRow.warehouseId(),
                    firstRow.warehouseCode(),
                    firstRow.warehouseName(),
                    areas
            ));
        }

        return new MapBulkLoadResponse(warehouses);
    }

    private LocationNode buildLocationNode(BulkLoadRow row) {
        LocationPreview preview;

        if (row.lastPhotoDate() == null) {
            preview = LocationPreview.pending();
        } else {
            preview = new LocationPreview(
                    row.currentQuantity(),
                    row.previousQuantity(),
                    row.lastPhotoDate(),
                    row.daysWithoutUpdate(),
                    row.status(),
                    row.qualityScore(),
                    row.totalCactus(),
                    row.totalSuculentas(),
                    row.totalInjertos(),
                    row.totalEmptyContainers()
            );
        }

        return new LocationNode(
                row.locationId(),
                row.locationCode(),
                row.locationName(),
                preview
        );
    }
}
