// =================================================================
// BillingType.java
// =================================================================
// Ubicación: src/main/java/com/example/novisapp/entity/BillingType.java

package com.example.novisapp.entity;

/**
 * Enum que define los tipos de facturación en el sistema financiero
 */
public enum BillingType {

    HOURLY("Por Hora", "Facturación basada en tiempo trabajado"),
    FIXED("Tarifa Fija", "Precio fijo acordado por el caso"),
    HYBRID("Híbrido", "Combinación de tarifa fija y por horas"),
    CONTINGENCY("Contingencia", "Porcentaje del resultado obtenido"),
    RETAINER("Retainer", "Pago mensual fijo por servicios");

    private final String displayName;
    private final String description;

    BillingType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}