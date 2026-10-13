package com.restocontrol.ui;

import com.mycompany.restocontrol.model.Pedido;
import com.restocontrol.model.PedidoKDS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class PedidoFrame extends JFrame {

    private final Pedido pedido;
    private final PedidoKDS pedidoKds;
    private final Runnable onActualizado;

    public PedidoFrame(Pedido pedido, Runnable onActualizado) {
        this.pedido = pedido;
        this.pedidoKds = null;
        this.onActualizado = onActualizado;
        initUI();
    }

    public PedidoFrame(PedidoKDS pedidoKds, Runnable onActualizado) {
        this.pedido = null;
        this.pedidoKds = pedidoKds;
        this.onActualizado = onActualizado;
        initUI();
    }

    private void initUI() {
        setTitle("RestoControl | Detalle de Pedido");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 480);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0x0B, 0x13, 0x2B));
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x0F, 0x1A, 0x30));
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        String id = pedido != null ? pedido.getId().toString().substring(0, 8).toUpperCase() : (pedidoKds != null ? pedidoKds.getId() : "CD18E747");
        int mesa = pedido != null ? pedido.getTableNumber() : (pedidoKds != null ? pedidoKds.getMesa() : 11);

        JLabel title = new JLabel("Pedido #" + id + " - Mesa " + mesa);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(0x0B, 0x13, 0x2B));
        content.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel info = new JLabel("<html><b>Productos:</b><br>"
                + (pedido != null ? pedido.getProductSummary() : (pedidoKds != null ? pedidoKds.getProductos() : ""))
                + "<br><br><b>Total:</b> "
                + (pedido != null ? pedido.getTotal() : (pedidoKds != null ? pedidoKds.getTotal() : ""))
                + "</html>");
        info.setFont(new Font("SansSerif", Font.PLAIN, 14));
        info.setForeground(new Color(0xF8, 0xFA, 0xFC));
        content.add(info);
        content.add(Box.createVerticalGlue());
        add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        footer.setBackground(new Color(0x0F, 0x1A, 0x30));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x33, 0x41, 0x55)));

        JButton btnCerrar = new JButton("Volver");
        btnCerrar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnCerrar.setBackground(new Color(0x1E, 0x29, 0x3B));
        btnCerrar.setForeground(Color.WHITE);
        btnCerrar.addActionListener(e -> dispose());

        JButton btnCobrar = new JButton("Cobrar y Facturar");
        btnCobrar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCobrar.setBackground(new Color(0x00, 0xAC, 0xC1));
        btnCobrar.setForeground(Color.WHITE);
        btnCobrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCobrar.addActionListener(e -> {
            dispose();
            if (pedido != null) {
                new CobrarFrame(pedido, onActualizado).setVisible(true);
            } else {
                new CobrarFrame(pedidoKds, onActualizado).setVisible(true);
            }
        });

        footer.add(btnCerrar);
        footer.add(btnCobrar);
        add(footer, BorderLayout.SOUTH);
    }
}
