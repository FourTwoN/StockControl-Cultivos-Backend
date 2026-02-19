package com.fortytwo.demeter.app.map.dto;

import java.util.List;
import java.util.UUID;

/**
 * Storage area node in the map hierarchy.
 */
public record AreaNode(
        UUID areaId,
        String code,
        String name,
        String position,
        List<LocationNode> locations
) {}
