package com.fortytwo.demeter.inventario.controller;

import com.fortytwo.demeter.common.auth.CurrentUser;
import com.fortytwo.demeter.common.auth.RoleConstants;
import com.fortytwo.demeter.inventario.dto.*;
import com.fortytwo.demeter.inventario.service.StockMovementService;
import com.fortytwo.demeter.usuarios.model.User;
import com.fortytwo.demeter.usuarios.repository.UserRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

/**
 * REST controller for specialized stock movement operations.
 * Handles muerte, plantado, desplazamiento, and ajuste operations.
 */
@Path("/api/v1/stock/movements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
public class StockOperationsController {

    private static final Logger log = LoggerFactory.getLogger(StockOperationsController.class);

    @Inject
    StockMovementService stockMovementService;

    @Inject
    UserRepository userRepository;

    @Inject
    CurrentUser currentUser;

    private UUID getCurrentUserId() {
        String userId = currentUser.getUserId();
        // Try to parse as UUID first (production case)
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            // Fallback: look up user by externalId (test case)
            return userRepository.findByExternalId(userId)
                .map(User::getId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        }
    }

    /**
     * Register plant mortality (egreso - stock decrease).
     *
     * @param request Muerte request with batchId, quantity, and optional reason
     * @return MuerteResponse with movement details and updated batch quantity
     */
    @POST
    @Path("/muerte")
    public Response registerMuerte(@Valid MuerteRequest request) {
        log.info("Muerte request: batchId={}, quantity={}", request.batchId(), request.quantity());

        MuerteResponse response = stockMovementService.executeMuerte(getCurrentUserId(), request);

        log.info("Muerte completed: movementId={}, newQuantity={}",
            response.movement().id(), response.newQuantity());

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /**
     * Register new planting (ingreso - stock increase).
     *
     * @param request Plantado request with batchId, quantity, and optional reason
     * @return PlantadoResponse with movement details and updated batch quantity
     */
    @POST
    @Path("/plantado")
    public Response registerPlantado(@Valid PlantadoRequest request) {
        log.info("Plantado request: batchId={}, quantity={}", request.batchId(), request.quantity());

        PlantadoResponse response = stockMovementService.executePlantado(getCurrentUserId(), request);

        log.info("Plantado completed: movementId={}, newQuantity={}",
            response.movement().id(), response.newQuantity());

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /**
     * Register displacement/movement operation.
     * Auto-detects operation type by comparing batch configurations:
     * - MOVIMIENTO: Different location, same config
     * - MOVIMIENTO_TRASPLANTE: Different location, different config
     * - TRASPLANTE: Same location, different config
     *
     * @param request Desplazamiento request with sourceBatchId, destinationBatchId, and quantity
     * @return DesplazamientoResponse with operation type, both movements, and batch info
     */
    @POST
    @Path("/desplazamiento")
    public Response registerDesplazamiento(@Valid DesplazamientoRequest request) {
        log.info("Desplazamiento request: source={}, dest={}, quantity={}",
            request.sourceBatchId(), request.destinationBatchId(), request.quantity());

        DesplazamientoResponse response = stockMovementService.executeDesplazamiento(
            getCurrentUserId(), request);

        log.info("Desplazamiento completed: type={}, quantity={}",
            response.operationType(), response.quantity());

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /**
     * Register stock adjustment (correction).
     * Quantity can be positive (add) or negative (subtract), but NOT zero.
     *
     * @param request Ajuste request with batchId, quantity (+/-), and optional reason
     * @return AjusteResponse with movement details and updated batch quantity
     */
    @POST
    @Path("/ajuste")
    public Response registerAjuste(@Valid AjusteRequest request) {
        log.info("Ajuste request: batchId={}, quantity={}", request.batchId(), request.quantity());

        AjusteResponse response = stockMovementService.executeAjuste(getCurrentUserId(), request);

        log.info("Ajuste completed: movementId={}, quantityAdjusted={}, newQuantity={}",
            response.movement().id(), response.quantityAdjusted(), response.newQuantity());

        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
