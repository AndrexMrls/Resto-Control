package com.mycompany.restocontrol.controller;

import com.mycompany.restocontrol.model.CashClosure;
import com.mycompany.restocontrol.model.OrderItem;
import com.mycompany.restocontrol.model.OrderStatus;
import com.mycompany.restocontrol.model.Pedido;
import com.mycompany.restocontrol.repository.RestaurantRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RestaurantController {

    private static final Map<String, BigDecimal> MENU = Map.ofEntries(
            Map.entry("Arepa de huevo", new BigDecimal("8000")),
            Map.entry("Carimañolas de queso", new BigDecimal("7000")),
            Map.entry("Patacón con hogao", new BigDecimal("8000")),
            Map.entry("Ceviche de camarón", new BigDecimal("28000")),
            Map.entry("Pescado frito con arroz de coco y patacón", new BigDecimal("38000")),
            Map.entry("Mojarra frita con arroz de coco", new BigDecimal("40000")),
            Map.entry("Arroz de mariscos", new BigDecimal("38000")),
            Map.entry("Cazuela de mariscos", new BigDecimal("40000")),
            Map.entry("Posta cartagenera con arroz de coco", new BigDecimal("36000")),
            Map.entry("Sancocho de pescado", new BigDecimal("32000")),
            Map.entry("Limonada de coco", new BigDecimal("12000")),
            Map.entry("Jugo de corozo", new BigDecimal("9000")),
            Map.entry("Agua de panela con limón", new BigDecimal("7000")),
            Map.entry("Gaseosa", new BigDecimal("5000")),
            Map.entry("Agua embotellada", new BigDecimal("4000")));

    private final RestaurantRepository repository;

    public RestaurantController(RestaurantRepository repository) {
        this.repository = repository;
    }

    public List<Pedido> getOrders() {
        return repository.getOrders().stream()
                .sorted(Comparator.comparing(Pedido::getCreatedAt).reversed())
                .toList();
    }

    public List<CashClosure> getClosures() {
        return repository.getClosures().stream()
                .sorted(Comparator.comparing(CashClosure::getClosedAt).reversed())
                .toList();
    }

    public Map<String, BigDecimal> getMenu() {
        return MENU;
    }

    public Set<Integer> getOutOfServiceTables() {
        return repository.getOutOfServiceTables();
    }

    public void setOutOfServiceTables(Set<Integer> tableNumbers) throws IOException {
        if (tableNumbers == null || tableNumbers.stream().anyMatch(number -> number < 1 || number > 12)) {
            throw new IllegalArgumentException("Selecciona mesas válidas entre la 1 y la 12.");
        }
        for (int tableNumber : tableNumbers) {
            if (getActiveOrderForTable(tableNumber) != null) {
                throw new IllegalArgumentException(
                        "No se puede dejar fuera de servicio la mesa " + tableNumber
                        + " porque tiene un pedido activo.");
            }
        }
        repository.setOutOfServiceTables(tableNumbers);
    }

    public Pedido createOrder(int tableNumber, Map<String, Integer> quantities, String note) throws IOException {
        if (getOutOfServiceTables().contains(tableNumber)) {
            throw new IllegalArgumentException("La mesa " + tableNumber + " está fuera de servicio.");
        }
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            if (entry.getValue() > 0) {
                BigDecimal unitPrice = MENU.get(entry.getKey());
                if (unitPrice == null) {
                    throw new IllegalArgumentException("El producto seleccionado no pertenece al menú.");
                }
                items.add(new OrderItem(entry.getKey(), entry.getValue(), unitPrice));
            }
        }
        Pedido order = new Pedido(tableNumber, items, note);
        if (getActiveOrderForTable(tableNumber) != null) {
            throw new IllegalArgumentException("La mesa " + tableNumber + " ya tiene un pedido activo.");
        }
        repository.addOrder(order);
        return order;
    }

    public void updateStatus(UUID orderId, OrderStatus status) throws IOException {
        Pedido order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.FINISHED) {
            throw new IllegalArgumentException("No se puede cambiar el estado de un pedido finalizado.");
        }
        Pedido updatedOrder = new Pedido(order.getId(), order.getTableNumber(), order.getCreatedAt(),
                order.getItems(), order.getNote(), status);
        repository.updateOrder(updatedOrder);
    }

    public void removeOrder(UUID orderId) throws IOException {
        Pedido order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Solo se pueden eliminar pedidos cancelados.");
        }
        repository.removeOrder(orderId);
    }

    public Pedido getActiveOrderForTable(int tableNumber) {
        return repository.getOrders().stream()
                .filter(order -> order.getTableNumber() == tableNumber && order.getStatus().occupiesTable())
                .findFirst()
                .orElse(null);
    }

    public List<Pedido> getActiveOrders() {
        return repository.getOrders().stream()
                .filter(order -> order.getStatus().occupiesTable())
                .sorted(Comparator.comparing(Pedido::getCreatedAt).reversed())
                .toList();
    }

    public long getOccupiedTableCount() {
        return repository.getOrders().stream()
                .filter(order -> order.getStatus().occupiesTable())
                .map(Pedido::getTableNumber)
                .distinct()
                .count();
    }

    public BigDecimal getTodaySales() {
        return repository.getOrders().stream()
                .filter(order -> order.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .filter(order -> order.getStatus().isServed())
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPendingCashTotal() {
        return getPendingCashOrders().stream()
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public CashClosure closeCash() throws IOException {
        List<Pedido> pending = getPendingCashOrders();
        if (pending.isEmpty()) {
            throw new IllegalArgumentException("No hay pedidos entregados pendientes para cerrar.");
        }
        BigDecimal total = pending.stream()
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CashClosure closure = new CashClosure(total, pending.stream().map(Pedido::getId).toList());
        repository.addClosure(closure);
        return closure;
    }

    public Map<OrderStatus, Long> getStatusCounts() {
        return repository.getOrders().stream()
                .collect(Collectors.groupingBy(Pedido::getStatus, Collectors.counting()));
    }

    private List<Pedido> getPendingCashOrders() {
        return repository.getOrders().stream()
                .filter(order -> order.getStatus().isServed())
                .filter(order -> repository.getClosures().stream()
                        .noneMatch(closure -> closure.getOrderIds().contains(order.getId())))
                .toList();
    }

    private Pedido findOrder(UUID id) {
        return repository.getOrders().stream()
                .filter(order -> order.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el pedido seleccionado."));
    }
}
