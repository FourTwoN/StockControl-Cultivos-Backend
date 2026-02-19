package com.fortytwo.demeter.app.map.dto;

import java.util.List;
import java.util.UUID;

/**
 * Warehouse node in the map hierarchy.
 */
public record WarehouseNode(
        UUID warehouseId,
        String code,
        String name,
        List<AreaNode> areas
) {}
