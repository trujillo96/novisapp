package com.example.novisapp.entity;

/**
 * Enum que define las categorías de gastos
 */
public enum ExpenseCategory {

    TRAVEL("Viajes", "Gastos de transporte y hospedaje"),
    DOCUMENTS("Documentos", "Impresiones, copias, notarías"),
    COURT_FEES("Tasas Judiciales", "Pagos a tribunales y juzgados"),
    EXPERT_WITNESS("Peritos", "Honorarios de peritos y expertos"),
    RESEARCH("Investigación", "Bases de datos, consultas especializadas"),
    COMMUNICATIONS("Comunicaciones", "Teléfono, internet, mensajería"),
    OFFICE_SUPPLIES("Materiales", "Materiales de oficina para el caso"),
    EXTERNAL_COUNSEL("Abogado Externo", "Honorarios de abogados externos"),
    OTHER("Otros", "Otros gastos relacionados al caso");

    private final String displayName;
    private final String description;

    ExpenseCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresReceipt() {
        return this != COMMUNICATIONS && this != OTHER;
    }

    public boolean requiresApproval() {
        return this == TRAVEL || this == EXPERT_WITNESS || this == EXTERNAL_COUNSEL;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

