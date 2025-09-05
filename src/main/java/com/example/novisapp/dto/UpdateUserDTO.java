package com.example.novisapp.dto;

import com.example.novisapp.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para actualizar usuarios existentes
 * Compatible con UpdateUserDto del frontend TypeScript
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserDTO {

    @Email(message = "Formato de email inválido")
    private String email;

    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String firstName;

    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    private String lastName;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String phoneNumber;

    private List<Long> roleIds; // IDs de roles

    private List<String> specializations;

    private Boolean isActive;

    // Campos adicionales
    private String country;

    /**
     * Convierte roleIds a UserRole enum
     */
    public UserRole getPrimaryRole() {
        if (roleIds == null || roleIds.isEmpty()) {
            return null; // No cambiar rol si no se especifica
        }

        Long roleId = roleIds.get(0);
        return switch (roleId.intValue()) {
            case 1 -> UserRole.ADMIN;
            case 2 -> UserRole.MANAGING_PARTNER;
            case 3 -> UserRole.LAWYER;
            case 4 -> UserRole.COLLABORATOR;
            default -> null;
        };
    }

    /**
     * Obtiene especialización principal como string
     */
    public String getPrimarySpecialization() {
        if (specializations == null || specializations.isEmpty()) {
            return null;
        }
        return String.join(", ", specializations);
    }
}