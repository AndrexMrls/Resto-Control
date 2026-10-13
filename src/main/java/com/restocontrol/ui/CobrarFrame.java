package com.restocontrol.ui;

import com.mycompany.restocontrol.model.OrderItem;
import com.mycompany.restocontrol.model.Pedido;
import com.mycompany.restocontrol.model.SesionActual;
import com.mycompany.restocontrol.repository.DatabaseManager;
import com.restocontrol.dao.PedidoDAO;
import com.restocontrol.model.PedidoKDS;
import com.restocontrol.util.TicketPrinter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Locale;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class CobrarFrame extends JFrame {

    private static final Color BG_DARK = new Color(0x0B, 0x13, 0x2B);
    private static final Color CARD_BG = new Color(0x1E, 0x29, 0x3B);
    private static final Color SIDEBAR_BG = new Color(0x0F, 0x1A, 0x30);
    private static final Color ACTIVE_CYAN = new Color(0x00, 0xAC, 0xC1);
    private static final Color GREEN_ACCENT = new Color(0x10, 0xB9, 0x81);
    private static final Color TEXT_PRIMARY = new Color(0xF8, 0xFA, 0xFC);
    private static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);
    private static final Color BORDER_COLOR = new Color(0x33, 0x41, 0x55);

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    static {
        CURRENCY.setMinimumFractionDigits(0);
        CURRENCY.setMaximumFractionDigits(0);
    }

    private final Pedido pedidoActual;
    private final PedidoKDS pedidoKdsActual;
    private final Runnable onCobradoCallback;

    private final ButtonGroup metodoGroup = new ButtonGroup();
    private final JCheckBox chkPropina = new JCheckBox("Propina sugerida voluntaria (10%)");
    private final JTextField txtDescuento = new JTextField("0");
    private final JTextField txtMontoRecibido = new JTextField();
    private final JLabel lblSubtotal = new JLabel();
    private final JLabel lblPropinaValor = new JLabel("$0");
    private final JLabel lblTotalPagar = new JLabel();
    private final JLabel lblCambio = new JLabel("$0");

    private BigDecimal subtotalBase = BigDecimal.ZERO;
    private BigDecimal propinaCalculada = BigDecimal.ZERO;
    private BigDecimal descuentoAplicado = BigDecimal.ZERO;
    private BigDecimal totalCalculado = BigDecimal.ZERO;

    public CobrarFrame(Pedido pedido, Runnable onCobrado) {
        this.pedidoActual = pedido;
        this.pedidoKdsActual = null;
        this.onCobradoCallback = onCobrado;
        this.subtotalBase = pedido != null ? pedido.getTotal() : BigDecimal.ZERO;
        initUI();
    }

    public CobrarFrame(PedidoKDS pedidoKds, Runnable onCobrado) {
        this.pedidoActual = null;
        this.pedidoKdsActual = pedidoKds;
        this.onCobradoCallback = onCobrado;
        this.subtotalBase = pedidoKds != null ? pedidoKds.getTotal() : BigDecimal.ZERO;
        initUI();
    }

    private void initUI() {
        setTitle("RestoControl | Cobro y Facturación");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(560, 680);
        setMinimumSize(new Dimension(500, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        // Header
        add(buildHeader(), BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_DARK);
        content.setBorder(new EmptyBorder(16, 20, 16, 20));

        content.add(buildDetallePedidoCard());
        content.add(Box.createVerticalStrut(14));
        content.add(buildCalculoCard());
        content.add(Box.createVerticalStrut(14));
        content.add(buildMetodoPagoCard());

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_DARK);
        add(scrollPane, BorderLayout.CENTER);

        // Footer Actions
        add(buildActionButtons(), BorderLayout.SOUTH);

        recalcularTotales();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SIDEBAR_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(16, 20, 16, 20)
        ));

        String idStr = getPedidoId();
        int mesaNum = getMesaNum();

        JLabel title = new JLabel("Cobrar y Facturar");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Mesa " + mesaNum + " • Factura #" + idStr);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(ACTIVE_CYAN);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(subtitle);

        header.add(titles, BorderLayout.WEST);
        return header;
    }

    private JPanel buildDetallePedidoCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(0, 8));

        JLabel title = new JLabel("Resumen de Consumo");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(TEXT_PRIMARY);
        card.add(title, BorderLayout.NORTH);

        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setOpaque(false);

        if (pedidoActual != null && pedidoActual.getItems() != null && !pedidoActual.getItems().isEmpty()) {
            for (OrderItem it : pedidoActual.getItems()) {
                itemsPanel.add(buildItemRow(it.getQuantity() + "x " + it.getName(), CURRENCY.format(it.getSubtotal())));
            }
        } else if (pedidoKdsActual != null) {
            String prod = pedidoKdsActual.getProductos();
            if (prod != null) {
                for (String part : prod.split(",")) {
                    itemsPanel.add(buildItemRow(part.trim(), ""));
                }
            }
        } else {
            itemsPanel.add(buildItemRow("Consumo general", CURRENCY.format(subtotalBase)));
        }

        card.add(itemsPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildItemRow(String textLeft, String textRight) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));

        JLabel lblLeft = new JLabel(textLeft);
        lblLeft.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblLeft.setForeground(TEXT_PRIMARY);

        JLabel lblRight = new JLabel(textRight);
        lblRight.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRight.setForeground(TEXT_MUTED);

        row.add(lblLeft, BorderLayout.WEST);
        row.add(lblRight, BorderLayout.EAST);
        return row;
    }

    private JPanel buildCalculoCard() {
        JPanel card = createCardPanel();
        card.setLayout(new GridLayout(4, 2, 8, 8));

        // 1. Subtotal
        JLabel lblSubTitle = new JLabel("Subtotal:");
        lblSubTitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblSubTitle.setForeground(TEXT_MUTED);

        lblSubtotal.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblSubtotal.setForeground(TEXT_PRIMARY);
        lblSubtotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblSubtotal.setText(CURRENCY.format(subtotalBase));

        card.add(lblSubTitle);
        card.add(lblSubtotal);

        // 2. Propina
        chkPropina.setFont(new Font("SansSerif", Font.PLAIN, 12));
        chkPropina.setForeground(TEXT_PRIMARY);
        chkPropina.setOpaque(false);
        chkPropina.setSelected(true); // Propina voluntaria sugerida por defecto
        chkPropina.addActionListener(e -> recalcularTotales());

        lblPropinaValor.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblPropinaValor.setForeground(ACTIVE_CYAN);
        lblPropinaValor.setHorizontalAlignment(SwingConstants.RIGHT);

        card.add(chkPropina);
        card.add(lblPropinaValor);

        // 3. Descuento
        JLabel lblDescTitle = new JLabel("Descuento ($):");
        lblDescTitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblDescTitle.setForeground(TEXT_MUTED);

        txtDescuento.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txtDescuento.setBackground(SIDEBAR_BG);
        txtDescuento.setForeground(TEXT_PRIMARY);
        txtDescuento.setCaretColor(Color.WHITE);
        txtDescuento.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        txtDescuento.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                recalcularTotales();
            }
        });

        card.add(lblDescTitle);
        card.add(txtDescuento);

        // 4. Total General
        JLabel lblTotalTitle = new JLabel("TOTAL A PAGAR:");
        lblTotalTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTotalTitle.setForeground(TEXT_PRIMARY);

        lblTotalPagar.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTotalPagar.setForeground(ACTIVE_CYAN);
        lblTotalPagar.setHorizontalAlignment(SwingConstants.RIGHT);

        card.add(lblTotalTitle);
        card.add(lblTotalPagar);

        return card;
    }

    private JPanel buildMetodoPagoCard() {
        JPanel card = createCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Método de Pago y Arqueo");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        // Radio buttons metodo
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        radios.setOpaque(false);
        radios.setAlignmentX(Component.LEFT_ALIGNMENT);

        JRadioButton rbEfectivo = createRadioButton("Efectivo", true);
        JRadioButton rbNequi = createRadioButton("Nequi", false);
        JRadioButton rbTarjeta = createRadioButton("Tarjeta", false);

        metodoGroup.add(rbEfectivo);
        metodoGroup.add(rbNequi);
        metodoGroup.add(rbTarjeta);

        radios.add(rbEfectivo);
        radios.add(rbNequi);
        radios.add(rbTarjeta);
        card.add(radios);
        card.add(Box.createVerticalStrut(12));

        // Monto recibido
        JLabel lblRecibido = new JLabel("Monto recibido del cliente:");
        lblRecibido.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblRecibido.setForeground(TEXT_MUTED);
        lblRecibido.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblRecibido);
        card.add(Box.createVerticalStrut(4));

        txtMontoRecibido.setFont(new Font("SansSerif", Font.BOLD, 14));
        txtMontoRecibido.setBackground(SIDEBAR_BG);
        txtMontoRecibido.setForeground(TEXT_PRIMARY);
        txtMontoRecibido.setCaretColor(Color.WHITE);
        txtMontoRecibido.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        txtMontoRecibido.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        txtMontoRecibido.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtMontoRecibido.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                recalcularCambio();
            }
        });
        card.add(txtMontoRecibido);
        card.add(Box.createVerticalStrut(8));

        // Botones de billetes rapidos ($50.000, $100.000, Exacto)
        JPanel quickCash = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quickCash.setOpaque(false);
        quickCash.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnExacto = createQuickCashButton("Exacto");
        btnExacto.addActionListener(e -> {
            txtMontoRecibido.setText(totalCalculado.setScale(0, RoundingMode.HALF_UP).toString());
            recalcularCambio();
        });

        JButton btn50k = createQuickCashButton("$50.000");
        btn50k.addActionListener(e -> {
            txtMontoRecibido.setText("50000");
            recalcularCambio();
        });

        JButton btn100k = createQuickCashButton("$100.000");
        btn100k.addActionListener(e -> {
            txtMontoRecibido.setText("100000");
            recalcularCambio();
        });

        quickCash.add(btnExacto);
        quickCash.add(btn50k);
        quickCash.add(btn100k);
        card.add(quickCash);
        card.add(Box.createVerticalStrut(10));

        // Cambio / Devuelta
        JPanel cambioRow = new JPanel(new BorderLayout());
        cambioRow.setOpaque(false);
        cambioRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblCambioTitle = new JLabel("Cambio / Devuelta:");
        lblCambioTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblCambioTitle.setForeground(TEXT_MUTED);

        lblCambio.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblCambio.setForeground(GREEN_ACCENT);
        lblCambio.setHorizontalAlignment(SwingConstants.RIGHT);

        cambioRow.add(lblCambioTitle, BorderLayout.WEST);
        cambioRow.add(lblCambio, BorderLayout.EAST);
        card.add(cambioRow);

        return card;
    }

    private JPanel buildActionButtons() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 16));
        footer.setBackground(SIDEBAR_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnCancelar.setBackground(CARD_BG);
        btnCancelar.setForeground(TEXT_PRIMARY);
        btnCancelar.setFocusPainted(false);
        btnCancelar.setBorder(new EmptyBorder(10, 18, 10, 18));
        btnCancelar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCancelar.addActionListener(e -> dispose());

        JButton btnCobrar = new JButton("Cobrar y Facturar");
        btnCobrar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCobrar.setBackground(ACTIVE_CYAN);
        btnCobrar.setForeground(Color.WHITE);
        btnCobrar.setFocusPainted(false);
        btnCobrar.setBorder(new EmptyBorder(10, 24, 10, 24));
        btnCobrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCobrar.addActionListener(e -> ejecutarCobroYFacturacion());

        footer.add(btnCancelar);
        footer.add(btnCobrar);
        return footer;
    }

    // --- LOGICA DE COBRO ---

    private void recalcularTotales() {
        if (chkPropina.isSelected()) {
            propinaCalculada = subtotalBase.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.HALF_UP);
        } else {
            propinaCalculada = BigDecimal.ZERO;
        }
        lblPropinaValor.setText(CURRENCY.format(propinaCalculada));

        try {
            String descStr = txtDescuento.getText().replaceAll("[^0-9]", "");
            descuentoAplicado = descStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(descStr);
        } catch (Exception ex) {
            descuentoAplicado = BigDecimal.ZERO;
        }

        totalCalculado = subtotalBase.add(propinaCalculada).subtract(descuentoAplicado);
        if (totalCalculado.compareTo(BigDecimal.ZERO) < 0) {
            totalCalculado = BigDecimal.ZERO;
        }
        lblTotalPagar.setText(CURRENCY.format(totalCalculado));

        if (txtMontoRecibido.getText().isBlank()) {
            txtMontoRecibido.setText(totalCalculado.setScale(0, RoundingMode.HALF_UP).toString());
        }
        recalcularCambio();
    }

    private void recalcularCambio() {
        try {
            String recStr = txtMontoRecibido.getText().replaceAll("[^0-9]", "");
            BigDecimal recibido = recStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(recStr);
            BigDecimal cambio = recibido.subtract(totalCalculado);
            if (cambio.compareTo(BigDecimal.ZERO) < 0) {
                lblCambio.setText("Faltan " + CURRENCY.format(cambio.abs()));
                lblCambio.setForeground(new Color(0xEF, 0x44, 0x44));
            } else {
                lblCambio.setText(CURRENCY.format(cambio));
                lblCambio.setForeground(GREEN_ACCENT);
            }
        } catch (Exception ex) {
            lblCambio.setText("$0");
        }
    }

    private void ejecutarCobroYFacturacion() {
        String metodo = "Efectivo";
        for (Enumeration<AbstractButton> buttons = metodoGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                metodo = button.getText();
                break;
            }
        }

        BigDecimal montoRecibido;
        try {
            String recStr = txtMontoRecibido.getText().replaceAll("[^0-9]", "");
            montoRecibido = recStr.isEmpty() ? totalCalculado : new BigDecimal(recStr);
        } catch (Exception ex) {
            montoRecibido = totalCalculado;
        }

        if ("Efectivo".equalsIgnoreCase(metodo) && montoRecibido.compareTo(totalCalculado) < 0) {
            JOptionPane.showMessageDialog(this,
                    "El monto recibido (" + CURRENCY.format(montoRecibido) + ") es menor al total (" + CURRENCY.format(totalCalculado) + ").",
                    "Monto insuficiente", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal cambio = montoRecibido.subtract(totalCalculado);
        if (cambio.compareTo(BigDecimal.ZERO) < 0) {
            cambio = BigDecimal.ZERO;
        }

        String pedidoId = getPedidoId();
        int mesaNum = getMesaNum();

        // 1. Guardar pago en BD
        guardarPagoEnBD(pedidoId, mesaNum, metodo, subtotalBase, propinaCalculada, descuentoAplicado, totalCalculado, montoRecibido, cambio);

        // 2. Imprimir ticket con try/catch sin crashear
        try {
            if (pedidoActual != null) {
                TicketPrinter.imprimirTicket(pedidoActual, metodo, montoRecibido, cambio, propinaCalculada, descuentoAplicado);
            } else if (pedidoKdsActual != null) {
                TicketPrinter.imprimirTicket(pedidoKdsActual, metodo, montoRecibido, cambio, propinaCalculada, descuentoAplicado);
            } else {
                TicketPrinter.imprimirTicketPorId(pedidoId, metodo, montoRecibido, cambio, propinaCalculada, descuentoAplicado);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ticket guardado en carpeta tickets", "Aviso de Impresión", JOptionPane.INFORMATION_MESSAGE);
        }

        // 3. Callback y cierre
        dispose();
        if (onCobradoCallback != null) {
            onCobradoCallback.run();
        }
    }

    private void guardarPagoEnBD(String pedidoId, int mesaNum, String metodo, BigDecimal subtotal,
                                 BigDecimal propina, BigDecimal descuento, BigDecimal total,
                                 BigDecimal montoRecibido, BigDecimal cambio) {
        try {
            // Actualizar estado en pedidos y liberar mesa
            DatabaseManager.getInstance().actualizarEstadoPedido(pedidoId, "ENTREGADO");
            DatabaseManager.getInstance().liberarMesa(mesaNum);
            PedidoDAO.getInstance().liberarMesa(mesaNum);

            // Guardar registro en tabla pagos
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                conn.createStatement().execute("""
                    CREATE TABLE IF NOT EXISTS pagos (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        pedido_id VARCHAR(64) NOT NULL,
                        turno_id INT,
                        fecha TIMESTAMP NOT NULL,
                        metodo VARCHAR(30) NOT NULL,
                        subtotal DECIMAL(12,2) NOT NULL,
                        propina DECIMAL(12,2) DEFAULT 0,
                        descuento DECIMAL(12,2) DEFAULT 0,
                        total DECIMAL(12,2) NOT NULL,
                        monto_recibido DECIMAL(12,2) NOT NULL,
                        cambio DECIMAL(12,2) NOT NULL
                    )
                """);

                String sql = "INSERT INTO pagos (pedido_id, turno_id, fecha, metodo, subtotal, propina, descuento, total, monto_recibido, cambio) VALUES (?, ?, NOW(), ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, pedidoId);
                    ps.setInt(2, SesionActual.getInstance().getTurnoId());
                    ps.setString(3, metodo);
                    ps.setBigDecimal(4, subtotal);
                    ps.setBigDecimal(5, propina);
                    ps.setBigDecimal(6, descuento);
                    ps.setBigDecimal(7, total);
                    ps.setBigDecimal(8, montoRecibido);
                    ps.setBigDecimal(9, cambio);
                    ps.executeUpdate();
                }
            }
        } catch (Exception ex) {
            System.err.println("Aviso al registrar pago en BD: " + ex.getMessage());
        }
    }

    private String getPedidoId() {
        if (pedidoActual != null) {
            String s = pedidoActual.getId().toString().replace("-", "").toUpperCase();
            return s.length() >= 8 ? s.substring(0, 8) : s;
        }
        if (pedidoKdsActual != null) {
            return pedidoKdsActual.getId();
        }
        return "CD18E747";
    }

    private int getMesaNum() {
        if (pedidoActual != null) return pedidoActual.getTableNumber();
        if (pedidoKdsActual != null) return pedidoKdsActual.getMesa();
        return 11;
    }

    // --- COMPONENTES UI HELPERS ---

    private JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        return card;
    }

    private JRadioButton createRadioButton(String text, boolean selected) {
        JRadioButton rb = new JRadioButton(text, selected);
        rb.setFont(new Font("SansSerif", Font.PLAIN, 13));
        rb.setForeground(TEXT_PRIMARY);
        rb.setOpaque(false);
        rb.setFocusPainted(false);
        return rb;
    }

    private JButton createQuickCashButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setBackground(CARD_BG);
        btn.setForeground(ACTIVE_CYAN);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACTIVE_CYAN, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
