package com.mycompany.restocontrol.model;

import java.time.Duration;
import java.time.LocalDateTime;

public final class Mesa {

    private final int number;
    private final boolean occupied;
    private final boolean outOfService;
    private final Pedido activeOrder;

    public Mesa(int number, boolean occupied, boolean outOfService, Pedido activeOrder) {
        this.number = number;
        this.occupied = occupied;
        this.outOfService = outOfService;
        this.activeOrder = activeOrder;
    }

    public int getNumber() {
        return number;
    }

    public String getFormattedNumber() {
        return String.format("Mesa %02d", number);
    }

    public boolean isOccupied() {
        return occupied;
    }

    public boolean isOutOfService() {
        return outOfService;
    }

    public boolean isAvailable() {
        return !occupied && !outOfService;
    }

    public Pedido getActiveOrder() {
        return activeOrder;
    }

    public Pedido getOrder() {
        return activeOrder;
    }

    public long getElapsedMinutes() {
        if (activeOrder == null || activeOrder.getCreatedAt() == null) {
            return 0;
        }
        return Math.max(0, Duration.between(activeOrder.getCreatedAt(), LocalDateTime.now()).toMinutes());
    }

    public String getTimerText() {
        long mins = getElapsedMinutes();
        if (mins < 60) {
            return mins + " min";
        }
        return (mins / 60) + "h " + (mins % 60) + "m";
    }
}
