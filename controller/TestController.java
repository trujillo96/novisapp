package com.example.novisapp.controller;

import com.example.novisapp.entity.User;
import com.example.novisapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*") // Permitir CORS para testing
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DataSource dataSource;

    // 1. Health Check básico
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Sistema Novis funcionando correctamente");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Novis Legal System");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    // 2. Test de conexión a base de datos
    @GetMapping("/db-connection")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection() {
        Map<String, Object> response = new HashMap<>();

        try (Connection connection = dataSource.getConnection()) {
            // Test conexión básica
            response.put("connected", true);
            response.put("database", connection.getCatalog());
            response.put("url", connection.getMetaData().getURL());
            response.put("driver", connection.getMetaData().getDriverName());
            response.put("driver_version", connection.getMetaData().getDriverVersion());

            // Test consulta SQL
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT @@VERSION AS version, DB_NAME() AS database_name, GETDATE() AS current_time")) {

                if (rs.next()) {
                    response.put("sql_server_version", rs.getString("version"));
                    response.put("current_database", rs.getString("database_name"));
                    response.put("server_time", rs.getString("current_time"));
                }
            }

            response.put("status", "SUCCESS");
            response.put("message", "Conexión a Azure SQL Database exitosa");

        } catch (Exception e) {
            response.put("connected", false);
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("error_type", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // 3. Test de JPA/Hibernate
    @GetMapping("/jpa-test")
    public ResponseEntity<Map<String, Object>> testJPA() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Contar usuarios existentes
            long userCount = userRepository.count();
            long activeUsers = userRepository.countActiveUsers();

            response.put("total_users", userCount);
            response.put("active_users", activeUsers);
            response.put("jpa_working", true);
            response.put("hibernate_working", true);
            response.put("status", "SUCCESS");
            response.put("message", "JPA/Hibernate funcionando correctamente");

        } catch (Exception e) {
            response.put("jpa_working", false);
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("error_type", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // 4. Crear usuario de prueba
    @PostMapping("/create-user")
    public ResponseEntity<Map<String, Object>> createTestUser() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Verificar si el usuario ya existe
            if (userRepository.findByEmail("test@novis.com").isPresent()) {
                response.put("status", "INFO");
                response.put("message", "Usuario de prueba ya existe");
                User existingUser = userRepository.findByEmail("test@novis.com").get();
                response.put("user_id", existingUser.getId());
                response.put("user_email", existingUser.getEmail());
                response.put("created_at", existingUser.getCreatedAt());
                return ResponseEntity.ok(response);
            }

            User testUser = new User();
            testUser.setEmail("test@novis.com");
            testUser.setFirstName("Usuario");
            testUser.setLastName("Prueba");
            testUser.setRole("ADMIN");
            testUser.setActive(true);

            User savedUser = userRepository.save(testUser);

            response.put("status", "SUCCESS");
            response.put("message", "Usuario creado exitosamente en Azure SQL Database");
            response.put("user_id", savedUser.getId());
            response.put("user_email", savedUser.getEmail());
            response.put("user_name", savedUser.getFirstName() + " " + savedUser.getLastName());
            response.put("user_role", savedUser.getRole());
            response.put("created_at", savedUser.getCreatedAt());
            response.put("updated_at", savedUser.getUpdatedAt());

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("error_type", e.getClass().getSimpleName());

            // Información adicional para debugging
            if (e.getMessage().contains("connection")) {
                response.put("suggestion", "Verificar conexión a Azure SQL Database");
            } else if (e.getMessage().contains("constraint")) {
                response.put("suggestion", "Email duplicado o restricción de base de datos");
            }

            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // 5. Listar todos los usuarios
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<User> users = userRepository.findAll();
            List<User> activeUsers = userRepository.findByActiveTrue();

            response.put("status", "SUCCESS");
            response.put("total_count", users.size());
            response.put("active_count", activeUsers.size());
            response.put("users", users);
            response.put("message", "Usuarios obtenidos desde Azure SQL Database");

            if (users.isEmpty()) {
                response.put("info", "No hay usuarios en la base de datos. Usa POST /api/test/create-user para crear uno.");
            }

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("error_type", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // 6. Obtener usuario por email
    @GetMapping("/user/{email}")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@PathVariable String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            var userOptional = userRepository.findByEmail(email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                response.put("status", "SUCCESS");
                response.put("found", true);
                response.put("user", user);
                response.put("message", "Usuario encontrado");
            } else {
                response.put("status", "NOT_FOUND");
                response.put("found", false);
                response.put("message", "Usuario no encontrado con email: " + email);
            }

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // 7. Test de tablas en la base de datos
    @GetMapping("/db-tables")
    public ResponseEntity<Map<String, Object>> getDatabaseTables() {
        Map<String, Object> response = new HashMap<>();

        try (Connection connection = dataSource.getConnection()) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT TABLE_NAME, TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'"
                 )) {

                java.util.List<Map<String, String>> tables = new java.util.ArrayList<>();
                while (rs.next()) {
                    Map<String, String> table = new HashMap<>();
                    table.put("name", rs.getString("TABLE_NAME"));
                    table.put("type", rs.getString("TABLE_TYPE"));
                    tables.add(table);
                }

                response.put("status", "SUCCESS");
                response.put("tables", tables);
                response.put("table_count", tables.size());
                response.put("message", "Tablas obtenidas desde Azure SQL Database");
            }

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // 8. Limpiar datos de prueba
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        Map<String, Object> response = new HashMap<>();

        try {
            long countBefore = userRepository.count();
            userRepository.deleteAll();
            long countAfter = userRepository.count();

            response.put("status", "SUCCESS");
            response.put("message", "Datos de prueba eliminados exitosamente");
            response.put("users_deleted", countBefore);
            response.put("users_remaining", countAfter);
            response.put("timestamp", LocalDateTime.now().toString());

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("error_type", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }

    // 9. Endpoint de información del sistema
    @GetMapping("/system-info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> response = new HashMap<>();

        response.put("status", "SUCCESS");
        response.put("application_name", "Novis Legal System");
        response.put("version", "1.0.0-SNAPSHOT");
        response.put("java_version", System.getProperty("java.version"));
        response.put("spring_boot_version", "3.1.5");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("timezone", java.time.ZoneId.systemDefault().toString());

        // Información de memoria
        Runtime runtime = Runtime.getRuntime();
        response.put("memory_total", runtime.totalMemory());
        response.put("memory_free", runtime.freeMemory());
        response.put("memory_used", runtime.totalMemory() - runtime.freeMemory());

        return ResponseEntity.ok(response);
    }
}