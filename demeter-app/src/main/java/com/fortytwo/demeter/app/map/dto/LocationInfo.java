package com.fortytwo.demeter.app.map.dto;

import java.util.UUID;

/**
 * Basic location information used in detail and history responses.
 */
public record LocationInfo(
        UUID locationId,
        String code,
        String name
) {}
