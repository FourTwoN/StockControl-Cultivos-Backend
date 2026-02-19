package com.fortytwo.demeter.analytics.controller;

import com.fortytwo.demeter.analytics.dto.DashboardSummary;
import com.fortytwo.demeter.analytics.dto.InventoryValuation;
import com.fortytwo.demeter.analytics.dto.KpiDTO;
import com.fortytwo.demeter.analytics.dto.LocationOccupancy;
import com.fortytwo.demeter.analytics.dto.MovementHistory;
import com.fortytwo.demeter.analytics.dto.MovementSummary;
import com.fortytwo.demeter.analytics.dto.SalesSummaryDTO;
import com.fortytwo.demeter.analytics.dto.StockHistoryPointDTO;
import com.fortytwo.demeter.analytics.dto.StockSummary;
import com.fortytwo.demeter.analytics.dto.TopProductSales;
import com.fortytwo.demeter.analytics.service.AnalyticsService;
import com.fortytwo.demeter.common.auth.RoleConstants;
import com.fortytwo.demeter.common.dto.PagedResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.List;

@Path("/api/v1/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalyticsController {

    @Inject
    AnalyticsService analyticsService;

    @GET
    @Path("/stock-summary")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public List<StockSummary> getStockSummary() {
        return analyticsService.getStockSummary();
    }

    @GET
    @Path("/movements")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public List<MovementSummary> getMovements(
            @QueryParam("from") String from,
            @QueryParam("to") String to) {
        Instant fromInstant = from != null ? parseDateTime(from, false) : Instant.EPOCH;
        Instant toInstant = to != null ? parseDateTime(to, true) : Instant.now();
        return analyticsService.getMovementsByDateRange(fromInstant, toInstant);
    }

    @GET
    @Path("/inventory-valuation")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public List<InventoryValuation> getInventoryValuation() {
        return analyticsService.getInventoryValuation();
    }

    @GET
    @Path("/top-products")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public List<TopProductSales> getTopProducts(
            @QueryParam("limit") @DefaultValue("10") int limit) {
        return analyticsService.getTopProductsBySales(limit);
    }

    @GET
    @Path("/location-occupancy")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public List<LocationOccupancy> getLocationOccupancy() {
        return analyticsService.getLocationOccupancy();
    }

    @GET
    @Path("/dashboard")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public DashboardSummary getDashboard() {
        return analyticsService.getDashboard();
    }

    @GET
    @Path("/movement-history")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public PagedResponse<MovementHistory> getMovementHistory(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("type") String movementType,
            @QueryParam("from") String from,
            @QueryParam("to") String to) {
        Instant fromInstant = parseDateTime(from, false);
        Instant toInstant = parseDateTime(to, true);
        return analyticsService.getMovementHistory(page, size, movementType, fromInstant, toInstant);
    }

    @GET
    @Path("/kpis")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public List<KpiDTO> getKpis() {
        return analyticsService.getKpis();
    }

    @GET
    @Path("/stock-history")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public List<StockHistoryPointDTO> getStockHistory(
            @QueryParam("from") String from,
            @QueryParam("to") String to) {
        Instant fromInstant = from != null
                ? parseDateTime(from, false)
                : Instant.now().minusSeconds(30L * 24 * 60 * 60);
        Instant toInstant = to != null
                ? parseDateTime(to, true)
                : Instant.now();
        return analyticsService.getStockHistory(fromInstant, toInstant);
    }

    @GET
    @Path("/sales-summary")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public List<SalesSummaryDTO> getSalesSummary(
            @QueryParam("period") @DefaultValue("monthly") String period) {
        return analyticsService.getSalesSummary(period);
    }

    /**
     * Parses a date/datetime string into an Instant.
     * Supports both ISO-8601 datetime (2020-01-01T00:00:00Z) and date-only (2020-01-01) formats.
     */
    private Instant parseDateTime(String dateStr, boolean endOfDay) {
        if (dateStr == null) {
            return null;
        }
        try {
            return Instant.parse(dateStr);
        } catch (DateTimeParseException e) {
            LocalDate date = LocalDate.parse(dateStr);
            if (endOfDay) {
                return date.atStartOfDay(ZoneOffset.UTC).plusDays(1).toInstant();
            }
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        }
    }
}
