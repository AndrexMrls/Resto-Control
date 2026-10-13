package com.restocontrol.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PedidoKDS {

    private final String id;
    private final int mesa;
    private final LocalDateTime fecha;
    private final String productos;
    private final String notas;
    private String estado; // "POR_PREPARAR", "EN_PREPARACION", "LISTO", "ENTREGADO"
    private final BigDecimal total;
    private final int usuarioId;
    private final int turnoId;

    public PedidoKDS(String id, int mesa, LocalDateTime fecha, String productos,
                     String notas, String estado, BigDecimal total, int usuarioId, int turnoId) {
        this.id = id != null ? id : "";
        this.mesa = mesa;
        this.fecha = fecha != null ? fecha : LocalDateTime.now();
        this.productos = productos != null ? productos : "";
        this.notas = notas != null ? notas : "";
        this.estado = estado != null ? estado : "POR_PREPARAR";
        this.total = total != null ? total : BigDecimal.ZERO;
        this.usuarioId = usuarioId;
        this.turnoId = turnoId;
    }

    public String getId() {
        return id;
    }

    public int getMesa() {
        return mesa;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getProductos() {
        return productos;
    }

    public String getNotas() {
        return notas;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public int getTurnoId() {
        return turnoId;
    }

    public long getMinutosTranscurridos() {
        return Math.max(0, Duration.between(fecha, LocalDateTime.now()).toMinutes());
    }

    public boolean isUrgente() {
        return getMinutosTranscurridos() > 10;
    }

    public boolean isCritico() {
        return getMinutosTranscurridos() > 15;
    }

    public List<String> getItemsList() {
        List<String> list = new ArrayList<>();
        if (productos == null || productos.isBlank()) {
            return list;
        }
        String[] parts = productos.split(",\\s*");
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }
}
