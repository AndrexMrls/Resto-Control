package com.mycompany.restocontrol.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Turno {

    public enum Estado {
        ABIERTO, CERRADO
    }

    private final int id;
    private final int usuarioId;
    private final LocalDate fecha;
    private final String turno;
    private final LocalDateTime horaInicio;
    private final LocalDateTime horaFin;
    private final BigDecimal ventasTurno;
    private final Estado estado;

    public Turno(int id, int usuarioId, LocalDate fecha, String turno,
                 LocalDateTime horaInicio, LocalDateTime horaFin,
                 BigDecimal ventasTurno, Estado estado) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.fecha = fecha;
        this.turno = turno;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.ventasTurno = ventasTurno != null ? ventasTurno : BigDecimal.ZERO;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getTurno() {
        return turno;
    }

    public LocalDateTime getHoraInicio() {
        return horaInicio;
    }

    public LocalDateTime getHoraFin() {
        return horaFin;
    }

    public BigDecimal getVentasTurno() {
        return ventasTurno;
    }

    public Estado getEstado() {
        return estado;
    }
}
