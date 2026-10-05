package com.mycompany.restocontrol.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class Pedido {

    private final UUID id;
    private final int tableNumber;
    private final LocalDateTime createdAt;
    private final List<OrderItem> items;
    private final String note;
    private final OrderStatus status;

    public Pedido(int tableNumber, List<OrderItem> items, String note) {
        this(UUID.randomUUID(), tableNumber, LocalDateTime.now(), items, note, OrderStatus.NEW);
    }

    public Pedido(UUID id, int tableNumber, LocalDateTime createdAt, List<OrderItem> items,
            String note, OrderStatus status) {
        if (tableNumber < 1 || tableNumber > 12) {
            throw new IllegalArgumentException("La mesa debe estar entre 1 y 12.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El pedido debe incluir al menos un producto.");
        }
        if (id == null || createdAt == null || status == null) {
            throw new IllegalArgumentException("El pedido contiene datos obligatorios vacíos.");
        }
        this.id = id;
        this.tableNumber = tableNumber;
        this.createdAt = createdAt;
        this.items = List.copyOf(items);
        this.note = note == null ? "" : note.trim();
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public String getNote() {
        return note;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getProductSummary() {
        return items.stream()
                .map(item -> item.getQuantity() + " x " + item.getName())
                .reduce((first, second) -> first + ", " + second)
                .orElse("");
    }
}
