package com.example.novisapp.service;

// ✅ IMPORTS CORREGIDOS
import com.example.novisapp.dto.*;
import com.example.novisapp.entity.*;
import com.example.novisapp.repository.CaseLawyerAssignmentRepository;
import com.example.novisapp.repository.LegalCaseRepository;
import com.example.novisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SimplifiedTeamService {

    private final UserRepository userRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final CaseLawyerAssignmentRepository assignmentRepository;

    /**
     * Asignar equipo usando Users - VERSIÓN CORREGIDA
     */
    public TeamAssignmentResult assignTeamToCase(Long caseId, List<Long> userIds) {
        log.info("Asignando equipo al caso ID: {} con usuarios: {}", caseId, userIds);

        try {
            // Validar caso existe
            LegalCase legalCase = legalCaseRepository.findById(caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Caso no encontrado"));

            // Validación: mínimo 2 abogados
            if (userIds == null || userIds.size() < 2) {
                return TeamAssignmentResult.failure("Se requieren mínimo 2 abogados para el caso");
            }

            // Validar que los users existen
            List<User> lawyers = userRepository.findAllById(userIds);
            if (lawyers.size() != userIds.size()) {
                return TeamAssignmentResult.failure("Algunos abogados no fueron encontrados");
            }

            // Verificar disponibilidad
            List<User> unavailableLawyers = lawyers.stream()
                    .filter(user -> !user.isAvailable())
                    .collect(Collectors.toList());

            if (!unavailableLawyers.isEmpty()) {
                String unavailableNames = unavailableLawyers.stream()
                        .map(User::getFullName)
                        .collect(Collectors.joining(", "));
                return TeamAssignmentResult.failure("Abogados no disponibles: " + unavailableNames);
            }

            // Limpiar asignaciones previas
            clearPreviousAssignments(caseId);

            // Crear nuevas asignaciones
            List<CaseLawyerAssignment> assignments = createTeamAssignments(legalCase, lawyers);

            // Actualizar el caso
            updateLegalCaseWithTeam(legalCase, lawyers);

            log.info("Equipo asignado exitosamente: {} abogados para caso {}", lawyers.size(), caseId);

            return TeamAssignmentResult.success(assignments, lawyers);

        } catch (Exception e) {
            log.error("Error asignando equipo: ", e);
            return TeamAssignmentResult.failure("Error: " + e.getMessage());
        }
    }

    /**
     * Obtener dashboard de carga - VERSIÓN CORREGIDA
     */
    public SimplifiedWorkloadDashboard getWorkloadDashboard() {
        try {
            // Obtener usuarios activos
            List<User> allUsers = userRepository.findByActiveTrue();

            // Contar casos por usuario
            Map<Long, Integer> caseCountByUser = getCaseCountByUser();

            // Crear lista de workloads
            List<LawyerWorkloadSimple> userWorkloads = allUsers.stream()
                    .map(user -> {
                        int caseCount = caseCountByUser.getOrDefault(user.getId(), 0);
                        return new LawyerWorkloadSimple(
                                user.getId(),
                                user.getFullName(),
                                user.getEmail(),
                                caseCount,
                                calculateWorkloadPercentage(caseCount)
                        );
                    })
                    .sorted(Comparator.comparing(LawyerWorkloadSimple::getCaseCount).reversed())
                    .collect(Collectors.toList());

            // Crear dashboard
            SimplifiedWorkloadDashboard dashboard = new SimplifiedWorkloadDashboard();
            dashboard.setTotalLawyers(allUsers.size());
            dashboard.setLawyerWorkloads(userWorkloads);
            dashboard.setTopWorkloadLawyers(userWorkloads.stream().limit(5).collect(Collectors.toList()));

            // Promedio de casos
            double avgCases = userWorkloads.stream()
                    .mapToInt(LawyerWorkloadSimple::getCaseCount)
                    .average()
                    .orElse(0.0);
            dashboard.setAverageCasesPerLawyer(avgCases);

            return dashboard;

        } catch (Exception e) {
            log.error("Error obteniendo dashboard: ", e);
            return new SimplifiedWorkloadDashboard();
        }
    }

    /**
     * Obtener equipo de un caso - VERSIÓN CORREGIDA
     */
    public List<TeamMemberInfo> getCaseTeam(Long caseId) {
        try {
            List<CaseLawyerAssignment> assignments = assignmentRepository
                    .findByLegalCaseIdAndStatus(caseId, AssignmentStatus.ACTIVE);

            return assignments.stream()
                    .map(assignment -> {
                        User user = assignment.getLawyer(); // ✅ CORREGIDO: usar getLawyer()
                        if (user != null) {
                            return new TeamMemberInfo(
                                    assignment.getId(),
                                    user.getId(),
                                    user.getFullName(),
                                    user.getEmail(),
                                    assignment.getRole(), // Ahora es String
                                    assignment.getAssignedDate(), // ✅ CORREGIDO: usar getAssignedDate()
                                    false // Por simplicidad
                            );
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error obteniendo equipo del caso: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Validar asignación de equipo
     */
    public ValidationResult validateTeamAssignment(Long caseId, List<Long> userIds) {
        ValidationResult result = new ValidationResult();

        try {
            // Validar caso existe
            if (!legalCaseRepository.existsById(caseId)) {
                result.addError("Caso no encontrado");
                return result;
            }

            // Validar número mínimo
            if (userIds == null || userIds.size() < 2) {
                result.addError("Se requieren mínimo 2 abogados");
            }

            // Validar que usuarios existen
            List<User> users = userRepository.findAllById(userIds);
            if (users.size() != userIds.size()) {
                result.addError("Algunos usuarios no existen");
            }

            // Validar disponibilidad
            long unavailableCount = users.stream()
                    .filter(user -> !user.isAvailable())
                    .count();

            if (unavailableCount > 0) {
                result.addError(String.format("%d usuarios no están disponibles", unavailableCount));
            }

        } catch (Exception e) {
            result.addError("Error en validación: " + e.getMessage());
        }

        return result;
    }

    // ========================================
    // MÉTODOS PRIVADOS DE APOYO
    // ========================================

    private void clearPreviousAssignments(Long caseId) {
        List<CaseLawyerAssignment> existing = assignmentRepository.findByLegalCaseId(caseId);
        for (CaseLawyerAssignment assignment : existing) {
            assignment.setStatus(AssignmentStatus.INACTIVE);
            assignmentRepository.save(assignment);
        }
    }

    private List<CaseLawyerAssignment> createTeamAssignments(LegalCase legalCase, List<User> users) {
        List<CaseLawyerAssignment> assignments = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);

            CaseLawyerAssignment assignment = new CaseLawyerAssignment();
            assignment.setLegalCase(legalCase);
            assignment.setLawyer(user); // ✅ CORREGIDO: usar setLawyer()
            assignment.setRole(i == 0 ? "LEAD" : "ASSOCIATE"); // ✅ CORREGIDO: usar String
            assignment.setStatus(AssignmentStatus.ACTIVE);
            assignment.setAssignedDate(LocalDateTime.now()); // ✅ CORREGIDO: usar setAssignedDate()

            assignments.add(assignmentRepository.save(assignment));

            // Actualizar workload del usuario si tiene el campo
            if (user.getCurrentWorkload() != null) {
                user.setCurrentWorkload(user.getCurrentWorkload() + 1);
                userRepository.save(user);
            }
        }

        return assignments;
    }

    private void updateLegalCaseWithTeam(LegalCase legalCase, List<User> users) {
        // Actualizar relaciones en el caso
        legalCase.getAssignedLawyers().clear();
        legalCase.getAssignedLawyers().addAll(new HashSet<>(users));
        legalCase.setPrimaryLawyer(users.get(0));
        legalCase.setTeamAssigned(true);

        legalCaseRepository.save(legalCase);
    }

    private Map<Long, Integer> getCaseCountByUser() {
        Map<Long, Integer> caseCount = new HashMap<>();
        List<CaseLawyerAssignment> activeAssignments = assignmentRepository.findByStatus(AssignmentStatus.ACTIVE);

        for (CaseLawyerAssignment assignment : activeAssignments) {
            if (assignment.getLawyer() != null) { // ✅ CORREGIDO: usar getLawyer()
                Long userId = assignment.getLawyer().getId();
                caseCount.merge(userId, 1, Integer::sum);
            }
        }

        return caseCount;
    }

    private double calculateWorkloadPercentage(int caseCount) {
        int maxCases = 10; // Máximo asumido
        return (caseCount * 100.0) / maxCases;
    }
}