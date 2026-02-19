package com.fortytwo.demeter.ubicaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a new storage location.
 *
 * @param code Optional location code (auto-generated if not provided)
 * @param name Required location name
 * @param description Optional description
 */
public record CreateStorageLocationRequest(
        @Size(min = 2, max = 50) String code,
        @NotBlank String name,
        String description
) {}
