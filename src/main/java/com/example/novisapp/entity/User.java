package com.example.novisapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password; // ✅ AGREGADO PARA JWT

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(length = 200)
    private String specialization; // Especialidad del abogado

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "current_workload")
    private Integer currentWorkload = 0; // Número de casos activos asignados

    @Column(length = 20)
    private String phoneNumber;

    @Column(length = 100)
    private String country; // País donde opera (para reportes por país)

    // ✅ CAMPOS ADICIONALES PARA JWT
    @Column(name = "account_non_expired")
    private Boolean accountNonExpired = true;

    @Column(name = "account_non_locked")
    private Boolean accountNonLocked = true;

    @Column(name = "credentials_non_expired")
    private Boolean credentialsNonExpired = true;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    // Relación con casos asignados
    @ManyToMany(mappedBy = "assignedLawyers", fetch = FetchType.LAZY)
    private Set<LegalCase> assignedCases = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ MÉTODOS REQUERIDOS POR UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email; // Usamos email como username
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired != null ? accountNonExpired : true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked != null ? accountNonLocked : true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired != null ? credentialsNonExpired : true;
    }

    @Override
    public boolean isEnabled() {
        return enabled != null ? enabled : true;
    }

    // ✅ MÉTODOS UTILITARIOS EXISTENTES
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isAvailable() {
        return active && (currentWorkload == null || currentWorkload < 10);
    }

    // ✅ MÉTODOS ADICIONALES PARA JWT
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
    }

    // Agregar estos métodos a la clase User.java después de los métodos existentes:

    /**
     * Verificar si el usuario tiene un rol específico
     */
    public boolean hasRole(String roleName) {
        if (this.role == null || roleName == null) {
            return false;
        }
        // UserRole es tu enum, no Role
        return this.role.name().equalsIgnoreCase(roleName);
    }

    /**
     * Verificar si el usuario es abogado
     */
    public boolean isLawyer() {
        return hasRole("LAWYER");
    }

    /**
     * Verificar si el usuario es administrador
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Verificar si el usuario puede ser asignado a casos
     */
    public boolean canBeAssignedToCases() {
        return getActive() && (isLawyer() || isAdmin());
    }

    /**
     * Obtener la carga de trabajo actual (manejo seguro de null)
     */
    public Integer getCurrentWorkload() {
        return currentWorkload != null ? currentWorkload : 0;
    }

    /**
     * Establecer la carga de trabajo actual (manejo seguro de null)
     */
    public void setCurrentWorkload(Integer workload) {
        this.currentWorkload = workload != null ? Math.max(0, workload) : 0;
    }

    /**
     * Verificar si el usuario está activo
     */
    public Boolean getActive() {
        return active != null ? active : true;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    public boolean isLocked() {
        return failedLoginAttempts != null && failedLoginAttempts >= 5;
    }
}