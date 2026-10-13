package com.restocontrol;

import com.restocontrol.dao.PedidoDAO;
import com.restocontrol.model.PedidoKDS;
import com.restocontrol.ui.KitchenFrame;
import com.mycompany.restocontrol.repository.DatabaseManager;
import java.awt.GraphicsEnvironment;
import java.util.List;

public class KdsTest {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO PRUEBAS KDS RESTOCONTROL ===");

        // 1. Inicializar Base de Datos H2
        DatabaseManager db = DatabaseManager.getInstance();
        System.out.println("1. DatabaseManager inicializado correctamente.");

        // 2. Probar PedidoDAO
        PedidoDAO dao = PedidoDAO.getInstance();
        dao.restablecerPedidosDemo();

        List<PedidoKDS> pedidos = dao.obtenerPedidosKDS();
        System.out.println("2. Pedidos activos obtenidos: " + pedidos.size());
        if (pedidos.size() < 4) {
            throw new AssertionError("Se esperaban al menos 4 pedidos, se obtuvieron: " + pedidos.size());
        }

        long porPreparar = pedidos.stream().filter(p -> "POR_PREPARAR".equals(p.getEstado())).count();
        long enPreparacion = pedidos.stream().filter(p -> "EN_PREPARACION".equals(p.getEstado())).count();
        long listo = pedidos.stream().filter(p -> "LISTO".equals(p.getEstado())).count();

        System.out.println("   - Por Preparar: " + porPreparar);
        System.out.println("   - En Preparación: " + enPreparacion);
        System.out.println("   - Listo: " + listo);

        if (porPreparar != 1 || enPreparacion != 2 || listo != 1) {
            throw new AssertionError("Distribución incorrecta en las columnas: "
                    + porPreparar + " / " + enPreparacion + " / " + listo);
        }

        // 3. Probar transición de estados
        PedidoKDS orden1 = pedidos.stream().filter(p -> "CD18E747".equals(p.getId())).findFirst().orElseThrow();
        System.out.println("3. Probando mover orden #" + orden1.getId() + " a EN_PREPARACION...");
        dao.prepararPedido(orden1.getId());

        List<PedidoKDS> trasPreparar = dao.obtenerPedidosKDS();
        PedidoKDS orden1Mod = trasPreparar.stream().filter(p -> "CD18E747".equals(p.getId())).findFirst().orElseThrow();
        if (!"EN_PREPARACION".equals(orden1Mod.getEstado())) {
            throw new AssertionError("El estado debería ser EN_PREPARACION pero es: " + orden1Mod.getEstado());
        }
        System.out.println("   -> Estado actualizado correctamente a EN_PREPARACION.");

        System.out.println("4. Probando marcar orden #" + orden1.getId() + " como LISTO...");
        dao.marcarListo(orden1.getId());
        List<PedidoKDS> trasListo = dao.obtenerPedidosKDS();
        PedidoKDS orden1Listo = trasListo.stream().filter(p -> "CD18E747".equals(p.getId())).findFirst().orElseThrow();
        if (!"LISTO".equals(orden1Listo.getEstado())) {
            throw new AssertionError("El estado debería ser LISTO pero es: " + orden1Listo.getEstado());
        }
        System.out.println("   -> Estado actualizado correctamente a LISTO.");

        System.out.println("5. Probando entregar orden #" + orden1.getId() + " y liberar mesa 11...");
        dao.entregarPedidoYLiberarMesa(orden1.getId(), orden1.getMesa());
        List<PedidoKDS> trasEntregar = dao.obtenerPedidosKDS();
        boolean todaviaActivo = trasEntregar.stream().anyMatch(p -> "CD18E747".equals(p.getId()));
        if (todaviaActivo) {
            throw new AssertionError("La orden entregada no debería aparecer en pedidos activos de KDS.");
        }
        System.out.println("   -> Orden entregada y retirada de KDS correctamente.");

        // 6. Probar simulación de nuevo pedido (beep / bell test)
        System.out.println("6. Probando agregar pedido simulado...");
        PedidoKDS nuevo = dao.agregarPedidoSimulado();
        System.out.println("   -> Nuevo pedido generado: #" + nuevo.getId() + " en Mesa " + nuevo.getMesa());

        // 7. Restablecer demo para dejar el estado original
        System.out.println("7. Restableciendo 4 pedidos canónicos de demo...");
        dao.restablecerPedidosDemo();
        List<PedidoKDS> demoFinal = dao.obtenerPedidosKDS();
        System.out.println("   -> Pedidos demo restaurados: " + demoFinal.size());

        // 8. Instanciar KitchenFrame si el entorno gráfico lo permite
        if (!GraphicsEnvironment.isHeadless()) {
            System.out.println("8. Instanciando KitchenFrame en GUI...");
            KitchenFrame frame = new KitchenFrame(null);
            System.out.println("   -> KitchenFrame instanciado sin errores con título: " + frame.getTitle());
            frame.dispose();
        } else {
            System.out.println("8. Entorno headless detectado, saltando renderizado de ventana física.");
        }

        System.out.println("=== TODAS LAS PRUEBAS KDS COMPLETADAS CON ÉXITO ===");
    }
}
