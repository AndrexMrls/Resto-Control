package com.mycompany.restocontrol.view;

import com.mycompany.restocontrol.model.Mesa;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class MesaCard extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final Color COLOR_DISPONIBLE = new Color(0x2E, 0x7D, 0x32); // #2E7D32
    public static final Color COLOR_OCUPADA = new Color(0xFF, 0x8F, 0x00);    // #FF8F00
    public static final Color COLOR_FUERA_SERVICIO = new Color(0x47, 0x55, 0x69); // #475569

    private final int tableNumber;
    private final boolean occupied;
    private final boolean outOfService;
    private final String timerText;
    private final Color cardBg;
    private boolean hovered = false;
    private final int cornerRadius;

    public MesaCard(Mesa mesa, boolean compact, Consumer<Integer> onClick) {
        this(
            mesa.getNumber(),
            mesa.isOccupied(),
            mesa.isOutOfService(),
            mesa.getTimerText(),
            compact,
            onClick
        );
    }

    public MesaCard(int tableNumber, boolean occupied, boolean outOfService, String timerText,
                    boolean compact, Consumer<Integer> onClick) {
        this.tableNumber = tableNumber;
        this.occupied = occupied;
        this.outOfService = outOfService;
        this.timerText = timerText != null && !timerText.isBlank() ? timerText : "0 min";
        this.cornerRadius = compact ? 12 : 16;

        if (outOfService) {
            this.cardBg = COLOR_FUERA_SERVICIO;
        } else if (occupied) {
            this.cardBg = COLOR_OCUPADA;
        } else {
            this.cardBg = COLOR_DISPONIBLE;
        }

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(compact ? 8 : 14, compact ? 10 : 14, compact ? 8 : 14, compact ? 10 : 14));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Table Number (e.g., Mesa 01)
        String mesaName = String.format("Mesa %02d", tableNumber);
        JLabel titleLabel = new JLabel(mesaName);
        titleLabel.setFont(resolveFont("Inter", Font.BOLD, compact ? 12 : 14));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(titleLabel);

        content.add(Box.createVerticalStrut(compact ? 4 : 8));

        // State & details
        if (outOfService) {
            JLabel stateLabel = new JLabel("Fuera de servicio");
            stateLabel.setFont(resolveFont("Inter", Font.PLAIN, compact ? 10 : 11));
            stateLabel.setForeground(new Color(226, 232, 240));
            stateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(stateLabel);
        } else if (occupied) {
            JLabel stateLabel = new JLabel("Ocupada");
            stateLabel.setFont(resolveFont("Inter", Font.BOLD, compact ? 10 : 11));
            stateLabel.setForeground(Color.WHITE);
            stateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(stateLabel);

            content.add(Box.createVerticalStrut(compact ? 2 : 4));

            JLabel timerLabel = new JLabel("\u23F1 " + this.timerText);
            timerLabel.setFont(resolveEmojiFont(Font.BOLD, compact ? 10 : 11));
            timerLabel.setForeground(new Color(255, 243, 224));
            timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(timerLabel);
        } else {
            JLabel stateLabel = new JLabel("Disponible \u2705");
            stateLabel.setFont(resolveEmojiFont(Font.BOLD, compact ? 11 : 12));
            stateLabel.setForeground(Color.WHITE);
            stateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(stateLabel);
        }

        add(content, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (onClick != null) {
                    onClick.accept(tableNumber);
                }
            }
        });
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public boolean isOutOfService() {
        return outOfService;
    }

    public String getTimerText() {
        return timerText;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();

        // Subtle drop shadow
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRoundRect(1, 2, width - 2, height - 3, cornerRadius, cornerRadius);

        // Fill background
        Color fill = hovered ? brighten(cardBg, 1.12f) : cardBg;
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, width - 1, height - 2, cornerRadius, cornerRadius);

        // Subtle highlight or border
        if (hovered) {
            g2.setColor(new Color(255, 255, 255, 120));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, width - 1, height - 2, cornerRadius, cornerRadius);
        } else {
            g2.setColor(new Color(255, 255, 255, 35));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, width - 1, height - 2, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);
    }

    private static Color brighten(Color c, float factor) {
        int r = Math.min(255, (int) (c.getRed() * factor));
        int g = Math.min(255, (int) (c.getGreen() * factor));
        int b = Math.min(255, (int) (c.getBlue() * factor));
        return new Color(r, g, b, c.getAlpha());
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
}
