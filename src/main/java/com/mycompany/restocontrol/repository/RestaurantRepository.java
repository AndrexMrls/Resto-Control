package com.mycompany.restocontrol.repository;

import com.mycompany.restocontrol.model.CashClosure;
import com.mycompany.restocontrol.model.OrderItem;
import com.mycompany.restocontrol.model.OrderStatus;
import com.mycompany.restocontrol.model.Pedido;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RestaurantRepository {

    private static final int FILE_VERSION = 1;
    private static final int MAX_RECORDS = 100_000;
    private static final int MAX_ITEMS_PER_ORDER = 1_000;
    private final Path dataFile = Path.of(System.getProperty("user.home"), ".restocontrol", "restocontrol.dat");
    private List<Pedido> orders;
    private List<CashClosure> closures;

    public RestaurantRepository() throws IOException {
        load();
    }

    public synchronized List<Pedido> getOrders() {
        return List.copyOf(orders);
    }

    public synchronized List<CashClosure> getClosures() {
        return List.copyOf(closures);
    }

    public synchronized void addOrder(Pedido order) throws IOException {
        List<Pedido> updatedOrders = new ArrayList<>(orders);
        updatedOrders.add(order);
        save(updatedOrders, closures);
        orders = updatedOrders;
    }

    public synchronized void updateOrder(Pedido order) throws IOException {
        int index = indexOf(order.getId());
        if (index < 0) {
            throw new IllegalArgumentException("No se encontró el pedido seleccionado.");
        }
        List<Pedido> updatedOrders = new ArrayList<>(orders);
        updatedOrders.set(index, order);
        save(updatedOrders, closures);
        orders = updatedOrders;
    }

    public synchronized void removeOrder(UUID id) throws IOException {
        List<Pedido> updatedOrders = new ArrayList<>(orders);
        if (!updatedOrders.removeIf(order -> order.getId().equals(id))) {
            throw new IllegalArgumentException("No se encontró el pedido seleccionado.");
        }
        save(updatedOrders, closures);
        orders = updatedOrders;
    }

    public synchronized void addClosure(CashClosure closure) throws IOException {
        List<CashClosure> updatedClosures = new ArrayList<>(closures);
        updatedClosures.add(closure);
        save(orders, updatedClosures);
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

    private void load() throws IOException {
        if (!Files.exists(dataFile)) {
            orders = new ArrayList<>();
            closures = new ArrayList<>();
            return;
        }
        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(dataFile)))) {
            int version = input.readInt();
            if (version != FILE_VERSION) {
                throw new IOException("La versión de los datos guardados no es compatible.");
            }
            orders = new ArrayList<>();
            int orderCount = readCount(input, MAX_RECORDS, "pedidos");
            for (int i = 0; i < orderCount; i++) {
                orders.add(readOrder(input));
            }
            closures = new ArrayList<>();
            int closureCount = readCount(input, MAX_RECORDS, "cierres");
            for (int i = 0; i < closureCount; i++) {
                closures.add(readClosure(input));
            }
            if (input.read() != -1) {
                throw new IOException("El archivo de datos contiene información adicional no reconocida.");
            }
        } catch (IOException | RuntimeException ex) {
            throw new IOException("No se pudieron leer los datos guardados en " + dataFile + ": "
                    + ex.getMessage(), ex);
        }
    }

    private void save(List<Pedido> ordersToSave, List<CashClosure> closuresToSave) throws IOException {
        Path parent = dataFile.getParent();
        Files.createDirectories(parent);
        Path tempFile = Files.createTempFile(parent, "restocontrol-", ".tmp");
        try {
            try (DataOutputStream output = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
                output.writeInt(FILE_VERSION);
                output.writeInt(ordersToSave.size());
                for (Pedido order : ordersToSave) {
                    writeOrder(output, order);
                }
                output.writeInt(closuresToSave.size());
                for (CashClosure closure : closuresToSave) {
                    writeClosure(output, closure);
                }
            }
            try {
                Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void writeOrder(DataOutputStream output, Pedido order) throws IOException {
        writeUuid(output, order.getId());
        output.writeInt(order.getTableNumber());
        writeDateTime(output, order.getCreatedAt());
        output.writeUTF(order.getStatus().name());
        output.writeUTF(order.getNote());
        output.writeInt(order.getItems().size());
        for (OrderItem item : order.getItems()) {
            output.writeUTF(item.getName());
            output.writeInt(item.getQuantity());
            output.writeUTF(item.getUnitPrice().toPlainString());
        }
    }

    private static Pedido readOrder(DataInputStream input) throws IOException {
        UUID id = readUuid(input);
        int tableNumber = input.readInt();
        LocalDateTime createdAt = readDateTime(input);
        OrderStatus status = OrderStatus.valueOf(input.readUTF());
        String note = input.readUTF();
        int itemCount = readCount(input, MAX_ITEMS_PER_ORDER, "productos del pedido");
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(new OrderItem(input.readUTF(), input.readInt(), new BigDecimal(input.readUTF())));
        }
        return new Pedido(id, tableNumber, createdAt, items, note, status);
    }

    private static void writeClosure(DataOutputStream output, CashClosure closure) throws IOException {
        writeUuid(output, closure.getId());
        writeDateTime(output, closure.getClosedAt());
        output.writeUTF(closure.getTotal().toPlainString());
        output.writeInt(closure.getOrderCount());
        output.writeInt(closure.getOrderIds().size());
        for (UUID orderId : closure.getOrderIds()) {
            writeUuid(output, orderId);
        }
    }

    private static CashClosure readClosure(DataInputStream input) throws IOException {
        UUID id = readUuid(input);
        LocalDateTime closedAt = readDateTime(input);
        BigDecimal total = new BigDecimal(input.readUTF());
        int orderCount = input.readInt();
        int idCount = readCount(input, MAX_RECORDS, "pedidos del cierre");
        List<UUID> orderIds = new ArrayList<>();
        for (int i = 0; i < idCount; i++) {
            orderIds.add(readUuid(input));
        }
        if (orderCount < 0) {
            throw new IOException("El archivo contiene un cierre inválido.");
        }
        return new CashClosure(id, closedAt, total, orderCount, orderIds);
    }

    private static int readCount(DataInputStream input, int max, String description) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > max) {
            throw new IOException("La cantidad de " + description + " guardada no es válida.");
        }
        return count;
    }

    private static void writeUuid(DataOutputStream output, UUID id) throws IOException {
        output.writeLong(id.getMostSignificantBits());
        output.writeLong(id.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream input) throws IOException {
        return new UUID(input.readLong(), input.readLong());
    }

    private static void writeDateTime(DataOutputStream output, LocalDateTime dateTime) throws IOException {
        output.writeUTF(dateTime.toString());
    }

    private static LocalDateTime readDateTime(DataInputStream input) throws IOException {
        return LocalDateTime.parse(input.readUTF());
    }
}
