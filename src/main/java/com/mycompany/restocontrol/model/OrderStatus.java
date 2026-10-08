package com.mycompany.restocontrol.model;

public enum OrderStatus {
    NEW("Nuevo"),
    PREPARING("En preparación"),
    READY("Listo"),
    DELIVERED("Entregado"),
    FINISHED("Finalizado"),
    CANCELLED("Cancelado");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean occupiesTable() {
        return this == NEW || this == PREPARING || this == READY || this == DELIVERED;
    }

    public boolean isServed() {
        return this == DELIVERED || this == FINISHED;
    }
}
