package com.mycompany.restocontrol.view;

import com.mycompany.restocontrol.controller.RestaurantController;
import com.mycompany.restocontrol.model.CashClosure;
import com.mycompany.restocontrol.model.Mesa;
import com.mycompany.restocontrol.model.OrderStatus;
import com.mycompany.restocontrol.model.Pedido;
import com.mycompany.restocontrol.model.SesionActual;
import com.mycompany.restocontrol.model.Turno;
import com.mycompany.restocontrol.model.Usuario;
import com.mycompany.restocontrol.repository.DatabaseManager;
import com.restocontrol.model.PedidoKDS;
import com.restocontrol.ui.CobrarFrame;
import com.restocontrol.util.TicketPrinter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public final class DashboardFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    // SaaS Premium Dark Palette 2026
    private static final Color SIDEBAR_BG = new Color(0x0F, 0x1A, 0x30);   // #0F1A30
    private static final Color APP_BG = new Color(0x0B, 0x13, 0x2B);       // #0B132B
    private static final Color CARD_BG = new Color(0x1E, 0x29, 0x3B);      // #1E293B
    private static final Color BORDER_COLOR = new Color(0x33, 0x41, 0x55); // #334155
    private static final Color ACTIVE_CYAN = new Color(0x00, 0xAC, 0xC1);  // #00ACC1
    private static final Color TEXT_PRIMARY = new Color(0xF8, 0xFA, 0xFC); // #F8FAFC
    private static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);   // #94A3B8
    private static final Color GREEN_ACCENT = new Color(0x10, 0xB9, 0x81); // #10B981
    private static final Color ORANGE_ACCENT = new Color(0xFF, 0x8F, 0x00);// #FF8F00
    private static final Color RED_ACCENT = new Color(0xEF, 0x44, 0x44);   // #EF4444
    private static final Color PURPLE_ACCENT = new Color(0xA8, 0x55, 0xF7);// #A855F7

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY = createCurrencyFormat();
    private static final Map<String, ImageIcon> DISH_ICON_CACHE = new LinkedHashMap<>();

    private final RestaurantController controller;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pageContainer = new JPanel(pageLayout);
    private final Map<String, JButton> navigationButtons = new LinkedHashMap<>();
    private final JLabel pageTitle = new JLabel();
    private final JLabel pageSubtitle = new JLabel();
    private final JPanel sidebar = new JPanel();
    private String activePage = "dashboard";

    // Required business logic variables and collections
    private String ventasHoy = "$0";
    private int pedidosActivos = 0;
    private final ArrayList<Mesa> mesas = new ArrayList<>();

    public DashboardFrame(RestaurantController controller) {
        this.controller = controller;
        setTitle("RestoControl | Panel de Administración");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setContentPane(buildApplication());

        Usuario.Rol rol = SesionActual.getInstance().getRol();
        String initialPage = "dashboard";
        if (rol == Usuario.Rol.MESERO) {
            initialPage = "tables";
        } else if (rol == Usuario.Rol.CAJERO) {
            initialPage = "cash";
        }
        showPage(initialPage);
    }

    // Required business logic methods (preserves original signatures and filters by user and shift)
    public BigDecimal calcularVentasHoy() {
        Usuario usuario = SesionActual.getInstance().getUsuario();
        int turnoId = SesionActual.getInstance().getTurnoId();
        if (usuario != null && usuario.getRol() != Usuario.Rol.ADMIN) {
            return controller.getTodaySales(usuario.getId(), turnoId);
        }
        return controller.getTodaySales();
    }

    public BigDecimal cargarVentasHoy() {
        return calcularVentasHoy();
    }

    public void cargarMesas() {
        mesas.clear();
        for (int number = 1; number <= 12; number++) {
            Pedido order = controller.getActiveOrderForTable(number);
            boolean outOfService = controller.getOutOfServiceTables().contains(number);
            boolean occupied = !outOfService && order != null;
            mesas.add(new Mesa(number, occupied, outOfService, order));
        }
    }

    public Map<String, BigDecimal> cargarProductos() {
        return controller.getMenu();
    }

    private JPanel buildApplication() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(APP_BG);
        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(APP_BG);
        main.add(buildHeader(), BorderLayout.NORTH);
        pageContainer.setBackground(APP_BG);
        main.add(pageContainer, BorderLayout.CENTER);
        root.add(main, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildSidebar() {
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBorder(new EmptyBorder(22, 16, 20, 16));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        Component logo = buildLogoWithGlow();
        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(18));

        Usuario.Rol rol = SesionActual.getInstance().getRol();
        if (rol == null) {
            rol = Usuario.Rol.ADMIN;
        }

        if (rol == Usuario.Rol.ADMIN) {
            addNavigation("dashboard", "Dashboard");
            addNavigation("orders", "Pedidos");
            addNavigation("tables", "Mesas");
            addNavigation("reports", "Reportes");
            addNavigation("cash", "Cierre de caja");
        } else if (rol == Usuario.Rol.MESERO) {
            addNavigation("tables", "Mesas");
            addNavigation("orders", "Pedidos");
        } else if (rol == Usuario.Rol.CAJERO) {
            addNavigation("orders", "Pedidos");
            addNavigation("cash", "Cierre de caja");
        } else {
            addNavigation("orders", "Pedidos");
        }

        sidebar.add(Box.createVerticalGlue());

        // Footer info in sidebar
        JLabel footer = new JLabel("RestoControl SaaS · v2.6");
        footer.setFont(resolveFont("Inter", Font.PLAIN, 11));
        footer.setForeground(new Color(100, 116, 139));
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.setBorder(new EmptyBorder(10, 0, 4, 0));
        sidebar.add(footer);

        return sidebar;
    }

    private Component buildLogoWithGlow() {
        Image logoImage = null;
        try (var input = DashboardFrame.class.getResourceAsStream("/images/logo-restocontrol.png")) {
            if (input != null) {
                logoImage = ImageIO.read(input);
            }
        } catch (IOException ignored) {
        }
        return new LogoGlowPanel(logoImage);
    }

    private void addNavigation(String key, String text) {
        JButton button = new JButton(text);
        button.setIcon(new NavigationIcon(key));
        button.setIconTextGap(14);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(resolveFont("Inter", Font.BOLD, 13));
        button.setForeground(TEXT_MUTED);
        button.setBackground(SIDEBAR_BG);
        button.setBorder(new EmptyBorder(12, 16, 12, 16));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (!key.equals(activePage)) {
                    button.setBackground(new Color(30, 41, 59));
                    button.setForeground(TEXT_PRIMARY);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (!key.equals(activePage)) {
                    button.setBackground(SIDEBAR_BG);
                    button.setForeground(TEXT_MUTED);
                }
            }
        });
        button.addActionListener(event -> showPage(key));
        navigationButtons.put(key, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(6));
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SIDEBAR_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(16, 26, 16, 26)));

        JPanel headings = new JPanel();
        headings.setOpaque(false);
        headings.setLayout(new BoxLayout(headings, BoxLayout.Y_AXIS));
        pageTitle.setFont(resolveFont("Inter", Font.BOLD, 22));
        pageTitle.setForeground(TEXT_PRIMARY);

        JPanel subRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        subRow.setOpaque(false);
        pageSubtitle.setFont(resolveFont("Inter", Font.PLAIN, 12));
        pageSubtitle.setForeground(TEXT_MUTED);
        subRow.add(pageSubtitle);

        // Turno info badge: "Caja: 01 • Usuario: Andrea • Turno: Mañana • 13 oct 2026 14:32"
        JLabel shiftBadge = new JLabel(SesionActual.getInstance().getSessionHeaderSummary());
        shiftBadge.setFont(resolveEmojiFont(Font.BOLD, 11));
        shiftBadge.setForeground(ACTIVE_CYAN);
        shiftBadge.setOpaque(true);
        shiftBadge.setBackground(new Color(0, 172, 193, 28));
        shiftBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 172, 193, 80), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));
        subRow.add(shiftBadge);

        headings.add(pageTitle);
        headings.add(Box.createVerticalStrut(4));
        headings.add(subRow);
        header.add(headings, BorderLayout.CENTER);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.setOpaque(false);

        JButton addOrder = primaryButton("+  Nuevo pedido");
        addOrder.addActionListener(event -> openOrderDialog(null));
        headerActions.add(addOrder);

        JButton reprintBtn = styledButton("Reimprimir último ticket", new Color(0, 172, 193, 35), ACTIVE_CYAN);
        reprintBtn.addActionListener(event -> {
            try {
                boolean ok = TicketPrinter.reimprimirUltimoTicket();
                if (!ok) {
                    JOptionPane.showMessageDialog(this,
                            "No se encontró un ticket reciente para reimprimir.",
                            "Reimpresión", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Ticket guardado en carpeta tickets",
                        "Aviso de Impresión", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        headerActions.add(reprintBtn);

        JButton logoutBtn = styledButton("Cerrar turno", new Color(239, 68, 68, 40), RED_ACCENT);
        logoutBtn.addActionListener(event -> cerrarTurno());
        headerActions.add(logoutBtn);

        header.add(headerActions, BorderLayout.EAST);
        return header;
    }

    private void cerrarTurno() {
        Usuario user = SesionActual.getInstance().getUsuario();
        int turnoId = SesionActual.getInstance().getTurnoId();
        BigDecimal ventasTurno = controller.getTodaySales(user != null ? user.getId() : 0, turnoId);

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Deseas finalizar tu turno y cerrar sesión?\n"
                + "Ventas acumuladas en este turno: " + money(ventasTurno),
                "Confirmar Cierre de Turno", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            DatabaseManager.getInstance().cerrarTurno(turnoId, ventasTurno);
            Turno turno = DatabaseManager.getInstance().getTurno(turnoId);
            if (turno != null) {
                try {
                    TicketPrinter.imprimirCierre(turno);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Ticket guardado en carpeta tickets", "Cierre de Caja", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            SesionActual.getInstance().cerrarSesion();
            dispose();
            new LoginFrame(controller).setVisible(true);
        }
    }

    private void showPage(String page) {
        activePage = page;
        String title;
        String subtitle;
        switch (page) {
            case "orders" -> {
                title = "Gestión de pedidos";
                subtitle = "Registra pedidos y actualiza su estado de preparación en tiempo real.";
            }
            case "tables" -> {
                title = "Estado de Mesas";
                subtitle = "Consulta la disponibilidad y pedidos activos de las 12 mesas.";
            }
            case "reports" -> {
                title = "Reportes y Métricas";
                subtitle = "Resumen de ventas consolidadas, actividad registrada y órdenes.";
            }
            case "cash" -> {
                title = "Cierre de caja";
                subtitle = "Revisa los pedidos entregados que aún no se han incluido en un cierre.";
            }
            default -> {
                page = "dashboard";
                title = "Dashboard Principal";
                subtitle = "Operación en tiempo real del restaurante · Saas Premium";
            }
        }
        pageTitle.setText(title);
        pageSubtitle.setText(subtitle);

        for (Map.Entry<String, JButton> entry : navigationButtons.entrySet()) {
            boolean selected = entry.getKey().equals(page);
            if (selected) {
                entry.getValue().setBackground(ACTIVE_CYAN);
                entry.getValue().setForeground(Color.WHITE);
                entry.getValue().setFont(resolveFont("Inter", Font.BOLD, 13));
            } else {
                entry.getValue().setBackground(SIDEBAR_BG);
                entry.getValue().setForeground(TEXT_MUTED);
                entry.getValue().setFont(resolveFont("Inter", Font.PLAIN, 13));
            }
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
        // Refresh business logic state
        cargarMesas();
        BigDecimal sales = calcularVentasHoy();
        ventasHoy = money(sales);
        pedidosActivos = controller.getActiveOrders().size();
        long occupiedCount = controller.getOccupiedTableCount();
        int totalOrdersCount = controller.getOrders().size();

        JPanel page = pagePanel();

        // 4 KPI Cards using new reusable KpiCard class
        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        cardsGrid.setOpaque(false);
        cardsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 136));
        cardsGrid.setPreferredSize(new Dimension(0, 136));

        cardsGrid.add(new KpiCard("Ventas de hoy", ventasHoy, "Pedidos entregados", "💰", GREEN_ACCENT));
        cardsGrid.add(new KpiCard("Pedidos activos", String.valueOf(pedidosActivos), "En proceso en cocina", "⚡", ACTIVE_CYAN));
        cardsGrid.add(new KpiCard("Mesas ocupadas", occupiedCount + " / 12", "Mesas con pedido activo", "🪑", ORANGE_ACCENT));
        cardsGrid.add(new KpiCard("Pedidos registrados", String.valueOf(totalOrdersCount), "Historial completo", "📊", PURPLE_ACCENT));

        page.add(cardsGrid);
        page.add(Box.createVerticalStrut(18));

        // Center split overview: Products & Prices (Left) and Tables Status (Right)
        JPanel overview = new JPanel(new GridLayout(1, 2, 16, 0));
        overview.setOpaque(false);
        overview.setPreferredSize(new Dimension(0, 310));
        overview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 310));

        overview.add(sectionCard("Productos y precios", "Menú de la costa colombiana · COP", buildMenuTable()));
        overview.add(buildTableStatusCard(true));
        page.add(overview);
        page.add(Box.createVerticalStrut(18));

        // Bottom section: Pending orders with Empty State support
        List<Pedido> pendingOrders = controller.getActiveOrders().stream().limit(4).toList();
        page.add(buildPendingOrdersSection(pendingOrders));

        return page;
    }

    private JPanel buildPendingOrdersSection(List<Pedido> pendingOrders) {
        if (pendingOrders.isEmpty()) {
            RoundedPanel emptyCard = new RoundedPanel(16, CARD_BG);
            emptyCard.setLayout(new BorderLayout(0, 10));
            emptyCard.setBorder(new EmptyBorder(16, 20, 18, 20));

            JPanel headings = new JPanel();
            headings.setOpaque(false);
            headings.setLayout(new BoxLayout(headings, BoxLayout.Y_AXIS));
            JLabel title = new JLabel("Pedidos pendientes");
            title.setFont(resolveFont("Inter", Font.BOLD, 15));
            title.setForeground(TEXT_PRIMARY);
            JLabel sub = new JLabel("Pedidos que todavía están en atención");
            sub.setFont(resolveFont("Inter", Font.PLAIN, 11));
            sub.setForeground(TEXT_MUTED);
            headings.add(title);
            headings.add(Box.createVerticalStrut(2));
            headings.add(sub);
            emptyCard.add(headings, BorderLayout.NORTH);

            // Centered empty state
            JPanel centerPanel = new JPanel();
            centerPanel.setOpaque(false);
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

            JLabel iconLabel = new JLabel("✨");
            iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(iconLabel);

            centerPanel.add(Box.createVerticalStrut(6));

            JLabel mainMsg = new JLabel("¡Todo al día!");
            mainMsg.setFont(resolveFont("Inter", Font.BOLD, 17));
            mainMsg.setForeground(TEXT_PRIMARY);
            mainMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(mainMsg);

            centerPanel.add(Box.createVerticalStrut(4));

            JLabel subMsg = new JLabel("No hay pedidos pendientes en cocina.");
            subMsg.setFont(resolveFont("Inter", Font.PLAIN, 12));
            subMsg.setForeground(TEXT_MUTED);
            subMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(subMsg);

            emptyCard.add(centerPanel, BorderLayout.CENTER);
            emptyCard.setPreferredSize(new Dimension(0, 185));
            emptyCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 185));
            return emptyCard;
        }

        JPanel pendingCard = sectionCard(
                "Pedidos pendientes",
                "Pedidos que todavía están en atención",
                buildOrdersTable(pendingOrders, false)
        );
        pendingCard.setPreferredSize(new Dimension(0, 185));
        pendingCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 185));
        return pendingCard;
    }

    private JPanel buildOrdersPage() {
        JPanel page = pagePanel();
        RoundedPanel card = new RoundedPanel(16, CARD_BG);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        JButton nextStatus = primaryButton("Avanzar pedido");
        JButton cobrar = primaryButton("Cobrar y Facturar");
        cobrar.setBackground(new Color(0x10, 0xB9, 0x81));
        JButton cancel = secondaryButton("Cancelar pedido");
        JButton delete = dangerButton("Eliminar cancelado");
        actions.add(nextStatus);
        actions.add(cobrar);
        actions.add(cancel);
        actions.add(delete);
        card.add(actions, BorderLayout.NORTH);

        JTable table = buildOrdersTable(controller.getOrders(), true);
        JScrollPane scroll = new JScrollPane(table);
        styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        cobrar.addActionListener(event -> withSelectedOrder(table, order -> {
            openCobrarFrame(order);
        }));

        nextStatus.addActionListener(event -> withSelectedOrder(table, order -> {
            OrderStatus next = switch (order.getStatus()) {
                case NEW -> OrderStatus.PREPARING;
                case PREPARING -> OrderStatus.READY;
                case READY -> OrderStatus.DELIVERED;
                case DELIVERED -> OrderStatus.FINISHED;
                default -> throw new IllegalArgumentException("El pedido ya está finalizado.");
            };
            controller.updateStatus(order.getId(), next);
            showPage("orders");
        }));

        cancel.addActionListener(event -> withSelectedOrder(table, order -> {
            if (order.getStatus() == OrderStatus.DELIVERED
                    || order.getStatus() == OrderStatus.FINISHED
                    || order.getStatus() == OrderStatus.CANCELLED) {
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
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        JButton configure = primaryButton("Configurar mesas fuera de servicio");
        configure.addActionListener(event -> openOutOfServiceTablesDialog());
        actions.add(configure);
        page.add(actions);
        page.add(Box.createVerticalStrut(12));
        page.add(buildTableStatusCard(false));
        return page;
    }

    private JPanel buildReportsPage() {
        JPanel page = pagePanel();
        JPanel summary = new JPanel(new GridLayout(1, 4, 16, 0));
        summary.setOpaque(false);
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 136));
        summary.setPreferredSize(new Dimension(0, 136));

        Map<OrderStatus, Long> counts = controller.getStatusCounts();
        summary.add(new KpiCard("Nuevos", String.valueOf(counts.getOrDefault(OrderStatus.NEW, 0L)),
                "Esperando atención", "🆕", ACTIVE_CYAN));
        summary.add(new KpiCard("En preparación", String.valueOf(counts.getOrDefault(OrderStatus.PREPARING, 0L)),
                "En cocina", "🔥", ORANGE_ACCENT));
        long deliveredCount = counts.getOrDefault(OrderStatus.DELIVERED, 0L)
                + counts.getOrDefault(OrderStatus.FINISHED, 0L);
        summary.add(new KpiCard("Entregados", String.valueOf(deliveredCount),
                "Pedidos completados", "✅", GREEN_ACCENT));
        summary.add(new KpiCard("Cancelados", String.valueOf(counts.getOrDefault(OrderStatus.CANCELLED, 0L)),
                "Pedidos cancelados", "❌", RED_ACCENT));
        page.add(summary);
        page.add(Box.createVerticalStrut(18));
        page.add(sectionCard("Historial de pedidos", "Incluye pedidos activos, entregados y cancelados",
                buildOrdersTable(controller.getOrders(), false)));
        return page;
    }

    private JPanel buildCashPage() {
        JPanel page = pagePanel();
        List<CashClosure> closures = controller.getClosures();
        JPanel summary = new JPanel(new GridLayout(1, 2, 16, 0));
        summary.setOpaque(false);
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 136));
        summary.setPreferredSize(new Dimension(0, 136));

        summary.add(new KpiCard("Pendiente de cierre", money(controller.getPendingCashTotal()),
                "Pedidos entregados desde el último cierre", "💵", GREEN_ACCENT));
        summary.add(new KpiCard("Cierres registrados", String.valueOf(closures.size()),
                "Historial de caja", "📁", ACTIVE_CYAN));
        page.add(summary);
        page.add(Box.createVerticalStrut(18));

        RoundedPanel current = new RoundedPanel(16, CARD_BG);
        current.setLayout(new BorderLayout(0, 14));
        current.setBorder(new EmptyBorder(18, 18, 18, 18));
        JLabel heading = label("Pedidos entregados pendientes", 15, TEXT_PRIMARY, Font.BOLD);
        current.add(heading, BorderLayout.NORTH);

        JTable pendingTable = buildOrdersTable(controller.getOrders().stream()
                .filter(order -> order.getStatus().isServed())
                .filter(order -> closures.stream()
                        .noneMatch(closure -> closure.getOrderIds().contains(order.getId())))
                .toList(), false);
        JScrollPane pendingScroll = new JScrollPane(pendingTable);
        styleScrollPane(pendingScroll);
        current.add(pendingScroll, BorderLayout.CENTER);

        JButton closeButton = primaryButton("Realizar cierre de caja");
        JPanel closeAction = new JPanel(new BorderLayout());
        closeAction.setOpaque(false);
        JLabel total = label("Total a cerrar: " + money(controller.getPendingCashTotal()), 15, TEXT_PRIMARY, Font.BOLD);
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
                    int turnoId = SesionActual.getInstance().getTurnoId();
                    Turno turno = DatabaseManager.getInstance().getTurno(turnoId);
                    if (turno != null) {
                        try {
                            TicketPrinter.imprimirCierre(turno);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "Ticket guardado en carpeta tickets", "Cierre de Caja", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
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
        cargarMesas();

        RoundedPanel card = new RoundedPanel(16, CARD_BG);
        card.setLayout(new BorderLayout(0, compact ? 10 : 18));
        card.setBorder(new EmptyBorder(compact ? 15 : 22, compact ? 16 : 24,
                compact ? 14 : 22, compact ? 16 : 24));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(label("Estado de Mesas", compact ? 15 : 19, TEXT_PRIMARY, Font.BOLD), BorderLayout.WEST);

        JButton seeAll = new JButton("Ver todas  →");
        seeAll.setFont(resolveFont("Inter", Font.BOLD, 12));
        seeAll.setForeground(ACTIVE_CYAN);
        seeAll.setBorder(new EmptyBorder(4, 8, 4, 0));
        seeAll.setContentAreaFilled(false);
        seeAll.setFocusPainted(false);
        seeAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        seeAll.addActionListener(event -> showPage("tables"));
        heading.add(seeAll, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3, 4, compact ? 10 : 16, compact ? 10 : 14));
        grid.setOpaque(false);

        for (Mesa mesa : mesas) {
            MesaCard mesaCard = new MesaCard(mesa, compact, tableNumber -> {
                if (mesa.isAvailable()) {
                    openOrderDialog(tableNumber);
                } else if (mesa.isOccupied()) {
                    Pedido order = mesa.getActiveOrder();
                    if (order == null) {
                        order = controller.getActiveOrderForTable(tableNumber);
                    }
                    if (order != null) {
                        openCobrarFrame(order);
                    } else {
                        com.restocontrol.model.PedidoKDS kds = TicketPrinter.obtenerPedidoPorId(String.valueOf(tableNumber));
                        if (kds != null) {
                            openCobrarFrame(kds);
                        }
                    }
                }
            });
            grid.add(mesaCard);
        }

        JPanel content = new JPanel(new BorderLayout(0, compact ? 10 : 16));
        content.setOpaque(false);
        content.add(grid, BorderLayout.CENTER);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, compact ? 14 : 24, 0));
        legend.setOpaque(false);
        legend.add(legendItem(MesaCard.COLOR_DISPONIBLE, "Disponibles (\u2705)"));
        legend.add(legendItem(MesaCard.COLOR_OCUPADA, "Ocupadas (\u23F1 Timer)"));
        legend.add(legendItem(MesaCard.COLOR_FUERA_SERVICIO, "Fuera de servicio"));
        content.add(legend, BorderLayout.SOUTH);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel sectionCard(String title, String subtitle, Component content) {
        RoundedPanel card = new RoundedPanel(16, CARD_BG);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel headings = new JPanel();
        headings.setOpaque(false);
        headings.setLayout(new BoxLayout(headings, BoxLayout.Y_AXIS));
        headings.add(label(title, 15, TEXT_PRIMARY, Font.BOLD));
        headings.add(Box.createVerticalStrut(3));
        headings.add(label(subtitle, 11, TEXT_MUTED, Font.PLAIN));
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
        table.setRowHeight(42);
        table.setFont(resolveFont("Inter", Font.PLAIN, 13));
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(CARD_BG);
        table.setGridColor(BORDER_COLOR);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFocusable(false);
        table.setSelectionBackground(new Color(0, 172, 193, 70));
        table.setSelectionForeground(TEXT_PRIMARY);

        table.getTableHeader().setFont(resolveFont("Inter", Font.BOLD, 11));
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setBackground(new Color(15, 26, 48));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        table.setFillsViewportHeight(true);

        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(new PaddedCellRenderer());
        }
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new RightAlignedRenderer());
        return table;
    }

    private JTable buildMenuTable() {
        String[] columns = new String[]{"Foto", "Producto", "Precio", "Estado"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Icon.class;
                }
                return Object.class;
            }
        };

        cargarProductos().forEach((name, price) -> {
            ImageIcon icon = getDishImageIcon(name);
            model.addRow(new Object[]{icon, name, money(price), "Disponible"});
        });

        JTable table = new JTable(model);
        table.setRowHeight(60); // Requested: setRowHeight(60)
        table.setFont(resolveFont("Inter", Font.PLAIN, 14)); // Requested: Inter 14px
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(CARD_BG);
        table.setGridColor(BORDER_COLOR);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFocusable(false);
        table.setSelectionBackground(new Color(0, 172, 193, 70));
        table.setSelectionForeground(TEXT_PRIMARY);

        table.getTableHeader().setFont(resolveFont("Inter", Font.BOLD, 12));
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setBackground(new Color(15, 26, 48));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        table.setFillsViewportHeight(true);

        // Column widths and renderers (45x45 image renderer)
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMinWidth(55);
        table.getColumnModel().getColumn(0).setMaxWidth(68);
        table.getColumnModel().getColumn(0).setCellRenderer(new ImageCellRenderer());

        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setCellRenderer(new PaddedCellRenderer());

        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setCellRenderer(new RightAlignedRenderer());

        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setCellRenderer(new PillStatusRenderer()); // Requested: pill verde "Disponible"

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
        table.setRowHeight(42);
        table.setFont(resolveFont("Inter", Font.PLAIN, 13));
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(CARD_BG);
        table.setGridColor(BORDER_COLOR);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFocusable(false);
        table.getTableHeader().setFont(resolveFont("Inter", Font.BOLD, 11));
        table.getTableHeader().setForeground(TEXT_MUTED);
        table.getTableHeader().setBackground(new Color(15, 26, 48));
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        table.setFillsViewportHeight(true);
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(new PaddedCellRenderer());
        }
        table.getColumnModel().getColumn(3).setCellRenderer(new RightAlignedRenderer());
        return table;
    }

    private void openOrderDialog(Integer initialTable) {
        List<Integer> availableTables = java.util.stream.IntStream.rangeClosed(1, 12)
                .filter(number -> !controller.getOutOfServiceTables().contains(number))
                .filter(number -> controller.getActiveOrderForTable(number) == null)
                .boxed()
                .toList();
        if (availableTables.isEmpty()) {
            showError(new IllegalArgumentException("No hay mesas disponibles en este momento."));
            return;
        }
        JDialog dialog = new JDialog(this, "Registrar Pedido", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(560, 580);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(APP_BG);
        content.setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setOpaque(false);

        JPanel tableChoice = new JPanel(new BorderLayout(12, 0));
        tableChoice.setOpaque(false);
        tableChoice.add(label("Mesa disponible", 13, TEXT_PRIMARY, Font.BOLD), BorderLayout.WEST);
        JComboBox<Integer> tableCombo = new JComboBox<>(availableTables.toArray(Integer[]::new));
        tableCombo.putClientProperty("JComponent.roundRect", true);
        tableCombo.setPreferredSize(new Dimension(0, 40));
        if (initialTable != null) {
            tableCombo.setSelectedItem(initialTable);
        }
        tableChoice.add(tableCombo, BorderLayout.CENTER);
        form.add(tableChoice, BorderLayout.NORTH);

        JPanel products = new JPanel(new GridLayout(controller.getMenu().size(), 1, 0, 6));
        products.setBackground(CARD_BG);
        products.setBorder(new EmptyBorder(12, 14, 12, 14));
        Map<String, JSpinner> quantityFields = new LinkedHashMap<>();

        controller.getMenu().forEach((name, price) -> {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.add(label(name + "   ·   " + money(price), 13, TEXT_PRIMARY, Font.PLAIN), BorderLayout.CENTER);
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
        note.setPreferredSize(new Dimension(0, 40));
        note.putClientProperty("JTextField.placeholderText", "Ej. Sin cebolla, término medio, alergias...");
        JPanel notePanel = new JPanel(new BorderLayout(0, 6));
        notePanel.setOpaque(false);
        notePanel.add(label("Nota (opcional)", 12, TEXT_MUTED, Font.BOLD), BorderLayout.NORTH);
        notePanel.add(note, BorderLayout.CENTER);
        form.add(notePanel, BorderLayout.SOUTH);
        content.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
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

    private void openOutOfServiceTablesDialog() {
        JDialog dialog = new JDialog(this, "Mesas fuera de servicio", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(520, 360);
        dialog.setLocationRelativeTo(this);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(APP_BG);
        content.setBorder(new EmptyBorder(22, 24, 20, 24));
        content.add(label("Selecciona las mesas que no estarán disponibles para nuevos pedidos.",
                13, TEXT_PRIMARY, Font.BOLD), BorderLayout.NORTH);

        JPanel choices = new JPanel(new GridLayout(0, 3, 12, 10));
        choices.setOpaque(false);
        Map<Integer, JCheckBox> tableChoices = new LinkedHashMap<>();
        Set<Integer> outOfServiceTables = controller.getOutOfServiceTables();
        for (int number = 1; number <= 12; number++) {
            JCheckBox choice = new JCheckBox("Mesa " + number);
            choice.setOpaque(false);
            choice.setFont(resolveFont("Inter", Font.PLAIN, 12));
            choice.setForeground(TEXT_PRIMARY);
            choice.setSelected(outOfServiceTables.contains(number));
            if (controller.getActiveOrderForTable(number) != null) {
                choice.setText("Mesa " + number + " (ocupada)");
                choice.setEnabled(false);
            }
            tableChoices.put(number, choice);
            choices.add(choice);
        }
        content.add(choices, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.add(label("Las mesas con pedidos activos no se pueden deshabilitar.",
                11, TEXT_MUTED, Font.PLAIN), BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton cancel = secondaryButton("Cancelar");
        JButton save = primaryButton("Guardar selección");
        actions.add(cancel);
        actions.add(save);
        bottom.add(actions, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);

        cancel.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            Set<Integer> selectedTables = new LinkedHashSet<>();
            tableChoices.forEach((number, choice) -> {
                if (choice.isSelected()) {
                    selectedTables.add(number);
                }
            });
            try {
                controller.setOutOfServiceTables(selectedTables);
                dialog.dispose();
                showPage("tables");
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

    private void openCobrarFrame(Pedido order) {
        if (order == null) return;
        new CobrarFrame(order, () -> {
            cargarMesas();
            showPage(activePage);
        }).setVisible(true);
    }

    private void openCobrarFrame(com.restocontrol.model.PedidoKDS kds) {
        if (kds == null) return;
        new CobrarFrame(kds, () -> {
            cargarMesas();
            showPage(activePage);
        }).setVisible(true);
    }

    private JLabel legendItem(Color color, String text) {
        JLabel item = new JLabel(text);
        item.setFont(resolveEmojiFont(Font.BOLD, 12));
        item.setForeground(TEXT_MUTED);
        item.setIcon(new StatusDot(color));
        item.setIconTextGap(8);
        return item;
    }

    private JPanel pagePanel() {
        JPanel page = new JPanel();
        page.setBackground(APP_BG);
        page.setBorder(new EmptyBorder(22, 26, 24, 26));
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        return page;
    }

    private void styleScrollPane(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setBackground(CARD_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
    }

    private JButton primaryButton(String text) {
        return styledButton(text, ACTIVE_CYAN, Color.WHITE);
    }

    private JButton secondaryButton(String text) {
        return styledButton(text, new Color(51, 65, 85), TEXT_PRIMARY);
    }

    private JButton dangerButton(String text) {
        return styledButton(text, new Color(239, 68, 68, 40), RED_ACCENT);
    }

    private JButton styledButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(resolveFont("Inter", Font.BOLD, 12));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
        label.setFont(resolveFont("Inter", style, size));
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
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static Font resolveFont(String family, int style, int size) {
        Font font = new Font(family, style, size);
        if (!font.getFamily().equalsIgnoreCase(family)) {
            return new Font("Segoe UI", style, size);
        }
        return font;
    }

    private static Font resolveEmojiFont(int style, int size) {
        Font font = new Font("Segoe UI Emoji", style, size);
        if (!font.getFamily().equalsIgnoreCase("Segoe UI Emoji")) {
            return new Font(Font.SANS_SERIF, style, size);
        }
        return font;
    }

    // Creates 45x45 dish thumbnails loading real images from /images/ (posta.jpg, limonada.jpg, corozo.jpg)
    private static ImageIcon getDishImageIcon(String dishName) {
        return DISH_ICON_CACHE.computeIfAbsent(dishName, name -> {
            String lower = name.toLowerCase(Locale.ROOT);
            String resourceName = null;

            if (lower.contains("posta")) {
                resourceName = "/images/posta.jpg";
            } else if (lower.contains("limonada")) {
                resourceName = "/images/limonada.jpg";
            } else if (lower.contains("corozo")) {
                resourceName = "/images/corozo.jpg";
            }

            if (resourceName != null) {
                try (var input = DashboardFrame.class.getResourceAsStream(resourceName)) {
                    if (input != null) {
                        BufferedImage raw = ImageIO.read(input);
                        if (raw != null) {
                            return create45x45RoundedThumbnail(raw);
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            return create45x45FallbackThumbnail(name);
        });
    }

    private static ImageIcon create45x45RoundedThumbnail(BufferedImage raw) {
        int targetSize = 45;
        BufferedImage thumb = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = thumb.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Clip to smooth rounded rectangle (12px arc)
        g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, targetSize, targetSize, 12, 12));

        // Center-crop to fit 45x45 square perfectly without stretching
        int w = raw.getWidth();
        int h = raw.getHeight();
        int side = Math.min(w, h);
        int sx = (w - side) / 2;
        int sy = (h - side) / 2;
        g2.drawImage(raw, 0, 0, targetSize, targetSize, sx, sy, sx + side, sy + side, null);

        // Restore clip for border
        g2.setClip(null);
        g2.setColor(new Color(255, 255, 255, 70));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, targetSize - 1, targetSize - 1, 12, 12);

        g2.dispose();
        return new ImageIcon(thumb);
    }

    private static ImageIcon create45x45FallbackThumbnail(String name) {
        int targetSize = 45;
        BufferedImage image = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Gradient backdrop
        GradientPaint gp = new GradientPaint(0, 0, new Color(51, 65, 85), targetSize, targetSize, new Color(15, 23, 42));
        g2.setPaint(gp);
        g2.fillRoundRect(0, 0, targetSize, targetSize, 12, 12);

        // Border
        g2.setColor(new Color(100, 116, 139, 100));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, targetSize - 1, targetSize - 1, 12, 12);

        // Food Emoji
        String emoji = getEmojiForDish(name);
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        FontMetrics fm = g2.getFontMetrics();
        int x = (targetSize - fm.stringWidth(emoji)) / 2;
        int y = (targetSize - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(emoji, x, y);

        g2.dispose();
        return new ImageIcon(image);
    }

    private static String getEmojiForDish(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("mojarra") || lower.contains("pescado") || lower.contains("sancocho")) {
            return "🐟";
        } else if (lower.contains("mariscos") || lower.contains("camarón") || lower.contains("ceviche")) {
            return "🦐";
        } else if (lower.contains("posta")) {
            return "🥩";
        } else if (lower.contains("patacón")) {
            return "🍌";
        } else if (lower.contains("arepa")) {
            return "🫓";
        } else if (lower.contains("carimañolas")) {
            return "🧀";
        } else if (lower.contains("limonada") || lower.contains("coco")) {
            return "🥥";
        } else if (lower.contains("corozo") || lower.contains("jugo")) {
            return "🍹";
        } else if (lower.contains("panela")) {
            return "🍯";
        } else if (lower.contains("gaseosa")) {
            return "🥤";
        } else if (lower.contains("agua")) {
            return "💧";
        }
        return "🍽";
    }

    private interface OrderAction {
        void run(Pedido order) throws IOException;
    }

    // Logo panel with vibrant Cyan Glow Effect
    private static final class LogoGlowPanel extends JPanel {

        private static final long serialVersionUID = 1L;
        private final Image logoImage;

        private LogoGlowPanel(Image logoImage) {
            this.logoImage = logoImage;
            setOpaque(false);
            setPreferredSize(new Dimension(228, 92));
            setMaximumSize(new Dimension(228, 92));
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            // Cyan glow layers (#00ACC1 with decreasing alpha)
            for (int r = 38; r >= 8; r -= 5) {
                int alpha = (int) (32 * (1.0f - (float) r / 44));
                g2.setColor(new Color(0, 172, 193, Math.max(3, alpha)));
                g2.fillOval(cx - r - 35, cy - r, (r + 35) * 2, r * 2);
            }

            if (logoImage != null) {
                int imgW = logoImage.getWidth(null);
                int imgH = logoImage.getHeight(null);
                double scale = Math.min(195.0 / imgW, 70.0 / imgH);
                int dw = (int) (imgW * scale);
                int dh = (int) (imgH * scale);
                int x = (getWidth() - dw) / 2;
                int y = (getHeight() - dh) / 2;
                g2.drawImage(logoImage, x, y, dw, dh, null);
            } else {
                g2.setFont(resolveFont("Inter", Font.BOLD, 22));
                g2.setColor(ACTIVE_CYAN);
                String title = "RestoControl";
                int tx = (getWidth() - g2.getFontMetrics().stringWidth(title)) / 2;
                g2.drawString(title, tx, cy + 6);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Table cell renderers
    private static class PaddedCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focused, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            cell.setBorder(new EmptyBorder(0, 14, 0, 14));
            cell.setForeground(selected ? Color.WHITE : TEXT_PRIMARY);
            cell.setBackground(selected ? new Color(0, 172, 193, 70) : CARD_BG);
            return cell;
        }
    }

    // 45x45 Image renderer for dishes in menu JTable
    private static final class ImageCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focused, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            cell.setText("");
            cell.setHorizontalAlignment(SwingConstants.CENTER);
            cell.setVerticalAlignment(SwingConstants.CENTER);
            cell.setPreferredSize(new Dimension(45, 45));
            if (value instanceof Icon icon) {
                cell.setIcon(icon);
            } else {
                cell.setIcon(null);
            }
            cell.setOpaque(true);
            cell.setBackground(selected ? new Color(0, 172, 193, 70) : CARD_BG);
            return cell;
        }
    }

    // Pill renderer for products table ESTADO column: rounded green pill "Disponible"
    private static final class PillStatusRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focused, int row, int column) {
            JPanel container = new JPanel(new GridBagLayout());
            container.setOpaque(true);
            container.setBackground(selected ? new Color(0, 172, 193, 70) : CARD_BG);

            JLabel pill = new JLabel("Disponible", SwingConstants.CENTER) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(46, 125, 50, 50)); // #2E7D32 translucent
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                    g2.setColor(new Color(74, 222, 128));     // #4ADE80
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            pill.setFont(resolveFont("Inter", Font.BOLD, 12));
            pill.setForeground(new Color(74, 222, 128));
            pill.setBorder(new EmptyBorder(4, 12, 4, 12));

            container.add(pill);
            return container;
        }
    }

    private static final class StatusBadgeRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focused, int row, int column) {
            JLabel cell = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            cell.setHorizontalAlignment(SwingConstants.CENTER);
            cell.setFont(resolveFont("Inter", Font.BOLD, 11));
            cell.setBorder(new EmptyBorder(0, 8, 0, 8));
            cell.setBackground(selected ? new Color(0, 172, 193, 70) : CARD_BG);

            if (!selected) {
                String status = value == null ? "" : value.toString();
                if (status.equals(OrderStatus.NEW.getLabel())) {
                    cell.setForeground(ACTIVE_CYAN);
                } else if (status.equals(OrderStatus.PREPARING.getLabel())
                        || status.equals(OrderStatus.READY.getLabel())) {
                    cell.setForeground(ORANGE_ACCENT);
                } else if (status.equals(OrderStatus.DELIVERED.getLabel())
                        || status.equals(OrderStatus.FINISHED.getLabel())) {
                    cell.setForeground(GREEN_ACCENT);
                } else {
                    cell.setForeground(RED_ACCENT);
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
            cell.setBorder(new EmptyBorder(0, 0, 0, 16));
            cell.setFont(resolveFont("Inter", Font.BOLD, 13));
            cell.setForeground(selected ? Color.WHITE : TEXT_PRIMARY);
            cell.setBackground(selected ? new Color(0, 172, 193, 70) : CARD_BG);
            return cell;
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
            g2.fillOval(x, y + 3, 10, 10);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 16;
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
}