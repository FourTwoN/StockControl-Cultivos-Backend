package com.fortytwo.demeter.app.map.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * A single period/session in the location history.
 */
public record LocationHistoryItem(
        Instant fecha,
        Instant periodEnd,
        UUID sessionId,
        Integer cantidadInicial,
        Integer muertes,
        Integer trasplantes,
        Integer plantados,
        Integer cantidadVendida,
        Integer cantidadFinal,
        Integer netChange,
        String photoThumbnailUrl,
        String photoStorageKey  // For lazy loading URLs
) {}
