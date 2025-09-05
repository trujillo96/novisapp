package com.example.novisapp.entity;

/**
 * Enum que define los estados de los gastos
 */
public enum ExpenseStatus {

    PENDING("Pendiente", "Esperando aprobación"),
    APPROVED("Aprobado", "Aprobado para reembolso"),
    REJECTED("Rechazado", "Rechazado por supervisor"),
    REIMBURSED("Reembolsado", "Ya reembolsado al empleado"),
    BILLED("Facturado", "Incluido en factura al cliente");

    private final String displayName;
    private final String description;

    ExpenseStatus(String displayName, String description) {
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
        return this == PENDING || this == REJECTED;
    }

    public boolean canApprove() {
        return this == PENDING;
    }

    public boolean canReimburse() {
        return this == APPROVED;
    }

    public boolean canBillToClient() {
        return this == APPROVED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}