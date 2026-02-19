package com.fortytwo.demeter.app.map.dto;

/**
 * Pagination metadata for paginated responses.
 */
public record Pagination(
        int page,
        int perPage,
        int totalPages,
        int totalItems
) {
    public static Pagination of(int page, int perPage, int totalItems) {
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / perPage);
        return new Pagination(page, perPage, totalPages, totalItems);
    }
}
