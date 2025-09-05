package com.example.novisapp.dto;

import com.example.novisapp.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para crear nuevos usuarios
 * Compatible con CreateUserDto del frontend TypeScript
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserDTO {

    @NotBlank(message = "El nombre de usuario es requerido")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    private String username;

    @NotBlank(message = "El email es requerido")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es requerido")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    private String lastName;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String phoneNumber;

    @NotNull(message = "Debe seleccionar al menos un rol")
    private List<Long> roleIds; // IDs de roles (simulado desde enum)

    private List<String> specializations;

    private Boolean sendWelcomeEmail = true;

    // Campos adicionales para el sistema
    private String country = "El Salvador";

    private String tempPassword; // Para el frontend

    /**
     * Convierte roleIds a UserRole enum
     * Basado en el mapeo: 1=ADMIN, 2=MANAGING_PARTNER, 3=LAWYER, 4=COLLABORATOR
     */
    public UserRole getPrimaryRole() {
        if (roleIds == null || roleIds.isEmpty()) {
            return UserRole.COLLABORATOR; // Default
        }

        Long roleId = roleIds.get(0); // Tomar el primer rol
        return switch (roleId.intValue()) {
            case 1 -> UserRole.ADMIN;
            case 2 -> UserRole.MANAGING_PARTNER;
            case 3 -> UserRole.LAWYER;
            case 4 -> UserRole.COLLABORATOR;
            default -> UserRole.COLLABORATOR;
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