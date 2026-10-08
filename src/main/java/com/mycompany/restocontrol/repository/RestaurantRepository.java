package com.mycompany.restocontrol.repository;

import com.mycompany.restocontrol.model.CashClosure;
import com.mycompany.restocontrol.model.Pedido;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RestaurantRepository {

    private List<Pedido> orders = new ArrayList<>();
    private List<CashClosure> closures = new ArrayList<>();
    private Set<Integer> outOfServiceTables = Set.of();

    public RestaurantRepository() throws IOException {
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
    }

    public synchronized void updateOrder(Pedido order) throws IOException {
        int index = indexOf(order.getId());
        if (index < 0) {
            throw new IllegalArgumentException("No se encontró el pedido seleccionado.");
        }
        List<Pedido> updatedOrders = new ArrayList<>(orders);
        updatedOrders.set(index, order);
        orders = updatedOrders;
    }

    public synchronized void removeOrder(UUID id) throws IOException {
        List<Pedido> updatedOrders = new ArrayList<>(orders);
        if (!updatedOrders.removeIf(order -> order.getId().equals(id))) {
            throw new IllegalArgumentException("No se encontró el pedido seleccionado.");
        }
        orders = updatedOrders;
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
