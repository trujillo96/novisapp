package com.example.novisapp.dto;

import com.example.novisapp.entity.CaseLawyerAssignment;
import com.example.novisapp.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para representar el resultado de una asignación de equipo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamAssignmentResult {

    private boolean success;
    private String message;
    private List<CaseLawyerAssignment> assignments;
    private List<User> assignedUsers;
    private LocalDateTime assignmentDate;
    private String assignedBy;
    private Long caseId;
    private Integer teamSize;

    // ========================================
    // MÉTODOS ESTÁTICOS PARA CREAR RESULTADOS
    // ========================================

    /**
     * Crear resultado exitoso con asignaciones y usuarios
     */
    public static TeamAssignmentResult success(List<CaseLawyerAssignment> assignments, List<User> users) {
        TeamAssignmentResult result = new TeamAssignmentResult();
        result.success = true;
        result.message = "Equipo asignado exitosamente";
        result.assignments = assignments != null ? assignments : new ArrayList<>();
        result.assignedUsers = users != null ? users : new ArrayList<>();
        result.assignmentDate = LocalDateTime.now();
        result.teamSize = users != null ? users.size() : 0;

        // Obtener el ID del caso de la primera asignación
        if (assignments != null && !assignments.isEmpty() && assignments.get(0).getLegalCase() != null) {
            result.caseId = assignments.get(0).getLegalCase().getId();
        }

        return result;
    }

    /**
     * Crear resultado exitoso con mensaje personalizado
     */
    public static TeamAssignmentResult success(String message, List<CaseLawyerAssignment> assignments, List<User> users) {
        TeamAssignmentResult result = success(assignments, users);
        result.message = message;
        return result;
    }

    /**
     * Crear resultado de fallo con mensaje de error
     */
    public static TeamAssignmentResult failure(String message) {
        TeamAssignmentResult result = new TeamAssignmentResult();
        result.success = false;
        result.message = message;
        result.assignments = new ArrayList<>();
        result.assignedUsers = new ArrayList<>();
        result.assignmentDate = LocalDateTime.now();
        result.teamSize = 0;
        return result;
    }

    /**
     * Crear resultado de fallo con excepción
     */
    public static TeamAssignmentResult failure(String message, Exception exception) {
        String errorMessage = message + ": " + exception.getMessage();
        return failure(errorMessage);
    }

    /**
     * Crear resultado de fallo para caso no encontrado
     */
    public static TeamAssignmentResult caseNotFound(Long caseId) {
        return failure("Caso con ID " + caseId + " no fue encontrado");
    }

    /**
     * Crear resultado de fallo para usuarios no encontrados
     */
    public static TeamAssignmentResult usersNotFound(List<Long> userIds) {
        return failure("Algunos usuarios no fueron encontrados: " + userIds);
    }

    /**
     * Crear resultado de fallo para usuarios no disponibles
     */
    public static TeamAssignmentResult usersUnavailable(List<String> unavailableUsers) {
        String names = String.join(", ", unavailableUsers);
        return failure("Los siguientes abogados no están disponibles: " + names);
    }

    // ========================================
    // MÉTODOS UTILITARIOS
    // ========================================

    /**
     * Verificar si la asignación fue exitosa
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Verificar si la asignación falló
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * Obtener el número de abogados asignados
     */
    public int getAssignedCount() {
        return assignedUsers != null ? assignedUsers.size() : 0;
    }

    /**
     * Verificar si se asignó algún abogado
     */
    public boolean hasAssignments() {
        return getAssignedCount() > 0;
    }

    /**
     * Obtener los nombres de los abogados asignados
     */
    public List<String> getAssignedLawyerNames() {
        List<String> names = new ArrayList<>();
        if (assignedUsers != null) {
            for (User user : assignedUsers) {
                names.add(user.getFullName());
            }
        }
        return names;
    }

    /**
     * Obtener los IDs de los abogados asignados
     */
    public List<Long> getAssignedLawyerIds() {
        List<Long> ids = new ArrayList<>();
        if (assignedUsers != null) {
            for (User user : assignedUsers) {
                ids.add(user.getId());
            }
        }
        return ids;
    }

    /**
     * Obtener resumen de la asignación
     */
    public String getSummary() {
        if (success) {
            return String.format("Éxito: %d abogado(s) asignado(s) al caso %d",
                    getAssignedCount(), caseId);
        } else {
            return "Error: " + message;
        }
    }

    /**
     * Verificar si hay abogado principal asignado
     */
    public boolean hasPrimaryLawyer() {
        if (assignments == null || assignments.isEmpty()) {
            return false;
        }

        return assignments.stream()
                .anyMatch(assignment -> "LEAD".equals(assignment.getRole()) ||
                        "PRIMARY".equals(assignment.getRole()));
    }

    /**
     * Obtener el abogado principal
     */
    public User getPrimaryLawyer() {
        if (assignments == null || assignments.isEmpty()) {
            return null;
        }

        return assignments.stream()
                .filter(assignment -> "LEAD".equals(assignment.getRole()) ||
                        "PRIMARY".equals(assignment.getRole()))
                .map(CaseLawyerAssignment::getLawyer)
                .findFirst()
                .orElse(assignedUsers != null && !assignedUsers.isEmpty() ? assignedUsers.get(0) : null);
    }

    /**
     * Marcar quién realizó la asignación
     */
    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    /**
     * Añadir información adicional al mensaje
     */
    public void appendToMessage(String additionalInfo) {
        if (this.message == null) {
            this.message = additionalInfo;
        } else {
            this.message += ". " + additionalInfo;
        }
    }

    /**
     * Convertir a Map para respuestas JSON
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("success", success);
        map.put("message", message);
        map.put("team_size", teamSize);
        map.put("case_id", caseId);
        map.put("assignment_date", assignmentDate);
        map.put("assigned_by", assignedBy);
        map.put("lawyer_names", getAssignedLawyerNames());
        map.put("lawyer_ids", getAssignedLawyerIds());
        return map;
    }

    @Override
    public String toString() {
        return String.format("TeamAssignmentResult{success=%s, message='%s', teamSize=%d, caseId=%d}",
                success, message, teamSize, caseId);
    }
}