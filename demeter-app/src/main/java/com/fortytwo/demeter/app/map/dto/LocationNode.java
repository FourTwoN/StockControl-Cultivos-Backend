package com.fortytwo.demeter.app.map.dto;

import java.util.UUID;

/**
 * Storage location node in the map hierarchy.
 */
public record LocationNode(
        UUID locationId,
        String code,
        String name,
        LocationPreview preview
) {}
