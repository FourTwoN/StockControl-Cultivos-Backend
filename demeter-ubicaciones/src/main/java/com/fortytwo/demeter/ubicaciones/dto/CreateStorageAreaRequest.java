package com.fortytwo.demeter.ubicaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to create a new storage area.
 *
 * @param code Optional area code (auto-generated if not provided)
 * @param name Required area name
 * @param description Optional description
 * @param position Optional cardinal position (N/S/E/W/C)
 */
public record CreateStorageAreaRequest(
        @Size(min = 2, max = 50) String code,
        @NotBlank String name,
        String description,
        @Pattern(regexp = "^[NSEWC]?$", message = "Position must be N, S, E, W, or C") String position
) {}
