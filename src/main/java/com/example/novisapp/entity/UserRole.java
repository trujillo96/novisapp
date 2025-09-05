package com.example.novisapp.entity;

/**
 * Enum que define los roles de usuario en el sistema Legal Novis
 */
public enum UserRole {

    /**
     * Administrador del sistema - Acceso completo
     */
    ADMIN("Administrador", "Acceso completo al sistema", 1),

    /**
     * Socio Director - Gestión completa de casos y equipo
     */
    MANAGING_PARTNER("Socio Director", "Gestión completa de casos y equipo", 2),

    /**
     * Abogado - Trabajo en casos asignados
     */
    LAWYER("Abogado", "Trabajo en casos asignados", 3),

    /**
     * Colaborador - Soporte en casos específicos
     */
    COLLABORATOR("Colaborador", "Soporte en casos específicos", 4);

    private final String displayName;
    private final String description;
    private final int hierarchyLevel;

    UserRole(String displayName, String description, int hierarchyLevel) {
        this.displayName = displayName;
        this.description = description;
        this.hierarchyLevel = hierarchyLevel;
    }

    // ========================================
    // GETTERS BÁSICOS
    // ========================================

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getHierarchyLevel() {
        return hierarchyLevel;
    }

    // ========================================
    // MÉTODOS DE PERMISOS DE CASOS
    // ========================================

    /**
     * Verifica si este rol puede gestionar otros casos
     */
    public boolean canManageCases() {
        return this == ADMIN || this == MANAGING_PARTNER;
    }

    /**
     * Verifica si este rol puede asignar abogados
     */
    public boolean canAssignLawyers() {
        return this == ADMIN || this == MANAGING_PARTNER;
    }

    /**
     * Verifica si este rol puede crear casos
     */
    public boolean canCreateCases() {
        return this == ADMIN || this == MANAGING_PARTNER || this == LAWYER;
    }

    /**
     * Verifica si este rol puede editar casos
     */
    public boolean canEditCases() {
        return this == ADMIN || this == MANAGING_PARTNER || this == LAWYER;
    }

    /**
     * Verifica si este rol puede eliminar casos
     */
    public boolean canDeleteCases() {
        return this == ADMIN || this == MANAGING_PARTNER;
    }

    /**
     * Verifica si este rol puede cambiar el estado de casos
     */
    public boolean canChangeStatus() {
        return this == ADMIN || this == MANAGING_PARTNER || this == LAWYER;
    }

    // ========================================
    // MÉTODOS DE PERMISOS DE EQUIPOS
    // ========================================

    /**
     * Verifica si este rol puede asignar equipos a casos
     */
    public boolean canAssignTeams() {
        return this == ADMIN || this == MANAGING_PARTNER;
    }

    /**
     * Verifica si este rol puede reasignar abogados
     */
    public boolean canReassignLawyers() {
        return this == ADMIN || this == MANAGING_PARTNER;
    }

    /**
     * Verifica si este rol puede ser asignado a casos como abogado
     */
    public boolean canBeAssignedToCases() {
        return this == LAWYER || this == MANAGING_PARTNER;
    }

    /**
     * Verifica si este rol puede ser abogado principal
     */
    public boolean canBePrimaryLawyer() {
        return this == LAWYER || this == MANAGING_PARTNER;
    }

    // ========================================
    // MÉTODOS DE PERMISOS ADMINISTRATIVOS
    // ========================================

    /**
     * Verifica si este rol puede ver estadísticas avanzadas
     */
    public boolean canViewAdvancedStats() {
        return this == ADMIN || this == MANAGING_PARTNER;
    }

    /**
     * Verifica si este rol puede gestionar usuarios
     */
    public boolean canManageUsers() {
        return this == ADMIN || this == MANAGING_PARTNER;
    }

    /**
     * Verifica si este rol puede ver reportes financieros
     */
    public boolean canViewFinancialReports() {
        return this == ADMIN || this == MANAGING_PARTNER;
    }

    /**
     * Verifica si este rol puede exportar datos
     */
    public boolean canExportData() {
        return this == ADMIN || this == MANAGING_PARTNER;
    }

    /**
     * Verifica si este rol puede acceder a configuración del sistema
     */
    public boolean canAccessSystemSettings() {
        return this == ADMIN;
    }

    // ========================================
    // MÉTODOS DE JERARQUÍA
    // ========================================

    /**
     * Verifica si este rol es superior a otro
     */
    public boolean isHigherThan(UserRole otherRole) {
        return this.hierarchyLevel < otherRole.hierarchyLevel;
    }

    /**
     * Verifica si este rol es inferior a otro
     */
    public boolean isLowerThan(UserRole otherRole) {
        return this.hierarchyLevel > otherRole.hierarchyLevel;
    }

    /**
     * Verifica si este rol está al mismo nivel que otro
     */
    public boolean isEqualTo(UserRole otherRole) {
        return this.hierarchyLevel == otherRole.hierarchyLevel;
    }

    /**
     * Obtiene los roles que puede gestionar este rol
     */
    public UserRole[] getManageableRoles() {
        return switch (this) {
            case ADMIN -> UserRole.values(); // Puede gestionar todos
            case MANAGING_PARTNER -> new UserRole[]{LAWYER, COLLABORATOR};
            case LAWYER -> new UserRole[]{COLLABORATOR};
            case COLLABORATOR -> new UserRole[]{}; // No puede gestionar ninguno
        };
    }

    // ========================================
    // MÉTODOS DE IDENTIFICACIÓN RÁPIDA
    // ========================================

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isManagingPartner() {
        return this == MANAGING_PARTNER;
    }

    public boolean isLawyer() {
        return this == LAWYER;
    }

    public boolean isCollaborator() {
        return this == COLLABORATOR;
    }

    /**
     * Verifica si es un rol que puede trabajar directamente en casos
     */
    public boolean isLegalProfessional() {
        return this == LAWYER || this == MANAGING_PARTNER;
    }

    /**
     * Verifica si es un rol con permisos de gestión
     */
    public boolean isManager() {
        return this == ADMIN || this == MANAGING_PARTNER;
    }

    // ========================================
    // MÉTODOS UTILITARIOS
    // ========================================

    /**
     * Obtiene el rol por nombre (case-insensitive)
     */
    public static UserRole fromString(String roleName) {
        if (roleName == null) return null;

        for (UserRole role : UserRole.values()) {
            if (role.name().equalsIgnoreCase(roleName) ||
                    role.getDisplayName().equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        return null;
    }

    /**
     * Obtiene todos los roles como array de strings
     */
    public static String[] getAllRoleNames() {
        UserRole[] roles = UserRole.values();
        String[] names = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            names[i] = roles[i].name();
        }
        return names;
    }

    /**
     * Obtiene todos los nombres de display como array
     */
    public static String[] getAllDisplayNames() {
        UserRole[] roles = UserRole.values();
        String[] names = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            names[i] = roles[i].getDisplayName();
        }
        return names;
    }

    @Override
    public String toString() {
        return displayName;
    }
}