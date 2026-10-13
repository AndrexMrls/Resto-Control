package com.restocontrol.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.mycompany.restocontrol.controller.RestaurantController;
import com.mycompany.restocontrol.model.SesionActual;
import com.mycompany.restocontrol.model.Usuario;
import com.mycompany.restocontrol.repository.DatabaseManager;
import com.mycompany.restocontrol.repository.RestaurantRepository;
import com.mycompany.restocontrol.view.LoginFrame;
import com.restocontrol.dao.PedidoDAO;
import com.restocontrol.model.PedidoKDS;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class KitchenFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    // Palette colors specified in requirements
    private static final Color BG_DARK = new Color(0x12, 0x12, 0x12);       // #121212
    private static final Color HEADER_BG = new Color(0x18, 0x18, 0x18);     // #181818
    private static final Color CARD_BG = new Color(0x1E, 0x1E, 0x1E);       // #1E1E1E
    private static final Color BORDER_SUBTLE = new Color(0x2A, 0x2A, 0x2A); // #2A2A2A

    public static final Color COLOR_POR_PREPARAR = new Color(0xFF, 0x98, 0x00); // #FF9800 (Orange)
    public static final Color COLOR_EN_PREPARACION = new Color(0x21, 0x96, 0xF3); // #2196F3 (Blue)
    public static final Color COLOR_LISTO = new Color(0x4C, 0xAF, 0x50);       // #4CAF50 (Green)
    public static final Color COLOR_CRITICO = new Color(0xF4, 0x43, 0x36);     // #F44336 (Red)
    public static final Color COLOR_URGENTE = new Color(0xD8, 0x43, 0x15);     // Dark Orange/Red
    public static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);        // #94A3B8

    private final RestaurantController controller;
    private final PedidoDAO pedidoDAO = PedidoDAO.getInstance();

    // Data structures
    private final List<PedidoKDS> currentOrders = new ArrayList<>();
    private final Set<String> knownOrderIds = new HashSet<>();

    // Timers
    private Timer reloadTimer;
    private Timer secondTickTimer;

    // Blink & Animation states
    private boolean blinkPhase = false;
    private int bellBlinkTicks = 0;
    private int newOrdersBadgeCount = 0;

    // UI Components
    private final BellComponent bellComponent = new BellComponent();
    private final JLabel clockLabel = new JLabel("19:42:00");
    private final JLabel footerStatusLabel = new JLabel("Conectado • 4 pedidos activos • Última actualización: --:--:--");

    // Column Containers
    private final JPanel colPorPrepararList = new JPanel();
    private final JPanel colEnPreparacionList = new JPanel();
    private final JPanel colListoList = new JPanel();

    // Column Count Badges
    private final PillBadge badgePorPreparar = new PillBadge("1", COLOR_POR_PREPARAR);
    private final PillBadge badgeEnPreparacion = new PillBadge("2", COLOR_EN_PREPARACION);
    private final PillBadge badgeListo = new PillBadge("1", COLOR_LISTO);

    public KitchenFrame() {
        this(null);
    }

    public KitchenFrame(RestaurantController controller) {
        this.controller = controller;
        initFrame();
        buildUI();
        initTimers();
        cargarPedidos(true);
    }

    private void initFrame() {
        setTitle("RestoControl | Kitchen Display System (KDS)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);

        // 1. Header
        root.add(buildHeader(), BorderLayout.NORTH);

        // 2. Center: 3 Equal Columns
        root.add(buildColumnsPanel(), BorderLayout.CENTER);

        // 3. Footer
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_SUBTLE),
                new EmptyBorder(12, 24, 12, 24)
        ));

        // Left Header: Chef Icon + Title + Station
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);

        JLabel chefIcon = new JLabel("👨‍🍳");
        chefIcon.setFont(resolveEmojiFont(Font.PLAIN, 28));
        left.add(chefIcon);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("KDS Kitchen Display System");
        title.setFont(resolveFont("Inter", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        titleBlock.add(title);

        JLabel station = new JLabel("Cocina - Estación Principal");
        station.setFont(resolveFont("Inter", Font.PLAIN, 12));
        station.setForeground(TEXT_MUTED);
        titleBlock.add(station);

        left.add(titleBlock);
        header.add(left, BorderLayout.WEST);

        // Right Header: Bell with Badge + ONLINE Badge + Clock + Actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);

        // Interactive Bell
        bellComponent.setToolTipText("Haz clic para silenciar / reconocer pedidos nuevos");
        bellComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                newOrdersBadgeCount = 0;
                bellBlinkTicks = 0;
                bellComponent.repaint();
            }
        });
        right.add(bellComponent);

        // Online Badge
        JPanel onlinePill = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4)) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(76, 175, 80, 35));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(COLOR_LISTO);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        onlinePill.setOpaque(false);
        onlinePill.setBorder(new EmptyBorder(2, 10, 2, 10));

        JLabel onlineDot = new JLabel("●");
        onlineDot.setFont(resolveFont("Inter", Font.BOLD, 10));
        onlineDot.setForeground(COLOR_LISTO);
        onlinePill.add(onlineDot);

        JLabel onlineText = new JLabel("ONLINE");
        onlineText.setFont(resolveFont("Inter", Font.BOLD, 12));
        onlineText.setForeground(COLOR_LISTO);
        onlinePill.add(onlineText);
        right.add(onlinePill);

        // Current Clock
        clockLabel.setFont(resolveFont("Inter", Font.BOLD, 18));
        clockLabel.setForeground(Color.WHITE);
        right.add(clockLabel);

        // Button: Simulate incoming order (for testing beep and bell blink)
        JButton simulateBtn = createHeaderButton("➕ Simular Pedido", new Color(40, 50, 70), Color.WHITE);
        simulateBtn.setToolTipText("Genera un nuevo pedido para probar el sonido beep y la campana");
        simulateBtn.addActionListener(e -> simularNuevoPedido());
        right.add(simulateBtn);

        // Button: Reset demo
        JButton resetBtn = createHeaderButton("🔄 Reset Demo", new Color(45, 55, 72), new Color(203, 213, 225));
        resetBtn.setToolTipText("Restablece los 4 pedidos de muestra (1 por preparar, 2 en preparación, 1 listo)");
        resetBtn.addActionListener(e -> resetearDemo());
        right.add(resetBtn);

        // Button: Logout / Cerrar Turno
        JButton logoutBtn = createHeaderButton("Cerrar Turno", new Color(244, 67, 54, 35), COLOR_CRITICO);
        logoutBtn.addActionListener(e -> cerrarTurno());
        right.add(logoutBtn);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildColumnsPanel() {
        JPanel columnsRoot = new JPanel(new GridLayout(1, 3, 16, 0));
        columnsRoot.setBackground(BG_DARK);
        columnsRoot.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Columna 1: Por Preparar
        columnsRoot.add(buildColumn("Por Preparar", COLOR_POR_PREPARAR, badgePorPreparar, colPorPrepararList));

        // Columna 2: En Preparación
        columnsRoot.add(buildColumn("En Preparación", COLOR_EN_PREPARACION, badgeEnPreparacion, colEnPreparacionList));

        // Columna 3: Listo
        columnsRoot.add(buildColumn("Listo", COLOR_LISTO, badgeListo, colListoList));

        return columnsRoot;
    }

    private JPanel buildColumn(String titleText, Color accentColor, PillBadge badge, JPanel cardListPanel) {
        JPanel column = new JPanel(new BorderLayout());
        column.setBackground(BG_DARK);

        // Column Header with top color border
        JPanel colHeader = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                // 4px top colored border
                g2.setColor(accentColor);
                g2.fillRect(0, 0, getWidth(), 4);
                g2.dispose();
            }
        };
        colHeader.setBackground(new Color(0x1A, 0x1A, 0x1A));
        colHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_SUBTLE),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel title = new JLabel(titleText);
        title.setFont(resolveFont("Inter", Font.BOLD, 16));
        title.setForeground(accentColor);
        colHeader.add(title, BorderLayout.WEST);

        colHeader.add(badge, BorderLayout.EAST);
        column.add(colHeader, BorderLayout.NORTH);

        // Column List inside JScrollPane
        cardListPanel.setLayout(new BoxLayout(cardListPanel, BoxLayout.Y_AXIS));
        cardListPanel.setBackground(BG_DARK);
        cardListPanel.setBorder(new EmptyBorder(12, 4, 12, 4));

        // Wrapper to keep cards aligned to the top
        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setBackground(BG_DARK);
        topWrapper.add(cardListPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(topWrapper);
        scroll.setBorder(null);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        column.add(scroll, BorderLayout.CENTER);
        return column;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(HEADER_BG);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_SUBTLE),
                new EmptyBorder(10, 24, 10, 24)
        ));

        footerStatusLabel.setFont(resolveFont("Inter", Font.PLAIN, 13));
        footerStatusLabel.setForeground(new Color(0xCF, 0xD8, 0xDC));
        footer.add(footerStatusLabel, BorderLayout.WEST);

        JLabel roleLabel = new JLabel("Sesión: " + SesionActual.getInstance().getSessionHeaderSummary());
        roleLabel.setFont(resolveEmojiFont(Font.PLAIN, 12));
        roleLabel.setForeground(TEXT_MUTED);
        footer.add(roleLabel, BorderLayout.EAST);

        return footer;
    }

    private void initTimers() {
        // Timer 1: Reload orders from DB every 5 seconds (5000 ms)
        reloadTimer = new Timer(5000, e -> cargarPedidos(false));
        reloadTimer.start();

        // Timer 2: Tick every 1 second (1000 ms) for clocks, order elapsed times & blinking
        secondTickTimer = new Timer(1000, e -> onSecondTick());
        secondTickTimer.start();
    }

    private void onSecondTick() {
        // Live Clock
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        clockLabel.setText(LocalDateTime.now().format(dtf));

        // Toggle blink phase
        blinkPhase = !blinkPhase;

        // Bell blink countdown
        if (bellBlinkTicks > 0) {
            bellBlinkTicks--;
            bellComponent.repaint();
        }

        // Repaint cards to update elapsed minutes and blinking critical cards
        colPorPrepararList.repaint();
        colEnPreparacionList.repaint();
        colListoList.repaint();
    }

    private void cargarPedidos(boolean esPrimerInicio) {
        List<PedidoKDS> pedidos = pedidoDAO.obtenerPedidosKDS();

        // Detectar si hay pedidos nuevos
        boolean hayNuevos = false;
        if (!esPrimerInicio) {
            for (PedidoKDS p : pedidos) {
                if (!knownOrderIds.contains(p.getId())) {
                    hayNuevos = true;
                    newOrdersBadgeCount++;
                }
            }
        }

        if (hayNuevos) {
            // Reproducir sonido beep + activar parpadeo campana
            Toolkit.getDefaultToolkit().beep();
            bellBlinkTicks = 12; // parpadear por 6-12 ticks
            bellComponent.repaint();
        }

        // Actualizar set de IDs conocidos
        knownOrderIds.clear();
        for (PedidoKDS p : pedidos) {
            knownOrderIds.add(p.getId());
        }

        currentOrders.clear();
        currentOrders.addAll(pedidos);

        // Distribuir en columnas
        renderColumns();

        // Actualizar Footer
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        String ahora = LocalDateTime.now().format(timeFmt);
        footerStatusLabel.setText(String.format("● Conectado • %d pedidos activos • Última actualización: %s",
                pedidos.size(), ahora));
    }

    private void renderColumns() {
        colPorPrepararList.removeAll();
        colEnPreparacionList.removeAll();
        colListoList.removeAll();

        int countPorPreparar = 0;
        int countEnPreparacion = 0;
        int countListo = 0;

        for (PedidoKDS pedido : currentOrders) {
            String estado = pedido.getEstado();
            if ("POR_PREPARAR".equalsIgnoreCase(estado)) {
                countPorPreparar++;
                colPorPrepararList.add(new OrderCard(pedido, COLOR_POR_PREPARAR));
                colPorPrepararList.add(Box.createVerticalStrut(14));
            } else if ("EN_PREPARACION".equalsIgnoreCase(estado)) {
                countEnPreparacion++;
                colEnPreparacionList.add(new OrderCard(pedido, COLOR_EN_PREPARACION));
                colEnPreparacionList.add(Box.createVerticalStrut(14));
            } else if ("LISTO".equalsIgnoreCase(estado)) {
                countListo++;
                colListoList.add(new OrderCard(pedido, COLOR_LISTO));
                colListoList.add(Box.createVerticalStrut(14));
            }
        }

        // Agregar estados vacíos amigables si no hay pedidos
        if (countPorPreparar == 0) {
            colPorPrepararList.add(buildEmptyColumnState("No hay pedidos por preparar"));
        }
        if (countEnPreparacion == 0) {
            colEnPreparacionList.add(buildEmptyColumnState("No hay pedidos en preparación"));
        }
        if (countListo == 0) {
            colListoList.add(buildEmptyColumnState("No hay pedidos listos"));
        }

        // Actualizar pills de contadores
        badgePorPreparar.setText(String.valueOf(countPorPreparar));
        badgeEnPreparacion.setText(String.valueOf(countEnPreparacion));
        badgeListo.setText(String.valueOf(countListo));

        colPorPrepararList.revalidate();
        colPorPrepararList.repaint();
        colEnPreparacionList.revalidate();
        colEnPreparacionList.repaint();
        colListoList.revalidate();
        colListoList.repaint();
    }

    private JPanel buildEmptyColumnState(String message) {
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setBorder(new EmptyBorder(40, 20, 40, 20));

        JLabel icon = new JLabel("✨");
        icon.setFont(resolveEmojiFont(Font.PLAIN, 28));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        empty.add(icon);

        empty.add(Box.createVerticalStrut(8));

        JLabel text = new JLabel(message);
        text.setFont(resolveFont("Inter", Font.PLAIN, 13));
        text.setForeground(TEXT_MUTED);
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        empty.add(text);

        return empty;
    }

    private void moverPedidoAPreparar(PedidoKDS pedido) {
        pedidoDAO.prepararPedido(pedido.getId());
        pedido.setEstado("EN_PREPARACION");
        cargarPedidos(true);
    }

    private void moverPedidoAListo(PedidoKDS pedido) {
        pedidoDAO.marcarListo(pedido.getId());
        pedido.setEstado("LISTO");
        cargarPedidos(true);
    }

    private void entregarPedido(PedidoKDS pedido) {
        pedidoDAO.entregarPedidoYLiberarMesa(pedido.getId(), pedido.getMesa());
        cargarPedidos(true);
    }

    private void simularNuevoPedido() {
        PedidoKDS nuevo = pedidoDAO.agregarPedidoSimulado();
        // Disparar inmediatamente recarga
        cargarPedidos(false);
    }

    private void resetearDemo() {
        pedidoDAO.restablecerPedidosDemo();
        newOrdersBadgeCount = 0;
        bellBlinkTicks = 0;
        cargarPedidos(true);
    }

    private void cerrarTurno() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Deseas finalizar tu turno en cocina y cerrar sesión?",
                "Cerrar Turno de Cocina",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (reloadTimer != null) reloadTimer.stop();
            if (secondTickTimer != null) secondTickTimer.stop();

            int turnoId = SesionActual.getInstance().getTurnoId();
            DatabaseManager.getInstance().cerrarTurno(turnoId, BigDecimal.ZERO);
            SesionActual.getInstance().cerrarSesion();
            dispose();

            RestaurantController ctrl = this.controller != null ? this.controller :
                    new RestaurantController(newSafeRepo());
            new LoginFrame(ctrl).setVisible(true);
        }
    }

    private RestaurantRepository newSafeRepo() {
        try {
            return new RestaurantRepository();
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // Card Component
    // =========================================================================
    public final class OrderCard extends JPanel {

        private static final long serialVersionUID = 1L;
        private final PedidoKDS pedido;
        private final Color columnColor;
        private final JLabel timerLabel;

        public OrderCard(PedidoKDS pedido, Color columnColor) {
            this.pedido = pedido;
            this.columnColor = columnColor;
            setOpaque(false);
            setLayout(new BorderLayout(0, 8));
            setBorder(new EmptyBorder(14, 16, 14, 16));

            // 1. Top row: ORDEN #CD18E747 + Badge URGENTE (if >10min)
            JPanel topRow = new JPanel(new BorderLayout(8, 0));
            topRow.setOpaque(false);

            JLabel orderIdLabel = new JLabel("ORDEN #" + pedido.getId());
            orderIdLabel.setFont(resolveFont("Inter", Font.BOLD, 15));
            orderIdLabel.setForeground(COLOR_POR_PREPARAR);
            topRow.add(orderIdLabel, BorderLayout.WEST);

            if (pedido.isUrgente()) {
                JLabel urgenteBadge = new JLabel("URGENTE", SwingConstants.CENTER) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(COLOR_URGENTE);
                        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                urgenteBadge.setFont(resolveFont("Inter", Font.BOLD, 10));
                urgenteBadge.setForeground(Color.WHITE);
                urgenteBadge.setBorder(new EmptyBorder(3, 8, 3, 8));
                topRow.add(urgenteBadge, BorderLayout.EAST);
            }

            add(topRow, BorderLayout.NORTH);

            // 2. Center content: Table & Timer, Items bullet list, Notes
            JPanel centerPanel = new JPanel();
            centerPanel.setOpaque(false);
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

            // Segunda fila: Mesa 11 icono mesa + timer
            JPanel tableTimerRow = new JPanel(new BorderLayout(8, 0));
            tableTimerRow.setOpaque(false);
            tableTimerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel mesaLabel = new JLabel("🪑 Mesa " + String.format("%02d", pedido.getMesa()));
            mesaLabel.setFont(resolveEmojiFont(Font.BOLD, 14));
            mesaLabel.setForeground(Color.WHITE);
            tableTimerRow.add(mesaLabel, BorderLayout.WEST);

            long mins = pedido.getMinutosTranscurridos();
            timerLabel = new JLabel("⏱ " + mins + " min", SwingConstants.RIGHT);
            timerLabel.setFont(resolveEmojiFont(Font.BOLD, 13));
            applyTimerColor(timerLabel, mins);
            tableTimerRow.add(timerLabel, BorderLayout.EAST);

            centerPanel.add(tableTimerRow);
            centerPanel.add(Box.createVerticalStrut(10));

            // Items Bullet list
            List<String> items = pedido.getItemsList();
            for (String item : items) {
                JLabel itemLabel = new JLabel("• " + item);
                itemLabel.setFont(resolveFont("Inter", Font.BOLD, 13));
                itemLabel.setForeground(new Color(0xF1, 0xF5, 0xF9));
                itemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                centerPanel.add(itemLabel);
                centerPanel.add(Box.createVerticalStrut(3));
            }

            // Notas: italic gris "— Sin cilantro, picante extra"
            if (pedido.getNotas() != null && !pedido.getNotas().isBlank()) {
                centerPanel.add(Box.createVerticalStrut(6));
                JLabel notasLabel = new JLabel("— " + pedido.getNotas());
                notasLabel.setFont(resolveFont("Inter", Font.ITALIC, 12));
                notasLabel.setForeground(TEXT_MUTED);
                notasLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                centerPanel.add(notasLabel);
            }

            add(centerPanel, BorderLayout.CENTER);

            // 3. Bottom row: 3 botones (Preparar naranja, Listo azul, Entregar verde)
            JPanel buttonsRow = new JPanel(new GridLayout(1, 3, 8, 0));
            buttonsRow.setOpaque(false);
            buttonsRow.setBorder(new EmptyBorder(8, 0, 0, 0));

            JButton btnPreparar = createActionButton("Preparar", COLOR_POR_PREPARAR, Color.WHITE);
            btnPreparar.addActionListener(e -> moverPedidoAPreparar(pedido));
            buttonsRow.add(btnPreparar);

            JButton btnListo = createActionButton("Listo", COLOR_EN_PREPARACION, Color.WHITE);
            btnListo.addActionListener(e -> moverPedidoAListo(pedido));
            buttonsRow.add(btnListo);

            JButton btnEntregar = createActionButton("Entregar", COLOR_LISTO, Color.WHITE);
            btnEntregar.addActionListener(e -> entregarPedido(pedido));
            buttonsRow.add(btnEntregar);

            add(buttonsRow, BorderLayout.SOUTH);
        }

        private void applyTimerColor(JLabel label, long mins) {
            if (mins > 15) {
                // Si > 15 min rojo #F44336 y blink
                if (blinkPhase) {
                    label.setForeground(COLOR_CRITICO);
                } else {
                    label.setForeground(new Color(244, 67, 54, 70));
                }
            } else if (mins > 10) {
                label.setForeground(COLOR_POR_PREPARAR);
            } else {
                label.setForeground(COLOR_LISTO);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Fondo #1E1E1E rounded 16
            g2.setColor(CARD_BG);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);

            // Borde 1px color de la columna
            Color borderC = columnColor;
            if (pedido.isCritico() && blinkPhase) {
                // Alerta crítica: parpadea borde en rojo
                borderC = COLOR_CRITICO;
                g2.setStroke(new BasicStroke(1.8f));
            } else {
                g2.setStroke(new BasicStroke(1.0f));
            }

            g2.setColor(borderC);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);

            g2.dispose();
            super.paintComponent(g);

            // Actualizar timer dinámicamente en cada paint
            long mins = pedido.getMinutosTranscurridos();
            timerLabel.setText("⏱ " + mins + " min");
            applyTimerColor(timerLabel, mins);
        }
    }

    // =========================================================================
    // Bell Component with blink & badge
    // =========================================================================
    private final class BellComponent extends JPanel {

        private static final long serialVersionUID = 1L;

        public BellComponent() {
            setOpaque(false);
            setPreferredSize(new Dimension(46, 36));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Campana blink highlight if active
            if (bellBlinkTicks > 0 && blinkPhase) {
                g2.setColor(new Color(255, 152, 0, 60));
                g2.fillOval(4, 2, 32, 32);
            }

            // Bell Emoji icon
            g2.setFont(resolveEmojiFont(Font.PLAIN, 18));
            g2.drawString("🔔", 10, 24);

            // Badge overlay (+1, +2, etc.)
            if (newOrdersBadgeCount > 0) {
                String badgeText = "+" + newOrdersBadgeCount;
                g2.setFont(resolveFont("Inter", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                int bw = Math.max(18, fm.stringWidth(badgeText) + 6);
                int bh = 15;
                int bx = w - bw - 1;
                int by = 2;

                g2.setColor(COLOR_CRITICO);
                g2.fillRoundRect(bx, by, bw, bh, 8, 8);

                g2.setColor(Color.WHITE);
                int tx = bx + (bw - fm.stringWidth(badgeText)) / 2;
                int ty = by + ((bh - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(badgeText, tx, ty);
            }

            g2.dispose();
        }
    }

    // =========================================================================
    // Reusable Pill Badge
    // =========================================================================
    public static final class PillBadge extends JLabel {

        private static final long serialVersionUID = 1L;
        private final Color accentColor;

        public PillBadge(String text, Color accentColor) {
            super(text, SwingConstants.CENTER);
            this.accentColor = accentColor;
            setFont(resolveFont("Inter", Font.BOLD, 12));
            setForeground(Color.WHITE);
            setBorder(new EmptyBorder(3, 10, 3, 10));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(accentColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =========================================================================
    // UI Helpers & Styling
    // =========================================================================
    private JButton createActionButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(resolveFont("Inter", Font.BOLD, 11));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(6, 6, 6, 6));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    private JButton createHeaderButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(resolveEmojiFont(Font.BOLD, 11));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("JButton.buttonType", "roundRect");
        return btn;
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

    // Standalone Main execution method
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.put("Button.arc", 16);
        UIManager.put("Component.arc", 14);

        EventQueue.invokeLater(() -> {
            DatabaseManager.getInstance();
            KitchenFrame frame = new KitchenFrame(null);
            frame.setVisible(true);
        });
    }
}
