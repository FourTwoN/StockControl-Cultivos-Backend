package com.fortytwo.demeter.app.map.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request for batch generation of presigned URLs.
 * Used for lazy loading images in the frontend.
 */
public record PresignedUrlBatchRequest(
        @NotEmpty(message = "Storage keys list cannot be empty")
        @Size(max = 100, message = "Maximum 100 keys per batch request")
        List<String> storageKeys
) {}
