package com.mycompany.restocontrol.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class RoundedPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private int cornerRadius;
    private Color backgroundColor;
    private Color borderColor;
    private float borderWidth;
    private boolean showShadow;
    private int shadowSize = 3;

    public RoundedPanel() {
        this(16, new Color(30, 41, 59));
    }

    public RoundedPanel(int cornerRadius) {
        this(cornerRadius, new Color(30, 41, 59));
    }

    public RoundedPanel(int cornerRadius, Color backgroundColor) {
        super();
        this.cornerRadius = cornerRadius;
        this.backgroundColor = backgroundColor;
        this.borderColor = new Color(51, 65, 85, 120);
        this.borderWidth = 1f;
        this.showShadow = true;
        setOpaque(false);
    }

    public RoundedPanel(LayoutManager layout) {
        this(layout, 16, new Color(30, 41, 59));
    }

    public RoundedPanel(LayoutManager layout, int cornerRadius) {
        this(layout, cornerRadius, new Color(30, 41, 59));
    }

    public RoundedPanel(LayoutManager layout, int cornerRadius, Color backgroundColor) {
        super(layout);
        this.cornerRadius = cornerRadius;
        this.backgroundColor = backgroundColor;
        this.borderColor = new Color(51, 65, 85, 120);
        this.borderWidth = 1f;
        this.showShadow = true;
        setOpaque(false);
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        repaint();
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        repaint();
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public boolean isShowShadow() {
        return showShadow;
    }

    public void setShowShadow(boolean showShadow) {
        this.showShadow = showShadow;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();
        int bottomMargin = showShadow ? shadowSize : 0;

        if (showShadow && width > 8 && height > 8) {
            for (int i = shadowSize; i > 0; i--) {
                g2.setColor(new Color(0, 0, 0, 12 + (shadowSize - i) * 6));
                g2.fillRoundRect(1, i, width - 3, height - 1 - i, cornerRadius, cornerRadius);
            }
        }

        g2.setColor(backgroundColor != null ? backgroundColor : getBackground());
        g2.fillRoundRect(0, 0, width - 1, height - 1 - bottomMargin, cornerRadius, cornerRadius);

        if (borderColor != null && borderWidth > 0) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(borderWidth));
            g2.drawRoundRect(0, 0, width - 1, height - 1 - bottomMargin, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
