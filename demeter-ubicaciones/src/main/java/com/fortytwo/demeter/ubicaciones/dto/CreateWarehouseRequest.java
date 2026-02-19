package com.fortytwo.demeter.ubicaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request to create a new warehouse.
 *
 * @param code Optional warehouse code (auto-generated if not provided)
 * @param name Required warehouse name
 * @param address Optional address
 * @param latitude Optional latitude coordinate
 * @param longitude Optional longitude coordinate
 */
public record CreateWarehouseRequest(
        @Size(min = 2, max = 50) String code,
        @NotBlank String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {}
