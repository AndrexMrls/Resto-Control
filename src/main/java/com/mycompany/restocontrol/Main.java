package com.mycompany.restocontrol;

import com.mycompany.restocontrol.controller.RestaurantController;
import com.mycompany.restocontrol.repository.RestaurantRepository;
import com.mycompany.restocontrol.view.DashboardFrame;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.EventQueue;
import java.awt.Color;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
                setReadableDefaults();
                RestaurantController controller = new RestaurantController(new RestaurantRepository());
                new DashboardFrame(controller).setVisible(true);
            } catch (IOException | RuntimeException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "No se pudo iniciar RestoControl.\n" + ex.getMessage(),
                        "Error de inicio",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void setReadableDefaults() {
        Color text = new Color(37, 48, 63);
        Color background = Color.WHITE;
        UIManager.put("Label.foreground", text);
        UIManager.put("Button.foreground", text);
        UIManager.put("TextField.foreground", text);
        UIManager.put("TextField.background", background);
        UIManager.put("TextArea.foreground", text);
        UIManager.put("TextArea.background", background);
        UIManager.put("ComboBox.foreground", text);
        UIManager.put("ComboBox.background", background);
        UIManager.put("Spinner.foreground", text);
        UIManager.put("Spinner.background", background);
        UIManager.put("List.foreground", text);
        UIManager.put("List.background", background);
        UIManager.put("OptionPane.messageForeground", text);
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.width", 10);
    }
}
