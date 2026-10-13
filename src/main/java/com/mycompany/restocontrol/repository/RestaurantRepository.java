package com.mycompany.restocontrol.repository;

import com.mycompany.restocontrol.model.CashClosure;
import com.mycompany.restocontrol.model.OrderItem;
import com.mycompany.restocontrol.model.OrderStatus;
import com.mycompany.restocontrol.model.Pedido;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RestaurantRepository {

    private List<Pedido> orders = new ArrayList<>();
    private List<CashClosure> closures = new ArrayList<>();
    private Set<Integer> outOfServiceTables = Set.of();

    public RestaurantRepository() throws IOException {
        loadOrdersFromDatabase();
    }

    public synchronized void loadOrdersFromDatabase() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT id, mesa, fecha, productos, notas, estado, total, usuario_id, turno_id FROM pedidos WHERE estado NOT IN ('CANCELLED', 'ENTREGADO', 'FINALIZADO')";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                List<Pedido> loaded = new ArrayList<>();
                while (rs.next()) {
                    String idStr = rs.getString("id");
                    int mesa = rs.getInt("mesa");
                    Timestamp ts = rs.getTimestamp("fecha");
                    LocalDateTime fecha = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                    String prodStr = rs.getString("productos");
                    String notas = rs.getString("notas");
                    String est = rs.getString("estado");
                    BigDecimal total = rs.getBigDecimal("total");
                    int usuarioId = rs.getInt("usuario_id");
                    int turnoId = rs.getInt("turno_id");

                    UUID uuid = parseOrGenerateUuid(idStr);
                    OrderStatus status = parseOrderStatus(est);
                    List<OrderItem> items = parseItems(prodStr, total);

                    loaded.add(new Pedido(uuid, mesa, fecha, items, notas, status, usuarioId, turnoId));
                }
                if (!loaded.isEmpty()) {
                    this.orders = loaded;
                }
            }
        } catch (Exception ex) {
            System.err.println("Aviso al cargar pedidos de BD: " + ex.getMessage());
        }
    }

    private static OrderStatus parseOrderStatus(String est) {
        if (est == null) return OrderStatus.NEW;
        return switch (est.toUpperCase()) {
            case "POR_PREPARAR", "NEW" -> OrderStatus.NEW;
            case "EN_PREPARACION", "PREPARING" -> OrderStatus.PREPARING;
            case "LISTO", "READY" -> OrderStatus.READY;
            case "ENTREGADO", "DELIVERED" -> OrderStatus.DELIVERED;
            case "FINALIZADO", "FINISHED" -> OrderStatus.FINISHED;
            case "CANCELADO", "CANCELLED" -> OrderStatus.CANCELLED;
            default -> OrderStatus.NEW;
        };
    }

    private static List<OrderItem> parseItems(String prodStr, BigDecimal total) {
        List<OrderItem> list = new ArrayList<>();
        if (prodStr == null || prodStr.isBlank()) {
            list.add(new OrderItem("Consumo general", 1, total != null ? total : BigDecimal.ZERO));
            return list;
        }
        String[] parts = prodStr.split(",");
        for (String part : parts) {
            part = part.trim();
            int cant = 1;
            String name = part;
            if (part.contains(" x")) {
                String[] p = part.split(" x");
                name = p[0].trim();
                try {
                    cant = Integer.parseInt(p[1].trim());
                } catch (Exception ignored) {
                }
            }
            BigDecimal unitPrice = total != null ? total.divide(BigDecimal.valueOf(Math.max(parts.length * cant, 1)), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            list.add(new OrderItem(name, cant, unitPrice));
        }
        return list;
    }

    public static UUID parseOrGenerateUuid(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(idStr);
        } catch (IllegalArgumentException ex) {
            String hex = idStr.toLowerCase().replaceAll("[^0-9a-f]", "");
            while (hex.length() < 32) {
                hex = hex + "0";
            }
            hex = hex.substring(0, 32);
            String uuidStr = hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" + hex.substring(20, 32);
            return UUID.fromString(uuidStr);
        }
    }

    public synchronized List<Pedido> getOrders() {
        return List.copyOf(orders);
    }

    public synchronized List<CashClosure> getClosures() {
        return List.copyOf(closures);
    }

    public synchronized Set<Integer> getOutOfServiceTables() {
        return outOfServiceTables;
    }

    public synchronized void setOutOfServiceTables(Set<Integer> tableNumbers) throws IOException {
        outOfServiceTables = Set.copyOf(tableNumbers);
    }

    public synchronized void addOrder(Pedido order) throws IOException {
        List<Pedido> updatedOrders = new ArrayList<>(orders);
        updatedOrders.add(order);
        orders = updatedOrders;

        // Sincronizar a BD
        String shortId = order.getId().toString().substring(0, 8).toUpperCase();
        DatabaseManager.getInstance().guardarPedido(
                shortId,
                order.getTableNumber(),
                order.getCreatedAt(),
                order.getProductSummary(),
                order.getNote(),
                order.getStatus().name(),
                order.getTotal(),
                order.getUsuarioId(),
                order.getTurnoId()
        );
        DatabaseManager.getInstance().ocuparMesa(order.getTableNumber());
    }

    public synchronized void updateOrder(Pedido order) throws IOException {
        int index = indexOf(order.getId());
        if (index < 0) {
            throw new IllegalArgumentException("No se encontró el pedido seleccionado.");
        }
        List<Pedido> updatedOrders = new ArrayList<>(orders);
        updatedOrders.set(index, order);
        orders = updatedOrders;

        // Sincronizar estado a BD
        String shortId = order.getId().toString().substring(0, 8).toUpperCase();
        DatabaseManager.getInstance().actualizarEstadoPedido(shortId, order.getStatus().name());
    }

    public synchronized void removeOrder(UUID id) throws IOException {
        List<Pedido> updatedOrders = new ArrayList<>(orders);
        if (!updatedOrders.removeIf(order -> order.getId().equals(id))) {
            throw new IllegalArgumentException("No se encontró el pedido seleccionado.");
        }
        orders = updatedOrders;

        String shortId = id.toString().substring(0, 8).toUpperCase();
        DatabaseManager.getInstance().actualizarEstadoPedido(shortId, "CANCELLED");
    }

    public synchronized void addClosure(CashClosure closure) throws IOException {
        List<CashClosure> updatedClosures = new ArrayList<>(closures);
        updatedClosures.add(closure);
        closures = updatedClosures;
    }

    private int indexOf(UUID id) {
        for (int index = 0; index < orders.size(); index++) {
            if (orders.get(index).getId().equals(id)) {
                return index;
            }
        }
        return -1;
    }
}
