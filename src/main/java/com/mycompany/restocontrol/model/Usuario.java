package com.mycompany.restocontrol.model;

public final class Usuario {

    public enum Rol {
        ADMIN, MESERO, CAJERO, CHEF
    }

    private final int id;
    private final String nombre;
    private final String usuario;
    private final String password;
    private final Rol rol;
    private final boolean activo;

    public Usuario(int id, String nombre, String usuario, String password, Rol rol, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
        this.password = password;
        this.rol = rol;
        this.activo = activo;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getPassword() {
        return password;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public String toString() {
        return nombre + " (" + rol + ")";
    }
}
