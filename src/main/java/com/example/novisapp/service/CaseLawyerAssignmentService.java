package com.example.novisapp.service;

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
public class CaseLawyerAssignmentService {

    private final CaseLawyerAssignmentRepository assignmentRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final UserRepository userRepository;

    /**
     * Asignar abogados a un caso
     */
    public LegalCase assignLawyersToCase(Long caseId, List<Long> lawyerIds) {
        log.info("🔄 Iniciando asignación de abogados - Caso: {}, Abogados: {}", caseId, lawyerIds);

        // 1. Validar caso
        LegalCase legalCase = legalCaseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Caso no encontrado: " + caseId));

        if (!legalCase.canAssignLawyers()) {
            throw new IllegalStateException("El caso no permite asignación de abogados en su estado actual: " + legalCase.getStatus());
        }

        // 2. Validar lista de abogados
        if (lawyerIds == null || lawyerIds.isEmpty()) {
            throw new IllegalArgumentException("La lista de abogados no puede estar vacía");
        }

        // 3. Verificar que todos los IDs sean válidos y sean abogados activos
        List<User> lawyers = userRepository.findAllById(lawyerIds);

        if (lawyers.size() != lawyerIds.size()) {
            throw new IllegalArgumentException("Algunos IDs de abogados no son válidos");
        }

        // Filtrar solo usuarios activos con rol de abogado
        List<User> validLawyers = lawyers.stream()
                .filter(User::getActive)
                .filter(user -> user.hasRole("LAWYER") || user.hasRole("ADMIN"))
                .collect(Collectors.toList());

        if (validLawyers.size() != lawyers.size()) {
            throw new IllegalArgumentException("Algunos usuarios no son abogados activos");
        }

        // 4. Desactivar asignaciones actuales
        List<CaseLawyerAssignment> currentAssignments = assignmentRepository.findByLegalCaseIdAndStatus(caseId, AssignmentStatus.ACTIVE);
        for (CaseLawyerAssignment assignment : currentAssignments) {
            assignment.deactivate();
            assignment.setEndDate(LocalDateTime.now());
            log.debug("Desactivando asignación existente: {}", assignment.getId());
        }
        assignmentRepository.saveAll(currentAssignments);

        // 5. Crear nuevas asignaciones
        List<CaseLawyerAssignment> newAssignments = new ArrayList<>();
        boolean isFirstLawyer = true;

        for (User lawyer : validLawyers) {
            CaseLawyerAssignment assignment = new CaseLawyerAssignment();
            assignment.setLegalCase(legalCase);
            assignment.setUser(lawyer);
            assignment.setStatus(AssignmentStatus.ACTIVE);
            assignment.setAssignedDate(LocalDateTime.now());
            assignment.setStartDate(LocalDateTime.now());

            // Manejo seguro de assigned_specialty
            String specialty = getValidSpecialty(legalCase);
            assignment.setAssignedSpecialty(specialty);

            assignment.setEstimatedHours(calculateEstimatedHours(legalCase, validLawyers.size()));
            assignment.setAssignmentNotes("Asignado automáticamente");

            // El primer abogado es el principal (LEAD)
            if (isFirstLawyer) {
                assignment.setRole("LEAD");
                isFirstLawyer = false;
            } else {
                assignment.setRole("ASSOCIATE");
            }

            newAssignments.add(assignment);

            // Actualizar carga de trabajo del abogado
            Integer currentWorkload = lawyer.getCurrentWorkload() != null ? lawyer.getCurrentWorkload() : 0;
            lawyer.setCurrentWorkload(currentWorkload + 1);

            log.debug("Creando nueva asignación - Abogado: {} ({}), Rol: {}, Especialidad: {}",
                    lawyer.getFullName(), lawyer.getId(), assignment.getRole(), specialty);
        }

        // 6. Guardar nuevas asignaciones
        assignmentRepository.saveAll(newAssignments);
        userRepository.saveAll(validLawyers);

        // 7. Actualizar el caso (SIN tocar assignedLawyers para evitar conflictos)
        legalCase.setTeamAssigned(true);
        legalCase.setPrimaryLawyer(validLawyers.get(0));

        // Cambiar estado a IN_PROGRESS si estaba OPEN
        if (legalCase.getStatus() == CaseStatus.OPEN) {
            legalCase.setStatus(CaseStatus.IN_PROGRESS);
        }

        // NO actualizar assignedLawyers para evitar conflictos con UNIQUE constraint
        // La relación se maneja completamente a través de CaseLawyerAssignment

        LegalCase updatedCase = legalCaseRepository.save(legalCase);

        log.info("✅ Asignación completada - Caso: {}, {} abogados asignados", caseId, validLawyers.size());

        return updatedCase;
    }

    /**
     * Método para obtener una especialidad válida
     */
    private String getValidSpecialty(LegalCase legalCase) {
        try {
            // Intentar usar la especialidad requerida del caso
            if (legalCase.getRequiredSpecialty() != null) {
                String specialty = legalCase.getRequiredSpecialty().name();

                // Verificar que sea una de las especialidades válidas
                if (isValidSpecialty(specialty)) {
                    return specialty;
                }

                log.warn("Especialidad del caso no válida: {}, usando GENERAL", specialty);
            }
        } catch (Exception e) {
            log.warn("Error obteniendo especialidad del caso: {}, usando GENERAL", e.getMessage());
        }

        // Valor por defecto seguro
        return "GENERAL";
    }

    /**
     * Validar si una especialidad es permitida por el CHECK constraint
     */
    private boolean isValidSpecialty(String specialty) {
        Set<String> validSpecialties = Set.of(
                "GENERAL", "ARBITRATION", "INTERNATIONAL", "DATA_PRIVACY",
                "TECHNOLOGY", "RESTRUCTURING", "BANKRUPTCY", "ENERGY",
                "ENVIRONMENTAL", "IMMIGRATION", "FAMILY", "INSURANCE",
                "BANKING", "TAX", "EMPLOYMENT", "LABOR", "INTELLECTUAL_PROPERTY",
                "REAL_ESTATE", "CONSTRUCTION", "CONTRACTS", "CRIMINAL",
                "COMMERCIAL_LITIGATION", "LITIGATION", "SECURITIES",
                "MERGERS_ACQUISITIONS", "CORPORATE"
        );

        return validSpecialties.contains(specialty);
    }

    /**
     * Obtener asignaciones de un caso
     */
    public List<CaseLawyerAssignment> getCaseAssignments(Long caseId) {
        return assignmentRepository.findByLegalCaseId(caseId);
    }

    /**
     * Obtener asignaciones activas de un caso
     */
    public List<CaseLawyerAssignment> getActiveCaseAssignments(Long caseId) {
        return assignmentRepository.findByLegalCaseIdAndStatus(caseId, AssignmentStatus.ACTIVE);
    }

    /**
     * Obtener casos activos de un abogado
     */
    public List<CaseLawyerAssignment> getLawyerActiveAssignments(Long lawyerId) {
        return assignmentRepository.findByUserIdAndStatus(lawyerId, AssignmentStatus.ACTIVE);
    }

    /**
     * Remover un abogado específico de un caso
     */
    public void removeLawyerFromCase(Long caseId, Long lawyerId) {
        log.info("🔄 Removiendo abogado {} del caso {}", lawyerId, caseId);

        List<CaseLawyerAssignment> assignments = assignmentRepository.findByLegalCaseIdAndStatus(caseId, AssignmentStatus.ACTIVE)
                .stream()
                .filter(assignment -> assignment.getUser().getId().equals(lawyerId))
                .collect(Collectors.toList());

        for (CaseLawyerAssignment assignment : assignments) {
            assignment.deactivate();
            assignment.setEndDate(LocalDateTime.now());

            // Actualizar carga de trabajo
            User lawyer = assignment.getUser();
            Integer currentWorkload = lawyer.getCurrentWorkload() != null ? lawyer.getCurrentWorkload() : 0;
            lawyer.setCurrentWorkload(Math.max(0, currentWorkload - 1));
            userRepository.save(lawyer);
        }

        assignmentRepository.saveAll(assignments);
        log.info("✅ Abogado removido del caso");
    }

    /**
     * Obtener estadísticas de asignaciones
     */
    public Map<String, Object> getAssignmentStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalAssignments", assignmentRepository.count());
        stats.put("activeAssignments", assignmentRepository.findByStatus(AssignmentStatus.ACTIVE).size());
        stats.put("unassignedCases", assignmentRepository.countUnassignedCases(AssignmentStatus.ACTIVE));

        // Estadísticas por rol
        List<Object[]> roleStats = assignmentRepository.countAssignmentsByRole(AssignmentStatus.ACTIVE);
        Map<String, Long> roleStatsMap = new HashMap<>();
        for (Object[] stat : roleStats) {
            roleStatsMap.put((String) stat[0], (Long) stat[1]);
        }
        stats.put("assignmentsByRole", roleStatsMap);

        // Carga de trabajo
        stats.put("averageLawyersPerCase", assignmentRepository.getAverageLawyersPerCase(AssignmentStatus.ACTIVE));
        stats.put("todayAssignments", assignmentRepository.countTodayAssignments(AssignmentStatus.ACTIVE));

        return stats;
    }

    /**
     * Calcular horas estimadas basado en complejidad del caso
     */
    private Integer calculateEstimatedHours(LegalCase legalCase, int teamSize) {
        // Base de horas según complejidad
        int baseHours = switch (legalCase.getComplexity()) {
            case SIMPLE -> 20;
            case MEDIUM -> 40;
            case COMPLEX -> 80;
            case VERY_COMPLEX -> 120;
        };

        // Dividir entre el tamaño del equipo
        return Math.max(10, baseHours / teamSize);
    }

    /**
     * Validar si un caso puede tener más abogados asignados
     */
    public boolean canAssignMoreLawyers(Long caseId) {
        Optional<LegalCase> caseOpt = legalCaseRepository.findById(caseId);
        if (caseOpt.isEmpty()) return false;

        LegalCase legalCase = caseOpt.get();
        int currentAssignments = assignmentRepository.countByLegalCaseIdAndStatus(caseId, AssignmentStatus.ACTIVE);

        return currentAssignments < legalCase.getMaximumLawyersAllowed() && legalCase.canAssignLawyers();
    }

    /**
     * Verificar si un abogado ya está asignado a un caso
     */
    public boolean isLawyerAssignedToCase(Long caseId, Long lawyerId) {
        return assignmentRepository.existsByLegalCaseIdAndUserIdAndStatus(caseId, lawyerId, AssignmentStatus.ACTIVE);
    }
}