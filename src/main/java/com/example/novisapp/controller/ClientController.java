package com.example.novisapp.controller;

import com.example.novisapp.repository.ClientRepository;
import com.example.novisapp.entity.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
public class ClientController {

    @Autowired
    private ClientRepository clientRepository;

    // Crear nuevo cliente
    @PostMapping
    public ResponseEntity<Map<String, Object>> createClient(@RequestBody Client client) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validar email único
            if (clientRepository.findByEmail(client.getEmail()).isPresent()) {
                response.put("status", "ERROR");
                response.put("message", "Email ya existe en el sistema");
                return ResponseEntity.badRequest().body(response);
            }

            // Establecer valores por defecto
            if (client.getActive() == null) client.setActive(true);
            if (client.getClientType() == null) client.setClientType("Individual");

            Client savedClient = clientRepository.save(client);

            response.put("status", "SUCCESS");
            response.put("message", "Cliente creado exitosamente");
            response.put("client", savedClient);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al crear cliente: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Listar todos los clientes
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllClients() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Client> clients = clientRepository.findAll();

            response.put("status", "SUCCESS");
            response.put("clients", clients);
            response.put("total_count", clients.size());
            response.put("message", "Clientes obtenidos exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al obtener clientes: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Obtener cliente por ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getClientById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<Client> clientOptional = clientRepository.findById(id);

            if (clientOptional.isPresent()) {
                response.put("status", "SUCCESS");
                response.put("client", clientOptional.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_FOUND");
                response.put("message", "Cliente no encontrado");
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al obtener cliente: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Obtener clientes activos
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveClients() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Client> clients = clientRepository.findByActiveTrue();

            response.put("status", "SUCCESS");
            response.put("clients", clients);
            response.put("total_count", clients.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Buscar clientes por nombre
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchClients(@RequestParam String query) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Client> clients = clientRepository.findByNameContainingIgnoreCase(query);

            response.put("status", "SUCCESS");
            response.put("clients", clients);
            response.put("search_query", query);
            response.put("results_count", clients.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error en búsqueda: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Obtener clientes por país
    @GetMapping("/country/{country}")
    public ResponseEntity<Map<String, Object>> getClientsByCountry(@PathVariable String country) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Client> clients = clientRepository.findByCountry(country);

            response.put("status", "SUCCESS");
            response.put("clients", clients);
            response.put("country", country);
            response.put("total_count", clients.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Estadísticas básicas
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getClientStats() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> stats = new HashMap<>();

            stats.put("total_clients", clientRepository.count());
            stats.put("active_clients", clientRepository.countActiveClients());
            stats.put("clients_by_country", clientRepository.countClientsByCountry());
            stats.put("clients_by_type", clientRepository.countClientsByType());

            response.put("status", "SUCCESS");
            response.put("statistics", stats);
            response.put("generated_at", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al obtener estadísticas: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}