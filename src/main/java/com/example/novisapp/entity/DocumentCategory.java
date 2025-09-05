package com.example.novisapp.entity;

/**
 * Categorías de documentos legales soportados
 * Sistema de gestión documental avanzado
 */
public enum DocumentCategory {

    // Documentos legales principales
    CONTRATO("Contrato", "Contratos y acuerdos legales"),
    SENTENCIA("Sentencia", "Sentencias y resoluciones judiciales"),
    DEMANDA("Demanda", "Demandas y escritos de inicio"),
    CONTESTACION("Contestación", "Contestaciones y respuestas"),
    APELACION("Apelación", "Recursos de apelación"),
    RECURSO("Recurso", "Recursos y impugnaciones"),

    // Documentos probatorios
    EVIDENCIA("Evidencia", "Evidencias y pruebas"),
    TESTIMONIO("Testimonio", "Testimonios y declaraciones"),
    FOTOGRAFIA("Fotografía", "Fotografías y evidencia visual"),
    AUDIO("Audio", "Grabaciones de audio"),
    VIDEO("Video", "Grabaciones de video"),

    // Documentos de identidad
    CEDULA("Cédula", "Documentos de identidad"),
    PASAPORTE("Pasaporte", "Pasaportes y documentos de viaje"),
    ACTA_NACIMIENTO("Acta de Nacimiento", "Actas de nacimiento"),
    ACTA_MATRIMONIO("Acta de Matrimonio", "Actas de matrimonio"),
    ACTA_DEFUNCION("Acta de Defunción", "Actas de defunción"),

    // Documentos financieros
    FACTURA("Factura", "Facturas y comprobantes"),
    RECIBO("Recibo", "Recibos y comprobantes de pago"),
    ESTADO_CUENTA("Estado de Cuenta", "Estados de cuenta bancarios"),
    DECLARACION_RENTA("Declaración de Renta", "Declaraciones fiscales"),

    // Documentos corporativos
    ESCRITURA_PUBLICA("Escritura Pública", "Escrituras públicas"),
    ACTA_CONSTITUTIVA("Acta Constitutiva", "Actas constitutivas"),
    PODER("Poder", "Poderes y autorizaciones"),
    MINUTA("Minuta", "Minutas y actas de reunión"),

    // Documentos técnicos
    PERITAJE("Peritaje", "Peritajes técnicos"),
    AVALUO("Avalúo", "Avalúos y tasaciones"),
    INFORME_TECNICO("Informe Técnico", "Informes técnicos especializados"),

    // Correspondencia
    CARTA("Carta", "Cartas y comunicaciones"),
    EMAIL("Email", "Correos electrónicos"),
    NOTIFICACION("Notificación", "Notificaciones oficiales"),
    CITACION("Citación", "Citaciones judiciales"),

    // Otros documentos
    CERTIFICADO("Certificado", "Certificados diversos"),
    LICENCIA("Licencia", "Licencias y permisos"),
    POLIZA("Póliza", "Pólizas de seguros"),
    OTRO("Otro", "Otros documentos no categorizados");

    private final String displayName;
    private final String description;

    DocumentCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    // Método para obtener categoría por nombre de archivo
    public static DocumentCategory inferFromFileName(String fileName) {
        if (fileName == null) return OTRO;

        String lowerFileName = fileName.toLowerCase();

        // Buscar palabras clave en el nombre del archivo
        if (lowerFileName.contains("contrato")) return CONTRATO;
        if (lowerFileName.contains("sentencia")) return SENTENCIA;
        if (lowerFileName.contains("demanda")) return DEMANDA;
        if (lowerFileName.contains("cedula") || lowerFileName.contains("id")) return CEDULA;
        if (lowerFileName.contains("pasaporte")) return PASAPORTE;
        if (lowerFileName.contains("factura")) return FACTURA;
        if (lowerFileName.contains("recibo")) return RECIBO;
        if (lowerFileName.contains("poder")) return PODER;
        if (lowerFileName.contains("certificado")) return CERTIFICADO;
        if (lowerFileName.contains("evidencia") || lowerFileName.contains("prueba")) return EVIDENCIA;
        if (lowerFileName.contains("foto") || lowerFileName.contains("imagen")) return FOTOGRAFIA;
        if (lowerFileName.contains("acta")) {
            if (lowerFileName.contains("nacimiento")) return ACTA_NACIMIENTO;
            if (lowerFileName.contains("matrimonio")) return ACTA_MATRIMONIO;
            if (lowerFileName.contains("defuncion")) return ACTA_DEFUNCION;
            if (lowerFileName.contains("constitutiva")) return ACTA_CONSTITUTIVA;
        }

        return OTRO;
    }

    // Método para obtener categorías por tipo de archivo
    public static DocumentCategory inferFromFileType(String fileType) {
        if (fileType == null) return OTRO;

        String lowerFileType = fileType.toLowerCase();

        switch (lowerFileType) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
                return FOTOGRAFIA;
            case "mp3":
            case "wav":
            case "m4a":
                return AUDIO;
            case "mp4":
            case "avi":
            case "mov":
                return VIDEO;
            default:
                return OTRO;
        }
    }
}