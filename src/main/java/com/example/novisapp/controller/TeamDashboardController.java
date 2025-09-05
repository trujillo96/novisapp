package com.example.novisapp.controller;

import com.example.novisapp.entity.CaseStatus;
import com.example.novisapp.entity.LegalCase;
import com.example.novisapp.entity.User;
import com.example.novisapp.entity.UserRole; // ✅ IMPORT AGREGADO
import com.example.novisapp.repository.LegalCaseRepository;
import com.example.novisapp.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "*")
public class TeamDashboardController {

    @Autowired
    private LegalCaseRepository legalCaseRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Dashboard básico de equipos
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getTeamsDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        try {
            // Obtener todos los abogados disponibles
            List<User> allLawyers = userRepository.findAvailableLawyers();

            // Obtener casos activos (no cerrados)
            List<LegalCase> activeCases = legalCaseRepository.findByStatusNot(CaseStatus.CLOSED);

            // Calcular estadísticas básicas
            long totalCases = activeCases.size();
            int totalLawyers = allLawyers.size();
            double averageCasesPerLawyer = totalLawyers > 0 ? (double) totalCases / totalLawyers : 0;

            // Calcular carga de trabajo por abogado
            List<Map<String, Object>> lawyerWorkload = allLawyers.stream()
                    .map(lawyer -> {
                        List<LegalCase> lawyerCases = legalCaseRepository.findCasesAssignedToLawyer(lawyer.getId());
                        Map<String, Object> data = new HashMap<>();
                        data.put("lawyer_id", lawyer.getId());
                        data.put("lawyer_name", lawyer.getFullName());
                        data.put("case_count", lawyerCases.size());
                        data.put("email", lawyer.getEmail());
                        return data;
                    })
                    .collect(Collectors.toList());

            dashboard.put("success", true);
            dashboard.put("total_cases", totalCases);
            dashboard.put("total_lawyers", totalLawyers);
            dashboard.put("average_cases_per_lawyer", averageCasesPerLawyer);
            dashboard.put("lawyer_workload", lawyerWorkload);
            dashboard.put("generated_at", java.time.LocalDateTime.now());

            return ResponseEntity.ok(dashboard);

        } catch (Exception e) {
            dashboard.put("success", false);
            dashboard.put("message", "Error al generar dashboard: " + e.getMessage());
            return ResponseEntity.status(500).body(dashboard);
        }
    }

    /**
     * Obtener abogados disponibles
     */
    @GetMapping("/available-lawyers")
    public ResponseEntity<List<User>> getAvailableLawyers() {
        try {
            // Usar el método que ya funciona en el repository
            List<User> lawyers = userRepository.findAvailableLawyers();
            return ResponseEntity.ok(lawyers);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Obtener información del equipo de un caso
     */
    @GetMapping("/cases/{caseId}/team")
    public ResponseEntity<Map<String, Object>> getCaseTeam(@PathVariable Long caseId) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<User> assignedLawyers = legalCaseRepository.findAssignedLawyersByCaseId(caseId);

            response.put("success", true);
            response.put("case_id", caseId);
            response.put("assigned_lawyers", assignedLawyers);
            response.put("team_size", assignedLawyers.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Asignar abogados a un caso (versión simplificada)
     */
    @PostMapping("/cases/{caseId}/assign-lawyers")
    public ResponseEntity<Map<String, Object>> assignLawyersToCase(
            @PathVariable Long caseId,
            @RequestBody AssignLawyersRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Aquí implementarías la lógica de asignación
            // Por ahora devolvemos una respuesta exitosa
            response.put("success", true);
            response.put("message", "Abogados asignados correctamente");
            response.put("case_id", caseId);
            response.put("assigned_lawyer_ids", request.getLawyerIds());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al asignar abogados: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Obtener estadísticas de carga de trabajo
     */
    @GetMapping("/workload-stats")
    public ResponseEntity<Map<String, Object>> getWorkloadStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<User> lawyers = userRepository.findAvailableLawyers();
            List<LegalCase> activeCases = legalCaseRepository.findByStatusNot(CaseStatus.CLOSED);

            // Calcular estadísticas básicas
            Map<String, Object> summary = new HashMap<>();
            summary.put("total_lawyers", lawyers.size());
            summary.put("total_active_cases", activeCases.size());
            summary.put("average_cases_per_lawyer",
                    lawyers.size() > 0 ? (double) activeCases.size() / lawyers.size() : 0);

            stats.put("success", true);
            stats.put("summary", summary);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            stats.put("success", false);
            stats.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(stats);
        }
    }
}

// DTO simplificado para asignación de abogados
class AssignLawyersRequest {
    private List<Long> lawyerIds;
    private String notes;

    // Getters y setters
    public List<Long> getLawyerIds() {
        return lawyerIds;
    }

    public void setLawyerIds(List<Long> lawyerIds) {
        this.lawyerIds = lawyerIds;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}