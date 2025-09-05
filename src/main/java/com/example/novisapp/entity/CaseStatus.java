package com.example.novisapp.entity;

/**
 * Enum que define los posibles estados de un caso legal
 *
 * Estados del ciclo de vida de un caso:
 * - OPEN: Caso recién creado, esperando asignación de equipo
 * - IN_PROGRESS: Caso activo con equipo asignado trabajando
 * - ON_HOLD: Caso pausado temporalmente
 * - COMPLETED: Caso terminado exitosamente
 * - CLOSED: Caso cerrado (puede ser por finalización o archivo)
 * - CANCELLED: Caso cancelado antes de completarse
 */
public enum CaseStatus {

    /**
     * Caso abierto - Estado inicial
     * El caso ha sido creado pero aún no tiene equipo asignado o no ha comenzado el trabajo
     */
    OPEN("Abierto", "El caso está abierto y pendiente de asignación", true, false),

    /**
     * Caso en progreso - Estado activo
     * El equipo está trabajando activamente en el caso
     */
    IN_PROGRESS("En Progreso", "El equipo está trabajando activamente en el caso", true, false),

    /**
     * Caso en pausa - Estado temporal
     * El trabajo está pausado temporalmente por alguna razón
     */
    ON_HOLD("En Pausa", "El caso está temporalmente pausado", true, false),

    /**
     * Caso completado - Estado final exitoso
     * El trabajo ha sido completado satisfactoriamente
     */
    COMPLETED("Completado", "El caso ha sido completado exitosamente", false, true),

    /**
     * Caso cerrado - Estado final
     * El caso ha sido cerrado (archivado o finalizado administrativamente)
     */
    CLOSED("Cerrado", "El caso ha sido cerrado", false, true),

    /**
     * Caso cancelado - Estado final
     * El caso fue cancelado antes de completarse
     */
    CANCELLED("Cancelado", "El caso fue cancelado", false, true);

    // Propiedades del enum
    private final String displayName;
    private final String description;
    private final boolean isActive;
    private final boolean isFinal;

    /**
     * Constructor del enum
     *
     * @param displayName Nombre para mostrar en la UI
     * @param description Descripción detallada del estado
     * @param isActive Si el caso está activo (se puede trabajar en él)
     * @param isFinal Si es un estado final (no se puede cambiar fácilmente)
     */
    CaseStatus(String displayName, String description, boolean isActive, boolean isFinal) {
        this.displayName = displayName;
        this.description = description;
        this.isActive = isActive;
        this.isFinal = isFinal;
    }

    // Getters
    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isFinal() {
        return isFinal;
    }

    // Métodos utilitarios

    /**
     * Verifica si el caso puede ser editado en este estado
     */
    public boolean canBeEdited() {
        return !isFinal;
    }

    /**
     * Verifica si se pueden asignar abogados en este estado
     */
    public boolean canAssignLawyers() {
        return this == OPEN || this == IN_PROGRESS || this == ON_HOLD;
    }

    /**
     * Verifica si el caso está en un estado de trabajo activo
     */
    public boolean isWorkingStatus() {
        return this == IN_PROGRESS;
    }

    /**
     * Verifica si el caso está pendiente de trabajo
     */
    public boolean isPendingStatus() {
        return this == OPEN;
    }

    /**
     * Verifica si el caso está pausado
     */
    public boolean isPausedStatus() {
        return this == ON_HOLD;
    }

    /**
     * Verifica si el caso está en un estado terminado
     */
    public boolean isTerminatedStatus() {
        return isFinal;
    }

    /**
     * Obtiene el color recomendado para mostrar en la UI
     */
    public String getUIColor() {
        return switch (this) {
            case OPEN -> "info";           // Azul
            case IN_PROGRESS -> "success"; // Verde
            case ON_HOLD -> "warning";     // Amarillo
            case COMPLETED -> "default";   // Gris
            case CLOSED -> "default";      // Gris
            case CANCELLED -> "error";     // Rojo
        };
    }

    /**
     * Obtiene el ícono recomendado para mostrar en la UI
     */
    public String getUIIcon() {
        return switch (this) {
            case OPEN -> "schedule";
            case IN_PROGRESS -> "work";
            case ON_HOLD -> "pause";
            case COMPLETED -> "check_circle";
            case CLOSED -> "archive";
            case CANCELLED -> "cancel";
        };
    }

    /**
     * Obtiene el porcentaje de progreso estimado basado en el estado
     */
    public int getProgressPercentage() {
        return switch (this) {
            case OPEN -> 0;
            case IN_PROGRESS -> 50;
            case ON_HOLD -> 25;
            case COMPLETED -> 100;
            case CLOSED -> 100;
            case CANCELLED -> 0;
        };
    }

    /**
     * Obtiene los estados válidos a los que se puede transicionar desde este estado
     */
    public CaseStatus[] getValidTransitions() {
        return switch (this) {
            case OPEN -> new CaseStatus[]{IN_PROGRESS, ON_HOLD, CANCELLED};
            case IN_PROGRESS -> new CaseStatus[]{ON_HOLD, COMPLETED, CLOSED, CANCELLED};
            case ON_HOLD -> new CaseStatus[]{IN_PROGRESS, CLOSED, CANCELLED};
            case COMPLETED -> new CaseStatus[]{CLOSED};
            case CLOSED -> new CaseStatus[]{}; // No se puede cambiar desde cerrado
            case CANCELLED -> new CaseStatus[]{}; // No se puede cambiar desde cancelado
        };
    }

    /**
     * Verifica si es válido transicionar a otro estado
     */
    public boolean canTransitionTo(CaseStatus newStatus) {
        if (newStatus == this) return true; // Mismo estado siempre es válido

        for (CaseStatus validStatus : getValidTransitions()) {
            if (validStatus == newStatus) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtiene el siguiente estado lógico en el flujo normal
     */
    public CaseStatus getNextLogicalStatus() {
        return switch (this) {
            case OPEN -> IN_PROGRESS;
            case IN_PROGRESS -> COMPLETED;
            case ON_HOLD -> IN_PROGRESS;
            case COMPLETED -> CLOSED;
            case CLOSED -> null; // Ya es final
            case CANCELLED -> null; // Ya es final
        };
    }

    /**
     * Verifica si requiere confirmación especial para cambiar a este estado
     */
    public boolean requiresConfirmation() {
        return this == CANCELLED || this == CLOSED;
    }

    /**
     * Obtiene un mensaje de confirmación para estados que lo requieren
     */
    public String getConfirmationMessage() {
        return switch (this) {
            case CANCELLED -> "¿Está seguro de que desea cancelar este caso? Esta acción no se puede deshacer fácilmente.";
            case CLOSED -> "¿Está seguro de que desea cerrar este caso? Se archivará y será más difícil de modificar.";
            default -> null;
        };
    }

    /**
     * Método estático para obtener todos los estados activos
     */
    public static CaseStatus[] getActiveStatuses() {
        return new CaseStatus[]{OPEN, IN_PROGRESS, ON_HOLD};
    }

    /**
     * Método estático para obtener todos los estados finales
     */
    public static CaseStatus[] getFinalStatuses() {
        return new CaseStatus[]{COMPLETED, CLOSED, CANCELLED};
    }

    /**
     * Método estático para obtener estados que permiten trabajo
     */
    public static CaseStatus[] getWorkableStatuses() {
        return new CaseStatus[]{OPEN, IN_PROGRESS, ON_HOLD};
    }

    /**
     * Convierte un string a CaseStatus de manera segura
     */
    public static CaseStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return OPEN; // Estado por defecto
        }

        try {
            return CaseStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return OPEN; // Estado por defecto si no se encuentra
        }
    }

    /**
     * Override toString para logging y debugging
     */
    @Override
    public String toString() {
        return String.format("%s (%s)", name(), displayName);
    }
}