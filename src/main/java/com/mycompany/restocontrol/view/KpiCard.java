package com.mycompany.restocontrol.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class KpiCard extends RoundedPanel {

    private static final long serialVersionUID = 1L;

    private static final Color CARD_BG = new Color(30, 41, 59); // #1E293B
    private static final Color TITLE_COLOR = new Color(148, 163, 184); // #94A3B8
    private static final Color VALUE_COLOR = new Color(248, 250, 252); // #F8FAFC
    private static final Color SUBTITLE_COLOR = new Color(100, 116, 139); // #64748B

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;
    private final CircularIconBadge iconBadge;
    private final Color accentColor;

    public KpiCard(String title, String value, String subtitle, String iconEmoji, Color accentColor) {
        super(16, CARD_BG);
        this.accentColor = accentColor != null ? accentColor : new Color(0, 172, 193);

        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(16, 18, 16, 18));
        setPreferredSize(new Dimension(220, 140));
        setMinimumSize(new Dimension(180, 130));

        // Top bar with title and circular icon at the top
        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        topPanel.setOpaque(false);

        titleLabel = new JLabel(title);
        titleLabel.setFont(resolveFont("Inter", Font.BOLD, 12));
        titleLabel.setForeground(TITLE_COLOR);
        topPanel.add(titleLabel, BorderLayout.WEST);

        iconBadge = new CircularIconBadge(iconEmoji != null ? iconEmoji : "●", this.accentColor, 40);
        topPanel.add(iconBadge, BorderLayout.EAST);

        // Center content with large 32px value and subtitle
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        contentPanel.add(Box.createVerticalStrut(10));

        valueLabel = new JLabel(value);
        valueLabel.setFont(resolveFont("Inter", Font.BOLD, 32));
        valueLabel.setForeground(VALUE_COLOR);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(valueLabel);

        contentPanel.add(Box.createVerticalStrut(4));

        subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(resolveFont("Inter", Font.PLAIN, 11));
        subtitleLabel.setForeground(SUBTITLE_COLOR);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(subtitleLabel);

        add(topPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    public KpiCard(String title, String value, String subtitle, Color accentColor) {
        this(title, value, subtitle, "●", accentColor);
    }

    public void setValue(String value) {
        this.valueLabel.setText(value);
    }

    public String getValue() {
        return this.valueLabel.getText();
    }

    public void setTitle(String title) {
        this.titleLabel.setText(title);
    }

    public String getTitle() {
        return this.titleLabel.getText();
    }

    public void setSubtitle(String subtitle) {
        this.subtitleLabel.setText(subtitle);
    }

    public String getSubtitle() {
        return this.subtitleLabel.getText();
    }

    public JLabel getValueLabel() {
        return valueLabel;
    }

    public Color getAccentColor() {
        return accentColor;
    }

    private static Font resolveFont(String family, int style, int size) {
        Font font = new Font(family, style, size);
        if (!font.getFamily().equalsIgnoreCase(family)) {
            return new Font("Segoe UI", style, size);
        }
        return font;
    }

    private static final class CircularIconBadge extends JPanel {

        private static final long serialVersionUID = 1L;
        private final String iconText;
        private final Color accent;
        private final int diameter;

        private CircularIconBadge(String iconText, Color accent, int diameter) {
            this.iconText = iconText;
            this.accent = accent;
            this.diameter = diameter;
            setOpaque(false);
            setPreferredSize(new Dimension(diameter, diameter));
            setMinimumSize(new Dimension(diameter, diameter));
            setMaximumSize(new Dimension(diameter, diameter));
            setLayout(new BorderLayout());

            JLabel label = new JLabel(iconText, SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI Emoji", Font.PLAIN, diameter / 2));
            label.setForeground(accent);
            add(label, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;

            // Background circle with subtle translucent accent
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
            g2.fillOval(x, y, diameter, diameter);

            // Subtle border
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
            g2.drawOval(x, y, diameter - 1, diameter - 1);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
