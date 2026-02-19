package com.fortytwo.demeter.inventario.controller;

import com.fortytwo.demeter.common.auth.RoleConstants;
import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.inventario.dto.CreateStockMovementRequest;
import com.fortytwo.demeter.inventario.dto.StockMovementDTO;
import com.fortytwo.demeter.inventario.model.MovementType;
import com.fortytwo.demeter.inventario.service.StockMovementService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/stock-movements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StockMovementController {

    @Inject
    StockMovementService stockMovementService;

    @GET
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public PagedResponse<StockMovementDTO> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("batchId") UUID batchId,
            @QueryParam("type") String type,
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr) {
        Instant startDate = parseDateTime(startDateStr, false);
        Instant endDate = parseDateTime(endDateStr, true);
        return stockMovementService.findAll(page, size, batchId, type, startDate, endDate);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public StockMovementDTO getById(@PathParam("id") UUID id) {
        return stockMovementService.findById(id);
    }

    @GET
    @Path("/by-type/{type}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<StockMovementDTO> getByType(@PathParam("type") MovementType type) {
        return stockMovementService.findByMovementType(type);
    }

    @GET
    @Path("/by-date-range")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<StockMovementDTO> getByDateRange(
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr) {
        Instant from = fromStr != null ? parseDateTime(fromStr, false) : Instant.EPOCH;
        Instant to = toStr != null ? parseDateTime(toStr, true) : Instant.now();
        return stockMovementService.findByDateRange(from, to);
    }

    @GET
    @Path("/by-reference/{referenceId}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<StockMovementDTO> getByReference(@PathParam("referenceId") UUID referenceId) {
        return stockMovementService.findByReferenceId(referenceId);
    }

    @POST
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public Response create(@Valid CreateStockMovementRequest request) {
        StockMovementDTO created = stockMovementService.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    /**
     * Parses a date/datetime string into an Instant.
     * Supports both ISO-8601 datetime (2020-01-01T00:00:00Z) and date-only (2020-01-01) formats.
     *
     * @param dateStr the date string to parse
     * @param endOfDay if true and parsing date-only, returns end of day instead of start
     * @return the parsed Instant, or null if input is null
     */
    private Instant parseDateTime(String dateStr, boolean endOfDay) {
        if (dateStr == null) {
            return null;
        }
        try {
            // Try ISO-8601 datetime format first (e.g., 2020-01-01T00:00:00Z)
            return Instant.parse(dateStr);
        } catch (DateTimeParseException e) {
            // Fallback to date-only format (e.g., 2020-01-01)
            LocalDate date = LocalDate.parse(dateStr);
            if (endOfDay) {
                return date.atStartOfDay(ZoneOffset.UTC).plusDays(1).toInstant();
            }
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        }
    }
}
