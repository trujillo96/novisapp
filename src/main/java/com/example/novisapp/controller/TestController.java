package com.example.novisapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    @Autowired(required = false)
    private DataSource dataSource;

    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "¡Funcionando! Controlador detectado correctamente");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("package", "com.example.novisapp");
        return response;
    }

    @GetMapping("/hello")
    public String hello() {
        return "¡Hola! Spring Boot está funcionando perfectamente";
    }

    @GetMapping("/")
    public String root() {
        return "Sistema Novis - Página principal funcionando";
    }

    @GetMapping("/api/test/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Health check OK");
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    @GetMapping("/api/test/db-connection")
    public Map<String, Object> testDatabaseConnection() {
        Map<String, Object> response = new HashMap<>();

        if (dataSource == null) {
            response.put("status", "ERROR");
            response.put("message", "DataSource no configurado");
            response.put("connected", false);
            return response;
        }

        try (Connection connection = dataSource.getConnection()) {
            // Test conexión básica
            response.put("connected", true);
            response.put("database", connection.getCatalog());
            response.put("url", connection.getMetaData().getURL());
            response.put("driver", connection.getMetaData().getDriverName());
            response.put("driver_version", connection.getMetaData().getDriverVersion());

            // Test consulta SQL
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT @@VERSION AS version, DB_NAME() AS database_name, GETDATE() AS server_time")) {

                if (rs.next()) {
                    response.put("sql_server_version", rs.getString("version"));
                    response.put("current_database", rs.getString("database_name"));
                    response.put("server_time", rs.getString("server_time"));
                }
            }

            response.put("status", "SUCCESS");
            response.put("message", "🎉 ¡Conexión a Azure SQL Database exitosa!");

        } catch (Exception e) {
            response.put("connected", false);
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("error_type", e.getClass().getSimpleName());
        }

        return response;
    }

    @GetMapping("/api/test/db-info")
    public Map<String, Object> getDatabaseInfo() {
        Map<String, Object> response = new HashMap<>();

        if (dataSource == null) {
            response.put("status", "ERROR");
            response.put("message", "DataSource no configurado");
            return response;
        }

        try (Connection connection = dataSource.getConnection()) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT TABLE_NAME, TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'"
                 )) {

                java.util.List<String> tables = new java.util.ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }

                response.put("status", "SUCCESS");
                response.put("tables", tables);
                response.put("table_count", tables.size());
                response.put("message", "Información de tablas obtenida desde Azure SQL");
            }

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
        }

        return response;
    }
}