package com.restocontrol;

import com.mycompany.restocontrol.controller.RestaurantController;
import com.mycompany.restocontrol.model.Usuario;
import com.mycompany.restocontrol.repository.DatabaseManager;
import com.mycompany.restocontrol.view.LoginFrame;
import com.mycompany.restocontrol.view.LoginFrame.QuickUserPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.Map;

public class LoginFrameQuickUserTest {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println(" TEST LOGIN RÁPIDO - QUICK USER PANEL EN LOGIN FRAME");
        System.out.println("==================================================");

        DatabaseManager.getInstance();
        com.mycompany.restocontrol.repository.RestaurantRepository repo = new com.mycompany.restocontrol.repository.RestaurantRepository();
        RestaurantController controller = new RestaurantController(repo);
        LoginFrame frame = new LoginFrame(controller);

        // 1. Minimum size verification
        Dimension minSize = frame.getMinimumSize();
        System.out.println("Checking frame minimum size: " + minSize);
        if (minSize.width < 900 || minSize.height < 700) {
            throw new AssertionError("Frame minimum size should be at least (900, 700), was: " + minSize);
        }
        System.out.println("✔ 1. MinimumSize verificado: " + minSize.width + "x" + minSize.height);

        // 2. Reflect private fields
        Field avatarButtonsField = LoginFrame.class.getDeclaredField("avatarButtons");
        avatarButtonsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, QuickUserPanel> avatarButtons = (Map<String, QuickUserPanel>) avatarButtonsField.get(frame);

        if (avatarButtons.size() != 4) {
            throw new AssertionError("Expected 4 quick avatar panels, found: " + avatarButtons.size());
        }
        System.out.println("✔ 2. Avatares encontrados: " + avatarButtons.keySet());

        Field txtUserField = LoginFrame.class.getDeclaredField("txtUsuario");
        txtUserField.setAccessible(true);
        JTextField txtUsuario = (JTextField) txtUserField.get(frame);

        Field cbRolField = LoginFrame.class.getDeclaredField("cbRol");
        cbRolField.setAccessible(true);
        @SuppressWarnings("unchecked")
        JComboBox<Usuario.Rol> cbRol = (JComboBox<Usuario.Rol>) cbRolField.get(frame);

        // 3. Check panel properties (80x90, HAND_CURSOR, not opaque)
        for (Map.Entry<String, QuickUserPanel> entry : avatarButtons.entrySet()) {
            QuickUserPanel panel = entry.getValue();
            if (panel.getPreferredSize().width != 80 || panel.getPreferredSize().height != 90) {
                throw new AssertionError("Panel " + entry.getKey() + " size is not 80x90: " + panel.getPreferredSize());
            }
            if (panel.isOpaque()) {
                throw new AssertionError("Panel " + entry.getKey() + " must have setOpaque(false)");
            }
            if (panel.getCursor().getType() != Cursor.HAND_CURSOR) {
                throw new AssertionError("Panel " + entry.getKey() + " cursor must be HAND_CURSOR");
            }
        }
        System.out.println("✔ 3. Dimensiones 80x90, opacidad false y HAND_CURSOR verificados en todos los paneles.");

        // 4. Test click anywhere on panel (Carlos -> MESERO)
        QuickUserPanel carlosPanel = avatarButtons.get("carlos");
        MouseEvent clickCarlos = new MouseEvent(carlosPanel, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 10, 10, 1, false);
        for (var l : carlosPanel.getMouseListeners()) {
            l.mouseClicked(clickCarlos);
        }
        if (!"carlos".equals(txtUsuario.getText())) {
            throw new AssertionError("Expected txtUsuario = 'carlos', got: " + txtUsuario.getText());
        }
        if (cbRol.getSelectedItem() != Usuario.Rol.MESERO) {
            throw new AssertionError("Expected cbRol = MESERO, got: " + cbRol.getSelectedItem());
        }
        System.out.println("✔ 4. Clic en panel de Carlos autocompleta 'carlos' y selecciona rol MESERO.");

        // 5. Test click on Maria's circular image edge (x=2, y=2)
        QuickUserPanel mariaPanel = avatarButtons.get("maria");
        Component circularLabel = null;
        Component nameLabel = null;
        for (Component child : mariaPanel.getComponents()) {
            if (child instanceof JLabel lbl) {
                if (lbl.getPreferredSize().equals(new Dimension(70, 70))) {
                    circularLabel = lbl;
                } else {
                    nameLabel = lbl;
                }
            }
        }
        if (circularLabel == null || nameLabel == null) {
            throw new AssertionError("Missing child labels inside QuickUserPanel");
        }
        if (circularLabel.getCursor().getType() != Cursor.HAND_CURSOR) {
            throw new AssertionError("Circular label cursor must be HAND_CURSOR");
        }
        if (nameLabel.getCursor().getType() != Cursor.HAND_CURSOR) {
            throw new AssertionError("Name label cursor must be HAND_CURSOR");
        }

        MouseEvent clickMariaEdge = new MouseEvent(circularLabel, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 2, 2, 1, false);
        for (var l : circularLabel.getMouseListeners()) {
            l.mouseClicked(clickMariaEdge);
        }
        if (!"maria".equals(txtUsuario.getText())) {
            throw new AssertionError("Expected txtUsuario = 'maria', got: " + txtUsuario.getText());
        }
        if (cbRol.getSelectedItem() != Usuario.Rol.CHEF) {
            throw new AssertionError("Expected cbRol = CHEF, got: " + cbRol.getSelectedItem());
        }
        System.out.println("✔ 5. Clic en borde del círculo de Maria (70x70) autocompleta 'maria' y selecciona rol CHEF.");

        // 6. Test click on Admin name label
        QuickUserPanel adminPanel = avatarButtons.get("admin");
        Component adminNameLabel = null;
        for (Component child : adminPanel.getComponents()) {
            if (child instanceof JLabel lbl && !lbl.getPreferredSize().equals(new Dimension(70, 70))) {
                adminNameLabel = lbl;
            }
        }
        MouseEvent clickAdminName = new MouseEvent(adminNameLabel, MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(), 0, 10, 5, 1, false);
        for (var l : adminNameLabel.getMouseListeners()) {
            l.mouseClicked(clickAdminName);
        }
        if (!"admin".equals(txtUsuario.getText())) {
            throw new AssertionError("Expected txtUsuario = 'admin', got: " + txtUsuario.getText());
        }
        if (cbRol.getSelectedItem() != Usuario.Rol.ADMIN) {
            throw new AssertionError("Expected cbRol = ADMIN, got: " + cbRol.getSelectedItem());
        }
        System.out.println("✔ 6. Clic en label de nombre de Admin autocompleta 'admin' y selecciona rol ADMIN.");

        // 7. Hover effect test (mouseEntered -> cyan border, mouseExited -> removes border)
        carlosPanel.setSelected(false);
        MouseEvent enter = new MouseEvent(carlosPanel, MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(), 0, 40, 45, 0, false);
        for (var l : carlosPanel.getMouseListeners()) {
            l.mouseEntered(enter);
        }
        if (carlosPanel.getBorder() == null) {
            throw new AssertionError("Expected border to be set on mouseEntered");
        }
        System.out.println("✔ 7. Hover (mouseEntered) aplica borde cyan correctamente.");

        MouseEvent exit = new MouseEvent(carlosPanel, MouseEvent.MOUSE_EXITED,
                System.currentTimeMillis(), 0, 150, 150, 0, false);
        for (var l : carlosPanel.getMouseListeners()) {
            l.mouseExited(exit);
        }
        if (carlosPanel.getBorder() != null) {
            throw new AssertionError("Expected border to be removed on mouseExited");
        }
        System.out.println("✔ 8. Hover salida (mouseExited) remueve el borde correctamente.");

        frame.dispose();
        System.out.println("==================================================");
        System.out.println(" TODAS LAS PRUEBAS DE QUICK LOGIN PASARON CON ÉXITO");
        System.out.println("==================================================");
    }
}
