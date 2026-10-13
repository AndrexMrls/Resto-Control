package com.restocontrol.util;

import com.mycompany.restocontrol.model.OrderItem;
import com.mycompany.restocontrol.model.Pedido;
import com.mycompany.restocontrol.model.SesionActual;
import com.mycompany.restocontrol.model.Turno;
import com.mycompany.restocontrol.model.Usuario;
import com.mycompany.restocontrol.repository.DatabaseManager;
import com.restocontrol.dao.ConfigDAO;
import com.restocontrol.model.PedidoKDS;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.JOptionPane;

public class TicketPrinter {

    private static final int LINE_WIDTH = 42;
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static {
        CURRENCY.setMinimumFractionDigits(0);
        CURRENCY.setMaximumFractionDigits(0);
    }

    private static Pedido ultimoPedido;
    private static PedidoKDS ultimoPedidoKds;
    private static File ultimoTicketFile;
    private static String ultimoTextoTicket;

    // --- METODOS PUBLICOS DE IMPRESION DE TICKET ---

    public static boolean imprimirTicket(Pedido pedido) {
        if (pedido == null) return false;
        BigDecimal total = pedido.getTotal();
        return imprimirTicket(pedido, "Efectivo", total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static boolean imprimirTicket(PedidoKDS pedidoKds) {
        if (pedidoKds == null) return false;
        BigDecimal total = pedidoKds.getTotal();
        return imprimirTicket(pedidoKds, "Efectivo", total, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public static boolean imprimirTicket(Pedido pedido, String metodoPago, BigDecimal montoRecibido,
                                         BigDecimal cambio, BigDecimal propina, BigDecimal descuento) {
        if (pedido == null) return false;
        ultimoPedido = pedido;
        ultimoPedidoKds = null;

        String idCorto = shortId(pedido.getId() != null ? pedido.getId().toString() : "00000000");
        String contenido = generarTextoTicket(pedido, null, metodoPago, montoRecibido, cambio, propina, descuento);
        ultimoTextoTicket = contenido;

        return procesarImpresion(idCorto, contenido);
    }

    public static boolean imprimirTicket(PedidoKDS pedidoKds, String metodoPago, BigDecimal montoRecibido,
                                         BigDecimal cambio, BigDecimal propina, BigDecimal descuento) {
        if (pedidoKds == null) return false;
        ultimoPedidoKds = pedidoKds;
        ultimoPedido = null;

        String idCorto = shortId(pedidoKds.getId());
        String contenido = generarTextoTicket(null, pedidoKds, metodoPago, montoRecibido, cambio, propina, descuento);
        ultimoTextoTicket = contenido;

        return procesarImpresion(idCorto, contenido);
    }

    public static boolean reimprimirUltimoTicket() {
        if (ultimoPedido != null) {
            return imprimirTicket(ultimoPedido);
        }
        if (ultimoPedidoKds != null) {
            return imprimirTicket(ultimoPedidoKds);
        }
        if (ultimoTextoTicket != null && ultimoTicketFile != null) {
            return procesarImpresion(ultimoTicketFile.getName().replace("Ticket_", "").replace(".txt", ""), ultimoTextoTicket);
        }

        // Si no hay en memoria, buscar el último pedido en BD
        PedidoKDS ultimoBD = obtenerUltimoPedidoBD();
        if (ultimoBD != null) {
            return imprimirTicket(ultimoBD);
        }

        return false;
    }

    public static boolean imprimirTicketPorId(String pedidoId) {
        return imprimirTicketPorId(pedidoId, "Efectivo", null, null, null, null);
    }

    public static boolean imprimirTicketPorId(String pedidoId, String metodoPago, BigDecimal montoRecibido,
                                              BigDecimal cambio, BigDecimal propina, BigDecimal descuento) {
        PedidoKDS kds = obtenerPedidoPorId(pedidoId);
        if (kds != null) {
            return imprimirTicket(kds, metodoPago, montoRecibido, cambio, propina, descuento);
        }
        return false;
    }

    // --- METODO DE CIERRE DE CAJA / TURNO ---

    public static boolean imprimirCierre(Turno turno) {
        if (turno == null) return false;
        return imprimirCierre(turno, null, null, null, null);
    }

    public static boolean imprimirCierre(Turno turno, BigDecimal efectivo, BigDecimal nequi,
                                         BigDecimal tarjeta, BigDecimal diferencia) {
        if (turno == null) return false;

        String contenido = generarTextoCierre(turno, efectivo, nequi, tarjeta, diferencia);
        String idTurno = String.valueOf(turno.getId());

        File ticketsDir = new File("tickets");
        if (!ticketsDir.exists()) {
            ticketsDir.mkdirs();
        }
        File archivo = new File(ticketsDir, "Cierre_Turno_" + idTurno + ".txt");

        try (FileWriter writer = new FileWriter(archivo, StandardCharsets.UTF_8)) {
            writer.write(contenido);
        } catch (IOException ex) {
            System.err.println("Error escribiendo cierre a archivo: " + ex.getMessage());
        }

        // Intento de impresion fisica
        PrintService printer = buscarImpresoraTermica();
        boolean impresoFisico = false;
        if (printer != null) {
            try {
                impresoFisico = imprimirEnImpresora(printer, contenido);
            } catch (Exception ex) {
                System.err.println("Fallo al imprimir cierre térmico: " + ex.getMessage());
            }
        }

        if (!impresoFisico) {
            abrirConDesktop(archivo);
            mostrarAviso("Cierre guardado en carpeta tickets");
        }

        return true;
    }

    // --- LOGICA DE IMPRESION Y GUARDADO ---

    private static boolean procesarImpresion(String idCorto, String contenido) {
        File ticketsDir = new File("tickets");
        if (!ticketsDir.exists()) {
            ticketsDir.mkdirs();
        }
        File ticketFile = new File(ticketsDir, "Ticket_" + idCorto + ".txt");
        ultimoTicketFile = ticketFile;

        // 1. Guardar siempre como TXT
        try (FileWriter writer = new FileWriter(ticketFile, StandardCharsets.UTF_8)) {
            writer.write(contenido);
        } catch (IOException ex) {
            System.err.println("Error guardando ticket a TXT: " + ex.getMessage());
        }

        // 2. Intentar imprimir en impresora termica 80mm usando Java Print Service
        PrintService thermalPrinter = buscarImpresoraTermica();
        boolean impresoFisicamente = false;

        if (thermalPrinter != null) {
            try {
                impresoFisicamente = imprimirEnImpresora(thermalPrinter, contenido);
            } catch (Exception ex) {
                System.err.println("Aviso: No se pudo imprimir en " + thermalPrinter.getName() + ": " + ex.getMessage());
                mostrarAviso("Ticket guardado en carpeta tickets");
            }
        }

        // 3. Si no hay impresora o fallo la impresion fisica, abrir con Desktop
        if (!impresoFisicamente) {
            abrirConDesktop(ticketFile);
        }

        return true;
    }

    private static PrintService buscarImpresoraTermica() {
        try {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            if (services != null && services.length > 0) {
                // Primero buscar impresora con nombre POS / Thermal / 80 / Ticket
                for (PrintService ps : services) {
                    String name = ps.getName().toLowerCase();
                    if (name.contains("pos") || name.contains("thermal") || name.contains("ticket")
                            || name.contains("receipt") || name.contains("80") || name.contains("epson")
                            || name.contains("bixolon") || name.contains("xprinter") || name.contains("star")) {
                        return ps;
                    }
                }
                // Si hay impresora por defecto y no es PDF/XPS/Fax
                PrintService def = PrintServiceLookup.lookupDefaultPrintService();
                if (def != null) {
                    String defName = def.getName().toLowerCase();
                    if (!defName.contains("pdf") && !defName.contains("xps") && !defName.contains("onenote") && !defName.contains("fax")) {
                        return def;
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error buscando servicios de impresión: " + ex.getMessage());
        }
        return null;
    }

    private static boolean imprimirEnImpresora(PrintService service, String texto) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(service);

        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        // 80mm = ~226.77 puntos. Alto dinamico o standard roll
        double widthPts = (80.0 / 25.4) * 72.0;
        String[] lines = texto.split("\n");
        double heightPts = Math.max(lines.length * 12.0 + 40.0, 300.0);
        paper.setSize(widthPts, heightPts);
        paper.setImageableArea(5, 5, widthPts - 10, heightPts - 10);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);

        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
                if (pageIndex > 0) {
                    return Printable.NO_SUCH_PAGE;
                }
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                int y = 10;
                for (String rawLine : lines) {
                    String line = rawLine.replace("\r", "");
                    boolean isBold = line.contains("TOTAL") || line.contains("FACTURA") || line.contains("===");
                    g2d.setFont(new Font("Monospaced", isBold ? Font.BOLD : Font.PLAIN, 9));
                    g2d.drawString(line, 0, y);
                    y += 11;
                }
                return Printable.PAGE_EXISTS;
            }
        }, pf);

        job.print();
        return true;
    }

    private static void abrirConDesktop(File file) {
        try {
            if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN) && file.exists()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception ex) {
            System.err.println("Aviso: no se pudo abrir visor de escritorio: " + ex.getMessage());
        }
    }

    private static void mostrarAviso(String mensaje) {
        try {
            if (!GraphicsEnvironment.isHeadless() && !Boolean.getBoolean("skip.dialogs")) {
                JOptionPane.showMessageDialog(null, mensaje, "RestoControl | Ticket", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ignored) {
        }
    }

    // --- FORMATEO EXACTO DE TICKET ---

    public static String generarTextoTicket(Pedido pedido, PedidoKDS kds, String metodoPago,
                                            BigDecimal montoRecibido, BigDecimal cambio,
                                            BigDecimal propina, BigDecimal descuento) {
        ConfigDAO cfg = ConfigDAO.getInstance();
        StringBuilder sb = new StringBuilder();

        String id = pedido != null ? shortId(pedido.getId().toString()) : (kds != null ? shortId(kds.getId()) : "CD18E747");
        int mesa = pedido != null ? pedido.getTableNumber() : (kds != null ? kds.getMesa() : 11);
        LocalDateTime fecha = pedido != null ? pedido.getCreatedAt() : (kds != null ? kds.getFecha() : LocalDateTime.now());

        SesionActual sesion = SesionActual.getInstance();
        String mesero = resolverNombreMesero(pedido != null ? pedido.getUsuarioId() : (kds != null ? kds.getUsuarioId() : 2));
        String cajero = sesion.getUsuario() != null ? sesion.getUsuario().getNombre() : "Andrea";
        String turno = sesion.getTipoTurno() != null ? sesion.getTipoTurno() : "Mañana";

        if (metodoPago == null || metodoPago.isBlank()) {
            metodoPago = "Efectivo";
        }
        if (propina == null) propina = BigDecimal.ZERO;
        if (descuento == null) descuento = BigDecimal.ZERO;

        BigDecimal subtotal = pedido != null ? pedido.getTotal() : (kds != null ? kds.getTotal() : BigDecimal.ZERO);
        BigDecimal totalGeneral = subtotal.add(propina).subtract(descuento);
        if (montoRecibido == null || montoRecibido.compareTo(BigDecimal.ZERO) <= 0) {
            montoRecibido = totalGeneral;
        }
        if (cambio == null) {
            cambio = montoRecibido.subtract(totalGeneral);
            if (cambio.compareTo(BigDecimal.ZERO) < 0) cambio = BigDecimal.ZERO;
        }

        // 1. Header
        sb.append(doubleLine()).append("\n");
        sb.append(center(cfg.getNombreRestaurante().toUpperCase())).append("\n");
        sb.append(center("NIT: " + cfg.getNit())).append("\n");
        sb.append(center(cfg.getDireccion())).append("\n");
        sb.append(center("Tel: " + cfg.getTelefono())).append("\n");
        if (cfg.getResolucionDian() != null && !cfg.getResolucionDian().isBlank()) {
            sb.append(center(cfg.getResolucionDian())).append("\n");
        }
        sb.append(line('-')).append("\n");

        // 2. Cuerpo
        sb.append(center("FACTURA DE VENTA #" + id)).append("\n");
        sb.append(twoColumns("Mesa: " + mesa, "Fecha: " + fecha.format(DATE_FMT))).append("\n");
        sb.append(twoColumns("Mesero: " + mesero, "Turno: " + turno)).append("\n");
        sb.append(twoColumns("Cajero: " + cajero, "Caja: " + sesion.getCaja())).append("\n");
        sb.append(line('-')).append("\n");

        // 3. Items Header
        sb.append(padRight("CANT", 5)).append(padRight("DESCRIPCION", 23)).append(padLeft("TOTAL", 14)).append("\n");
        sb.append(line('-')).append("\n");

        // 4. Lista de items
        if (pedido != null && pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            for (OrderItem item : pedido.getItems()) {
                formatearItem(sb, item.getQuantity(), item.getName(), item.getSubtotal());
            }
        } else if (kds != null && kds.getProductos() != null && !kds.getProductos().isBlank()) {
            // Parsear productos de KDS ej: "Cazuela de mariscos x1"
            String[] parts = kds.getProductos().split(",");
            for (String part : parts) {
                part = part.trim();
                int cant = 1;
                String desc = part;
                if (part.contains(" x")) {
                    String[] sp = part.split(" x");
                    desc = sp[0].trim();
                    try {
                        cant = Integer.parseInt(sp[1].trim());
                    } catch (Exception ignored) {
                    }
                }
                formatearItem(sb, cant, desc, kds.getTotal());
            }
        } else {
            formatearItem(sb, 1, "Consumo general", subtotal);
        }

        String nota = pedido != null ? pedido.getNote() : (kds != null ? kds.getNotas() : "");
        if (nota != null && !nota.isBlank()) {
            sb.append("  * Nota: ").append(nota).append("\n");
        }
        sb.append(line('-')).append("\n");

        // 5. Totales
        sb.append(twoColumns("SUBTOTAL:", money(subtotal))).append("\n");
        if (propina.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(twoColumns("PROPINA (10%):", money(propina))).append("\n");
        }
        if (descuento.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(twoColumns("DESCUENTO:", "-" + money(descuento))).append("\n");
        }
        sb.append(doubleLine()).append("\n");
        sb.append(twoColumns("TOTAL GENERAL:", money(totalGeneral))).append("\n");
        sb.append(doubleLine()).append("\n");

        // 6. Metodo de pago
        sb.append(twoColumns("METODO DE PAGO:", metodoPago.toUpperCase())).append("\n");
        sb.append(twoColumns("MONTO RECIBIDO:", money(montoRecibido))).append("\n");
        sb.append(twoColumns("CAMBIO / DEVUELTA:", money(cambio))).append("\n");
        sb.append(line('-')).append("\n");

        // 7. Footer
        sb.append(center(cfg.getMensajePie())).append("\n");
        sb.append(center("¡GRACIAS POR SU COMPRA!")).append("\n");
        sb.append(center("Conserve este ticket para reclamaciones")).append("\n");
        sb.append(line('-')).append("\n");

        // 8. QR opcional con texto de factura
        sb.append(center("[ QR FACTURA ELECTRONICA ]")).append("\n");
        sb.append(center("https://factura.restocontrol.co/f/" + id)).append("\n");
        sb.append(center("  ▄▄▄▄▄▄▄  ▄ ▄▄  ▄▄▄▄▄▄▄  ")).append("\n");
        sb.append(center("  █ ▄▄▄ █  ▄▀▄ █ █ ▄▄▄ █  ")).append("\n");
        sb.append(center("  █ ███ █ █▄  ▄█ █ ███ █  ")).append("\n");
        sb.append(center("  █▄▄▄▄▄█ █ ▄▀ █ █▄▄▄▄▄█  ")).append("\n");
        sb.append(center("  ▄▄▄▄▄ ▄▄ ▄▄█▄▄ ▄▄▄ ▄▄▄  ")).append("\n");
        sb.append(center("  █ ▄▀▄▄▄▄█ ▄▀▄▄ █▀▄█▄▄█  ")).append("\n");
        sb.append(center("  ▄▄▄▄▄▄▄ █ ▄ █▄▄█▄ ▄█▄█  ")).append("\n");
        sb.append(center("  █ ▄▄▄ █ █▄▄ ▄▄▄ █ ▄█▄▄  ")).append("\n");
        sb.append(center("  █ ███ █ ▄▀█  █▄█▄▄▄▀█▄  ")).append("\n");
        sb.append(center("  █▄▄▄▄▄█ █▀  ▄█ ▄ ▄▀▄ █  ")).append("\n");
        sb.append(doubleLine()).append("\n");

        return sb.toString();
    }

    private static void formatearItem(StringBuilder sb, int cant, String desc, BigDecimal total) {
        String cantStr = padRight(String.valueOf(cant), 5);
        String totalStr = padLeft(money(total), 14);

        if (desc.length() <= 23) {
            sb.append(cantStr).append(padRight(desc, 23)).append(totalStr).append("\n");
        } else {
            String firstPart = desc.substring(0, 23);
            String secondPart = desc.substring(23).trim();
            sb.append(cantStr).append(firstPart).append(totalStr).append("\n");
            sb.append("     ").append(padRight(secondPart, 37)).append("\n");
        }
    }

    public static String generarTextoCierre(Turno turno, BigDecimal efectivo, BigDecimal nequi,
                                            BigDecimal tarjeta, BigDecimal diferencia) {
        ConfigDAO cfg = ConfigDAO.getInstance();
        StringBuilder sb = new StringBuilder();

        LocalDateTime inicio = turno.getHoraInicio() != null ? turno.getHoraInicio() : LocalDateTime.now();
        LocalDateTime fin = turno.getHoraFin() != null ? turno.getHoraFin() : LocalDateTime.now();
        String usuario = resolverNombreMesero(turno.getUsuarioId());

        BigDecimal totalVentas = turno.getVentasTurno() != null ? turno.getVentasTurno() : BigDecimal.ZERO;
        if (efectivo == null) efectivo = totalVentas.multiply(new BigDecimal("0.60")); // estimación por defecto
        if (nequi == null) nequi = totalVentas.multiply(new BigDecimal("0.25"));
        if (tarjeta == null) tarjeta = totalVentas.subtract(efectivo).subtract(nequi);
        if (tarjeta.compareTo(BigDecimal.ZERO) < 0) tarjeta = BigDecimal.ZERO;

        BigDecimal baseCaja = new BigDecimal("100000"); // Base estándar
        BigDecimal esperado = baseCaja.add(efectivo);
        BigDecimal contado = esperado;
        if (diferencia == null) diferencia = BigDecimal.ZERO;
        else contado = esperado.add(diferencia);

        sb.append(doubleLine()).append("\n");
        sb.append(center(cfg.getNombreRestaurante().toUpperCase())).append("\n");
        sb.append(center("NIT: " + cfg.getNit())).append("\n");
        sb.append(center(cfg.getDireccion())).append("\n");
        sb.append(center("Tel: " + cfg.getTelefono())).append("\n");
        sb.append(doubleLine()).append("\n");
        sb.append(center("*** CIERRE DE CAJA / TURNO ***")).append("\n");
        sb.append(doubleLine()).append("\n");

        sb.append(twoColumns("TURNO ID:", "#" + turno.getId())).append("\n");
        sb.append(twoColumns("FECHA:", turno.getFecha() != null ? turno.getFecha().toString() : "")).append("\n");
        sb.append(twoColumns("TIPO TURNO:", turno.getTurno())).append("\n");
        sb.append(twoColumns("CAJERO/USUARIO:", usuario)).append("\n");
        sb.append(twoColumns("HORA APERTURA:", inicio.format(DATE_FMT))).append("\n");
        sb.append(twoColumns("HORA CIERRE:", fin.format(DATE_FMT))).append("\n");
        sb.append(line('-')).append("\n");

        sb.append(center("VENTAS POR METODO DE PAGO")).append("\n");
        sb.append(line('-')).append("\n");
        sb.append(twoColumns("Efectivo:", money(efectivo))).append("\n");
        sb.append(twoColumns("Nequi / Transferencia:", money(nequi))).append("\n");
        sb.append(twoColumns("Tarjeta (Débito/Crédito):", money(tarjeta))).append("\n");
        sb.append(line('-')).append("\n");
        sb.append(twoColumns("TOTAL VENTAS:", money(totalVentas))).append("\n");
        sb.append(doubleLine()).append("\n");

        sb.append(center("ARQUEO DE CAJA EFECTIVO")).append("\n");
        sb.append(line('-')).append("\n");
        sb.append(twoColumns("Base de Caja:", money(baseCaja))).append("\n");
        sb.append(twoColumns("Ventas Efectivo:", money(efectivo))).append("\n");
        sb.append(twoColumns("Efectivo Esperado:", money(esperado))).append("\n");
        sb.append(twoColumns("Efectivo en Caja:", money(contado))).append("\n");
        sb.append(line('-')).append("\n");
        sb.append(twoColumns("DIFERENCIA:", money(diferencia))).append("\n");
        sb.append(doubleLine()).append("\n\n\n");

        sb.append(twoColumns("____________________", "____________________")).append("\n");
        sb.append(twoColumns("    Firma Cajero    ", " Firma Administrador")).append("\n\n");
        sb.append(center("RestoControl SaaS 2026")).append("\n");
        sb.append(doubleLine()).append("\n");

        return sb.toString();
    }

    // --- UTILITARIOS DE FORMATO ---

    public static String money(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        return CURRENCY.format(amount).replace("\u00A0", "").replace(" ", "");
    }

    private static String shortId(String id) {
        if (id == null) return "00000000";
        String clean = id.replace("-", "").toUpperCase();
        return clean.length() >= 8 ? clean.substring(0, 8) : clean;
    }

    private static String center(String text) {
        if (text == null) text = "";
        if (text.length() >= LINE_WIDTH) return text;
        int left = (LINE_WIDTH - text.length()) / 2;
        int right = LINE_WIDTH - text.length() - left;
        return " ".repeat(left) + text + " ".repeat(right);
    }

    private static String padRight(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text.substring(0, width);
        return text + " ".repeat(width - text.length());
    }

    private static String padLeft(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text.substring(0, width);
        return " ".repeat(width - text.length()) + text;
    }

    private static String twoColumns(String left, String right) {
        if (left == null) left = "";
        if (right == null) right = "";
        int space = LINE_WIDTH - left.length() - right.length();
        if (space < 1) space = 1;
        return left + " ".repeat(space) + right;
    }

    private static String line(char c) {
        return String.valueOf(c).repeat(LINE_WIDTH);
    }

    private static String doubleLine() {
        return "=".repeat(LINE_WIDTH);
    }

    private static String resolverNombreMesero(int usuarioId) {
        try {
            List<Usuario> usuarios = DatabaseManager.getInstance().getUsuarios();
            for (Usuario u : usuarios) {
                if (u.getId() == usuarioId) {
                    return u.getNombre();
                }
            }
        } catch (Exception ignored) {
        }
        if (SesionActual.getInstance().getUsuario() != null) {
            return SesionActual.getInstance().getUsuario().getNombre();
        }
        return "Carlos";
    }

    private static PedidoKDS obtenerUltimoPedidoBD() {
        String sql = "SELECT id, mesa, fecha, productos, notas, estado, total, usuario_id, turno_id FROM pedidos ORDER BY fecha DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp("fecha");
                return new PedidoKDS(
                        rs.getString("id"),
                        rs.getInt("mesa"),
                        ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                        rs.getString("productos"),
                        rs.getString("notas"),
                        rs.getString("estado"),
                        rs.getBigDecimal("total"),
                        rs.getInt("usuario_id"),
                        rs.getInt("turno_id")
                );
            }
        } catch (SQLException ex) {
            System.err.println("Error consultando último pedido: " + ex.getMessage());
        }
        return null;
    }

    public static PedidoKDS obtenerPedidoPorId(String pedidoId) {
        String sql = "SELECT id, mesa, fecha, productos, notas, estado, total, usuario_id, turno_id FROM pedidos WHERE id = ? OR id LIKE ? LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pedidoId);
            ps.setString(2, pedidoId + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("fecha");
                    return new PedidoKDS(
                            rs.getString("id"),
                            rs.getInt("mesa"),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                            rs.getString("productos"),
                            rs.getString("notas"),
                            rs.getString("estado"),
                            rs.getBigDecimal("total"),
                            rs.getInt("usuario_id"),
                            rs.getInt("turno_id")
                    );
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error consultando pedido por ID: " + ex.getMessage());
        }
        return null;
    }
}
