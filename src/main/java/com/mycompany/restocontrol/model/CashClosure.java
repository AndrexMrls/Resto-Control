package com.mycompany.restocontrol.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class CashClosure {

    private final UUID id;
    private final LocalDateTime closedAt;
    private final BigDecimal total;
    private final int orderCount;
    private final List<UUID> orderIds;

    public CashClosure(BigDecimal total, List<UUID> orderIds) {
        this(UUID.randomUUID(), LocalDateTime.now(), total, countOrders(orderIds), orderIds);
    }

    public CashClosure(UUID id, LocalDateTime closedAt, BigDecimal total, int orderCount, List<UUID> orderIds) {
        if (id == null || closedAt == null || total == null || orderIds == null) {
            throw new IllegalArgumentException("El cierre de caja contiene datos obligatorios vacíos.");
        }
        this.id = id;
        this.closedAt = closedAt;
        this.total = total;
        this.orderCount = orderCount;
        this.orderIds = List.copyOf(orderIds);
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public List<UUID> getOrderIds() {
        return orderIds;
    }

    private static int countOrders(List<UUID> orderIds) {
        if (orderIds == null) {
            throw new IllegalArgumentException("La lista de pedidos del cierre no puede estar vacía.");
        }
        return orderIds.size();
    }
}
