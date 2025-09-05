package com.example.novisapp.controller;

import com.example.novisapp.entity.*;
import com.example.novisapp.repository.ClientRepository;
import com.example.novisapp.repository.LegalCaseRepository;
import com.example.novisapp.repository.UserRepository;
import com.example.novisapp.service.CaseLawyerAssignmentService; // ✅ AGREGADO
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // ✅ AGREGADO
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/legal-cases")
@CrossOrigin(origins = "*")
@Slf4j
public class LegalCaseController {

    @Autowired
    private LegalCaseRepository legalCaseRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseLawyerAssignmentService assignmentService; // ✅ AGREGADO

    // ============================================================================
    // ENDPOINTS PRINCIPALES - CRUD COMPLETO
    // ============================================================================

    /**
     * ✅ GET /api/legal-cases - Listar todos los casos con paginación y filtros
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        Map<String, Object> response = new HashMap<>();

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
            List<LegalCase> casesList;

            if (status != null && !status.trim().isEmpty()) {
                try {
                    CaseStatus caseStatus = CaseStatus.valueOf(status.toUpperCase());
                    casesList = legalCaseRepository.findByStatusOrderByCreatedAtDesc(caseStatus);
                } catch (IllegalArgumentException e) {
                    casesList = legalCaseRepository.findAll();
                }
            } else if (search != null && !search.trim().isEmpty()) {
                casesList = legalCaseRepository.findByTitleContainingIgnoreCase(search);
            } else {
                Page<LegalCase> casePage = legalCaseRepository.findAll(pageable);
                casesList = casePage.getContent();

                List<Map<String, Object>> safeCases = new ArrayList<>();
                for (LegalCase legalCase : casesList) {
                    safeCases.add(createCaseInfo(legalCase));
                }

                response.put("status", "SUCCESS");
                response.put("cases", safeCases);
                response.put("total", casePage.getTotalElements());
                response.put("totalPages", casePage.getTotalPages());
                response.put("currentPage", page);
                response.put("pageSize", size);
                response.put("message", "Casos obtenidos exitosamente");
                return ResponseEntity.ok(response);
            }

            List<Map<String, Object>> safeCases = new ArrayList<>();
            for (LegalCase legalCase : casesList) {
                safeCases.add(createCaseInfo(legalCase));
            }

            response.put("status", "SUCCESS");
            response.put("cases", safeCases);
            response.put("total", casesList.size());
            response.put("totalPages", 1);
            response.put("currentPage", 0);
            response.put("pageSize", casesList.size());
            response.put("message", "Casos obtenidos exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al obtener casos: " + e.getMessage());
            response.put("cases", new ArrayList<>());
            response.put("total", 0);
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * ✅ POST /api/legal-cases - Crear nuevo caso
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCase(@RequestBody Map<String, Object> caseData) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (!caseData.containsKey("title") || caseData.get("title").toString().trim().isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "El título es requerido");
                return ResponseEntity.badRequest().body(response);
            }

            if (!caseData.containsKey("clientId")) {
                response.put("status", "ERROR");
                response.put("message", "El cliente es requerido");
                return ResponseEntity.badRequest().body(response);
            }

            Long clientId = Long.valueOf(caseData.get("clientId").toString());
            Optional<Client> clientOptional = clientRepository.findById(clientId);
            if (!clientOptional.isPresent()) {
                response.put("status", "ERROR");
                response.put("message", "Cliente no encontrado");
                return ResponseEntity.badRequest().body(response);
            }

            LegalCase legalCase = new LegalCase();
            legalCase.setTitle(caseData.get("title").toString());
            legalCase.setDescription(caseData.getOrDefault("description", "").toString());
            legalCase.setJustification(caseData.getOrDefault("justification", "").toString());
            legalCase.setClient(clientOptional.get());
            legalCase.setCaseNumber(generateCaseNumber());
            legalCase.setStatus(CaseStatus.OPEN);
            legalCase.setPriority(caseData.getOrDefault("priority", "MEDIUM").toString());

            if (caseData.containsKey("caseType")) {
                String caseType = caseData.get("caseType").toString();
                legalCase.setCaseType(caseType);
                legalCase.setRequiredSpecialty(LegalSpecialty.fromCaseType(caseType));
            } else {
                legalCase.setRequiredSpecialty(LegalSpecialty.CIVIL_LAW);
            }
            legalCase.setCountry(Country.MEXICO);
            legalCase.setComplexity(CaseComplexity.MEDIUM);

            LegalCase savedCase = legalCaseRepository.save(legalCase);

            response.put("status", "SUCCESS");
            response.put("message", "Caso creado exitosamente");
            response.put("case", createCaseInfo(savedCase));
            response.put("caseNumber", savedCase.getCaseNumber());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al crear caso: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * ✅ GET /api/legal-cases/{id} - Obtener caso por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCaseById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<LegalCase> caseOptional = legalCaseRepository.findById(id);

            if (caseOptional.isPresent()) {
                response.put("status", "SUCCESS");
                response.put("case", createDetailedCaseInfo(caseOptional.get()));
                response.put("message", "Caso obtenido exitosamente");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "ERROR");
                response.put("message", "Caso no encontrado");
                response.put("case", null);
                return ResponseEntity.status(404).body(response);
            }

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al obtener caso: " + e.getMessage());
            response.put("case", null);
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * ✅ PUT /api/legal-cases/{id} - Actualizar caso
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCase(
            @PathVariable Long id,
            @RequestBody Map<String, Object> caseData) {

        Map<String, Object> response = new HashMap<>();

        try {
            Optional<LegalCase> caseOptional = legalCaseRepository.findById(id);

            if (!caseOptional.isPresent()) {
                response.put("status", "ERROR");
                response.put("message", "Caso no encontrado");
                return ResponseEntity.status(404).body(response);
            }

            LegalCase existingCase = caseOptional.get();

            if (!existingCase.canBeEdited()) {
                response.put("status", "ERROR");
                response.put("message", "El caso no puede ser editado en su estado actual");
                return ResponseEntity.badRequest().body(response);
            }

            if (caseData.containsKey("title")) {
                existingCase.setTitle(caseData.get("title").toString());
            }

            if (caseData.containsKey("description")) {
                existingCase.setDescription(caseData.get("description").toString());
            }

            if (caseData.containsKey("justification")) {
                existingCase.setJustification(caseData.get("justification").toString());
            }

            if (caseData.containsKey("priority")) {
                existingCase.setPriority(caseData.get("priority").toString());
            }

            if (caseData.containsKey("status")) {
                String statusStr = caseData.get("status").toString();
                try {
                    CaseStatus newStatus = CaseStatus.valueOf(statusStr.toUpperCase());

                    if (!existingCase.getStatus().canTransitionTo(newStatus)) {
                        response.put("status", "ERROR");
                        response.put("message", "Transición de estado no válida");
                        return ResponseEntity.badRequest().body(response);
                    }

                    existingCase.setStatus(newStatus);

                    if (newStatus == CaseStatus.COMPLETED) {
                        existingCase.setActualCompletionDate(LocalDateTime.now());
                    }
                } catch (IllegalArgumentException e) {
                    response.put("status", "ERROR");
                    response.put("message", "Estado inválido: " + statusStr);
                    return ResponseEntity.badRequest().body(response);
                }
            }

            LegalCase updatedCase = legalCaseRepository.save(existingCase);

            response.put("status", "SUCCESS");
            response.put("message", "Caso actualizado exitosamente");
            response.put("case", createCaseInfo(updatedCase));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al actualizar caso: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * ✅ DELETE /api/legal-cases/{id} - Eliminar caso (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCase(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<LegalCase> caseOptional = legalCaseRepository.findById(id);

            if (!caseOptional.isPresent()) {
                response.put("status", "ERROR");
                response.put("message", "Caso no encontrado");
                return ResponseEntity.status(404).body(response);
            }

            LegalCase existingCase = caseOptional.get();
            existingCase.setStatus(CaseStatus.CANCELLED);
            legalCaseRepository.save(existingCase);

            response.put("status", "SUCCESS");
            response.put("message", "Caso cancelado exitosamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al eliminar caso: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    // ============================================================================
    // ENDPOINTS ESPECIALIZADOS
    // ============================================================================

    /**
     * ✅ GET /api/legal-cases/active - Obtener casos activos
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveCases() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<LegalCase> activeCases = legalCaseRepository.findActiveCases();

            List<Map<String, Object>> safeCases = new ArrayList<>();
            for (LegalCase legalCase : activeCases) {
                safeCases.add(createCaseInfo(legalCase));
            }

            response.put("status", "SUCCESS");
            response.put("cases", safeCases);
            response.put("count", safeCases.size());
            response.put("message", "Casos activos obtenidos");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error: " + e.getMessage());
            response.put("cases", new ArrayList<>());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * ✅ POST /api/legal-cases/{id}/assign-lawyers - Asignar abogados (VERSIÓN CORREGIDA)
     */
    @PostMapping("/{caseId}/assign-lawyers")
    public ResponseEntity<Map<String, Object>> assignLawyers(
            @PathVariable Long caseId,
            @RequestBody Map<String, Object> assignmentData) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.debug("=== LAWYER ASSIGNMENT DEBUG ===");
            log.debug("Case ID: {}", caseId);
            log.debug("Assignment data: {}", assignmentData);
            log.debug("Raw lawyerIds: {}", assignmentData.get("lawyerIds"));
            log.debug("LawyerIds class: {}",
                    assignmentData.get("lawyerIds") != null ? assignmentData.get("lawyerIds").getClass() : "null");

            // Validar parámetros de entrada
            if (!assignmentData.containsKey("lawyerIds") || assignmentData.get("lawyerIds") == null) {
                response.put("status", "ERROR");
                response.put("message", "Lista de abogados (lawyerIds) es requerida");
                return ResponseEntity.badRequest().body(response);
            }

            // Convertir lawyerIds a List<Long> - MANEJO ROBUSTO
            List<Long> lawyerIds = new ArrayList<>();
            Object lawyerIdsObj = assignmentData.get("lawyerIds");

            if (lawyerIdsObj instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<Object> rawList = (List<Object>) lawyerIdsObj;

                for (Object id : rawList) {
                    try {
                        if (id instanceof Integer) {
                            lawyerIds.add(((Integer) id).longValue());
                        } else if (id instanceof Long) {
                            lawyerIds.add((Long) id);
                        } else if (id instanceof String) {
                            lawyerIds.add(Long.parseLong((String) id));
                        } else {
                            throw new IllegalArgumentException("ID de abogado inválido: " + id);
                        }
                    } catch (NumberFormatException e) {
                        log.error("Error convirtiendo ID: {}", id);
                        response.put("status", "ERROR");
                        response.put("message", "ID de abogado inválido: " + id);
                        return ResponseEntity.badRequest().body(response);
                    }
                }
            } else {
                response.put("status", "ERROR");
                response.put("message", "lawyerIds debe ser una lista");
                return ResponseEntity.badRequest().body(response);
            }

            log.debug("Converted lawyerIds: {}", lawyerIds);

            if (lawyerIds.isEmpty()) {
                response.put("status", "ERROR");
                response.put("message", "Debe proporcionar al menos un abogado");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("Starting lawyer assignment - Case: {}, Lawyers: {}", caseId, lawyerIds);

            // Usar el nuevo service para asignar abogados
            LegalCase updatedCase = assignmentService.assignLawyersToCase(caseId, lawyerIds);

            // Preparar respuesta con información detallada
            Map<String, Object> caseInfo = createDetailedCaseInfo(updatedCase);

            // Agregar información de asignaciones
            List<CaseLawyerAssignment> assignments = assignmentService.getActiveCaseAssignments(caseId);
            List<Map<String, Object>> assignmentsList = new ArrayList<>();

            for (CaseLawyerAssignment assignment : assignments) {
                Map<String, Object> assignmentInfo = new HashMap<>();
                assignmentInfo.put("id", assignment.getId());
                assignmentInfo.put("lawyerId", assignment.getLawyer().getId());
                assignmentInfo.put("lawyerName", assignment.getLawyer().getFullName());
                assignmentInfo.put("role", assignment.getRole());
                assignmentInfo.put("status", assignment.getStatus().name());
                assignmentInfo.put("assignedDate", assignment.getAssignedDate().toString());
                assignmentInfo.put("estimatedHours", assignment.getEstimatedHours());
                assignmentsList.add(assignmentInfo);
            }

            response.put("status", "SUCCESS");
            response.put("message", "Abogados asignados exitosamente");
            response.put("case", caseInfo);
            response.put("assignments", assignmentsList);
            response.put("assignedCount", assignments.size());

            log.info("Lawyer assignment completed successfully - Case: {}, {} lawyers assigned",
                    caseId, assignments.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error - Case: {}, Error: {}", caseId, e.getMessage());
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            response.put("errorType", "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (IllegalStateException e) {
            log.error("State error - Case: {}, Error: {}", caseId, e.getMessage());
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            response.put("errorType", "STATE_ERROR");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Unexpected error during lawyer assignment - Case: {}", caseId, e);
            response.put("status", "ERROR");
            response.put("message", "Error interno del servidor: " + e.getMessage());
            response.put("errorType", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ✅ NUEVO: GET /api/legal-cases/{caseId}/assignments - Obtener asignaciones de un caso
     */
    @GetMapping("/{caseId}/assignments")
    public ResponseEntity<Map<String, Object>> getCaseAssignments(@PathVariable Long caseId) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<CaseLawyerAssignment> assignments = assignmentService.getCaseAssignments(caseId);

            List<Map<String, Object>> assignmentsList = new ArrayList<>();
            for (CaseLawyerAssignment assignment : assignments) {
                Map<String, Object> assignmentInfo = new HashMap<>();
                assignmentInfo.put("id", assignment.getId());
                assignmentInfo.put("lawyerId", assignment.getLawyer().getId());
                assignmentInfo.put("lawyerName", assignment.getLawyer().getFullName());
                assignmentInfo.put("lawyerEmail", assignment.getLawyer().getEmail());
                assignmentInfo.put("role", assignment.getRole());
                assignmentInfo.put("status", assignment.getStatus().name());
                assignmentInfo.put("statusDisplay", assignment.getStatus().getDisplayName());
                assignmentInfo.put("assignedDate", assignment.getAssignedDate());
                assignmentInfo.put("estimatedHours", assignment.getEstimatedHours());
                assignmentInfo.put("actualHours", assignment.getActualHours());
                assignmentsList.add(assignmentInfo);
            }

            response.put("status", "SUCCESS");
            response.put("assignments", assignmentsList);
            response.put("totalAssignments", assignmentsList.size());
            response.put("activeAssignments", assignmentsList.stream()
                    .mapToInt(a -> "ACTIVE".equals(a.get("status")) ? 1 : 0)
                    .sum());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting case assignments - Case: {}", caseId, e);
            response.put("status", "ERROR");
            response.put("message", "Error obteniendo asignaciones: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ✅ GET /api/legal-cases/stats - Estadísticas
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCaseStats() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> statistics = new HashMap<>();

            statistics.put("totalCases", legalCaseRepository.count());
            statistics.put("activeCases", legalCaseRepository.findActiveCases().size());
            statistics.put("completedCases", legalCaseRepository.countByStatus(CaseStatus.COMPLETED));
            statistics.put("closedCases", legalCaseRepository.countByStatus(CaseStatus.CLOSED));

            List<Object[]> statusStats = legalCaseRepository.getCaseCountByStatus();
            List<Map<String, Object>> casesByStatus = new ArrayList<>();
            for (Object[] stat : statusStats) {
                Map<String, Object> statusStat = new HashMap<>();
                statusStat.put("status", stat[0].toString());
                statusStat.put("count", stat[1]);
                casesByStatus.add(statusStat);
            }
            statistics.put("casesByStatus", casesByStatus);

            statistics.put("urgentCases", legalCaseRepository.findUrgentCases().size());
            statistics.put("casesWithoutPrimaryLawyer", legalCaseRepository.findCasesWithoutPrimaryLawyerAssigned().size());
            statistics.put("casesNeedingMoreLawyers", legalCaseRepository.findCasesNeedingMoreLawyers().size());

            response.put("status", "SUCCESS");
            response.put("statistics", statistics);
            response.put("message", "Estadísticas obtenidas");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error al obtener estadísticas: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * ✅ GET /api/legal-cases/urgent - Casos urgentes
     */
    @GetMapping("/urgent")
    public ResponseEntity<Map<String, Object>> getUrgentCases() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<LegalCase> urgentCases = legalCaseRepository.findUrgentCases();

            List<Map<String, Object>> safeCases = new ArrayList<>();
            for (LegalCase legalCase : urgentCases) {
                safeCases.add(createCaseInfo(legalCase));
            }

            response.put("status", "SUCCESS");
            response.put("cases", safeCases);
            response.put("count", safeCases.size());
            response.put("message", "Casos urgentes obtenidos");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error: " + e.getMessage());
            response.put("cases", new ArrayList<>());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * ✅ GET /api/legal-cases/unassigned - Casos sin equipo asignado
     */
    @GetMapping("/unassigned")
    public ResponseEntity<Map<String, Object>> getUnassignedCases() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<LegalCase> unassignedCases = legalCaseRepository.findOpenCasesWithoutTeam();

            List<Map<String, Object>> safeCases = new ArrayList<>();
            for (LegalCase legalCase : unassignedCases) {
                safeCases.add(createCaseInfo(legalCase));
            }

            response.put("status", "SUCCESS");
            response.put("cases", safeCases);
            response.put("count", safeCases.size());
            response.put("message", "Casos sin equipo obtenidos");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error: " + e.getMessage());
            response.put("cases", new ArrayList<>());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * ✅ GET /api/legal-cases/by-lawyer/{lawyerId} - Casos por abogado
     */
    @GetMapping("/by-lawyer/{lawyerId}")
    public ResponseEntity<Map<String, Object>> getCasesByLawyer(@PathVariable Long lawyerId) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<LegalCase> cases = legalCaseRepository.findCasesAssignedToLawyer(lawyerId);

            List<Map<String, Object>> safeCases = new ArrayList<>();
            for (LegalCase legalCase : cases) {
                safeCases.add(createCaseInfo(legalCase));
            }

            response.put("status", "SUCCESS");
            response.put("cases", safeCases);
            response.put("count", safeCases.size());
            response.put("message", "Casos del abogado obtenidos");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Error: " + e.getMessage());
            response.put("cases", new ArrayList<>());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ============================================================================
    // MÉTODOS AUXILIARES
    // ============================================================================

    private Map<String, Object> createCaseInfo(LegalCase legalCase) {
        Map<String, Object> caseInfo = new HashMap<>();

        caseInfo.put("id", legalCase.getId());
        caseInfo.put("caseNumber", legalCase.getCaseNumber());
        caseInfo.put("title", legalCase.getTitle());
        caseInfo.put("description", legalCase.getDescription());
        caseInfo.put("status", legalCase.getStatus().name());
        caseInfo.put("statusDisplay", legalCase.getStatus().getDisplayName());
        caseInfo.put("priority", legalCase.getPriority());
        caseInfo.put("createdAt", legalCase.getCreatedAt() != null ? legalCase.getCreatedAt().toString() : null);
        caseInfo.put("updatedAt", legalCase.getUpdatedAt() != null ? legalCase.getUpdatedAt().toString() : null);

        if (legalCase.getClient() != null) {
            Map<String, Object> clientInfo = new HashMap<>();
            clientInfo.put("id", legalCase.getClient().getId());
            clientInfo.put("name", legalCase.getClient().getName());
            clientInfo.put("email", legalCase.getClient().getEmail());
            caseInfo.put("client", clientInfo);
        }

        List<Map<String, Object>> lawyersList = new ArrayList<>();
        if (legalCase.getAssignedLawyers() != null && !legalCase.getAssignedLawyers().isEmpty()) {
            for (User lawyer : legalCase.getAssignedLawyers()) {
                Map<String, Object> lawyerInfo = new HashMap<>();
                lawyerInfo.put("id", lawyer.getId());
                lawyerInfo.put("name", lawyer.getFullName());
                lawyerInfo.put("email", lawyer.getEmail());
                lawyersList.add(lawyerInfo);
            }
        }
        caseInfo.put("assignedLawyers", lawyersList);
        caseInfo.put("lawyerCount", lawyersList.size());

        if (legalCase.getPrimaryLawyer() != null) {
            Map<String, Object> primaryInfo = new HashMap<>();
            primaryInfo.put("id", legalCase.getPrimaryLawyer().getId());
            primaryInfo.put("name", legalCase.getPrimaryLawyer().getFullName());
            primaryInfo.put("email", legalCase.getPrimaryLawyer().getEmail());
            caseInfo.put("primaryLawyer", primaryInfo);
        } else {
            caseInfo.put("primaryLawyer", null);
        }

        return caseInfo;
    }

    private Map<String, Object> createDetailedCaseInfo(LegalCase legalCase) {
        Map<String, Object> caseInfo = createCaseInfo(legalCase);

        // Agregar campos adicionales para vista detallada
        caseInfo.put("justification", legalCase.getJustification());
        caseInfo.put("caseType", legalCase.getCaseType());
        caseInfo.put("requiredSpecialty", legalCase.getRequiredSpecialty().name());
        caseInfo.put("specialtyDisplay", legalCase.getRequiredSpecialty().getDisplayName());
        caseInfo.put("country", legalCase.getCountry().name());
        caseInfo.put("countryDisplay", legalCase.getCountry().getDisplayName());
        caseInfo.put("complexity", legalCase.getComplexity().name());
        caseInfo.put("complexityDisplay", legalCase.getComplexity().getDisplayName());
        caseInfo.put("estimatedValue", legalCase.getEstimatedValue());
        caseInfo.put("teamAssigned", legalCase.getTeamAssigned());
        caseInfo.put("minimumLawyersRequired", legalCase.getMinimumLawyersRequired());
        caseInfo.put("maximumLawyersAllowed", legalCase.getMaximumLawyersAllowed());
        caseInfo.put("assignmentNotes", legalCase.getAssignmentNotes());
        caseInfo.put("documentCount", legalCase.getDocumentCount());

        // CORREGIDO: Obtener abogados desde asignaciones activas
        try {
            List<CaseLawyerAssignment> activeAssignments = assignmentService.getActiveCaseAssignments(legalCase.getId());

            List<Map<String, Object>> assignedLawyersList = new ArrayList<>();
            for (CaseLawyerAssignment assignment : activeAssignments) {
                Map<String, Object> lawyerInfo = new HashMap<>();
                lawyerInfo.put("id", assignment.getUser().getId());
                lawyerInfo.put("name", assignment.getUser().getFullName());
                lawyerInfo.put("email", assignment.getUser().getEmail());
                lawyerInfo.put("role", assignment.getRole());
                lawyerInfo.put("specialization", assignment.getUser().getSpecialization());
                assignedLawyersList.add(lawyerInfo);
            }

            // Sobrescribir los valores incorrectos
            caseInfo.put("assignedLawyers", assignedLawyersList);
            caseInfo.put("lawyerCount", assignedLawyersList.size());

        } catch (Exception e) {
            log.warn("Error obteniendo asignaciones activas para caso {}: {}", legalCase.getId(), e.getMessage());
            caseInfo.put("assignedLawyers", new ArrayList<>());
            caseInfo.put("lawyerCount", 0);
        }

        if (legalCase.getExpectedCompletionDate() != null) {
            caseInfo.put("expectedCompletionDate", legalCase.getExpectedCompletionDate().toString());
        }

        if (legalCase.getActualCompletionDate() != null) {
            caseInfo.put("actualCompletionDate", legalCase.getActualCompletionDate().toString());
        }

        return caseInfo;
    }

    private String generateCaseNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = legalCaseRepository.count() + 1;
        return String.format("NVS-%s-%04d", datePrefix, count);
    }
}