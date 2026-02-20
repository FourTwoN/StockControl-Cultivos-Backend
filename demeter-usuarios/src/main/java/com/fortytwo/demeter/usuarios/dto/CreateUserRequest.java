package com.fortytwo.demeter.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
    String externalId,  // Optional: if not provided, a random UUID will be generated
    @NotBlank String email,
    @NotBlank String name,
    String role
) {}
