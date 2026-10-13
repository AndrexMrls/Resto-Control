package com.mycompany.restocontrol.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SesionActual {

    private static SesionActual instance;

    private Usuario usuario;
    private int turnoId;
    private String tipoTurno = "Mañana";
    private LocalDateTime horaInicio;
    private String caja = "01";

    private SesionActual() {
    }

    public static synchronized SesionActual getInstance() {
        if (instance == null) {
            instance = new SesionActual();
        }
        return instance;
    }

    public synchronized void iniciarSesion(Usuario usuario, int turnoId, String tipoTurno) {
        this.usuario = usuario;
        this.turnoId = turnoId;
        this.tipoTurno = (tipoTurno != null && !tipoTurno.isBlank()) ? tipoTurno : "Mañana";
        this.horaInicio = LocalDateTime.now();
        this.caja = "01";
    }

    public synchronized void cerrarSesion() {
        this.usuario = null;
        this.turnoId = 0;
        this.horaInicio = null;
    }

    public synchronized boolean isAutenticado() {
        return usuario != null;
    }

    public synchronized Usuario getUsuario() {
        return usuario;
    }

    public synchronized Usuario.Rol getRol() {
        return usuario != null ? usuario.getRol() : null;
    }

    public synchronized int getTurnoId() {
        return turnoId;
    }

    public synchronized String getTipoTurno() {
        return tipoTurno;
    }

    public synchronized LocalDateTime getHoraInicio() {
        return horaInicio;
    }

    public synchronized String getCaja() {
        return caja;
    }

    public synchronized void setCaja(String caja) {
        this.caja = caja;
    }

    public synchronized String getSessionHeaderSummary() {
        String nombre = usuario != null ? usuario.getNombre() : "Usuario";
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", new Locale("es", "CO"));
        return "Caja: " + caja + " • Usuario: " + nombre + " • Turno: " + tipoTurno + " • " + now.format(dtf);
    }
}
