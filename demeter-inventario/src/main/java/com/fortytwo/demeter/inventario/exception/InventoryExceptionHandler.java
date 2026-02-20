package com.fortytwo.demeter.inventario.exception;

import com.fortytwo.demeter.common.exception.ErrorResponse;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.logging.Logger;

/**
 * Exception handlers for inventory-specific business exceptions.
 * Uses RESTEasy Reactive's @ServerExceptionMapper for type-safe handling.
 */
public class InventoryExceptionHandler {

    private static final Logger LOG = Logger.getLogger(InventoryExceptionHandler.class);

    @ServerExceptionMapper
    public Response handleInactiveBatch(InactiveBatchException e) {
        LOG.warnf("Batch inactive: %s", e.getMessage());
        return Response.status(Response.Status.CONFLICT)  // 409
                .entity(new ErrorResponse(409, "BATCH_INACTIVE", e.getMessage()))
                .build();
    }

    @ServerExceptionMapper
    public Response handleInsufficientStock(InsufficientStockException e) {
        LOG.warnf("Insufficient stock: %s", e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)  // 400
                .entity(new ErrorResponse(400, "INSUFFICIENT_STOCK", e.getMessage()))
                .build();
    }

    @ServerExceptionMapper
    public Response handleIllegalArgument(IllegalArgumentException e) {
        LOG.warnf("Invalid argument: %s", e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)  // 400
                .entity(new ErrorResponse(400, "INVALID_ARGUMENT", e.getMessage()))
                .build();
    }
}
