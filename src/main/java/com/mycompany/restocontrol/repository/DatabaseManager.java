package com.mycompany.restocontrol.repository;

import com.mycompany.restocontrol.model.Turno;
import com.mycompany.restocontrol.model.Usuario;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseManager {

    private static final String DB_DIR = "./data";
    private static final String DB_URL = "jdbc:h2:./data/restocontrol;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static DatabaseManager instance;

    private DatabaseManager() {
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void initDatabase() {
        File dataDir = new File(DB_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Tabla usuarios
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nombre VARCHAR(100) NOT NULL,
                    usuario VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    rol ENUM('ADMIN','MESERO','CAJERO','CHEF') NOT NULL,
                    activo BOOLEAN DEFAULT TRUE
                )
            """);

            // 2. Tabla turnos
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS turnos (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    usuario_id INT NOT NULL,
                    fecha DATE NOT NULL,
                    turno ENUM('Mañana','Tarde','Noche') NOT NULL,
                    hora_inicio TIMESTAMP NOT NULL,
                    hora_fin TIMESTAMP,
                    ventas_turno DECIMAL(12,2) DEFAULT 0,
                    estado ENUM('ABIERTO','CERRADO') NOT NULL DEFAULT 'ABIERTO',
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                )
            """);

            // 3. Tabla mesas
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mesas (
                    numero INT PRIMARY KEY,
                    estado VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE'
                )
            """);

            // 4. Tabla pedidos
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS pedidos (
                    id VARCHAR(64) PRIMARY KEY,
                    mesa INT NOT NULL,
                    fecha TIMESTAMP NOT NULL,
                    productos TEXT NOT NULL,
                    notas TEXT,
                    estado VARCHAR(30) NOT NULL,
                    total DECIMAL(12,2) DEFAULT 0,
                    usuario_id INT,
                    turno_id INT
                )
            """);

            // Insertar datos por defecto
            insertDefaultUsers(conn);
            insertDefaultMesas(conn);
            insertDefaultPedidos(conn);

        } catch (SQLException ex) {
            System.err.println("Error al inicializar base de datos: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void insertDefaultUsers(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM usuarios";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO usuarios (nombre, usuario, password, rol, activo) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    // admin / admin123 (ADMIN)
                    ps.setString(1, "Admin");
                    ps.setString(2, "admin");
                    ps.setString(3, "admin123");
                    ps.setString(4, "ADMIN");
                    ps.setBoolean(5, true);
                    ps.addBatch();

                    // carlos / carlos123 (MESERO)
                    ps.setString(1, "Carlos");
                    ps.setString(2, "carlos");
                    ps.setString(3, "carlos123");
                    ps.setString(4, "MESERO");
                    ps.setBoolean(5, true);
                    ps.addBatch();

                    // maria / maria123 (CHEF)
                    ps.setString(1, "Maria");
                    ps.setString(2, "maria");
                    ps.setString(3, "maria123");
                    ps.setString(4, "CHEF");
                    ps.setBoolean(5, true);
                    ps.addBatch();

                    // andrea / andrea123 (CAJERO)
                    ps.setString(1, "Andrea");
                    ps.setString(2, "andrea");
                    ps.setString(3, "andrea123");
                    ps.setString(4, "CAJERO");
                    ps.setBoolean(5, true);
                    ps.addBatch();

                    ps.executeBatch();
                }
            }
        }
    }

    public Usuario autenticar(String usuario, String password) {
        String sql = "SELECT id, nombre, usuario, password, rol, activo FROM usuarios WHERE LOWER(usuario) = LOWER(?) AND activo = TRUE";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPass = rs.getString("password");
                    if (password != null && password.equals(dbPass)) {
                        return new Usuario(
                                rs.getInt("id"),
                                rs.getString("nombre"),
                                rs.getString("usuario"),
                                rs.getString("password"),
                                Usuario.Rol.valueOf(rs.getString("rol")),
                                rs.getBoolean("activo")
                        );
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al autenticar: " + ex.getMessage());
        }
        return null;
    }

    public int iniciarTurno(int usuarioId, String tipoTurno) {
        String sql = "INSERT INTO turnos (usuario_id, fecha, turno, hora_inicio, ventas_turno, estado) VALUES (?, CURRENT_DATE, ?, NOW(), 0, 'ABIERTO')";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, tipoTurno != null ? tipoTurno : "Mañana");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al iniciar turno: " + ex.getMessage());
        }
        return 0;
    }

    public void cerrarTurno(int turnoId, BigDecimal ventasTurno) {
        String sql = "UPDATE turnos SET hora_fin = NOW(), ventas_turno = ?, estado = 'CERRADO' WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, ventasTurno != null ? ventasTurno : BigDecimal.ZERO);
            ps.setInt(2, turnoId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error al cerrar turno: " + ex.getMessage());
        }
    }

    public Turno getTurno(int turnoId) {
        String sql = "SELECT id, usuario_id, fecha, turno, hora_inicio, hora_fin, ventas_turno, estado FROM turnos WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, turnoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date d = rs.getDate("fecha");
                    Timestamp tsInicio = rs.getTimestamp("hora_inicio");
                    Timestamp tsFin = rs.getTimestamp("hora_fin");
                    return new Turno(
                            rs.getInt("id"),
                            rs.getInt("usuario_id"),
                            d != null ? d.toLocalDate() : LocalDate.now(),
                            rs.getString("turno"),
                            tsInicio != null ? tsInicio.toLocalDateTime() : LocalDateTime.now(),
                            tsFin != null ? tsFin.toLocalDateTime() : null,
                            rs.getBigDecimal("ventas_turno"),
                            Turno.Estado.valueOf(rs.getString("estado"))
                    );
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error al obtener turno: " + ex.getMessage());
        }
        return null;
    }

    private void insertDefaultMesas(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM mesas";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO mesas (numero, estado) VALUES (?, ?)")) {
                    for (int i = 1; i <= 12; i++) {
                        ps.setInt(1, i);
                        ps.setString(2, (i == 11 || i == 4 || i == 8 || i == 2) ? "OCUPADA" : "DISPONIBLE");
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
        }
    }

    private void insertDefaultPedidos(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM pedidos";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String sql = "INSERT INTO pedidos (id, mesa, fecha, productos, notas, estado, total, usuario_id, turno_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    LocalDateTime now = LocalDateTime.now();

                    // 1. POR PREPARAR - Mesa 11 (1 min)
                    ps.setString(1, "CD18E747");
                    ps.setInt(2, 11);
                    ps.setTimestamp(3, Timestamp.valueOf(now.minusMinutes(1)));
                    ps.setString(4, "Cazuela de mariscos x1");
                    ps.setString(5, "Sin cilantro, picante extra");
                    ps.setString(6, "POR_PREPARAR");
                    ps.setBigDecimal(7, new BigDecimal("40000"));
                    ps.setInt(8, 2);
                    ps.setInt(9, 1);
                    ps.addBatch();

                    // 2. EN PREPARACION - Mesa 4 (12 min -> URGENTE)
                    ps.setString(1, "A82F10B4");
                    ps.setInt(2, 4);
                    ps.setTimestamp(3, Timestamp.valueOf(now.minusMinutes(12)));
                    ps.setString(4, "Mojarra frita con arroz de coco x2, Limonada de coco x2");
                    ps.setString(5, "Bien dorada la mojarra");
                    ps.setString(6, "EN_PREPARACION");
                    ps.setBigDecimal(7, new BigDecimal("104000"));
                    ps.setInt(8, 2);
                    ps.setInt(9, 1);
                    ps.addBatch();

                    // 3. EN PREPARACION - Mesa 8 (18 min -> ROJO & BLINK)
                    ps.setString(1, "E59C34D1");
                    ps.setInt(2, 8);
                    ps.setTimestamp(3, Timestamp.valueOf(now.minusMinutes(18)));
                    ps.setString(4, "Arroz de mariscos x1, Jugo de corozo x1");
                    ps.setString(5, "Alergia a marisco crudo");
                    ps.setString(6, "EN_PREPARACION");
                    ps.setBigDecimal(7, new BigDecimal("47000"));
                    ps.setInt(8, 2);
                    ps.setInt(9, 1);
                    ps.addBatch();

                    // 4. LISTO - Mesa 2 (6 min)
                    ps.setString(1, "B209F831");
                    ps.setInt(2, 2);
                    ps.setTimestamp(3, Timestamp.valueOf(now.minusMinutes(6)));
                    ps.setString(4, "Posta cartagenera con arroz de coco x1");
                    ps.setString(5, "");
                    ps.setString(6, "LISTO");
                    ps.setBigDecimal(7, new BigDecimal("36000"));
                    ps.setInt(8, 2);
                    ps.setInt(9, 1);
                    ps.addBatch();

                    ps.executeBatch();
                }
            }
        }
    }

    public void actualizarEstadoPedido(String id, String nuevoEstado) {
        String sql = "UPDATE pedidos SET estado = ? WHERE id = ? OR id LIKE ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, id);
            ps.setString(3, id + "%");
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error actualizando estado pedido: " + ex.getMessage());
        }
    }

    public void entregarPedidoYLiberarMesa(String id, int mesaNumero) {
        actualizarEstadoPedido(id, "ENTREGADO");
        liberarMesa(mesaNumero);
    }

    public void liberarMesa(int mesaNumero) {
        String sql = "UPDATE mesas SET estado = 'DISPONIBLE' WHERE numero = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mesaNumero);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error liberando mesa: " + ex.getMessage());
        }
    }

    public void ocuparMesa(int mesaNumero) {
        String sql = "UPDATE mesas SET estado = 'OCUPADA' WHERE numero = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mesaNumero);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error ocupando mesa: " + ex.getMessage());
        }
    }

    public void guardarPedido(String id, int mesa, LocalDateTime fecha, String productos,
                              String notas, String estado, BigDecimal total, int usuarioId, int turnoId) {
        String sql = "MERGE INTO pedidos (id, mesa, fecha, productos, notas, estado, total, usuario_id, turno_id) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setInt(2, mesa);
            ps.setTimestamp(3, Timestamp.valueOf(fecha != null ? fecha : LocalDateTime.now()));
            ps.setString(4, productos);
            ps.setString(5, notas);
            ps.setString(6, estado);
            ps.setBigDecimal(7, total);
            ps.setInt(8, usuarioId);
            ps.setInt(9, turnoId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Error guardando pedido en BD: " + ex.getMessage());
        }
    }

    public List<Usuario> getUsuarios() {
        List<Usuario> list = new ArrayList<>();
        String sql = "SELECT id, nombre, usuario, password, rol, activo FROM usuarios WHERE activo = TRUE ORDER BY id";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Usuario(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("usuario"),
                        rs.getString("password"),
                        Usuario.Rol.valueOf(rs.getString("rol")),
                        rs.getBoolean("activo")
                ));
            }
        } catch (SQLException ex) {
            System.err.println("Error al listar usuarios: " + ex.getMessage());
        }
        return list;
    }
}
