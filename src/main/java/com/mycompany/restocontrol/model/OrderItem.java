package com.mycompany.restocontrol.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class OrderItem {

    private final String name;
    private final int quantity;
    private final BigDecimal unitPrice;

    public OrderItem(String name, int quantity, BigDecimal unitPrice) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El producto debe tener nombre.");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof OrderItem item)) {
            return false;
        }
        return quantity == item.quantity
                && name.equals(item.name)
                && unitPrice.equals(item.unitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, quantity, unitPrice);
    }
}
