package com.fortytwo.demeter.app.map.dto;

import java.util.List;

/**
 * Response for the bulk map load endpoint.
 * Contains the complete warehouse hierarchy with location preview metrics.
 */
public record MapBulkLoadResponse(
        List<WarehouseNode> warehouses
) {
    public static MapBulkLoadResponse empty() {
        return new MapBulkLoadResponse(List.of());
    }
}
