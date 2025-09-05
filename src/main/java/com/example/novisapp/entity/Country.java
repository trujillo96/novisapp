package com.example.novisapp.entity;

/**
 * Enum que define los países donde opera el despacho legal
 */
public enum Country {

    // América del Norte
    MEXICO("México", "MX", "🇲🇽", "America/Mexico_City", "Peso Mexicano", "MXN"),
    USA("Estados Unidos", "US", "🇺🇸", "America/New_York", "Dólar Estadounidense", "USD"),
    CANADA("Canadá", "CA", "🇨🇦", "America/Toronto", "Dólar Canadiense", "CAD"),

    // América Central
    GUATEMALA("Guatemala", "GT", "🇬🇹", "America/Guatemala", "Quetzal", "GTQ"),
    COSTA_RICA("Costa Rica", "CR", "🇨🇷", "America/Costa_Rica", "Colón Costarricense", "CRC"),
    PANAMA("Panamá", "PA", "🇵🇦", "America/Panama", "Balboa", "PAB"),

    // América del Sur
    COLOMBIA("Colombia", "CO", "🇨🇴", "America/Bogota", "Peso Colombiano", "COP"),
    VENEZUELA("Venezuela", "VE", "🇻🇪", "America/Caracas", "Bolívar", "VES"),
    ECUADOR("Ecuador", "EC", "🇪🇨", "America/Guayaquil", "Dólar Estadounidense", "USD"),
    PERU("Perú", "PE", "🇵🇪", "America/Lima", "Sol Peruano", "PEN"),
    BOLIVIA("Bolivia", "BO", "🇧🇴", "America/La_Paz", "Boliviano", "BOB"),
    BRAZIL("Brasil", "BR", "🇧🇷", "America/Sao_Paulo", "Real Brasileño", "BRL"),
    PARAGUAY("Paraguay", "PY", "🇵🇾", "America/Asuncion", "Guaraní", "PYG"),
    URUGUAY("Uruguay", "UY", "🇺🇾", "America/Montevideo", "Peso Uruguayo", "UYU"),
    ARGENTINA("Argentina", "AR", "🇦🇷", "America/Buenos_Aires", "Peso Argentino", "ARS"),
    CHILE("Chile", "CL", "🇨🇱", "America/Santiago", "Peso Chileno", "CLP"),

    // Europa
    SPAIN("España", "ES", "🇪🇸", "Europe/Madrid", "Euro", "EUR"),
    PORTUGAL("Portugal", "PT", "🇵🇹", "Europe/Lisbon", "Euro", "EUR"),
    FRANCE("Francia", "FR", "🇫🇷", "Europe/Paris", "Euro", "EUR"),
    GERMANY("Alemania", "DE", "🇩🇪", "Europe/Berlin", "Euro", "EUR"),
    ITALY("Italia", "IT", "🇮🇹", "Europe/Rome", "Euro", "EUR"),
    UNITED_KINGDOM("Reino Unido", "GB", "🇬🇧", "Europe/London", "Libra Esterlina", "GBP"),
    NETHERLANDS("Países Bajos", "NL", "🇳🇱", "Europe/Amsterdam", "Euro", "EUR"),
    SWITZERLAND("Suiza", "CH", "🇨🇭", "Europe/Zurich", "Franco Suizo", "CHF"),

    // Asia-Pacífico (principales mercados)
    CHINA("China", "CN", "🇨🇳", "Asia/Shanghai", "Yuan Chino", "CNY"),
    JAPAN("Japón", "JP", "🇯🇵", "Asia/Tokyo", "Yen Japonés", "JPY"),
    SOUTH_KOREA("Corea del Sur", "KR", "🇰🇷", "Asia/Seoul", "Won Surcoreano", "KRW"),
    SINGAPORE("Singapur", "SG", "🇸🇬", "Asia/Singapore", "Dólar de Singapur", "SGD"),
    AUSTRALIA("Australia", "AU", "🇦🇺", "Australia/Sydney", "Dólar Australiano", "AUD");

    private final String displayName;
    private final String isoCode;
    private final String flag;
    private final String timezone;
    private final String currency;
    private final String currencyCode;

    Country(String displayName, String isoCode, String flag, String timezone, String currency, String currencyCode) {
        this.displayName = displayName;
        this.isoCode = isoCode;
        this.flag = flag;
        this.timezone = timezone;
        this.currency = currency;
        this.currencyCode = currencyCode;
    }

    // Getters
    public String getDisplayName() {
        return displayName;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public String getFlag() {
        return flag;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    // Métodos de categorización
    public Region getRegion() {
        return switch (this) {
            case MEXICO, USA, CANADA -> Region.NORTH_AMERICA;
            case GUATEMALA, COSTA_RICA, PANAMA -> Region.CENTRAL_AMERICA;
            case COLOMBIA, VENEZUELA, ECUADOR, PERU, BOLIVIA, BRAZIL,
                 PARAGUAY, URUGUAY, ARGENTINA, CHILE -> Region.SOUTH_AMERICA;
            case SPAIN, PORTUGAL, FRANCE, GERMANY, ITALY, UNITED_KINGDOM,
                 NETHERLANDS, SWITZERLAND -> Region.EUROPE;
            case CHINA, JAPAN, SOUTH_KOREA, SINGAPORE, AUSTRALIA -> Region.ASIA_PACIFIC;
        };
    }

    public LegalSystem getLegalSystem() {
        return switch (this) {
            case USA, CANADA, UNITED_KINGDOM, AUSTRALIA, SINGAPORE -> LegalSystem.COMMON_LAW;
            case MEXICO, COLOMBIA, VENEZUELA, ECUADOR, PERU, BOLIVIA, BRAZIL,
                 PARAGUAY, URUGUAY, ARGENTINA, CHILE, GUATEMALA, COSTA_RICA,
                 PANAMA, SPAIN, PORTUGAL, FRANCE, GERMANY, ITALY, NETHERLANDS -> LegalSystem.CIVIL_LAW;
            case SWITZERLAND -> LegalSystem.MIXED;
            case CHINA, JAPAN, SOUTH_KOREA -> LegalSystem.MIXED;
        };
    }

    public String getLanguage() {
        return switch (this) {
            case MEXICO, COLOMBIA, VENEZUELA, ECUADOR, PERU, BOLIVIA, PARAGUAY,
                 URUGUAY, ARGENTINA, CHILE, GUATEMALA, COSTA_RICA, PANAMA, SPAIN -> "Español";
            case USA, CANADA, UNITED_KINGDOM, AUSTRALIA, SINGAPORE -> "Inglés";
            case BRAZIL -> "Portugués";
            case FRANCE -> "Francés";
            case GERMANY, SWITZERLAND -> "Alemán";
            case ITALY -> "Italiano";
            case NETHERLANDS -> "Holandés";
            case PORTUGAL -> "Portugués";
            case CHINA -> "Chino Mandarín";
            case JAPAN -> "Japonés";
            case SOUTH_KOREA -> "Coreano";
        };
    }

    // Métodos de utilidad para operaciones legales
    public boolean isSpanishSpeaking() {
        return getLanguage().equals("Español");
    }

    public boolean isLatinAmerica() {
        Region region = getRegion();
        return region == Region.CENTRAL_AMERICA ||
                region == Region.SOUTH_AMERICA ||
                this == MEXICO;
    }

    public boolean isEuropeanUnion() {
        return this == SPAIN || this == PORTUGAL || this == FRANCE ||
                this == GERMANY || this == ITALY || this == NETHERLANDS;
    }

    public boolean requiresSpecializedKnowledge() {
        return this == CHINA || this == JAPAN || this == SOUTH_KOREA ||
                getLegalSystem() == LegalSystem.MIXED;
    }

    public int getComplexityScore() {
        return switch (this) {
            case USA, UNITED_KINGDOM, GERMANY, SWITZERLAND -> 5; // Alta complejidad
            case CANADA, FRANCE, AUSTRALIA, SINGAPORE -> 4;
            case MEXICO, SPAIN, BRAZIL, ITALY -> 3; // Complejidad media
            case COLOMBIA, ARGENTINA, CHILE, NETHERLANDS -> 2;
            default -> 1; // Complejidad baja
        };
    }

    // Métodos estáticos de utilidad
    public static Country[] getLatinAmericanCountries() {
        return new Country[]{
                MEXICO, GUATEMALA, COSTA_RICA, PANAMA, COLOMBIA, VENEZUELA,
                ECUADOR, PERU, BOLIVIA, BRAZIL, PARAGUAY, URUGUAY, ARGENTINA, CHILE
        };
    }

    public static Country[] getSpanishSpeakingCountries() {
        return new Country[]{
                MEXICO, GUATEMALA, COSTA_RICA, PANAMA, COLOMBIA, VENEZUELA,
                ECUADOR, PERU, BOLIVIA, PARAGUAY, URUGUAY, ARGENTINA, CHILE, SPAIN
        };
    }

    public static Country[] getMajorMarkets() {
        return new Country[]{
                USA, MEXICO, CANADA, BRAZIL, GERMANY, UNITED_KINGDOM,
                FRANCE, SPAIN, CHINA, JAPAN, AUSTRALIA
        };
    }

    public static Country fromString(String country) {
        if (country == null || country.trim().isEmpty()) {
            return MEXICO; // País por defecto
        }

        try {
            return Country.valueOf(country.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            // Intentar buscar por nombre de display
            for (Country c : Country.values()) {
                if (c.getDisplayName().equalsIgnoreCase(country.trim()) ||
                        c.getIsoCode().equalsIgnoreCase(country.trim())) {
                    return c;
                }
            }
            return MEXICO; // País por defecto
        }
    }

    // Enums internos
    public enum Region {
        NORTH_AMERICA("América del Norte"),
        CENTRAL_AMERICA("América Central"),
        SOUTH_AMERICA("América del Sur"),
        EUROPE("Europa"),
        ASIA_PACIFIC("Asia-Pacífico");

        private final String displayName;

        Region(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum LegalSystem {
        COMMON_LAW("Common Law"),
        CIVIL_LAW("Derecho Civil"),
        MIXED("Sistema Mixto");

        private final String displayName;

        LegalSystem(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s (%s)", flag, displayName, isoCode);
    }
}
