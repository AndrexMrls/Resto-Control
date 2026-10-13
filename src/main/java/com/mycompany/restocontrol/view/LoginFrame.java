package com.mycompany.restocontrol.view;

import com.mycompany.restocontrol.controller.RestaurantController;
import com.mycompany.restocontrol.model.SesionActual;
import com.mycompany.restocontrol.model.Usuario;
import com.mycompany.restocontrol.repository.DatabaseManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public final class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color APP_BG = new Color(0x0B, 0x13, 0x2B);       // #0B132B
    private static final Color CARD_BG = new Color(0x1E, 0x29, 0x3B);      // #1E293B
    private static final Color BORDER_COLOR = new Color(0x33, 0x41, 0x55); // #334155
    private static final Color ACTIVE_CYAN = new Color(0x00, 0xAC, 0xC1);  // #00ACC1
    private static final Color TEXT_PRIMARY = new Color(0xF8, 0xFA, 0xFC); // #F8FAFC
    private static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);   // #94A3B8

    private final RestaurantController controller;

    private final JTextField txtUsuario = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JComboBox<Usuario.Rol> cbRol = new JComboBox<>(Usuario.Rol.values());
    private final ButtonGroup turnoGroup = new ButtonGroup();
    private final JRadioButton rbManana = new JRadioButton("Mañana", true);
    private final JRadioButton rbTarde = new JRadioButton("Tarde");
    private final JRadioButton rbNoche = new JRadioButton("Noche");

    private final Map<String, QuickUserPanel> avatarButtons = new LinkedHashMap<>();
    private String selectedAvatarUsername = null;

    public LoginFrame(RestaurantController controller) {
        this.controller = controller;
        setTitle("RestoControl | Iniciar Sesión por Turnos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(buildSplitLayout());
        setMinimumSize(new Dimension(900, 700));
        pack();
        setSize(Math.max(getWidth(), 980), Math.max(getHeight(), 720));
        setLocationRelativeTo(null);
    }

    private JPanel buildSplitLayout() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(APP_BG);

        // Panel Izquierdo: Imagen de restaurante costero + logo RestoControl
        root.add(buildLeftPanel(), BorderLayout.WEST);

        // Panel Derecho: Formulario de inicio de sesión
        root.add(buildRightPanel(), BorderLayout.CENTER);

        return root;
    }

    private JPanel buildLeftPanel() {
        Image bgImage = loadImage("/images/restaurant_coastal.jpg");
        Image logoImg = loadImage("/images/logo-restocontrol.png");

        JPanel leftPanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Draw background image scaled
                if (bgImage != null) {
                    double scale = Math.max((double) w / bgImage.getWidth(null), (double) h / bgImage.getHeight(null));
                    int dw = (int) (bgImage.getWidth(null) * scale);
                    int dh = (int) (bgImage.getHeight(null) * scale);
                    int x = (w - dw) / 2;
                    int y = (h - dh) / 2;
                    g2.drawImage(bgImage, x, y, dw, dh, null);
                }

                // Dark moody gradient overlay
                g2.setColor(new Color(11, 19, 43, 218));
                g2.fillRect(0, 0, w, h);

                // Subtle Cyan ambient glow
                int cx = w / 2;
                int cy = 160;
                for (int r = 80; r >= 15; r -= 10) {
                    g2.setColor(new Color(0, 172, 193, Math.max(3, (int) (28 * (1.0f - (float) r / 90)))));
                    g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                }

                g2.dispose();
            }
        };

        leftPanel.setPreferredSize(new Dimension(420, 0));
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(50, 40, 50, 40));

        leftPanel.add(Box.createVerticalGlue());

        // Logo image
        if (logoImg != null) {
            JLabel logoLabel = new JLabel(new ImageIcon(logoImg.getScaledInstance(180, 80, Image.SCALE_SMOOTH)));
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            leftPanel.add(logoLabel);
            leftPanel.add(Box.createVerticalStrut(20));
        }

        JLabel appTitle = new JLabel("RestoControl");
        appTitle.setFont(resolveFont("Inter", Font.BOLD, 30));
        appTitle.setForeground(TEXT_PRIMARY);
        appTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(appTitle);

        leftPanel.add(Box.createVerticalStrut(6));

        JLabel appSub = new JLabel("Gestión de Restaurante");
        appSub.setFont(resolveFont("Inter", Font.PLAIN, 15));
        appSub.setForeground(TEXT_MUTED);
        appSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(appSub);

        leftPanel.add(Box.createVerticalStrut(12));

        // Pill badge "Control de Turnos"
        JLabel badge = new JLabel("Control de Turnos 2026", SwingConstants.CENTER) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 172, 193, 35));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(ACTIVE_CYAN);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(resolveFont("Inter", Font.BOLD, 12));
        badge.setForeground(ACTIVE_CYAN);
        badge.setBorder(new EmptyBorder(6, 16, 6, 16));
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(badge);

        leftPanel.add(Box.createVerticalStrut(30));

        // Benefits features
        JPanel features = new JPanel();
        features.setOpaque(false);
        features.setLayout(new BoxLayout(features, BoxLayout.Y_AXIS));
        features.setAlignmentX(Component.CENTER_ALIGNMENT);

        addFeatureItem(features, "• Cuentas y ventas separadas por trabajador");
        addFeatureItem(features, "• Filtro de interfaz por rol (Mesero, Cajero, Chef)");
        addFeatureItem(features, "• Registro automático de horas de inicio y fin");

        leftPanel.add(features);
        leftPanel.add(Box.createVerticalGlue());

        return leftPanel;
    }

    private void addFeatureItem(JPanel container, String text) {
        JLabel label = new JLabel(text);
        label.setFont(resolveEmojiFont(Font.PLAIN, 12));
        label.setForeground(new Color(203, 213, 225));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        container.add(label);
        container.add(Box.createVerticalStrut(6));
    }

    private JPanel buildRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(APP_BG);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(new EmptyBorder(36, 48, 36, 48));

        // Form Title
        JLabel title = new JLabel("Iniciar Sesión");
        title.setFont(resolveFont("Inter", Font.BOLD, 26));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(title);

        rightPanel.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Accede para iniciar tu turno de trabajo");
        subtitle.setFont(resolveFont("Inter", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(subtitle);

        rightPanel.add(Box.createVerticalStrut(24));

        // Field: Usuario
        JLabel lblUser = new JLabel("👤 Usuario");
        lblUser.setFont(resolveEmojiFont(Font.BOLD, 12));
        lblUser.setForeground(TEXT_MUTED);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(lblUser);

        rightPanel.add(Box.createVerticalStrut(6));

        txtUsuario.putClientProperty("JComponent.roundRect", true);
        txtUsuario.putClientProperty("JTextField.placeholderText", "Ingresa tu usuario (ej. andrea, carlos)");
        txtUsuario.setPreferredSize(new Dimension(0, 38));
        txtUsuario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtUsuario.setFont(resolveFont("Inter", Font.PLAIN, 13));
        txtUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(txtUsuario);

        rightPanel.add(Box.createVerticalStrut(14));

        // Field: Contraseña con ojo para ver
        JLabel lblPass = new JLabel("🔒 Contraseña");
        lblPass.setFont(resolveEmojiFont(Font.BOLD, 12));
        lblPass.setForeground(TEXT_MUTED);
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(lblPass);

        rightPanel.add(Box.createVerticalStrut(6));

        JPanel passRow = new JPanel(new BorderLayout(6, 0));
        passRow.setOpaque(false);
        passRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword.putClientProperty("JComponent.roundRect", true);
        txtPassword.putClientProperty("JTextField.placeholderText", "Contraseña");
        txtPassword.setFont(resolveFont("Inter", Font.PLAIN, 13));
        passRow.add(txtPassword, BorderLayout.CENTER);

        JButton eyeButton = new JButton("👁");
        eyeButton.setFont(resolveEmojiFont(Font.PLAIN, 13));
        eyeButton.setPreferredSize(new Dimension(44, 38));
        eyeButton.putClientProperty("JButton.buttonType", "roundRect");
        eyeButton.setBackground(CARD_BG);
        eyeButton.setForeground(TEXT_PRIMARY);
        eyeButton.setFocusPainted(false);
        eyeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eyeButton.setToolTipText("Mostrar / ocultar contraseña");

        boolean[] showPass = {false};
        eyeButton.addActionListener(e -> {
            showPass[0] = !showPass[0];
            txtPassword.setEchoChar(showPass[0] ? (char) 0 : '•');
            eyeButton.setText(showPass[0] ? "🙈" : "👁");
        });
        passRow.add(eyeButton, BorderLayout.EAST);
        rightPanel.add(passRow);

        rightPanel.add(Box.createVerticalStrut(14));

        // RadioButtons: Turno (Mañana / Tarde / Noche)
        JLabel lblTurno = new JLabel("⏱ Turno de trabajo");
        lblTurno.setFont(resolveEmojiFont(Font.BOLD, 12));
        lblTurno.setForeground(TEXT_MUTED);
        lblTurno.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(lblTurno);

        rightPanel.add(Box.createVerticalStrut(6));

        JPanel turnoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        turnoPanel.setOpaque(false);
        turnoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        turnoGroup.add(rbManana);
        turnoGroup.add(rbTarde);
        turnoGroup.add(rbNoche);

        styleRadioButton(rbManana);
        styleRadioButton(rbTarde);
        styleRadioButton(rbNoche);

        turnoPanel.add(rbManana);
        turnoPanel.add(rbTarde);
        turnoPanel.add(rbNoche);
        rightPanel.add(turnoPanel);

        rightPanel.add(Box.createVerticalStrut(14));

        // ComboBox Rol
        JLabel lblRol = new JLabel("🛡 Rol asignado");
        lblRol.setFont(resolveEmojiFont(Font.BOLD, 12));
        lblRol.setForeground(TEXT_MUTED);
        lblRol.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(lblRol);

        rightPanel.add(Box.createVerticalStrut(6));

        cbRol.putClientProperty("JComponent.roundRect", true);
        cbRol.setPreferredSize(new Dimension(0, 38));
        cbRol.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cbRol.setFont(resolveFont("Inter", Font.BOLD, 13));
        cbRol.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(cbRol);

        rightPanel.add(Box.createVerticalStrut(20));

        // Botón grande cyan "Iniciar Turno"
        JButton btnLogin = new JButton("Iniciar Turno →");
        btnLogin.setFont(resolveFont("Inter", Font.BOLD, 14));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setBackground(ACTIVE_CYAN);
        btnLogin.setPreferredSize(new Dimension(0, 44));
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnLogin.setFocusPainted(false);
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.addActionListener(e -> ejecutarLogin());
        rightPanel.add(btnLogin);

        rightPanel.add(Box.createVerticalStrut(22));

        // Avatares circulares rápidos
        JLabel lblAvatares = new JLabel("O selecciona tu usuario rápido:");
        lblAvatares.setFont(resolveFont("Inter", Font.PLAIN, 11));
        lblAvatares.setForeground(new Color(148, 163, 184));
        lblAvatares.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightPanel.add(lblAvatares);

        rightPanel.add(Box.createVerticalStrut(8));

        JPanel avatarsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        avatarsRow.setOpaque(false);
        avatarsRow.setPreferredSize(new Dimension(420, 130));
        avatarsRow.setMinimumSize(new Dimension(380, 130));
        avatarsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        avatarsRow.setBorder(new EmptyBorder(10, 10, 20, 10));
        avatarsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        addAvatarCard(avatarsRow, "Admin", "admin", "admin123", Usuario.Rol.ADMIN, "/images/avatar_admin.jpg", "👑");
        addAvatarCard(avatarsRow, "Carlos", "carlos", "carlos123", Usuario.Rol.MESERO, "/images/avatar_carlos.jpg", "🍽");
        addAvatarCard(avatarsRow, "Maria", "maria", "maria123", Usuario.Rol.CHEF, "/images/avatar_maria.jpg", "👩‍🍳");
        addAvatarCard(avatarsRow, "Andrea", "andrea", "andrea123", Usuario.Rol.CAJERO, "/images/avatar_andrea.jpg", "💵");

        rightPanel.add(avatarsRow);

        // Preselect Andrea by default for nice demo
        selectAvatar("andrea", "andrea123", Usuario.Rol.CAJERO);

        return rightPanel;
    }

    private void styleRadioButton(JRadioButton rb) {
        rb.setOpaque(false);
        rb.setForeground(TEXT_PRIMARY);
        rb.setFont(resolveFont("Inter", Font.PLAIN, 12));
        rb.setFocusPainted(false);
        rb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void addAvatarCard(JPanel container, String nombre, String username, String password,
                               Usuario.Rol rol, String imagePath, String fallbackEmoji) {
        QuickUserPanel panel = new QuickUserPanel(nombre, username, password, rol, imagePath, fallbackEmoji, () -> {
            selectAvatar(username, password, rol);
        });
        avatarButtons.put(username, panel);
        container.add(panel);
    }

    private void selectAvatar(String username, String password, Usuario.Rol rol) {
        selectedAvatarUsername = username;
        txtUsuario.setText(username);
        txtPassword.setText(password);
        cbRol.setSelectedItem(rol);
        txtUsuario.requestFocus();

        for (var entry : avatarButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equalsIgnoreCase(username));
        }
    }

    private void ejecutarLogin() {
        String usuario = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword());
        Usuario.Rol rolSeleccionado = (Usuario.Rol) cbRol.getSelectedItem();

        if (usuario.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingresa usuario y contraseña.",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Usuario user = DatabaseManager.getInstance().autenticar(usuario, pass);
        if (user == null) {
            JOptionPane.showMessageDialog(this,
                    "Credenciales incorrectas o usuario inactivo.",
                    "Error de autenticación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Determinar turno seleccionado
        String tipoTurno = "Mañana";
        for (Enumeration<AbstractButton> buttons = turnoGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                tipoTurno = button.getText();
                break;
            }
        }

        // Crear turno en Base de Datos (hora_inicio = NOW(), estado = ABIERTO)
        int turnoId = DatabaseManager.getInstance().iniciarTurno(user.getId(), tipoTurno);

        // Guardar sesión en Singleton
        SesionActual.getInstance().iniciarSesion(user, turnoId, tipoTurno);

        dispose();

        // Enrutar según rol: Si CHEF abre KitchenFrame, si no abre DashboardFrame filtrado
        if (user.getRol() == Usuario.Rol.CHEF) {
            new KitchenFrame(controller).setVisible(true);
        } else {
            new DashboardFrame(controller).setVisible(true);
        }
    }

    private static Image loadImage(String path) {
        try (var in = LoginFrame.class.getResourceAsStream(path)) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (Exception ignored) {
        }
        return null;
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

    // Quick User Panel component
    public static class QuickUserPanel extends JPanel {

        private static final long serialVersionUID = 1L;
        private final String nombre;
        private final BufferedImage avatarImage;
        private final String emoji;
        private final Runnable onClick;

        private final JLabel lblAvatar;
        private final JLabel lblNombre;

        private boolean selected = false;
        private boolean hovered = false;

        public QuickUserPanel(String nombre, String username, String password, Usuario.Rol rol,
                              String imagePath, String emoji, Runnable onClick) {
            this.nombre = nombre;
            this.emoji = emoji;
            this.onClick = onClick;

            BufferedImage img = null;
            try (var in = LoginFrame.class.getResourceAsStream(imagePath)) {
                if (in != null) {
                    img = ImageIO.read(in);
                }
            } catch (Exception ignored) {
            }
            this.avatarImage = img;

            setPreferredSize(new Dimension(80, 90));
            setMinimumSize(new Dimension(80, 90));
            setMaximumSize(new Dimension(80, 90));
            setLayout(new BorderLayout(0, 2));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Circular avatar label (70x70)
            lblAvatar = new JLabel() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                    int w = getWidth();
                    int h = getHeight();
                    int size = Math.min(w, h) - 4;
                    if (size > 68) {
                        size = 68;
                    }
                    int x = (w - size) / 2;
                    int y = (h - size) / 2;

                    Ellipse2D.Float circleClip = new Ellipse2D.Float(x, y, size, size);

                    if (avatarImage != null) {
                        Shape oldClip = g2.getClip();
                        g2.setClip(circleClip);
                        int imgW = avatarImage.getWidth();
                        int imgH = avatarImage.getHeight();
                        int side = Math.min(imgW, imgH);
                        int sx = (imgW - side) / 2;
                        int sy = (imgH - side) / 2;
                        g2.drawImage(avatarImage, x, y, x + size, y + size, sx, sy, sx + side, sy + side, null);
                        g2.setClip(oldClip);
                    } else {
                        g2.setColor(CARD_BG);
                        g2.fill(circleClip);
                        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                        FontMetrics fm = g2.getFontMetrics();
                        int ex = x + (size - fm.stringWidth(emoji)) / 2;
                        int ey = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(emoji, ex, ey);
                    }

                    // Circular border outline
                    if (selected || hovered) {
                        g2.setColor(ACTIVE_CYAN);
                        g2.setStroke(new BasicStroke(2.5f));
                    } else {
                        g2.setColor(new Color(100, 116, 139, 140));
                        g2.setStroke(new BasicStroke(1.2f));
                    }
                    g2.draw(circleClip);

                    g2.dispose();
                }
            };
            lblAvatar.setPreferredSize(new Dimension(70, 70));
            lblAvatar.setHorizontalAlignment(SwingConstants.CENTER);
            lblAvatar.setOpaque(false);
            lblAvatar.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Name label below
            lblNombre = new JLabel(nombre, SwingConstants.CENTER);
            lblNombre.setFont(resolveFont("Inter", Font.BOLD, 11));
            lblNombre.setForeground(TEXT_PRIMARY);
            lblNombre.setOpaque(false);
            lblNombre.setCursor(new Cursor(Cursor.HAND_CURSOR));

            add(lblAvatar, BorderLayout.CENTER);
            add(lblNombre, BorderLayout.SOUTH);

            // Mouse listener on entire QuickUserPanel + children
            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    setBorder(BorderFactory.createLineBorder(ACTIVE_CYAN, 2, true));
                    lblNombre.setForeground(ACTIVE_CYAN);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), QuickUserPanel.this);
                    if (!contains(p)) {
                        hovered = false;
                        if (!selected) {
                            setBorder(null);
                            lblNombre.setForeground(TEXT_PRIMARY);
                        }
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    triggerAction();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    triggerAction();
                }
            };

            addMouseListener(mouseHandler);
            lblAvatar.addMouseListener(mouseHandler);
            lblNombre.addMouseListener(mouseHandler);
        }

        private void triggerAction() {
            if (onClick != null) {
                onClick.run();
            }
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                setBorder(BorderFactory.createLineBorder(ACTIVE_CYAN, 2, true));
                lblNombre.setForeground(ACTIVE_CYAN);
            } else {
                if (!hovered) {
                    setBorder(null);
                    lblNombre.setForeground(TEXT_PRIMARY);
                }
            }
            repaint();
        }
    }

    // Backwards-compatibility alias
    public static class AvatarButton extends QuickUserPanel {
        private static final long serialVersionUID = 1L;

        public AvatarButton(String nombre, String username, String password, Usuario.Rol rol,
                            String imagePath, String emoji, Runnable onClick) {
            super(nombre, username, password, rol, imagePath, emoji, onClick);
        }
    }
}
