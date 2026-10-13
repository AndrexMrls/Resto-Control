package com.restocontrol.dao;

import com.mycompany.restocontrol.repository.DatabaseManager;
import com.restocontrol.model.PedidoKDS;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PedidoDAO {

    private static PedidoDAO instance;

    public PedidoDAO() {
    }

    public static synchronized PedidoDAO getInstance() {
        if (instance == null) {
            instance = new PedidoDAO();
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    public List<PedidoKDS> obtenerPedidosKDS() {
        List<PedidoKDS> list = new ArrayList<>();
        String sql = "SELECT id, mesa, fecha, productos, notas, estado, total, usuario_id, turno_id "
                   + "FROM pedidos "
                   + "WHERE estado IN ('POR_PREPARAR', 'EN_PREPARACION', 'LISTO') "
                   + "ORDER BY fecha ASC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("fecha");
                LocalDateTime fecha = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                list.add(new PedidoKDS(
                        rs.getString("id"),
                        rs.getInt("mesa"),
                        fecha,
                        rs.getString("productos"),
                        rs.getString("notas"),
                        rs.getString("estado"),
                        rs.getBigDecimal("total"),
                        rs.getInt("usuario_id"),
                        rs.getInt("turno_id")
                ));
            }
        } catch (SQLException ex) {
            System.err.println("Error al consultar pedidos KDS: " + ex.getMessage());
        }

        // Si la tabla está vacía de pedidos activos en primer inicio, inicializamos los 4 pedidos de muestra
        if (list.isEmpty()) {
            restablecerPedidosDemo();
            return obtenerPedidosKDSInterno();
        }

        return list;
    }

    private List<PedidoKDS> obtenerPedidosKDSInterno() {
        List<PedidoKDS> list = new ArrayList<>();
        String sql = "SELECT id, mesa, fecha, productos, notas, estado, total, usuario_id, turno_id "
                   + "FROM pedidos "
                   + "WHERE estado IN ('POR_PREPARAR', 'EN_PREPARACION', 'LISTO') "
                   + "ORDER BY fecha ASC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("fecha");
                LocalDateTime fecha = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                list.add(new PedidoKDS(
                        rs.getString("id"),
                        rs.getInt("mesa"),
                        fecha,
                        rs.getString("productos"),
                        rs.getString("notas"),
                        rs.getString("estado"),
                        rs.getBigDecimal("total"),
                        rs.getInt("usuario_id"),
                        rs.getInt("turno_id")
                ));
            }
        } catch (SQLException ignored) {
        }
        return list;
    }

    public boolean actualizarEstado(String id, String nuevoEstado) {
        String sql = "UPDATE pedidos SET estado = ? WHERE id = ? OR id LIKE ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, id);
            ps.setString(3, id + "%");
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Error al actualizar estado de pedido " + id + ": " + ex.getMessage());
            return false;
        }
    }

    public boolean prepararPedido(String id) {
        return actualizarEstado(id, "EN_PREPARACION");
    }

    public boolean marcarListo(String id) {
        return actualizarEstado(id, "LISTO");
    }

    public boolean entregarPedidoYLiberarMesa(String id, int mesaNumero) {
        boolean ok = actualizarEstado(id, "ENTREGADO");
        liberarMesa(mesaNumero);
        return ok;
    }

    public void liberarMesa(int mesaNumero) {
        String sql = "UPDATE mesas SET estado = 'DISPONIBLE' WHERE numero = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mesaNumero);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error al liberar mesa " + mesaNumero + ": " + ex.getMessage());
        }
    }

    public void ocuparMesa(int mesaNumero) {
        String sql = "UPDATE mesas SET estado = 'OCUPADA' WHERE numero = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mesaNumero);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error al ocupar mesa " + mesaNumero + ": " + ex.getMessage());
        }
    }

    public boolean agregarPedido(PedidoKDS pedido) {
        String sql = "MERGE INTO pedidos (id, mesa, fecha, productos, notas, estado, total, usuario_id, turno_id) "
                   + "KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pedido.getId());
            ps.setInt(2, pedido.getMesa());
            ps.setTimestamp(3, Timestamp.valueOf(pedido.getFecha()));
            ps.setString(4, pedido.getProductos());
            ps.setString(5, pedido.getNotas());
            ps.setString(6, pedido.getEstado());
            ps.setBigDecimal(7, pedido.getTotal());
            ps.setInt(8, pedido.getUsuarioId());
            ps.setInt(9, pedido.getTurnoId());
            boolean res = ps.executeUpdate() > 0;
            ocuparMesa(pedido.getMesa());
            return res;
        } catch (SQLException ex) {
            System.err.println("Error al agregar pedido: " + ex.getMessage());
            return false;
        }
    }

    public PedidoKDS agregarPedidoSimulado() {
        String randomHex = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int[] mesasDisponibles = {1, 3, 5, 6, 7, 9, 10, 12};
        int mesa = mesasDisponibles[(int) (Math.random() * mesasDisponibles.length)];
        String[] productosEjemplo = {
            "Ceviche de camarón x1, Jugo de corozo x1",
            "Pescado frito con arroz de coco x1, Limonada de coco x1",
            "Arepa de huevo x2, Gaseosa x2",
            "Sancocho de pescado x1, Agua de panela con limón x1"
        };
        String[] notasEjemplo = {
            "Sin cebolla, picante suave",
            "Bien tostado el patacón",
            "Para compartir, servir caliente",
            "Sin sal añadida en el pescado"
        };
        int idx = (int) (Math.random() * productosEjemplo.length);

        PedidoKDS nuevo = new PedidoKDS(
                randomHex,
                mesa,
                LocalDateTime.now(),
                productosEjemplo[idx],
                notasEjemplo[idx],
                "POR_PREPARAR",
                new BigDecimal("42000"),
                2,
                1
        );
        agregarPedido(nuevo);
        return nuevo;
    }

    public void restablecerPedidosDemo() {
        LocalDateTime now = LocalDateTime.now();
        try (Connection conn = getConnection()) {
            // Eliminar anteriores o actualizar
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM pedidos WHERE id IN ('CD18E747', 'A82F10B4', 'E59C34D1', 'B209F831')");
            }

            String sql = "INSERT INTO pedidos (id, mesa, fecha, productos, notas, estado, total, usuario_id, turno_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // 1. POR PREPARAR - Mesa 11 (1 min)
                ps.setString(1, "CD18E747");
                ps.setInt(2, 11);
                ps.setTimestamp(3, Timestamp.valueOf(now.minusMinutes(1)));
                ps.setString(4, "Cazuela de mariscos x1");
                ps.setString(5, "Sin cilantro, picante extra");
                ps.setString(6, "POR_PREPARAR");
                ps.setBigDecimal(7, new BigDecimal("40000"));
                ps.setInt(8, 2);
                ps.setInt(9, 1);
                ps.addBatch();

                // 2. EN PREPARACION - Mesa 4 (12 min -> URGENTE)
                ps.setString(1, "A82F10B4");
                ps.setInt(2, 4);
                ps.setTimestamp(3, Timestamp.valueOf(now.minusMinutes(12)));
                ps.setString(4, "Mojarra frita con arroz de coco x2, Limonada de coco x2");
                ps.setString(5, "Bien dorada la mojarra");
                ps.setString(6, "EN_PREPARACION");
                ps.setBigDecimal(7, new BigDecimal("104000"));
                ps.setInt(8, 2);
                ps.setInt(9, 1);
                ps.addBatch();

                // 3. EN PREPARACION - Mesa 8 (18 min -> ROJO & BLINK)
                ps.setString(1, "E59C34D1");
                ps.setInt(2, 8);
                ps.setTimestamp(3, Timestamp.valueOf(now.minusMinutes(18)));
                ps.setString(4, "Arroz de mariscos x1, Jugo de corozo x1");
                ps.setString(5, "Alergia a marisco crudo");
                ps.setString(6, "EN_PREPARACION");
                ps.setBigDecimal(7, new BigDecimal("47000"));
                ps.setInt(8, 2);
                ps.setInt(9, 1);
                ps.addBatch();

                // 4. LISTO - Mesa 2 (6 min)
                ps.setString(1, "B209F831");
                ps.setInt(2, 2);
                ps.setTimestamp(3, Timestamp.valueOf(now.minusMinutes(6)));
                ps.setString(4, "Posta cartagenera con arroz de coco x1");
                ps.setString(5, "");
                ps.setString(6, "LISTO");
                ps.setBigDecimal(7, new BigDecimal("36000"));
                ps.setInt(8, 2);
                ps.setInt(9, 1);
                ps.addBatch();

                ps.executeBatch();
            }

            // Ocupar mesas correspondientes
            ocuparMesa(11);
            ocuparMesa(4);
            ocuparMesa(8);
            ocuparMesa(2);

        } catch (SQLException ex) {
            System.err.println("Error al restablecer pedidos demo: " + ex.getMessage());
        }
    }
}
