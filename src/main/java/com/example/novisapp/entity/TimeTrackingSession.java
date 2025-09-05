// =================================================================
// TimeTrackingSession.java
// =================================================================
// Ubicación: src/main/java/com/example/novisapp/entity/TimeTrackingSession.java

package com.example.novisapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Entidad para sesiones de seguimiento de tiempo en tiempo real
 * Permite a los abogados iniciar/parar cronómetros para casos específicos
 */
@Entity
@Table(name = "time_tracking_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeTrackingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================
    // RELACIONES
    // ========================================

    /**
     * Caso legal al que pertenece esta sesión de tiempo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_case_id", nullable = false)
    private LegalCase legalCase;

    /**
     * Abogado que está registrando el tiempo
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lawyer_id", nullable = false)
    private User lawyer;

    // ========================================
    // INFORMACIÓN DE LA SESIÓN
    // ========================================

    /**
     * Fecha y hora de inicio de la sesión
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * Fecha y hora de fin de la sesión (null si está activa)
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * Descripción del trabajo realizado
     */
    @Column(name = "description", length = 2000)
    private String description;

    /**
     * Indica si la sesión está activa (cronómetro corriendo)
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // ========================================
    // INFORMACIÓN FINANCIERA
    // ========================================

    /**
     * Tarifa por hora para esta sesión
     */
    @Column(name = "hourly_rate", precision = 19, scale = 2)
    private BigDecimal hourlyRate;

    /**
     * Categoría de la tarea (Research, Drafting, Court, etc.)
     */
    @Column(name = "task_category", length = 100)
    private String taskCategory;

    // ========================================
    // CAMPOS DE AUDITORÍA
    // ========================================

    /**
     * Fecha de creación del registro
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Usuario que creó la sesión
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    // ========================================
    // MÉTODOS UTILITARIOS
    // ========================================

    /**
     * Obtiene el tiempo transcurrido en segundos
     * @return segundos transcurridos desde el inicio
     */
    public Long getElapsedSeconds() {
        LocalDateTime endingTime = endTime != null ? endTime : LocalDateTime.now();
        return Duration.between(startTime, endingTime).getSeconds();
    }

    /**
     * Obtiene el tiempo transcurrido formateado como HH:mm:ss
     * @return tiempo formateado (ej: "02:35:42")
     */
    public String getElapsedTimeFormatted() {
        long totalSeconds = getElapsedSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Calcula el monto estimado basado en el tiempo transcurrido
     * @return monto estimado según la tarifa por hora
     */
    public BigDecimal calculateEstimatedAmount() {
        if (hourlyRate == null) return BigDecimal.ZERO;

        BigDecimal hoursWorked = BigDecimal.valueOf(getElapsedSeconds())
                .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);

        return hoursWorked.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Obtiene la duración en horas como BigDecimal
     * @return duración en horas con 2 decimales
     */
    public BigDecimal getDurationInHours() {
        return BigDecimal.valueOf(getElapsedSeconds())
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }

    /**
     * Para la sesión de seguimiento de tiempo
     */
    public void stop() {
        this.endTime = LocalDateTime.now();
        this.isActive = false;
    }

    /**
     * Verifica si la sesión está actualmente corriendo
     * @return true si el cronómetro está activo
     */
    public boolean isRunning() {
        return isActive != null && isActive;
    }

    /**
     * Verifica si la sesión ha durado más de las horas especificadas
     * @param hours número de horas para comparar
     * @return true si ha excedido el tiempo
     */
    public boolean hasExceededTime(int hours) {
        return getDurationInHours().compareTo(BigDecimal.valueOf(hours)) > 0;
    }

    /**
     * Obtiene información resumida de la sesión
     * @return resumen de la sesión
     */
    public String getSummary() {
        return String.format("Sesión %s - %s (%s) - %s - %s",
                id,
                legalCase != null ? legalCase.getCaseNumber() : "N/A",
                lawyer != null ? lawyer.getFullName() : "N/A",
                getElapsedTimeFormatted(),
                isActive ? "ACTIVA" : "FINALIZADA");
    }

    /**
     * Verifica si la sesión es facturable
     * @return true si puede ser facturada
     */
    public boolean isBillable() {
        // Mínimo 6 minutos para ser facturable (regla común en legal)
        return getDurationInHours().compareTo(BigDecimal.valueOf(0.1)) >= 0;
    }

    /**
     * Obtiene el total de minutos trabajados
     * @return minutos trabajados
     */
    public long getTotalMinutes() {
        return getElapsedSeconds() / 60;
    }

    /**
     * Verifica si la sesión necesita una pausa (más de 4 horas continuas)
     * @return true si necesita pausa
     */
    public boolean needsBreak() {
        return hasExceededTime(4) && isActive;
    }

    /**
     * Obtiene el costo estimado por minuto
     * @return costo por minuto
     */
    public BigDecimal getCostPerMinute() {
        if (hourlyRate == null) return BigDecimal.ZERO;
        return hourlyRate.divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
    }

    /**
     * Verifica si la sesión es de larga duración (más de 8 horas)
     * @return true si es una sesión larga
     */
    public boolean isLongSession() {
        return hasExceededTime(8);
    }

    // ========================================
    // MÉTODOS DE VALIDACIÓN
    // ========================================

    /**
     * Valida si la sesión puede ser editada
     * @return true si puede ser editada
     */
    public boolean canEdit() {
        return isActive || endTime == null;
    }

    /**
     * Valida si la sesión puede ser eliminada
     * @return true si puede ser eliminada
     */
    public boolean canDelete() {
        return !isActive && getDurationInHours().compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Verifica si la sesión puede convertirse en entrada de tiempo
     * @return true si puede convertirse
     */
    public boolean canConvertToTimeEntry() {
        return !isActive && isBillable();
    }

    // ========================================
    // MÉTODOS PARA REPORTES
    // ========================================

    /**
     * Obtiene información detallada para reportes
     * @return información detallada
     */
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Caso: ").append(legalCase != null ? legalCase.getCaseReference() : "N/A").append("\n");
        info.append("Abogado: ").append(lawyer != null ? lawyer.getFullName() : "N/A").append("\n");
        info.append("Inicio: ").append(startTime).append("\n");
        info.append("Fin: ").append(endTime != null ? endTime : "En curso").append("\n");
        info.append("Duración: ").append(getElapsedTimeFormatted()).append("\n");
        info.append("Tarifa: ").append(hourlyRate != null ? "$" + hourlyRate : "N/A").append("/hora\n");
        info.append("Monto estimado: $").append(calculateEstimatedAmount()).append("\n");
        info.append("Estado: ").append(isActive ? "ACTIVA" : "FINALIZADA").append("\n");

        if (description != null && !description.trim().isEmpty()) {
            info.append("Descripción: ").append(description).append("\n");
        }

        return info.toString();
    }

    // ========================================
    // MÉTODO toString PERSONALIZADO
    // ========================================

    @Override
    public String toString() {
        return String.format("TimeTrackingSession{id=%d, case='%s', lawyer='%s', duration='%s', active=%s}",
                id,
                legalCase != null ? legalCase.getCaseNumber() : "N/A",
                lawyer != null ? lawyer.getFullName() : "N/A",
                getElapsedTimeFormatted(),
                isActive);
    }
}