package com.example.novisapp.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamMemberInfo {
    private Long assignmentId;
    private Long lawyerId;
    private String lawyerName;
    private String email;
    private String role; // ✅ CAMBIADO de AssignmentRole a String
    private LocalDateTime assignedAt;
    private Boolean confirmed;

    /**
     * Constructor con valores por defecto
     */
    public TeamMemberInfo(Long assignmentId, Long lawyerId, String lawyerName, String email) {
        this.assignmentId = assignmentId;
        this.lawyerId = lawyerId;
        this.lawyerName = lawyerName;
        this.email = email;
        this.role = "ASSOCIATE"; // Valor por defecto
        this.assignedAt = LocalDateTime.now();
        this.confirmed = false;
    }

    /**
     * Verificar si es abogado principal
     */
    public boolean isLead() {
        return "LEAD".equalsIgnoreCase(role) || "PRIMARY".equalsIgnoreCase(role);
    }

    /**
     * Verificar si es abogado asociado
     */
    public boolean isAssociate() {
        return "ASSOCIATE".equalsIgnoreCase(role);
    }

    /**
     * Verificar si es consultor
     */
    public boolean isConsultant() {
        return "CONSULTANT".equalsIgnoreCase(role);
    }

    /**
     * Obtener rol formateado para mostrar
     */
    public String getDisplayRole() {
        if (role == null) return "Sin asignar";

        return switch (role.toUpperCase()) {
            case "LEAD", "PRIMARY" -> "Abogado Principal";
            case "ASSOCIATE" -> "Abogado Asociado";
            case "CONSULTANT" -> "Consultor";
            case "SUPPORTING" -> "Abogado de Apoyo";
            default -> role;
        };
    }

    /**
     * Verificar si la asignación ha sido confirmada
     */
    public boolean isConfirmed() {
        return confirmed != null && confirmed;
    }

    /**
     * Marcar como confirmado
     */
    public void markAsConfirmed() {
        this.confirmed = true;
    }

    /**
     * Obtener información resumida
     */
    public String getSummary() {
        return String.format("%s (%s) - %s", lawyerName, email, getDisplayRole());
    }
}