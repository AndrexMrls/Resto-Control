package com.restocontrol;

import com.mycompany.restocontrol.model.OrderItem;
import com.mycompany.restocontrol.model.OrderStatus;
import com.mycompany.restocontrol.model.Pedido;
import com.mycompany.restocontrol.model.SesionActual;
import com.mycompany.restocontrol.model.Turno;
import com.mycompany.restocontrol.model.Usuario;
import com.mycompany.restocontrol.repository.DatabaseManager;
import com.mycompany.restocontrol.repository.RestaurantRepository;
import com.restocontrol.dao.ConfigDAO;
import com.restocontrol.dao.PedidoDAO;
import com.restocontrol.model.PedidoKDS;
import com.restocontrol.ui.CobrarFrame;
import com.restocontrol.util.TicketPrinter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TicketPrinterTest {

    public static void main(String[] args) throws IOException {
        System.setProperty("skip.dialogs", "true");
        System.out.println("=================================================");
        System.out.println(" INICIANDO PRUEBAS DE IMPRESION DE TICKET TERMICO");
        System.out.println("=================================================");

        // 1. Inicializar Base de Datos y Sesion
        DatabaseManager db = DatabaseManager.getInstance();
        PedidoDAO dao = PedidoDAO.getInstance();
        dao.restablecerPedidosDemo();

        Usuario userCajero = new Usuario(4, "Andrea", "andrea", "andrea123", Usuario.Rol.CAJERO, true);
        SesionActual.getInstance().iniciarSesion(userCajero, 1, "Mañana");
        System.out.println("✔ 1. Sesión actual configurada con Cajero Andrea y Turno Mañana.");

        // 2. Verificar ConfigDAO
        ConfigDAO cfg = ConfigDAO.getInstance();
        System.out.println("✔ 2. ConfigDAO cargado:");
        System.out.println("     - Restaurante: " + cfg.getNombreRestaurante());
        System.out.println("     - NIT: " + cfg.getNit());
        System.out.println("     - Dirección: " + cfg.getDireccion());
        System.out.println("     - Teléfono: " + cfg.getTelefono());

        if (cfg.getNombreRestaurante().isBlank() || cfg.getNit().isBlank()) {
            throw new AssertionError("ConfigDAO no devolvió datos válidos del restaurante.");
        }

        // 3. Probar pedido CD18E747 ($40.000, Mesa 11)
        UUID orderUuid = RestaurantRepository.parseOrGenerateUuid("CD18E747");
        OrderItem itemCazuela = new OrderItem("Cazuela de mariscos", 1, new BigDecimal("40000"));
        Pedido pedidoCD18E747 = new Pedido(
                orderUuid,
                11,
                LocalDateTime.now().minusMinutes(1),
                List.of(itemCazuela),
                "Sin cilantro, picante extra",
                OrderStatus.NEW,
                2, // Carlos (Mesero)
                1  // Turno 1
        );

        System.out.println("\n✔ 3. Probando imprimirTicket(pedidoCD18E747) con Efectivo $50.000...");
        boolean ticketImpreso = TicketPrinter.imprimirTicket(
                pedidoCD18E747,
                "Efectivo",
                new BigDecimal("50000"),
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        if (!ticketImpreso) {
            throw new AssertionError("TicketPrinter.imprimirTicket devolvió false.");
        }

        File ticketFile = new File("tickets/Ticket_CD18E747.txt");
        if (!ticketFile.exists()) {
            throw new AssertionError("El archivo " + ticketFile.getAbsolutePath() + " no fue generado.");
        }
        System.out.println("✔ Archivo de ticket encontrado en: " + ticketFile.getAbsolutePath());

        String ticketContent = Files.readString(ticketFile.toPath());
        System.out.println("\n--- CONTENIDO DEL TICKET GENERADO (80mm) ---");
        System.out.println(ticketContent);
        System.out.println("--------------------------------------------\n");

        // Validaciones del contenido del ticket
        assertContains(ticketContent, cfg.getNombreRestaurante().toUpperCase(), "Nombre restaurante");
        assertContains(ticketContent, cfg.getNit(), "NIT restaurante");
        assertContains(ticketContent, "CD18E747", "ID de Factura");
        assertContains(ticketContent, "Mesa: 11", "Número de mesa 11");
        assertContains(ticketContent, "Carlos", "Mesero (Carlos)");
        assertContains(ticketContent, "Andrea", "Cajero (Andrea)");
        assertContains(ticketContent, "Mañana", "Turno (Mañana)");
        assertContains(ticketContent, "Cazuela de mariscos", "Producto");
        assertContains(ticketContent, "$40.000", "Total $40.000");
        assertContains(ticketContent, "EFECTIVO", "Método de pago");
        assertContains(ticketContent, "$50.000", "Monto recibido");
        assertContains(ticketContent, "$10.000", "Cambio devuelta");
        assertContains(ticketContent, "GRACIAS", "Agradecimiento");
        assertContains(ticketContent, "QR FACTURA", "Sección QR");

        System.out.println("✔ 4. Formato exacto y campos del ticket CD18E747 validados exitosamente.");

        // 4. Probar Reimprimir último ticket
        System.out.println("\n✔ 5. Probando TicketPrinter.reimprimirUltimoTicket()...");
        boolean reimpreso = TicketPrinter.reimprimirUltimoTicket();
        if (!reimpreso) {
            throw new AssertionError("reimprimirUltimoTicket devolvió false.");
        }
        System.out.println("✔ Reimpresión completada exitosamente.");

        // 5. Probar imprimirCierre(Turno)
        System.out.println("\n✔ 6. Probando TicketPrinter.imprimirCierre(turno)...");
        Turno turno = new Turno(
                1,
                4, // Andrea
                LocalDate.now(),
                "Mañana",
                LocalDateTime.now().minusHours(8),
                LocalDateTime.now(),
                new BigDecimal("40000"),
                Turno.Estado.CERRADO
        );

        boolean cierreImpreso = TicketPrinter.imprimirCierre(
                turno,
                new BigDecimal("40000"), // Efectivo
                BigDecimal.ZERO,         // Nequi
                BigDecimal.ZERO,         // Tarjeta
                BigDecimal.ZERO          // Diferencia
        );

        if (!cierreImpreso) {
            throw new AssertionError("TicketPrinter.imprimirCierre devolvió false.");
        }

        File cierreFile = new File("tickets/Cierre_Turno_1.txt");
        if (!cierreFile.exists()) {
            throw new AssertionError("El archivo de cierre " + cierreFile.getAbsolutePath() + " no fue generado.");
        }

        String cierreContent = Files.readString(cierreFile.toPath());
        System.out.println("\n--- CONTENIDO DEL CIERRE DE CAJA (80mm) ---");
        System.out.println(cierreContent);
        System.out.println("-------------------------------------------\n");

        assertContains(cierreContent, "CIERRE DE CAJA", "Título de cierre");
        assertContains(cierreContent, "TURNO ID:", "ID del turno");
        assertContains(cierreContent, "VENTAS POR METODO DE PAGO", "Sección método pago");
        assertContains(cierreContent, "ARQUEO DE CAJA EFECTIVO", "Sección arqueo");
        assertContains(cierreContent, "DIFERENCIA:", "Diferencia");

        System.out.println("✔ 7. Reporte de cierre de caja validado exitosamente.");

        // 6. Probar CobrarFrame instanciación y lógica de cobro
        System.out.println("\n✔ 8. Verificando CobrarFrame...");
        CobrarFrame cobrarFrame = new CobrarFrame(pedidoCD18E747, () -> {
            System.out.println("   [Callback de cobro ejecutado correctamente]");
        });
        System.out.println("   CobrarFrame creado con título: " + cobrarFrame.getTitle());
        cobrarFrame.dispose();

        System.out.println("\n=================================================");
        System.out.println(" ¡TODAS LAS PRUEBAS DE TICKET COMPLETADAS CON EXITO!");
        System.out.println("=================================================");
    }

    private static void assertContains(String text, String expected, String label) {
        if (!text.contains(expected)) {
            throw new AssertionError("El texto no contiene [" + expected + "] para: " + label);
        }
    }
}
