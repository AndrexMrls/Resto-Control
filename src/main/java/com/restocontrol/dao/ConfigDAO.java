package com.restocontrol.dao;

import com.mycompany.restocontrol.repository.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class ConfigDAO {

    private static ConfigDAO instance;

    private String nombreRestaurante = "RestoControl Bistro & Bar";
    private String nit = "901.458.732-1";
    private String direccion = "Cra. 43A # 1-50, El Poblado, Medellín";
    private String telefono = "(604) 444-8899";
    private String resolucionDian = "Resolución DIAN No. 18764000123";
    private String rangoFacturacion = "Prefijo POS 001 - 10000";
    private String regimen = "IVA Responsable - Régimen Común";
    private String mensajePie = "¡Gracias por su visita! Vuelva pronto.";

    public ConfigDAO() {
        cargarConfiguracion();
    }

    public static synchronized ConfigDAO getInstance() {
        if (instance == null) {
            instance = new ConfigDAO();
        }
        return instance;
    }

    private void cargarConfiguracion() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // Crear tabla si no existe
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS configuracion (
                    clave VARCHAR(50) PRIMARY KEY,
                    valor VARCHAR(255) NOT NULL
                )
            """);

            Map<String, String> dbConfig = new HashMap<>();
            try (ResultSet rs = stmt.executeQuery("SELECT clave, valor FROM configuracion")) {
                while (rs.next()) {
                    dbConfig.put(rs.getString("clave"), rs.getString("valor"));
                }
            }

            if (dbConfig.isEmpty()) {
                // Insertar defaults
                String insertSql = "INSERT INTO configuracion (clave, valor) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    insertClave(ps, "nombre_restaurante", nombreRestaurante);
                    insertClave(ps, "nit", nit);
                    insertClave(ps, "direccion", direccion);
                    insertClave(ps, "telefono", telefono);
                    insertClave(ps, "resolucion_dian", resolucionDian);
                    insertClave(ps, "rango_facturacion", rangoFacturacion);
                    insertClave(ps, "regimen", regimen);
                    insertClave(ps, "mensaje_pie", mensajePie);
                    ps.executeBatch();
                }
            } else {
                if (dbConfig.containsKey("nombre_restaurante")) nombreRestaurante = dbConfig.get("nombre_restaurante");
                if (dbConfig.containsKey("nit")) nit = dbConfig.get("nit");
                if (dbConfig.containsKey("direccion")) direccion = dbConfig.get("direccion");
                if (dbConfig.containsKey("telefono")) telefono = dbConfig.get("telefono");
                if (dbConfig.containsKey("resolucion_dian")) resolucionDian = dbConfig.get("resolucion_dian");
                if (dbConfig.containsKey("rango_facturacion")) rangoFacturacion = dbConfig.get("rango_facturacion");
                if (dbConfig.containsKey("regimen")) regimen = dbConfig.get("regimen");
                if (dbConfig.containsKey("mensaje_pie")) mensajePie = dbConfig.get("mensaje_pie");
            }
        } catch (SQLException ex) {
            System.err.println("Aviso: usando configuración por defecto de restaurante (" + ex.getMessage() + ")");
        }
    }

    private void insertClave(PreparedStatement ps, String clave, String valor) throws SQLException {
        ps.setString(1, clave);
        ps.setString(2, valor);
        ps.addBatch();
    }

    public synchronized void guardarClave(String clave, String valor) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("MERGE INTO configuracion (clave, valor) KEY(clave) VALUES (?, ?)")) {
            ps.setString(1, clave);
            ps.setString(2, valor);
            ps.executeUpdate();
            cargarConfiguracion();
        } catch (SQLException ex) {
            System.err.println("Error guardando clave de configuración: " + ex.getMessage());
        }
    }

    public String getNombreRestaurante() {
        return nombreRestaurante;
    }

    public void setNombreRestaurante(String nombreRestaurante) {
        this.nombreRestaurante = nombreRestaurante;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getResolucionDian() {
        return resolucionDian;
    }

    public void setResolucionDian(String resolucionDian) {
        this.resolucionDian = resolucionDian;
    }

    public String getRangoFacturacion() {
        return rangoFacturacion;
    }

    public void setRangoFacturacion(String rangoFacturacion) {
        this.rangoFacturacion = rangoFacturacion;
    }

    public String getRegimen() {
        return regimen;
    }

    public void setRegimen(String regimen) {
        this.regimen = regimen;
    }

    public String getMensajePie() {
        return mensajePie;
    }

    public void setMensajePie(String mensajePie) {
        this.mensajePie = mensajePie;
    }
}
