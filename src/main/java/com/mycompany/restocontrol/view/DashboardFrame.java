package com.mycompany.restocontrol.view;

import com.mycompany.restocontrol.controller.RestaurantController;
import com.mycompany.restocontrol.model.CashClosure;
import com.mycompany.restocontrol.model.OrderStatus;
import com.mycompany.restocontrol.model.Pedido;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Icon;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public final class DashboardFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color NAVY_LIGHT = new Color(30, 41, 59);
    private static final Color NAVY_HOVER = new Color(34, 46, 68);
    private static final Color BACKGROUND = new Color(245, 247, 250);
    private static final Color PRIMARY = new Color(37, 99, 235);
    private static final Color TEXT = new Color(30, 41, 59);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color ORANGE = new Color(180, 83, 9);
    private static final Color RED = new Color(220, 38, 38);
    private static final Color TABLE_GRAY = new Color(148, 163, 184);
    private static final Set<Integer> OUT_OF_SERVICE_TABLES = Set.of();
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY = createCurrencyFormat();
    private final RestaurantController controller;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageContainer = new JPanel(pageLayout);
    private final Map<String, JButton> navigationButtons = new LinkedHashMap<>();
    private final JLabel pageTitle = new JLabel();
    private final JLabel pageSubtitle = new JLabel();
    private final JPanel sidebar = new JPanel();
    private String activePage = "dashboard";

    public DashboardFrame(RestaurantController controller) {
        this.controller = controller;
        setTitle("RestoControl | Administración del restaurante");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1020, 700));
        setSize(1240, 800);
        setLocationRelativeTo(null);
        setContentPane(buildApplication());
        showPage("dashboard");
    }

    private JPanel buildApplication() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BACKGROUND);
        main.add(buildHeader(), BorderLayout.NORTH);
        pageContainer.setBackground(BACKGROUND);
        main.add(pageContainer, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildSidebar() {
        sidebar.setBackground(NAVY);
        sidebar.setPreferredSize(new Dimension(225, 0));
        sidebar.setBorder(new EmptyBorder(18, 16, 18, 16));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel logo = buildLogo();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(new EmptyBorder(0, 0, 22, 0));
        sidebar.add(logo);
        addNavigation("dashboard", "Inicio");
        addNavigation("orders", "Pedidos");
        addNavigation("tables", "Mesas");
        addNavigation("reports", "Reportes");
        addNavigation("cash", "Cierre de caja");
        sidebar.add(Box.createVerticalGlue());
        JLabel footer = label("Panel de administración", 11, new Color(167, 180, 198), Font.PLAIN);
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.setBorder(new EmptyBorder(10, 0, 4, 0));
        sidebar.add(footer);
        return sidebar;
    }

    private JLabel buildLogo() {
        try (var input = DashboardFrame.class.getResourceAsStream("/images/logo-restocontrol.png")) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el logo en /images/logo-restocontrol.png.");
            }
            Image image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalStateException("El archivo del logo no tiene un formato de imagen válido.");
            }
            int maxWidth = 193;
            int maxHeight = 112;
            double scale = Math.min((double) maxWidth / image.getWidth(null),
                    (double) maxHeight / image.getHeight(null));
            int width = Math.max(1, (int) Math.round(image.getWidth(null) * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight(null) * scale));
            JLabel logo = new JLabel(new javax.swing.ImageIcon(
                    image.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
            logo.setHorizontalAlignment(SwingConstants.CENTER);
            logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, maxHeight + 22));
            return logo;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar el logo de RestoControl.", ex);
        }
    }

    private void addNavigation(String key, String text) {
        JButton button = new JButton(text);
        button.setIcon(new NavigationIcon(key));
        button.setIconTextGap(13);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(new Color(207, 216, 227));
        button.setBackground(NAVY);
        button.setBorder(new EmptyBorder(12, 13, 12, 10));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (!key.equals(activePage)) {
                    button.setBackground(NAVY_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (!key.equals(activePage)) {
                    button.setBackground(NAVY);
                }
            }
        });
        button.addActionListener(event -> showPage(key));
        navigationButtons.put(key, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(5));
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(20, 30, 18, 28));
        JPanel headings = new JPanel();
        headings.setOpaque(false);
        headings.setLayout(new BoxLayout(headings, BoxLayout.Y_AXIS));
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 23));
        pageTitle.setForeground(TEXT);
        pageSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pageSubtitle.setForeground(MUTED);
        headings.add(pageTitle);
        headings.add(Box.createVerticalStrut(4));
        headings.add(pageSubtitle);
        header.add(headings, BorderLayout.CENTER);

        JButton addOrder = primaryButton("+  Nuevo pedido");
        addOrder.addActionListener(event -> openOrderDialog(null));
        header.add(addOrder, BorderLayout.EAST);
        return header;
    }

    private void showPage(String page) {
        activePage = page;
        String title;
        String subtitle;
        switch (page) {
            case "orders" -> {
                title = "Gestión de pedidos";
                subtitle = "Registra pedidos y actualiza su estado de preparación.";
            }
            case "tables" -> {
                title = "Estado de Mesas";
                subtitle = "Consulta la disponibilidad de las 12 mesas del restaurante.";
            }
            case "reports" -> {
                title = "Reportes";
                subtitle = "Resumen de pedidos, ventas y actividad registrada.";
            }
            case "cash" -> {
                title = "Cierre de caja";
                subtitle = "Revisa los pedidos entregados que aún no se han incluido en un cierre.";
            }
            default -> {
                page = "dashboard";
                title = "Panel principal";
                subtitle = "Resumen en tiempo real de la operación del restaurante.";
            }
        }
        pageTitle.setText(title);
        pageSubtitle.setText(subtitle);
        for (Map.Entry<String, JButton> entry : navigationButtons.entrySet()) {
            boolean selected = entry.getKey().equals(page);
            entry.getValue().setBackground(selected ? NAVY_LIGHT : NAVY);
            entry.getValue().setForeground(selected ? Color.WHITE : new Color(207, 216, 227));
        }

        pageContainer.removeAll();
        pageContainer.add(switch (page) {
            case "orders" -> buildOrdersPage();
            case "tables" -> buildTablesPage();
            case "reports" -> buildReportsPage();
            case "cash" -> buildCashPage();
            default -> buildDashboardPage();
        }, page);
        activePage = page;
        pageLayout.show(pageContainer, page);
        pageContainer.revalidate();
        pageContainer.repaint();
    }

    private JPanel buildDashboardPage() {
        JPanel page = pagePanel();
        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 112));
        cards.add(statCard("Ventas de hoy", money(controller.getTodaySales()), "Pedidos entregados", GREEN));
        cards.add(statCard("Pedidos activos", String.valueOf(controller.getActiveOrders().size()),
                "En proceso en cocina", PRIMARY));
        cards.add(statCard("Mesas ocupadas", controller.getOccupiedTableCount() + " / 12",
                "Mesas con pedido activo", ORANGE));
        cards.add(statCard("Pedidos registrados", String.valueOf(controller.getOrders().size()),
                "Historial completo", new Color(126, 91, 190)));
        page.add(cards);
        page.add(Box.createVerticalStrut(20));

        JPanel overview = new JPanel(new GridLayout(1, 2, 14, 0));
        overview.setOpaque(false);
        overview.setPreferredSize(new Dimension(0, 300));
        overview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        overview.add(sectionCard("Productos y precios", "Menú de la costa colombiana · COP",
                buildMenuTable()));
        overview.add(buildTableStatusCard(true));
        page.add(overview);
        page.add(Box.createVerticalStrut(16));

        List<Pedido> pendingOrders = controller.getActiveOrders().stream().limit(4).toList();
        JPanel pendingCard = sectionCard("Pedidos pendientes",
                "Pedidos que todavía están en atención",
                buildOrdersTable(pendingOrders, false));
        pendingCard.setPreferredSize(new Dimension(0, 190));
        pendingCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        page.add(pendingCard);
        return page;
    }

    private JPanel buildOrdersPage() {
        JPanel page = pagePanel();
        JPanel card = new CardPanel(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton nextStatus = primaryButton("Avanzar estado");
        JButton cancel = secondaryButton("Cancelar pedido");
        JButton delete = dangerButton("Eliminar cancelado");
        actions.add(nextStatus);
        actions.add(cancel);
        actions.add(delete);
        card.add(actions, BorderLayout.NORTH);
        JTable table = buildOrdersTable(controller.getOrders(), true);
        JScrollPane scroll = new JScrollPane(table);
        styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        nextStatus.addActionListener(event -> withSelectedOrder(table, order -> {
            OrderStatus next = switch (order.getStatus()) {
                case NEW -> OrderStatus.PREPARING;
                case PREPARING -> OrderStatus.READY;
                case READY -> OrderStatus.DELIVERED;
                default -> throw new IllegalArgumentException("El pedido ya está finalizado.");
            };
            controller.updateStatus(order.getId(), next);
            showPage("orders");
        }));
        cancel.addActionListener(event -> withSelectedOrder(table, order -> {
            if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
                throw new IllegalArgumentException("El pedido ya está finalizado.");
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "¿Seguro que quieres cancelar el pedido de la mesa " + order.getTableNumber() + "?",
                    "Confirmar cancelación", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                controller.updateStatus(order.getId(), OrderStatus.CANCELLED);
                showPage("orders");
            }
        }));
        delete.addActionListener(event -> withSelectedOrder(table, order -> {
            if (order.getStatus() != OrderStatus.CANCELLED) {
                throw new IllegalArgumentException("Solo puedes eliminar pedidos cancelados.");
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Esta acción eliminará permanentemente el pedido cancelado. ¿Continuar?",
                    "Eliminar pedido", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                controller.removeOrder(order.getId());
                showPage("orders");
            }
        }));
        page.add(card);
        return page;
    }

    private JPanel buildTablesPage() {
        JPanel page = pagePanel();
        page.add(buildTableStatusCard(false));
        return page;
    }

    private JPanel buildReportsPage() {
        JPanel page = pagePanel();
        JPanel summary = new JPanel(new GridLayout(1, 4, 14, 0));
        summary.setOpaque(false);
        Map<OrderStatus, Long> counts = controller.getStatusCounts();
        summary.add(statCard("Nuevos", String.valueOf(counts.getOrDefault(OrderStatus.NEW, 0L)),
                "Esperando atención", PRIMARY));
        summary.add(statCard("En preparación", String.valueOf(counts.getOrDefault(OrderStatus.PREPARING, 0L)),
                "En cocina", ORANGE));
        summary.add(statCard("Entregados", String.valueOf(counts.getOrDefault(OrderStatus.DELIVERED, 0L)),
                "Pedidos completados", GREEN));
        summary.add(statCard("Cancelados", String.valueOf(counts.getOrDefault(OrderStatus.CANCELLED, 0L)),
                "Pedidos cancelados", RED));
        page.add(summary);
        page.add(Box.createVerticalStrut(18));
        page.add(sectionCard("Historial de pedidos", "Incluye pedidos activos, entregados y cancelados",
                buildOrdersTable(controller.getOrders(), false)));
        return page;
    }

    private JPanel buildCashPage() {
        JPanel page = pagePanel();
        List<CashClosure> closures = controller.getClosures();
        JPanel summary = new JPanel(new GridLayout(1, 2, 14, 0));
        summary.setOpaque(false);
        summary.add(statCard("Pendiente de cierre", money(controller.getPendingCashTotal()),
                "Pedidos entregados desde el último cierre", GREEN));
        summary.add(statCard("Cierres registrados", String.valueOf(closures.size()),
                "Historial de caja", PRIMARY));
        page.add(summary);
        page.add(Box.createVerticalStrut(18));

        JPanel current = new CardPanel(new BorderLayout(0, 12));
        current.setBorder(new EmptyBorder(18, 18, 18, 18));
        JLabel heading = label("Pedidos entregados pendientes", 15, TEXT, Font.BOLD);
        current.add(heading, BorderLayout.NORTH);
        JTable pendingTable = buildOrdersTable(controller.getOrders().stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .filter(order -> closures.stream()
                        .noneMatch(closure -> closure.getOrderIds().contains(order.getId())))
                .toList(), false);
        JScrollPane pendingScroll = new JScrollPane(pendingTable);
        styleScrollPane(pendingScroll);
        current.add(pendingScroll, BorderLayout.CENTER);
        JButton closeButton = primaryButton("Realizar cierre de caja");
        JPanel closeAction = new JPanel(new BorderLayout());
        closeAction.setOpaque(false);
        JLabel total = label("Total a cerrar: " + money(controller.getPendingCashTotal()), 15, TEXT, Font.BOLD);
        closeAction.add(total, BorderLayout.WEST);
        closeAction.add(closeButton, BorderLayout.EAST);
        current.add(closeAction, BorderLayout.SOUTH);
        closeButton.addActionListener(event -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Se registrará un cierre por " + money(controller.getPendingCashTotal()) + ". ¿Continuar?",
                    "Confirmar cierre de caja", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                try {
                    CashClosure closure = controller.closeCash();
                    showPage("cash");
                    JOptionPane.showMessageDialog(this,
                            "Cierre registrado correctamente.\nTotal: " + money(closure.getTotal())
                            + "\nPedidos incluidos: " + closure.getOrderCount(),
                            "Cierre completado", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException | IllegalArgumentException ex) {
                    showError(ex);
                }
            }
        });
        page.add(current);
        page.add(Box.createVerticalStrut(18));
        page.add(sectionCard("Cierres anteriores", "Registro de cierres confirmados",
                buildClosuresTable(closures)));
        return page;
    }

    private JPanel buildTableStatusCard(boolean compact) {
        JPanel card = new CardPanel(new BorderLayout(0, compact ? 10 : 18));
        card.setBorder(new EmptyBorder(compact ? 15 : 22, compact ? 16 : 24,
                compact ? 14 : 22, compact ? 16 : 24));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(label("Estado de Mesas", compact ? 15 : 19, TEXT, Font.BOLD), BorderLayout.WEST);
        JButton seeAll = new JButton("Ver todas  →");
        seeAll.setFont(new Font("SansSerif", Font.BOLD, 11));
        seeAll.setForeground(PRIMARY);
        seeAll.setBorder(new EmptyBorder(4, 8, 4, 0));
        seeAll.setContentAreaFilled(false);
        seeAll.setFocusPainted(false);
        seeAll.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        seeAll.addActionListener(event -> showPage("tables"));
        heading.add(seeAll, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3, 4, compact ? 8 : 22, compact ? 7 : 18));
        grid.setOpaque(false);
        for (int number = 1; number <= 12; number++) {
            Pedido order = controller.getActiveOrderForTable(number);
            boolean outOfService = OUT_OF_SERVICE_TABLES.contains(number);
            boolean occupied = !outOfService && order != null;
            Color stateColor = outOfService ? TABLE_GRAY : occupied ? RED : GREEN;
            String stateText = outOfService ? "Fuera de servicio" : occupied ? "Ocupada" : "Disponible";

            JPanel table = new TableTile(stateColor, compact);
            table.setLayout(new BoxLayout(table, BoxLayout.Y_AXIS));
            TableIndicator indicator = new TableIndicator(number, stateColor, compact ? 38 : 62);
            indicator.setAlignmentX(Component.CENTER_ALIGNMENT);
            table.add(indicator);
            JLabel state = label(stateText, compact ? 9 : 11, stateColor, Font.BOLD);
            state.setAlignmentX(Component.CENTER_ALIGNMENT);
            table.add(Box.createVerticalStrut(compact ? 2 : 6));
            table.add(state);
            grid.add(table);
        }

        JPanel content = new JPanel(new BorderLayout(0, compact ? 9 : 18));
        content.setOpaque(false);
        content.add(grid, BorderLayout.CENTER);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, compact ? 10 : 24, 0));
        legend.setOpaque(false);
        legend.add(legendItem(GREEN, "Disponibles"));
        legend.add(legendItem(RED, "Ocupadas"));
        legend.add(legendItem(TABLE_GRAY, "Fuera de servicio"));
        content.add(legend, BorderLayout.SOUTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel sectionCard(String title, String subtitle, Component content) {
        JPanel card = new CardPanel(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(17, 17, 17, 17));
        JPanel headings = new JPanel();
        headings.setOpaque(false);
        headings.setLayout(new BoxLayout(headings, BoxLayout.Y_AXIS));
        headings.add(label(title, 15, TEXT, Font.BOLD));
        headings.add(Box.createVerticalStrut(3));
        headings.add(label(subtitle, 11, MUTED, Font.PLAIN));
        card.add(headings, BorderLayout.NORTH);
        if (content instanceof JTable table) {
            JScrollPane scroll = new JScrollPane(table);
            styleScrollPane(scroll);
            card.add(scroll, BorderLayout.CENTER);
        } else {
            card.add(content, BorderLayout.CENTER);
        }
        return card;
    }

    private JPanel statCard(String title, String value, String detail, Color accent) {
        JPanel card = new CardPanel();
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                new EmptyBorder(14, 15, 13, 14)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(label(title, 12, MUTED, Font.BOLD));
        card.add(Box.createVerticalStrut(9));
        card.add(label(value, 22, TEXT, Font.BOLD));
        card.add(Box.createVerticalStrut(5));
        card.add(label(detail, 10, MUTED, Font.PLAIN));
        return card;
    }

    private JTable buildOrdersTable(List<Pedido> orders, boolean includeId) {
        String[] columns = includeId
                ? new String[]{"Pedido", "Mesa", "Hora", "Productos", "Estado", "Total"}
                : new String[]{"Pedido", "Mesa", "Hora", "Productos", "Estado", "Total"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Pedido order : orders) {
            model.addRow(new Object[]{
                shortId(order.getId()),
                "Mesa " + order.getTableNumber(),
                DATE_TIME.format(order.getCreatedAt()),
                order.getProductSummary(),
                order.getStatus().getLabel(),
                money(order.getTotal())
            });
        }
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(237, 241, 246));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFocusable(false);
        table.setSelectionBackground(new Color(225, 236, 255));
        table.setSelectionForeground(TEXT);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(226, 232, 240)));
        table.setFillsViewportHeight(true);
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(new PaddedCellRenderer());
        }
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new RightAlignedRenderer());
        if (orders.isEmpty()) {
            table.setToolTipText("Aún no hay pedidos registrados.");
        }
        return table;
    }

    private JTable buildMenuTable() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Producto", "Precio"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        controller.getMenu().forEach((name, price) -> model.addRow(new Object[]{name, money(price)}));
        JTable table = new JTable(model);
        table.setRowHeight(29);
        table.setFont(new Font("SansSerif", Font.PLAIN, 11));
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(237, 241, 246));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFocusable(false);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 10));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(226, 232, 240)));
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setCellRenderer(new PaddedCellRenderer());
        table.getColumnModel().getColumn(1).setCellRenderer(new RightAlignedRenderer());
        return table;
    }

    private JTable buildClosuresTable(List<CashClosure> closures) {
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Cierre", "Fecha y hora", "Pedidos incluidos", "Total"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (CashClosure closure : closures) {
            model.addRow(new Object[]{
                shortId(closure.getId()),
                DATE_TIME.format(closure.getClosedAt()),
                closure.getOrderCount(),
                money(closure.getTotal())
            });
        }
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(TEXT);
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(237, 241, 246));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFocusable(false);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(226, 232, 240)));
        table.setFillsViewportHeight(true);
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(new PaddedCellRenderer());
        }
        table.getColumnModel().getColumn(3).setCellRenderer(new RightAlignedRenderer());
        return table;
    }

    private void openOrderDialog(Integer initialTable) {
        List<Integer> availableTables = java.util.stream.IntStream.rangeClosed(1, 12)
                .filter(number -> controller.getActiveOrderForTable(number) == null)
                .boxed()
                .toList();
        if (availableTables.isEmpty()) {
            showError(new IllegalArgumentException("No hay mesas disponibles en este momento."));
            return;
        }
        JDialog dialog = new JDialog(this, "Registrar pedido", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(540, 560);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(BACKGROUND);
        content.setBorder(new EmptyBorder(18, 20, 18, 20));
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.setOpaque(false);
        JPanel tableChoice = new JPanel(new BorderLayout(10, 0));
        tableChoice.setOpaque(false);
        tableChoice.add(label("Mesa disponible", 12, TEXT, Font.BOLD), BorderLayout.WEST);
        JComboBox<Integer> tableCombo = new JComboBox<>(availableTables.toArray(Integer[]::new));
        tableCombo.putClientProperty("JComponent.roundRect", true);
        tableCombo.setPreferredSize(new Dimension(0, 38));
        if (initialTable != null) {
            tableCombo.setSelectedItem(initialTable);
        }
        tableChoice.add(tableCombo, BorderLayout.CENTER);
        form.add(tableChoice, BorderLayout.NORTH);

        JPanel products = new JPanel(new GridLayout(controller.getMenu().size(), 1, 0, 5));
        products.setBackground(Color.WHITE);
        products.setBorder(new EmptyBorder(12, 12, 12, 12));
        Map<String, JSpinner> quantityFields = new LinkedHashMap<>();
        controller.getMenu().forEach((name, price) -> {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.add(label(name + "   ·   " + money(price), 12, TEXT, Font.PLAIN), BorderLayout.CENTER);
            JSpinner quantity = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));
            quantity.putClientProperty("JComponent.roundRect", true);
            quantity.setPreferredSize(new Dimension(75, 34));
            quantityFields.put(name, quantity);
            row.add(quantity, BorderLayout.EAST);
            products.add(row);
        });
        JScrollPane productScroll = new JScrollPane(products);
        styleScrollPane(productScroll);
        form.add(productScroll, BorderLayout.CENTER);

        JTextField note = new JTextField();
        note.putClientProperty("JComponent.roundRect", true);
        note.setMargin(new java.awt.Insets(8, 10, 8, 10));
        note.setPreferredSize(new Dimension(0, 40));
        note.putClientProperty("JTextField.placeholderText", "Ej. Sin cebolla, alergias, indicaciones...");
        JPanel notePanel = new JPanel(new BorderLayout(0, 6));
        notePanel.setOpaque(false);
        notePanel.add(label("Nota (opcional)", 12, TEXT, Font.BOLD), BorderLayout.NORTH);
        notePanel.add(note, BorderLayout.CENTER);
        form.add(notePanel, BorderLayout.SOUTH);
        content.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton cancel = secondaryButton("Cancelar");
        JButton save = primaryButton("Guardar pedido");
        actions.add(cancel);
        actions.add(save);
        content.add(actions, BorderLayout.SOUTH);
        cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            Map<String, Integer> quantities = new LinkedHashMap<>();
            quantityFields.forEach((name, spinner) -> quantities.put(name, (Integer) spinner.getValue()));
            try {
                Pedido order = controller.createOrder((Integer) tableCombo.getSelectedItem(), quantities, note.getText());
                dialog.dispose();
                showPage(activePage);
                JOptionPane.showMessageDialog(this,
                        "Pedido " + shortId(order.getId()) + " registrado para la mesa "
                        + order.getTableNumber() + ".",
                        "Pedido creado", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException | IllegalArgumentException ex) {
                showError(ex);
            }
        });
        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void withSelectedOrder(JTable table, OrderAction action) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            showError(new IllegalArgumentException("Selecciona un pedido de la tabla."));
            return;
        }
        String id = table.getValueAt(table.convertRowIndexToModel(selectedRow), 0).toString();
        Pedido selected = controller.getOrders().stream()
                .filter(order -> shortId(order.getId()).equals(id))
                .findFirst()
                .orElse(null);
        if (selected == null) {
            showError(new IllegalArgumentException("No se pudo encontrar el pedido seleccionado."));
            return;
        }
        try {
            action.run(selected);
        } catch (IOException | IllegalArgumentException ex) {
            showError(ex);
        }
    }

    private JLabel legendItem(Color color, String text) {
        JLabel item = label(text, 11, MUTED, Font.BOLD);
        item.setIcon(new StatusDot(color));
        item.setIconTextGap(7);
        return item;
    }

    private JPanel pagePanel() {
        JPanel page = new JPanel();
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(22, 26, 24, 26));
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        return page;
    }

    private void styleScrollPane(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        scroll.getViewport().setBackground(Color.WHITE);
    }

    private JButton primaryButton(String text) {
        return styledButton(text, PRIMARY, Color.WHITE);
    }

    private JButton secondaryButton(String text) {
        return styledButton(text, new Color(238, 243, 250), TEXT);
    }

    private JButton dangerButton(String text) {
        return styledButton(text, new Color(252, 235, 235), RED);
    }

    private JButton styledButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setBorder(new EmptyBorder(10, 14, 10, 14));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                button.setBackground(background.brighter());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(background);
            }
        });
        return button;
    }

    private JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", style, size));
        label.setForeground(color);
        return label;
    }

    private String money(BigDecimal amount) {
        return CURRENCY.format(amount);
    }

    private static NumberFormat createCurrencyFormat() {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        format.setCurrency(Currency.getInstance("COP"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(0);
        return format;
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo completar la acción",
                JOptionPane.ERROR_MESSAGE);
    }

    private interface OrderAction {

        void run(Pedido order) throws IOException;
    }

    private static final class CardPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private CardPanel() {
            super();
            setOpaque(false);
        }

        private CardPanel(java.awt.LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int offset = 4; offset > 0; offset--) {
                g2.setColor(new Color(15, 23, 42, 4 + offset * 2));
                g2.fillRoundRect(0, offset, getWidth() - 1, getHeight() - 1, 16, 16);
            }
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 5, 16, 16);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class TableIndicator extends JComponent {

        private static final long serialVersionUID = 1L;
        private final int number;
        private final Color fill;
        private final int diameter;

        private TableIndicator(int number, Color fill, int diameter) {
            this.number = number;
            this.fill = fill;
            this.diameter = diameter;
            setOpaque(false);
            setPreferredSize(new Dimension(diameter + 8, diameter + 8));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;
            g2.setColor(fill);
            g2.fillOval(x, y, diameter, diameter);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, diameter / 3)));
            String text = Integer.toString(number);
            int textX = (getWidth() - g2.getFontMetrics().stringWidth(text)) / 2;
            int textY = (getHeight() - g2.getFontMetrics().getHeight()) / 2
                    + g2.getFontMetrics().getAscent();
            g2.drawString(text, textX, textY);
            g2.dispose();
        }
    }

    private static final class TableTile extends JPanel {

        private static final long serialVersionUID = 1L;
        private final Color stateColor;
        private final boolean compact;

        private TableTile(Color stateColor, boolean compact) {
            this.stateColor = stateColor;
            this.compact = compact;
            setOpaque(false);
            setBorder(new EmptyBorder(compact ? 6 : 12, compact ? 3 : 8,
                    compact ? 6 : 12, compact ? 3 : 8));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = compact ? 12 : 16;
            int inset = 1;
            g2.setColor(new Color(stateColor.getRed(), stateColor.getGreen(), stateColor.getBlue(), 12));
            g2.fillRoundRect(inset, inset, getWidth() - 2 * inset - 1,
                    getHeight() - 2 * inset - 1, arc, arc);
            g2.setColor(new Color(stateColor.getRed(), stateColor.getGreen(), stateColor.getBlue(), 90));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(inset, inset, getWidth() - 2 * inset - 1,
                    getHeight() - 2 * inset - 1, arc, arc);
            g2.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class StatusDot implements Icon {

        private final Color color;

        private StatusDot(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y + 3, 9, 9);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 15;
        }
    }

    private static final class NavigationIcon implements Icon {

        private final String page;

        private NavigationIcon(String page) {
            this.page = page;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(component.getForeground());
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int left = x + 2;
            int top = y + 2;
            switch (page) {
                case "dashboard" -> {
                    Polygon roof = new Polygon(
                            new int[]{left + 1, left + 8, left + 15},
                            new int[]{top + 8, top + 2, top + 8}, 3);
                    g2.drawPolygon(roof);
                    g2.drawRoundRect(left + 3, top + 7, 10, 9, 2, 2);
                    g2.drawLine(left + 7, top + 16, left + 7, top + 11);
                }
                case "orders" -> {
                    g2.drawRoundRect(left + 3, top + 1, 11, 16, 2, 2);
                    g2.drawLine(left + 6, top + 6, left + 11, top + 6);
                    g2.drawLine(left + 6, top + 9, left + 11, top + 9);
                    g2.drawLine(left + 6, top + 12, left + 10, top + 12);
                }
                case "tables" -> {
                    g2.drawRoundRect(left + 1, top + 2, 7, 6, 2, 2);
                    g2.drawRoundRect(left + 10, top + 2, 7, 6, 2, 2);
                    g2.drawRoundRect(left + 1, top + 11, 7, 6, 2, 2);
                    g2.drawRoundRect(left + 10, top + 11, 7, 6, 2, 2);
                }
                case "reports" -> {
                    g2.drawLine(left + 2, top + 16, left + 2, top + 2);
                    g2.drawLine(left + 2, top + 16, left + 17, top + 16);
                    g2.drawRoundRect(left + 5, top + 9, 3, 6, 1, 1);
                    g2.drawRoundRect(left + 10, top + 5, 3, 10, 1, 1);
                    g2.drawRoundRect(left + 15, top + 2, 3, 13, 1, 1);
                }
                case "cash" -> {
                    g2.drawRoundRect(left + 1, top + 3, 17, 12, 3, 3);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                    g2.drawString("$", left + 7, top + 13);
                }
                default -> g2.drawOval(left + 3, top + 3, 12, 12);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }
    }

    private static class PaddedCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focused, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            cell.setBorder(new EmptyBorder(0, 12, 0, 12));
            return cell;
        }
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focused, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            cell.setHorizontalAlignment(SwingConstants.CENTER);
            cell.setFont(new Font("SansSerif", Font.BOLD, 11));
            cell.setBorder(new EmptyBorder(0, 8, 0, 8));
            if (!selected) {
                String status = value == null ? "" : value.toString();
                if (status.equals(OrderStatus.NEW.getLabel())) {
                    cell.setForeground(PRIMARY);
                } else if (status.equals(OrderStatus.PREPARING.getLabel())
                        || status.equals(OrderStatus.READY.getLabel())) {
                    cell.setForeground(ORANGE);
                } else if (status.equals(OrderStatus.DELIVERED.getLabel())) {
                    cell.setForeground(GREEN);
                } else {
                    cell.setForeground(RED);
                }
            }
            return cell;
        }
    }

    private static final class RightAlignedRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focused, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            cell.setHorizontalAlignment(SwingConstants.RIGHT);
            cell.setBorder(new EmptyBorder(0, 0, 0, 12));
            cell.setFont(new Font("SansSerif", Font.BOLD, 12));
            return cell;
        }
    }
}
