package com.mycompany.restocontrol;

import com.mycompany.restocontrol.controller.RestaurantController;
import com.mycompany.restocontrol.repository.DatabaseManager;
import com.mycompany.restocontrol.repository.RestaurantRepository;
import com.mycompany.restocontrol.view.LoginFrame;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.EventQueue;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.put("Button.arc", 20);
        EventQueue.invokeLater(() -> {
            try {
                setReadableDefaults();
                DatabaseManager.getInstance(); // Inicializar tablas H2 y usuarios por defecto
                RestaurantController controller = new RestaurantController(new RestaurantRepository());
                new LoginFrame(controller).setVisible(true);
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
        UIManager.put("Component.arc", 16);
        UIManager.put("Button.arc", 20);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.width", 8);
        UIManager.put("ScrollBar.showButtons", false);
    }
}
