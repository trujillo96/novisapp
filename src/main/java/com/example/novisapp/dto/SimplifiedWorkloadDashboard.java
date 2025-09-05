package com.example.novisapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class SimplifiedWorkloadDashboard {
    private Integer totalLawyers = 0;
    private Double averageCasesPerLawyer = 0.0;
    private List<LawyerWorkloadSimple> lawyerWorkloads = new ArrayList<>();
    private List<LawyerWorkloadSimple> topWorkloadLawyers = new ArrayList<>();

    /**
     * Constructor con parámetros básicos
     */
    public SimplifiedWorkloadDashboard(Integer totalLawyers, Double averageCasesPerLawyer) {
        this.totalLawyers = totalLawyers;
        this.averageCasesPerLawyer = averageCasesPerLawyer;
    }

    /**
     * Añadir abogado a la lista de workload
     */
    public void addLawyerWorkload(LawyerWorkloadSimple lawyer) {
        if (this.lawyerWorkloads == null) {
            this.lawyerWorkloads = new ArrayList<>();
        }
        this.lawyerWorkloads.add(lawyer);
    }

    /**
     * Establecer top abogados (primeros 5)
     */
    public void setTopWorkloadLawyers() {
        if (this.lawyerWorkloads != null && !this.lawyerWorkloads.isEmpty()) {
            this.topWorkloadLawyers = this.lawyerWorkloads.stream()
                    .limit(5)
                    .collect(java.util.stream.Collectors.toList());
        }
    }

    /**
     * Calcular estadísticas automáticamente
     */
    public void calculateStats() {
        if (lawyerWorkloads != null && !lawyerWorkloads.isEmpty()) {
            this.totalLawyers = lawyerWorkloads.size();
            this.averageCasesPerLawyer = lawyerWorkloads.stream()
                    .mapToInt(LawyerWorkloadSimple::getCaseCount)
                    .average()
                    .orElse(0.0);
            setTopWorkloadLawyers();
        }
    }

    /**
     * Verificar si hay datos
     */
    public boolean hasData() {
        return totalLawyers != null && totalLawyers > 0;
    }

    /**
     * Obtener resumen
     */
    public String getSummary() {
        return String.format("Total: %d abogados, Promedio: %.1f casos por abogado",
                totalLawyers, averageCasesPerLawyer);
    }
}