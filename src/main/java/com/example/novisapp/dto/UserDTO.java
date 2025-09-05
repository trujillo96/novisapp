package com.example.novisapp.dto;

import com.example.novisapp.entity.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para transferencia de datos de Usuario
 * Compatible con el frontend React TypeScript
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private Boolean isActive;
    private Boolean isEmailVerified;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLogin;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // Roles - Lista para compatibilidad con frontend
    private List<RoleDTO> roles;

    // Especializaciones
    private List<String> specializations;

    // Información adicional
    private String country;
    private Integer currentWorkload;

    /**
     * DTO interno para Roles
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleDTO {
        private Long id;
        private String name;
        private String displayName;
        private String description;
        private Boolean isActive;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;

        // Constructor de conveniencia para UserRole enum
        public static RoleDTO fromUserRole(UserRole userRole) {
            return RoleDTO.builder()
                    .id(Long.valueOf(userRole.ordinal() + 1))
                    .name(userRole.name())
                    .displayName(userRole.getDisplayName())
                    .description(userRole.getDescription())
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
    }
}