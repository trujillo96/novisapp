package com.example.novisapp.entity;

/**
 * Enum para definir la complejidad de casos legales
 */
public enum CaseComplexity {
    SIMPLE("Caso Simple (1-2 abogados)"),
    MEDIUM("Caso Medio (2-3 abogados)"),
    COMPLEX("Caso Complejo (3-4 abogados)"),
    VERY_COMPLEX("Caso Muy Complejo (4-5 abogados)");

    private final String displayName;

    CaseComplexity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRecommendedLawyers() {
        return switch (this) {
            case SIMPLE -> 2;
            case MEDIUM -> 3;
            case COMPLEX -> 4;
            case VERY_COMPLEX -> 5;
        };
    }

    public int getMinimumLawyers() {
        return switch (this) {
            case SIMPLE -> 1;
            case MEDIUM -> 2;
            case COMPLEX -> 3;
            case VERY_COMPLEX -> 4;
        };
    }

    public int getMaximumLawyers() {
        return switch (this) {
            case SIMPLE -> 2;
            case MEDIUM -> 4;
            case COMPLEX -> 6;
            case VERY_COMPLEX -> 8;
        };
    }

    public static CaseComplexity fromString(String complexity) {
        if (complexity == null || complexity.trim().isEmpty()) {
            return MEDIUM;
        }

        try {
            return CaseComplexity.valueOf(complexity.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}