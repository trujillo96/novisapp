package com.example.novisapp.service;

import com.example.novisapp.entity.LegalCase;
import com.example.novisapp.entity.User;
import com.example.novisapp.entity.UserRole;
import com.example.novisapp.repository.LegalCaseRepository;
import com.example.novisapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssignmentValidationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LegalCaseRepository legalCaseRepository;

    /**
     * Valida si un equipo puede ser asignado a un caso
     */
    public Map<String, Object> validateTeamAssignment(LegalCase legalCase, List<User> proposedTeam) {
        Map<String, Object> result = new HashMap<>();

        // Validación 1: Mínimo 2 abogados
        if (proposedTeam.size() < 2) {
            result.put("valid", false);
            result.put("reason", "Se requieren mínimo 2 abogados por caso");
            result.put("error_code", "MIN_LAWYERS");
            return result;
        }

        // Validación 2: Todos los usuarios deben ser abogados activos
        for (User lawyer : proposedTeam) {
            if (!isValidLawyer(lawyer)) {
                result.put("valid", false);
                result.put("reason", "Usuario " + lawyer.getFullName() + " no es un abogado válido");
                result.put("error_code", "INVALID_LAWYER");
                result.put("invalid_user", lawyer);
                return result;
            }
        }

        // Validación 3: Verificar disponibilidad de carga de trabajo
        for (User lawyer : proposedTeam) {
            if (!isWorkloadAcceptable(lawyer)) {
                result.put("valid", false);
                result.put("reason", "Abogado " + lawyer.getFullName() + " tiene sobrecarga de trabajo");
                result.put("error_code", "WORKLOAD_EXCEEDED");
                result.put("overloaded_lawyer", lawyer);
                result.put("current_workload", lawyer.getCurrentWorkload());
                return result;
            }
        }

        // Validación 4: No duplicar asignaciones
        if (hasDuplicateAssignments(proposedTeam)) {
            result.put("valid", false);
            result.put("reason", "Hay abogados duplicados en el equipo propuesto");
            result.put("error_code", "DUPLICATE_LAWYERS");
            return result;
        }

        // Validación 5: Verificar conflictos de especialización (opcional)
        Map<String, Object> specializationValidation = validateSpecialization(legalCase, proposedTeam);
        if (!(Boolean) specializationValidation.get("valid")) {
            return specializationValidation;
        }

        // Todas las validaciones pasaron
        result.put("valid", true);
        result.put("message", "Equipo válido para asignación");
        result.put("team_size", proposedTeam.size());
        result.put("validation_date", java.time.LocalDateTime.now());

        return result;
    }

    /**
     * Valida si un usuario es un abogado válido
     */
    private boolean isValidLawyer(User user) {
        if (user == null || !user.getActive()) {
            return false;
        }

        UserRole role = user.getRole();
        return role == UserRole.LAWYER || role == UserRole.MANAGING_PARTNER;
    }

    /**
     * Verifica si la carga de trabajo del abogado es aceptable
     */
    private boolean isWorkloadAcceptable(User lawyer) {
        Integer currentWorkload = lawyer.getCurrentWorkload();
        if (currentWorkload == null) {
            currentWorkload = 0;
        }

        // Límite de 10 casos activos por abogado
        return currentWorkload < 10;
    }

    /**
     * Verifica si hay abogados duplicados en el equipo
     */
    private boolean hasDuplicateAssignments(List<User> team) {
        return team.stream()
                .map(User::getId)
                .distinct()
                .count() != team.size();
    }

    /**
     * Valida especialización vs tipo de caso
     */
    private Map<String, Object> validateSpecialization(LegalCase legalCase, List<User> team) {
        Map<String, Object> result = new HashMap<>();

        String caseType = legalCase.getCaseType();
        if (caseType == null || caseType.isEmpty()) {
            result.put("valid", true);
            result.put("message", "No hay restricciones de especialización");
            return result;
        }

        // Verificar si al menos un abogado tiene la especialización requerida
        boolean hasSpecialist = team.stream().anyMatch(lawyer ->
                lawyer.getSpecialization() != null &&
                        lawyer.getSpecialization().toLowerCase().contains(caseType.toLowerCase())
        );

        if (!hasSpecialist) {
            result.put("valid", false);
            result.put("reason", "Ningún abogado tiene especialización en " + caseType);
            result.put("error_code", "MISSING_SPECIALIZATION");
            result.put("required_specialization", caseType);
            return result;
        }

        result.put("valid", true);
        result.put("message", "Especialización validada correctamente");
        return result;
    }

    /**
     * Valida si un abogado puede ser asignado como principal
     */
    public Map<String, Object> validatePrimaryLawyerAssignment(User lawyer, LegalCase legalCase) {
        Map<String, Object> result = new HashMap<>();

        // Validación 1: Debe ser un abogado válido
        if (!isValidLawyer(lawyer)) {
            result.put("valid", false);
            result.put("reason", "Usuario no es un abogado válido");
            result.put("error_code", "INVALID_LAWYER");
            return result;
        }

        // Validación 2: Debe estar asignado al caso
        if (!legalCase.getAssignedLawyers().contains(lawyer)) {
            result.put("valid", false);
            result.put("reason", "El abogado no está asignado a este caso");
            result.put("error_code", "NOT_ASSIGNED");
            return result;
        }

        // Validación 3: Verificar experiencia (Managing Partner preferido para casos importantes)
        if (isHighValueCase(legalCase) && lawyer.getRole() == UserRole.LAWYER) {
            result.put("valid", false);
            result.put("reason", "Casos de alto valor requieren Managing Partner como principal");
            result.put("error_code", "INSUFFICIENT_AUTHORITY");
            result.put("suggestion", "Asignar Managing Partner como abogado principal");
            return result;
        }

        result.put("valid", true);
        result.put("message", "Abogado válido para asignación principal");
        return result;
    }

    /**
     * Determina si un caso es de alto valor
     */
    private boolean isHighValueCase(LegalCase legalCase) {
        if (legalCase.getEstimatedValue() != null) {
            return legalCase.getEstimatedValue().doubleValue() > 100000; // $100K+
        }

        return "HIGH".equals(legalCase.getPriority()) || "URGENT".equals(legalCase.getPriority());
    }

    /**
     * Valida la remoción de un abogado del equipo
     */
    public Map<String, Object> validateLawyerRemoval(LegalCase legalCase, User lawyerToRemove) {
        Map<String, Object> result = new HashMap<>();

        // Validación 1: El abogado debe estar asignado
        if (!legalCase.getAssignedLawyers().contains(lawyerToRemove)) {
            result.put("valid", false);
            result.put("reason", "El abogado no está asignado a este caso");
            result.put("error_code", "NOT_ASSIGNED");
            return result;
        }

        // Validación 2: No debe quedar con menos de 2 abogados
        if (legalCase.getAssignedLawyers().size() <= 2) {
            result.put("valid", false);
            result.put("reason", "No se puede remover. Se requieren mínimo 2 abogados");
            result.put("error_code", "MIN_LAWYERS_REQUIRED");
            result.put("current_team_size", legalCase.getAssignedLawyers().size());
            return result;
        }

        // Validación 3: Si es el abogado principal, debe haber otro disponible
        if (legalCase.getPrimaryLawyer().equals(lawyerToRemove)) {
            boolean hasAlternativePrimary = legalCase.getAssignedLawyers().stream()
                    .anyMatch(lawyer -> !lawyer.equals(lawyerToRemove) &&
                            (lawyer.getRole() == UserRole.MANAGING_PARTNER || lawyer.getRole() == UserRole.LAWYER));

            if (!hasAlternativePrimary) {
                result.put("valid", false);
                result.put("reason", "No hay otro abogado disponible para ser principal");
                result.put("error_code", "NO_ALTERNATIVE_PRIMARY");
                return result;
            }
        }

        result.put("valid", true);
        result.put("message", "Remoción válida");
        result.put("is_primary_lawyer", legalCase.getPrimaryLawyer().equals(lawyerToRemove));
        return result;
    }

    /**
     * Obtiene estadísticas de validación para reportes
     */
    public Map<String, Object> getValidationStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Casos con equipos insuficientes
            List<LegalCase> casesWithInsufficientLawyers = legalCaseRepository.findCasesWithInsufficientLawyers();

            // Casos sin abogado principal
            List<LegalCase> casesWithoutPrimary = legalCaseRepository.findCasesWithoutPrimaryLawyer();

            // Abogados sobrecargados
            List<User> overloadedLawyers = userRepository.findLawyersWithLowWorkload().stream()
                    .filter(lawyer -> !isWorkloadAcceptable(lawyer))
                    .toList();

            stats.put("cases_insufficient_lawyers", casesWithInsufficientLawyers.size());
            stats.put("cases_without_primary", casesWithoutPrimary.size());
            stats.put("overloaded_lawyers_count", overloadedLawyers.size());
            stats.put("total_validation_issues",
                    casesWithInsufficientLawyers.size() + casesWithoutPrimary.size());

            stats.put("cases_with_issues", casesWithInsufficientLawyers);
            stats.put("overloaded_lawyers", overloadedLawyers);

        } catch (Exception e) {
            stats.put("error", "Error al calcular estadísticas: " + e.getMessage());
        }

        return stats;
    }
}