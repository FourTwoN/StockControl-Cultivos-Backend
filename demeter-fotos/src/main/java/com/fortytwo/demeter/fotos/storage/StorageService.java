package com.fortytwo.demeter.fotos.storage;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Abstract interface for cloud storage operations.
 *
 * <p>Implementations can target different storage backends:
 * <ul>
 *   <li>{@code LocalStorageService} - Local filesystem (development)
 *   <li>{@code GcsStorageService} - Google Cloud Storage (production)
 *   <li>{@code S3StorageService} - Amazon S3 (future)
 * </ul>
 *
 * <p>This abstraction allows switching storage providers via configuration
 * without changing application code. Despite the name, this service handles
 * all types of files, not just images.
 */
public interface StorageService {

    /**
     * Upload data to storage.
     *
     * @param data        File bytes
     * @param path        Storage path (e.g., "sessions/{sessionId}/images/{imageId}.jpg")
     * @param contentType MIME type (e.g., "image/jpeg", "application/pdf")
     * @return Storage URL or path that can be used to retrieve the file
     */
    String upload(byte[] data, String path, String contentType);

    /**
     * Generate a signed URL for reading a file.
     *
     * <p>The URL will be valid for the specified duration and can be used
     * directly in browser (e.g., in img src attribute or download link).
     *
     * @param storagePath Path returned from upload()
     * @param expiration  How long the URL should be valid
     * @return Signed URL for direct browser access
     */
    String generateReadUrl(String storagePath, Duration expiration);

    /**
     * Generate signed URLs for multiple files in batch.
     *
     * <p>Default implementation calls {@link #generateReadUrl} sequentially.
     * Implementations may override for parallel processing.
     *
     * @param storagePaths List of paths returned from upload()
     * @param expiration   How long the URLs should be valid
     * @return Map of storage path to signed URL
     */
    default Map<String, String> generateReadUrlsBatch(List<String> storagePaths, Duration expiration) {
        return storagePaths.stream()
                .collect(Collectors.toMap(
                        path -> path,
                        path -> {
                            try {
                                return generateReadUrl(path, expiration);
                            } catch (Exception e) {
                                // Return empty string for failed URLs - caller should handle
                                return "";
                            }
                        }
                ));
    }

    /**
     * Generate a signed URL for uploading directly to storage.
     *
     * <p>Useful for large files where client uploads directly to storage
     * without going through the backend.
     *
     * @param path        Target storage path
     * @param contentType Expected MIME type
     * @param expiration  How long the URL should be valid
     * @return Signed URL for direct upload
     */
    String generateUploadUrl(String path, String contentType, Duration expiration);

    /**
     * Download file data from storage.
     *
     * @param storagePath Path returned from upload()
     * @return File bytes, or empty if not found
     */
    Optional<byte[]> download(String storagePath);

    /**
     * Delete a file from storage.
     *
     * @param storagePath Path returned from upload()
     * @return true if deleted, false if not found
     */
    boolean delete(String storagePath);

    /**
     * Check if a file exists in storage.
     *
     * @param storagePath Path to check
     * @return true if exists
     */
    boolean exists(String storagePath);

    /**
     * Get the storage provider name (for logging/debugging).
     *
     * @return Provider name (e.g., "local", "gcs", "s3")
     */
    String getProviderName();
}
