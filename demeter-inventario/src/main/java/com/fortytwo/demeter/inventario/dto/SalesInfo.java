package com.fortytwo.demeter.inventario.dto;

/**
 * Information about quantity changes between cycles.
 *
 * @param previousQty Quantity in the previous cycle
 * @param newQty Quantity detected in the new cycle
 * @param diff Difference (previousQty - newQty, positive means sales)
 * @param type Type of change: "ventas", "plantado_no_registrado", "sin_cambio"
 * @param movementCreated Whether a stock movement was created for this change
 */
public record SalesInfo(
    int previousQty,
    int newQty,
    int diff,
    String type,
    boolean movementCreated
) {
    public static final String TYPE_VENTAS = "ventas";
    public static final String TYPE_PLANTADO_NO_REGISTRADO = "plantado_no_registrado";
    public static final String TYPE_SIN_CAMBIO = "sin_cambio";

    public static SalesInfo sales(int previousQty, int newQty, boolean movementCreated) {
        return new SalesInfo(previousQty, newQty, previousQty - newQty, TYPE_VENTAS, movementCreated);
    }

    public static SalesInfo unregisteredPlanting(int previousQty, int newQty) {
        return new SalesInfo(previousQty, newQty, newQty - previousQty, TYPE_PLANTADO_NO_REGISTRADO, false);
    }

    public static SalesInfo noChange(int quantity) {
        return new SalesInfo(quantity, quantity, 0, TYPE_SIN_CAMBIO, false);
    }
}
