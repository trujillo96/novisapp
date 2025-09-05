package com.example.novisapp.entity;

/**
 * Enum que define los estados de las entradas de tiempo
 */
public enum TimeEntryStatus {

    DRAFT("Borrador", "Entrada de tiempo en borrador"),
    SUBMITTED("Enviado", "Enviado para aprobación"),
    APPROVED("Aprobado", "Aprobado para facturación"),
    BILLED("Facturado", "Ya incluido en factura"),
    REJECTED("Rechazado", "Rechazado por supervisor");

    private final String displayName;
    private final String description;

    TimeEntryStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean canEdit() {
        return this == DRAFT || this == REJECTED;
    }

    public boolean canApprove() {
        return this == SUBMITTED;
    }

    public boolean canBill() {
        return this == APPROVED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}