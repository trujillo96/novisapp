package com.example.novisapp.entity;

/**
 * Estados de asignación de abogados a casos
 */
public enum AssignmentStatus {
    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    PENDING("Pendiente"),
    COMPLETED("Completado"),
    CANCELLED("Cancelado");

    private final String displayName;

    AssignmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Verificar si el estado permite nuevas acciones
     */
    public boolean isActive() {
        return this == ACTIVE || this == PENDING;
    }

    /**
     * Verificar si el estado es final (no se puede cambiar)
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * Verificar si cuenta para la carga de trabajo
     */
    public boolean countsForWorkload() {
        return this == ACTIVE || this == PENDING;
    }

    /**
     * Obtener color para UI
     */
    public String getUIColor() {
        return switch (this) {
            case ACTIVE -> "success";    // Verde
            case PENDING -> "warning";   // Amarillo
            case COMPLETED -> "info";    // Azul
            case INACTIVE -> "secondary"; // Gris
            case CANCELLED -> "danger";   // Rojo
        };
    }

    /**
     * Obtener prioridad para ordenamiento
     */
    public int getPriority() {
        return switch (this) {
            case ACTIVE -> 1;
            case PENDING -> 2;
            case INACTIVE -> 3;
            case COMPLETED -> 4;
            case CANCELLED -> 5;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}