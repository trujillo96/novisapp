package com.example.novisapp.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LawyerWorkloadSimple {
    private Long lawyerId;
    private String fullName;
    private String email;
    private Integer caseCount;
    private Double workloadPercentage;

    /**
     * Constructor sin workloadPercentage (se calcula automáticamente)
     */
    public LawyerWorkloadSimple(Long lawyerId, String fullName, String email, Integer caseCount) {
        this.lawyerId = lawyerId;
        this.fullName = fullName;
        this.email = email;
        this.caseCount = caseCount;
        this.workloadPercentage = calculateWorkloadPercentage(caseCount);
    }

    /**
     * Calcular porcentaje de carga de trabajo
     */
    private Double calculateWorkloadPercentage(Integer caseCount) {
        if (caseCount == null) return 0.0;
        int maxCases = 10; // Máximo asumido
        return (caseCount * 100.0) / maxCases;
    }

    /**
     * Verificar si está sobrecargado
     */
    public boolean isOverloaded() {
        return workloadPercentage != null && workloadPercentage > 80.0;
    }

    /**
     * Verificar si está disponible
     */
    public boolean isAvailable() {
        return workloadPercentage != null && workloadPercentage < 80.0;
    }

    /**
     * Obtener estado de carga
     */
    public String getWorkloadStatus() {
        if (workloadPercentage == null || workloadPercentage == 0) return "Sin casos";
        if (workloadPercentage < 30) return "Baja carga";
        if (workloadPercentage < 60) return "Carga normal";
        if (workloadPercentage < 80) return "Carga alta";
        return "Sobrecargado";
    }

    /**
     * Obtener color para UI
     */
    public String getStatusColor() {
        if (workloadPercentage == null || workloadPercentage == 0) return "secondary";
        if (workloadPercentage < 30) return "success";
        if (workloadPercentage < 60) return "info";
        if (workloadPercentage < 80) return "warning";
        return "danger";
    }
}