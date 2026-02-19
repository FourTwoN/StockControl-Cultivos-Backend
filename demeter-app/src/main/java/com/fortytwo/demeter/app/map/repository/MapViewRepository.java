package com.fortytwo.demeter.app.map.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for map view queries using native SQL for optimal performance.
 *
 * <p>Uses PostgreSQL CTEs to efficiently load the entire warehouse hierarchy
 * with location metrics in a single query, avoiding N+1 problems.
 */
@ApplicationScoped
public class MapViewRepository {

    private static final Logger log = Logger.getLogger(MapViewRepository.class);

    @Inject
    EntityManager em;

    /**
     * Bulk load all warehouse hierarchy data with location preview metrics.
     *
     * <p>Returns flat rows that need to be transformed into a nested hierarchy.
     * Each row represents one location with its warehouse, area, and metrics.
     *
     * <p>The query uses CTEs to:
     * <ol>
     *   <li>Find the latest 2 sessions per location for current/previous comparison</li>
     *   <li>Aggregate estimation counts by product family for category breakdown</li>
     *   <li>Join everything with the location hierarchy</li>
     * </ol>
     *
     * @return List of BulkLoadRow records with all data
     */
    @SuppressWarnings("unchecked")
    public List<BulkLoadRow> getBulkLoadData() {
        String sql = """
            WITH latest_sessions AS (
                -- Get the 2 most recent COMPLETED sessions per location
                SELECT
                    ps.storage_location_id,
                    ps.id as session_id,
                    ps.created_at,
                    ps.status,
                    ROW_NUMBER() OVER (
                        PARTITION BY ps.storage_location_id
                        ORDER BY ps.created_at DESC
                    ) as rn
                FROM photo_processing_sessions ps
                WHERE ps.tenant_id = current_setting('app.current_tenant')
                  AND ps.status = 'COMPLETED'
            ),
            session_estimations AS (
                -- Get total estimated count per session (type = 'COUNT')
                SELECT
                    ls.storage_location_id,
                    ls.session_id,
                    ls.created_at,
                    ls.rn,
                    COALESCE(SUM(e.estimated_count), 0) as total_count,
                    MAX(e.confidence) as avg_confidence
                FROM latest_sessions ls
                LEFT JOIN estimations e ON e.session_id = ls.session_id
                    AND e.estimation_type = 'COUNT'
                WHERE ls.rn <= 2
                GROUP BY ls.storage_location_id, ls.session_id, ls.created_at, ls.rn
            ),
            category_counts AS (
                -- Get counts by product family for latest session only
                SELECT
                    ls.storage_location_id,
                    COALESCE(SUM(CASE WHEN pf.name ILIKE '%cactus%' THEN e.estimated_count ELSE 0 END), 0) as total_cactus,
                    COALESCE(SUM(CASE WHEN pf.name ILIKE '%suculenta%' THEN e.estimated_count ELSE 0 END), 0) as total_suculentas,
                    COALESCE(SUM(CASE WHEN pf.name ILIKE '%injerto%' THEN e.estimated_count ELSE 0 END), 0) as total_injertos,
                    COALESCE(SUM(CASE WHEN e.estimation_type = 'EMPTY_CONTAINER' THEN e.estimated_count ELSE 0 END), 0) as total_empty
                FROM latest_sessions ls
                LEFT JOIN estimations e ON e.session_id = ls.session_id
                LEFT JOIN classifications c ON c.id = e.classification_id
                LEFT JOIN products p ON p.id = c.product_id
                LEFT JOIN product_families pf ON pf.id = p.family_id
                WHERE ls.rn = 1
                GROUP BY ls.storage_location_id
            ),
            location_metrics AS (
                -- Pivot session data to get current and previous values
                SELECT
                    se.storage_location_id,
                    MAX(CASE WHEN se.rn = 1 THEN se.total_count END) as current_quantity,
                    MAX(CASE WHEN se.rn = 2 THEN se.total_count END) as previous_quantity,
                    MAX(CASE WHEN se.rn = 1 THEN se.created_at END) as last_photo_date,
                    MAX(CASE WHEN se.rn = 1 THEN se.avg_confidence END) as quality_score
                FROM session_estimations se
                GROUP BY se.storage_location_id
            )
            SELECT
                w.id as warehouse_id,
                w.code as warehouse_code,
                w.name as warehouse_name,
                sa.id as area_id,
                sa.code as area_code,
                sa.name as area_name,
                sa.position as area_position,
                sl.id as location_id,
                sl.code as location_code,
                sl.name as location_name,
                COALESCE(lm.current_quantity, 0) as current_quantity,
                lm.previous_quantity,
                lm.last_photo_date,
                CASE
                    WHEN lm.last_photo_date IS NOT NULL
                    THEN EXTRACT(DAY FROM NOW() - lm.last_photo_date)::int
                    ELSE NULL
                END as days_without_update,
                lm.quality_score,
                CASE
                    WHEN lm.last_photo_date IS NOT NULL THEN 'COMPLETED'
                    ELSE 'PENDING'
                END as status,
                COALESCE(cc.total_cactus, 0) as total_cactus,
                COALESCE(cc.total_suculentas, 0) as total_suculentas,
                COALESCE(cc.total_injertos, 0) as total_injertos,
                COALESCE(cc.total_empty, 0) as total_empty_containers
            FROM warehouses w
            JOIN storage_areas sa ON sa.warehouse_id = w.id AND sa.active = true
            JOIN storage_locations sl ON sl.area_id = sa.id AND sl.active = true
            LEFT JOIN location_metrics lm ON lm.storage_location_id = sl.id
            LEFT JOIN category_counts cc ON cc.storage_location_id = sl.id
            WHERE w.tenant_id = current_setting('app.current_tenant')
              AND w.active = true
            ORDER BY w.code, sa.code, sl.code
            """;

        Query query = em.createNativeQuery(sql);

        List<Object[]> results = query.getResultList();

        log.infof("Bulk load query returned %d rows", results.size());

        return results.stream()
                .map(this::mapToBulkLoadRow)
                .toList();
    }

    /**
     * Get detailed data for a single location.
     *
     * @param locationId Storage location UUID
     * @return LocationDetailRow or null if not found
     */
    @SuppressWarnings("unchecked")
    public LocationDetailRow getLocationDetail(UUID locationId) {
        String sql = """
            WITH latest_session AS (
                SELECT
                    ps.id as session_id,
                    ps.status,
                    ps.created_at,
                    ps.storage_location_id
                FROM photo_processing_sessions ps
                WHERE ps.storage_location_id = :locationId
                  AND ps.tenant_id = current_setting('app.current_tenant')
                  AND ps.status = 'COMPLETED'
                ORDER BY ps.created_at DESC
                LIMIT 1
            ),
            session_stats AS (
                SELECT
                    ls.session_id,
                    ls.status,
                    ls.created_at,
                    COALESCE(SUM(e.estimated_count), 0) as total_detected,
                    COALESCE(SUM(CASE WHEN e.estimation_type = 'EMPTY_CONTAINER' THEN e.estimated_count ELSE 0 END), 0) as total_empty,
                    AVG(e.confidence) as avg_confidence
                FROM latest_session ls
                LEFT JOIN estimations e ON e.session_id = ls.session_id
                GROUP BY ls.session_id, ls.status, ls.created_at
            ),
            category_counts AS (
                SELECT
                    COALESCE(SUM(CASE WHEN pf.name ILIKE '%cactus%' THEN e.estimated_count ELSE 0 END), 0) as total_cactus,
                    COALESCE(SUM(CASE WHEN pf.name ILIKE '%suculenta%' THEN e.estimated_count ELSE 0 END), 0) as total_suculentas,
                    COALESCE(SUM(CASE WHEN pf.name ILIKE '%injerto%' THEN e.estimated_count ELSE 0 END), 0) as total_injertos
                FROM latest_session ls
                LEFT JOIN estimations e ON e.session_id = ls.session_id
                LEFT JOIN classifications c ON c.id = e.classification_id
                LEFT JOIN products p ON p.id = c.product_id
                LEFT JOIN product_families pf ON pf.id = p.family_id
            )
            SELECT
                sl.id as location_id,
                sl.code as location_code,
                sl.name as location_name,
                sa.position as area_position,
                ss.session_id,
                ss.status as session_status,
                ss.total_detected,
                ss.total_empty as total_empty_containers,
                ss.avg_confidence,
                ss.created_at as session_created_at,
                CASE
                    WHEN ss.created_at IS NOT NULL
                    THEN EXTRACT(DAY FROM NOW() - ss.created_at)::int
                    ELSE NULL
                END as days_without_update,
                cc.total_cactus,
                cc.total_suculentas,
                cc.total_injertos
            FROM storage_locations sl
            JOIN storage_areas sa ON sa.id = sl.area_id
            LEFT JOIN session_stats ss ON true
            LEFT JOIN category_counts cc ON true
            WHERE sl.id = :locationId
              AND sl.tenant_id = current_setting('app.current_tenant')
            """;

        Query query = em.createNativeQuery(sql)
                .setParameter("locationId", locationId);

        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return null;
        }

        return mapToLocationDetailRow(results.get(0));
    }

    /**
     * Get paginated photo history for a location.
     *
     * @param locationId Storage location UUID
     * @param page       Page number (1-indexed)
     * @param perPage    Items per page
     * @return List of LocationHistoryRow records
     */
    @SuppressWarnings("unchecked")
    public List<LocationHistoryRow> getLocationHistory(UUID locationId, int page, int perPage) {
        int offset = (page - 1) * perPage;

        String sql = """
            SELECT
                ps.created_at as fecha,
                ps.id as session_id,
                COALESCE(SUM(e.estimated_count), 0) as cantidad_final,
                i.thumbnail_url as photo_storage_key
            FROM photo_processing_sessions ps
            LEFT JOIN estimations e ON e.session_id = ps.id AND e.estimation_type = 'COUNT'
            LEFT JOIN images i ON i.session_id = ps.id
            WHERE ps.storage_location_id = :locationId
              AND ps.tenant_id = current_setting('app.current_tenant')
              AND ps.status = 'COMPLETED'
            GROUP BY ps.id, ps.created_at, i.thumbnail_url
            ORDER BY ps.created_at DESC
            LIMIT :limit OFFSET :offset
            """;

        Query query = em.createNativeQuery(sql)
                .setParameter("locationId", locationId)
                .setParameter("limit", perPage)
                .setParameter("offset", offset);

        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(this::mapToLocationHistoryRow)
                .toList();
    }

    /**
     * Count total sessions for a location (for pagination).
     *
     * @param locationId Storage location UUID
     * @return Total session count
     */
    public int countLocationSessions(UUID locationId) {
        String sql = """
            SELECT COUNT(*)
            FROM photo_processing_sessions ps
            WHERE ps.storage_location_id = :locationId
              AND ps.tenant_id = current_setting('app.current_tenant')
              AND ps.status = 'COMPLETED'
            """;

        Query query = em.createNativeQuery(sql)
                .setParameter("locationId", locationId);

        Number count = (Number) query.getSingleResult();
        return count.intValue();
    }

    // =========================================================================
    // Row mapping helpers
    // =========================================================================

    private BulkLoadRow mapToBulkLoadRow(Object[] row) {
        return new BulkLoadRow(
                toUUID(row[0]),   // warehouse_id
                toString(row[1]),  // warehouse_code
                toString(row[2]),  // warehouse_name
                toUUID(row[3]),   // area_id
                toString(row[4]),  // area_code
                toString(row[5]),  // area_name
                toString(row[6]),  // area_position
                toUUID(row[7]),   // location_id
                toString(row[8]),  // location_code
                toString(row[9]),  // location_name
                toInteger(row[10]), // current_quantity
                toInteger(row[11]), // previous_quantity
                toInstant(row[12]), // last_photo_date
                toInteger(row[13]), // days_without_update
                toFloat(row[14]),   // quality_score
                toString(row[15]),   // status
                toInteger(row[16]), // total_cactus
                toInteger(row[17]), // total_suculentas
                toInteger(row[18]), // total_injertos
                toInteger(row[19])  // total_empty_containers
        );
    }

    private LocationDetailRow mapToLocationDetailRow(Object[] row) {
        return new LocationDetailRow(
                toUUID(row[0]),     // location_id
                toString(row[1]),    // location_code
                toString(row[2]),    // location_name
                toString(row[3]),    // area_position
                toUUID(row[4]),     // session_id
                toString(row[5]),    // session_status
                toInteger(row[6]),  // total_detected
                toInteger(row[7]),  // total_empty_containers
                toFloat(row[8]),    // avg_confidence
                toInstant(row[9]),  // session_created_at
                toInteger(row[10]), // days_without_update
                toInteger(row[11]), // total_cactus
                toInteger(row[12]), // total_suculentas
                toInteger(row[13])  // total_injertos
        );
    }

    private LocationHistoryRow mapToLocationHistoryRow(Object[] row) {
        return new LocationHistoryRow(
                toInstant(row[0]),  // fecha
                toUUID(row[1]),     // session_id
                toInteger(row[2]),  // cantidad_final
                toString(row[3])     // photo_storage_key
        );
    }

    private String toString(Object value) {
        if (value == null) return null;
        if (value instanceof String str) return str;
        if (value instanceof Character ch) return ch.toString();
        return value.toString();
    }

    private UUID toUUID(Object value) {
        if (value == null) return null;
        if (value instanceof UUID uuid) return uuid;
        return UUID.fromString(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number num) return num.intValue();
        return Integer.parseInt(value.toString());
    }

    private Float toFloat(Object value) {
        if (value == null) return null;
        if (value instanceof Number num) return num.floatValue();
        if (value instanceof BigDecimal bd) return bd.floatValue();
        return Float.parseFloat(value.toString());
    }

    private Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp ts) return ts.toInstant();
        if (value instanceof Instant inst) return inst;
        return null;
    }

    // =========================================================================
    // Row record types
    // =========================================================================

    public record BulkLoadRow(
            UUID warehouseId,
            String warehouseCode,
            String warehouseName,
            UUID areaId,
            String areaCode,
            String areaName,
            String areaPosition,
            UUID locationId,
            String locationCode,
            String locationName,
            Integer currentQuantity,
            Integer previousQuantity,
            Instant lastPhotoDate,
            Integer daysWithoutUpdate,
            Float qualityScore,
            String status,
            Integer totalCactus,
            Integer totalSuculentas,
            Integer totalInjertos,
            Integer totalEmptyContainers
    ) {}

    public record LocationDetailRow(
            UUID locationId,
            String locationCode,
            String locationName,
            String areaPosition,
            UUID sessionId,
            String sessionStatus,
            Integer totalDetected,
            Integer totalEmptyContainers,
            Float avgConfidence,
            Instant sessionCreatedAt,
            Integer daysWithoutUpdate,
            Integer totalCactus,
            Integer totalSuculentas,
            Integer totalInjertos
    ) {}

    public record LocationHistoryRow(
            Instant fecha,
            UUID sessionId,
            Integer cantidadFinal,
            String photoStorageKey
    ) {}
}
