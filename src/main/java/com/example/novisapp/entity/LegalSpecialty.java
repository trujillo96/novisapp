package com.example.novisapp.entity;

/**
 * Enum que define las especialidades legales disponibles en el sistema
 * ✅ ACTUALIZADO para coincidir con los constraints de la base de datos
 */
public enum LegalSpecialty {

    // ✅ ESPECIALIDADES QUE COINCIDEN CON EL CONSTRAINT DE BD
    CIVIL_LAW("Derecho Civil", "Contratos civiles, responsabilidad civil, daños y perjuicios", "📋"),
    CRIMINAL_LAW("Derecho Penal", "Defensa penal, compliance penal, investigaciones", "🚨"),
    CORPORATE_LAW("Derecho Corporativo", "Constitución de empresas, governance, compliance corporativo", "🏢"),
    LABOR_LAW("Derecho Laboral", "Relaciones laborales, despidos, compliance laboral", "👥"),
    TAX_LAW("Derecho Fiscal", "Planeación fiscal, auditorías, disputas tributarias", "💰"),
    IMMIGRATION_LAW("Inmigración", "Visas, residencias, naturalizaciones", "🌍"),
    FAMILY_LAW("Derecho Familiar", "Divorcios, custodia, adopciones, sucesiones", "👨‍👩‍👧‍👦"),
    INTELLECTUAL_PROPERTY("Propiedad Intelectual", "Patentes, marcas, derechos de autor", "💡"),
    ENVIRONMENTAL_LAW("Derecho Ambiental", "Regulación ambiental, permisos, sostenibilidad", "🌱"),
    INTERNATIONAL_LAW("Derecho Internacional", "Comercio internacional, tratados", "🌐"),

    // ✅ ESPECIALIDADES ADICIONALES (compatibles con sistema existente)
    MERGERS_ACQUISITIONS("Fusiones y Adquisiciones", "M&A, due diligence, reestructuraciones corporativas", "🤝"),
    SECURITIES("Mercado de Valores", "Ofertas públicas, regulación bursátil, inversiones", "📈"),
    LITIGATION("Litigios", "Representación en procesos judiciales y arbitrales", "⚖️"),
    COMMERCIAL_LITIGATION("Litigio Comercial", "Disputas comerciales, incumplimientos contractuales", "💼"),
    CONTRACTS("Contratos", "Redacción, negociación y revisión de contratos", "📋"),
    CONSTRUCTION("Derecho de la Construcción", "Contratos de obra, disputas de construcción", "🏗️"),
    REAL_ESTATE("Bienes Raíces", "Transacciones inmobiliarias, desarrollo urbano", "🏠"),
    EMPLOYMENT("Derecho del Empleo", "Contratos de trabajo, discriminación, acoso", "📝"),
    BANKING("Derecho Bancario", "Regulación financiera, operaciones bancarias", "🏦"),
    INSURANCE("Seguros", "Pólizas de seguros, reclamaciones, regulación", "🛡️"),
    ENERGY("Energía", "Proyectos energéticos, regulación del sector", "⚡"),
    BANKRUPTCY("Quiebras e Insolvencia", "Procedimientos concursales, reestructuración", "📉"),
    RESTRUCTURING("Reestructuración", "Refinanciamiento, workout, turnaround", "🔄"),
    TECHNOLOGY("Tecnología", "Contratos tecnológicos, startups, fintech", "💻"),
    DATA_PRIVACY("Protección de Datos", "GDPR, LGPD, privacidad, ciberseguridad", "🔒"),
    ARBITRATION("Arbitraje", "Arbitraje comercial internacional y nacional", "🏛️"),
    GENERAL("Práctica General", "Asesoría legal general en múltiples áreas", "⚖️");

    private final String displayName;
    private final String description;
    private final String icon;

    LegalSpecialty(String displayName, String description, String icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    // Getters
    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    // Métodos de categorización
    public SpecialtyCategory getCategory() {
        return switch (this) {
            case CORPORATE_LAW, MERGERS_ACQUISITIONS, SECURITIES -> SpecialtyCategory.CORPORATE;
            case LITIGATION, COMMERCIAL_LITIGATION, CRIMINAL_LAW, ARBITRATION, CIVIL_LAW -> SpecialtyCategory.LITIGATION;
            case CONTRACTS, CONSTRUCTION -> SpecialtyCategory.CONTRACTS;
            case REAL_ESTATE, INTELLECTUAL_PROPERTY -> SpecialtyCategory.PROPERTY;
            case LABOR_LAW, EMPLOYMENT -> SpecialtyCategory.LABOR;
            case TAX_LAW, BANKING, INSURANCE -> SpecialtyCategory.FINANCIAL;
            case FAMILY_LAW, IMMIGRATION_LAW -> SpecialtyCategory.PERSONAL;
            case ENVIRONMENTAL_LAW, ENERGY -> SpecialtyCategory.REGULATORY;
            case BANKRUPTCY, RESTRUCTURING -> SpecialtyCategory.RESTRUCTURING;
            case TECHNOLOGY, DATA_PRIVACY -> SpecialtyCategory.TECHNOLOGY;
            case INTERNATIONAL_LAW -> SpecialtyCategory.INTERNATIONAL;
            case GENERAL -> SpecialtyCategory.GENERAL;
        };
    }

    public boolean isLitigious() {
        return getCategory() == SpecialtyCategory.LITIGATION ||
                this == CRIMINAL_LAW ||
                this == COMMERCIAL_LITIGATION ||
                this == ARBITRATION ||
                this == CIVIL_LAW;
    }

    public boolean requiresSpecializedKnowledge() {
        return this != GENERAL && this != CONTRACTS;
    }

    public boolean isHighComplexity() {
        return this == MERGERS_ACQUISITIONS ||
                this == SECURITIES ||
                this == INTERNATIONAL_LAW ||
                this == RESTRUCTURING ||
                this == BANKRUPTCY;
    }

    // ✅ MÉTODO PARA VALIDAR SI ES COMPATIBLE CON BD
    public boolean isValidForDatabase() {
        return this == CIVIL_LAW ||
                this == CRIMINAL_LAW ||
                this == CORPORATE_LAW ||
                this == LABOR_LAW ||
                this == TAX_LAW ||
                this == IMMIGRATION_LAW ||
                this == FAMILY_LAW ||
                this == INTELLECTUAL_PROPERTY ||
                this == ENVIRONMENTAL_LAW ||
                this == INTERNATIONAL_LAW;
    }

    // ✅ MÉTODO PARA MAPEAR ESPECIALIDADES NO VÁLIDAS A VÁLIDAS
    public LegalSpecialty getValidForDatabase() {
        if (isValidForDatabase()) {
            return this;
        }

        // Mapear especialidades no válidas a las más cercanas válidas
        return switch (this) {
            case MERGERS_ACQUISITIONS, SECURITIES, BANKING, INSURANCE -> CORPORATE_LAW;
            case LITIGATION, COMMERCIAL_LITIGATION, ARBITRATION -> CIVIL_LAW;
            case CONTRACTS, CONSTRUCTION, REAL_ESTATE -> CIVIL_LAW;
            case EMPLOYMENT -> LABOR_LAW;
            case ENERGY -> ENVIRONMENTAL_LAW;
            case BANKRUPTCY, RESTRUCTURING -> CORPORATE_LAW;
            case TECHNOLOGY, DATA_PRIVACY -> INTELLECTUAL_PROPERTY;
            case GENERAL -> CIVIL_LAW;
            default -> CIVIL_LAW;
        };
    }

    // Especialidades complementarias
    public LegalSpecialty[] getComplementarySpecialties() {
        return switch (this) {
            case CORPORATE_LAW -> new LegalSpecialty[]{TAX_LAW, LABOR_LAW, CONTRACTS};
            case MERGERS_ACQUISITIONS -> new LegalSpecialty[]{CORPORATE_LAW, TAX_LAW, SECURITIES, LABOR_LAW};
            case REAL_ESTATE -> new LegalSpecialty[]{TAX_LAW, CONSTRUCTION, ENVIRONMENTAL_LAW};
            case TECHNOLOGY -> new LegalSpecialty[]{INTELLECTUAL_PROPERTY, DATA_PRIVACY, CONTRACTS};
            case LABOR_LAW -> new LegalSpecialty[]{EMPLOYMENT, LITIGATION};
            case TAX_LAW -> new LegalSpecialty[]{CORPORATE_LAW, BANKRUPTCY};
            case LITIGATION -> new LegalSpecialty[]{CONTRACTS, COMMERCIAL_LITIGATION};
            case CIVIL_LAW -> new LegalSpecialty[]{CONTRACTS, FAMILY_LAW, REAL_ESTATE};
            case CRIMINAL_LAW -> new LegalSpecialty[]{LITIGATION, CIVIL_LAW};
            case FAMILY_LAW -> new LegalSpecialty[]{CIVIL_LAW, REAL_ESTATE};
            case IMMIGRATION_LAW -> new LegalSpecialty[]{INTERNATIONAL_LAW, CIVIL_LAW};
            case INTELLECTUAL_PROPERTY -> new LegalSpecialty[]{TECHNOLOGY, CIVIL_LAW};
            case ENVIRONMENTAL_LAW -> new LegalSpecialty[]{CIVIL_LAW, INTERNATIONAL_LAW};
            case INTERNATIONAL_LAW -> new LegalSpecialty[]{CIVIL_LAW, CORPORATE_LAW};
            default -> new LegalSpecialty[]{GENERAL};
        };
    }

    // Métodos estáticos de utilidad
    public static LegalSpecialty[] getCorporateSpecialties() {
        return new LegalSpecialty[]{CORPORATE_LAW, MERGERS_ACQUISITIONS, SECURITIES, TAX_LAW, BANKING};
    }

    public static LegalSpecialty[] getLitigiousSpecialties() {
        return new LegalSpecialty[]{LITIGATION, COMMERCIAL_LITIGATION, CRIMINAL_LAW, ARBITRATION, CIVIL_LAW};
    }

    public static LegalSpecialty[] getHighDemandSpecialties() {
        return new LegalSpecialty[]{CORPORATE_LAW, LITIGATION, TAX_LAW, LABOR_LAW, TECHNOLOGY, DATA_PRIVACY};
    }

    // ✅ OBTENER SOLO ESPECIALIDADES VÁLIDAS PARA LA BD
    public static LegalSpecialty[] getDatabaseValidSpecialties() {
        return new LegalSpecialty[]{
                CIVIL_LAW, CRIMINAL_LAW, CORPORATE_LAW, LABOR_LAW, TAX_LAW,
                IMMIGRATION_LAW, FAMILY_LAW, INTELLECTUAL_PROPERTY,
                ENVIRONMENTAL_LAW, INTERNATIONAL_LAW
        };
    }

    public static LegalSpecialty fromString(String specialty) {
        if (specialty == null || specialty.trim().isEmpty()) {
            return CIVIL_LAW; // ✅ Cambio: valor por defecto válido para BD
        }

        try {
            LegalSpecialty result = LegalSpecialty.valueOf(specialty.toUpperCase().trim());
            // ✅ Asegurar que sea válido para BD
            return result.getValidForDatabase();
        } catch (IllegalArgumentException e) {
            return CIVIL_LAW; // ✅ Cambio: valor por defecto válido para BD
        }
    }

    // ✅ MÉTODO PARA MAPEAR TIPOS DE CASO DEL FRONTEND
    public static LegalSpecialty fromCaseType(String caseType) {
        if (caseType == null || caseType.trim().isEmpty()) {
            return CIVIL_LAW;
        }

        return switch (caseType.toUpperCase()) {
            case "CIVIL" -> CIVIL_LAW;
            case "PENAL" -> CRIMINAL_LAW;
            case "LABORAL" -> LABOR_LAW;
            case "MERCANTIL" -> CORPORATE_LAW;
            case "ADMINISTRATIVO" -> CIVIL_LAW;
            case "FAMILIAR" -> FAMILY_LAW;
            case "FISCAL" -> TAX_LAW;
            case "INMOBILIARIO" -> CIVIL_LAW;
            default -> CIVIL_LAW;
        };
    }

    // Enum interno para categorías
    public enum SpecialtyCategory {
        CORPORATE("Corporativo"),
        LITIGATION("Contencioso"),
        CONTRACTS("Contractual"),
        PROPERTY("Propiedad"),
        LABOR("Laboral"),
        FINANCIAL("Financiero"),
        PERSONAL("Personal"),
        REGULATORY("Regulatorio"),
        RESTRUCTURING("Reestructuración"),
        TECHNOLOGY("Tecnología"),
        INTERNATIONAL("Internacional"),
        GENERAL("General");

        private final String displayName;

        SpecialtyCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", displayName, name());
    }
}