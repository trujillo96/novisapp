package com.example.novisapp.service;

// ✅ IMPORTS CORREGIDOS
import com.example.novisapp.dto.SimplifiedWorkloadDashboard;
import com.example.novisapp.dto.TeamAssignmentResult; // ✅ AÑADIDO
import com.example.novisapp.entity.LegalCase;
import com.example.novisapp.entity.User;
import com.example.novisapp.repository.LegalCaseRepository;
import com.example.novisapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamAssignmentService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LegalCaseRepository legalCaseRepository;

    @Autowired
    private AssignmentValidationService validationService;

    @Autowired
    private SimplifiedTeamService simplifiedTeamService;

    /**
     * Asigna un equipo óptimo a un caso legal
     */
    public Map<String, Object> assignOptimalTeam(Long caseId, String requiredSpecialization) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Obtener el caso
            Optional<LegalCase> caseOptional = legalCaseRepository.findById(caseId);
            if (!caseOptional.isPresent()) {
                result.put("success", false);
                result.put("message", "Caso no encontrado");
                return result;
            }

            LegalCase legalCase = caseOptional.get();

            // Obtener abogados disponibles
            List<User> availableLawyers = getAvailableLawyersForCase(requiredSpecialization);

            if (availableLawyers.size() < 2) {
                result.put("success", false);
                result.put("message", "No hay suficientes abogados disponibles");
                result.put("available_count", availableLawyers.size());
                return result;
            }

            // Seleccionar equipo óptimo
            List<User> optimalTeam = selectOptimalTeam(availableLawyers, legalCase);

            // Validar la asignación
            Map<String, Object> validation = validationService.validateTeamAssignment(legalCase, optimalTeam);
            if (!(Boolean) validation.get("valid")) {
                result.put("success", false);
                result.put("message", "Validación falló: " + validation.get("reason"));
                return result;
            }

            // Realizar la asignación usando SimplifiedTeamService
            List<Long> teamIds = optimalTeam.stream().map(User::getId).collect(Collectors.toList());
            // ✅ CORREGIDO: Usar TeamAssignmentResult directamente
            TeamAssignmentResult assignmentResult = simplifiedTeamService.assignTeamToCase(caseId, teamIds);

            if (assignmentResult.isSuccess()) {
                result.put("success", true);
                result.put("message", "Equipo asignado exitosamente");
                result.put("assigned_lawyers", optimalTeam);
                result.put("primary_lawyer", legalCase.getPrimaryLawyer());
                result.put("assignment_date", LocalDateTime.now());
            } else {
                result.put("success", false);
                result.put("message", assignmentResult.getMessage());
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error en asignación: " + e.getMessage());
        }

        return result;
    }

    /**
     * Obtiene abogados disponibles para un caso
     */
    public List<User> getAvailableLawyersForCase(String specialization) {
        List<User> allLawyers = userRepository.findLawyersWithLowWorkload();

        if (specialization != null && !specialization.isEmpty()) {
            return allLawyers.stream()
                    .filter(lawyer -> lawyer.getSpecialization() != null &&
                            lawyer.getSpecialization().toLowerCase().contains(specialization.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return allLawyers;
    }

    /**
     * Selecciona el equipo óptimo basado en carga de trabajo y especialización
     */
    private List<User> selectOptimalTeam(List<User> availableLawyers, LegalCase legalCase) {
        // Ordenar por carga de trabajo (menor carga primero)
        availableLawyers.sort(Comparator.comparing(
                user -> user.getCurrentWorkload() != null ? user.getCurrentWorkload() : 0
        ));

        List<User> team = new ArrayList<>();

        // Seleccionar abogado principal (menor carga de trabajo)
        User primaryLawyer = availableLawyers.get(0);
        team.add(primaryLawyer);

        // Seleccionar segundo abogado (siguiente menor carga)
        if (availableLawyers.size() > 1) {
            User secondLawyer = availableLawyers.get(1);
            team.add(secondLawyer);
        }

        // Si el caso es de alta prioridad, agregar un tercer abogado
        if ("HIGH".equals(legalCase.getPriority()) || "URGENT".equals(legalCase.getPriority())) {
            if (availableLawyers.size() > 2) {
                User thirdLawyer = availableLawyers.get(2);
                team.add(thirdLawyer);
            }
        }

        return team;
    }

    /**
     * Reasigna el abogado principal de un caso
     */
    public Map<String, Object> reassignPrimaryLawyer(Long caseId, Long newPrimaryLawyerId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<LegalCase> caseOptional = legalCaseRepository.findById(caseId);
            Optional<User> lawyerOptional = userRepository.findById(newPrimaryLawyerId);

            if (!caseOptional.isPresent()) {
                result.put("success", false);
                result.put("message", "Caso no encontrado");
                return result;
            }

            if (!lawyerOptional.isPresent()) {
                result.put("success", false);
                result.put("message", "Abogado no encontrado");
                return result;
            }

            LegalCase legalCase = caseOptional.get();
            User newPrimaryLawyer = lawyerOptional.get();

            // Verificar que el abogado esté asignado al caso
            if (!legalCase.getAssignedLawyers().contains(newPrimaryLawyer)) {
                result.put("success", false);
                result.put("message", "El abogado no está asignado a este caso");
                return result;
            }

            User previousPrimary = legalCase.getPrimaryLawyer();
            legalCase.setPrimaryLawyer(newPrimaryLawyer);
            legalCaseRepository.save(legalCase);

            result.put("success", true);
            result.put("message", "Abogado principal reasignado exitosamente");
            result.put("previous_primary", previousPrimary);
            result.put("new_primary", newPrimaryLawyer);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error en reasignación: " + e.getMessage());
        }

        return result;
    }

    /**
     * Remueve un abogado del equipo de un caso
     */
    public Map<String, Object> removeLawyerFromCase(Long caseId, Long lawyerId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<LegalCase> caseOptional = legalCaseRepository.findById(caseId);
            Optional<User> lawyerOptional = userRepository.findById(lawyerId);

            if (!caseOptional.isPresent() || !lawyerOptional.isPresent()) {
                result.put("success", false);
                result.put("message", "Caso o abogado no encontrado");
                return result;
            }

            LegalCase legalCase = caseOptional.get();
            User lawyer = lawyerOptional.get();

            // Verificar que no quede con menos de 2 abogados
            if (legalCase.getAssignedLawyers().size() <= 2) {
                result.put("success", false);
                result.put("message", "No se puede remover. Mínimo 2 abogados requeridos");
                return result;
            }

            // Remover abogado del sistema actual
            legalCase.getAssignedLawyers().remove(lawyer);

            // Si era el abogado principal, asignar otro
            if (legalCase.getPrimaryLawyer() != null && legalCase.getPrimaryLawyer().equals(lawyer)) {
                User newPrimary = legalCase.getAssignedLawyers().iterator().next();
                legalCase.setPrimaryLawyer(newPrimary);
            }

            // Actualizar carga de trabajo manualmente
            if (lawyer.getCurrentWorkload() != null && lawyer.getCurrentWorkload() > 0) {
                lawyer.setCurrentWorkload(lawyer.getCurrentWorkload() - 1);
                userRepository.save(lawyer);
            }

            legalCaseRepository.save(legalCase);

            result.put("success", true);
            result.put("message", "Abogado removido exitosamente");
            result.put("removed_lawyer", lawyer);
            result.put("remaining_team_size", legalCase.getAssignedLawyers().size());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error al remover abogado: " + e.getMessage());
        }

        return result;
    }

    /**
     * Obtiene información del equipo asignado a un caso
     */
    public Map<String, Object> getCaseTeamInfo(Long caseId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<LegalCase> caseOptional = legalCaseRepository.findById(caseId);
            if (!caseOptional.isPresent()) {
                result.put("success", false);
                result.put("message", "Caso no encontrado");
                return result;
            }

            LegalCase legalCase = caseOptional.get();

            result.put("success", true);
            result.put("case_number", legalCase.getCaseNumber());
            result.put("case_title", legalCase.getTitle());
            result.put("primary_lawyer", legalCase.getPrimaryLawyer());
            result.put("assigned_lawyers", legalCase.getAssignedLawyers());
            result.put("team_size", legalCase.getAssignedLawyers().size());
            result.put("has_minimum_lawyers", legalCase.hasMinimumLawyers());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error al obtener información: " + e.getMessage());
        }

        return result;
    }

    /**
     * MÉTODOS AUXILIARES PARA WORKLOAD (reemplazando WorkloadCalculatorService)
     */
    private void incrementWorkload(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            Integer currentWorkload = user.getCurrentWorkload() != null ? user.getCurrentWorkload() : 0;
            user.setCurrentWorkload(currentWorkload + 1);
            userRepository.save(user);
        }
    }

    private void decrementWorkload(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            Integer currentWorkload = user.getCurrentWorkload() != null ? user.getCurrentWorkload() : 0;
            if (currentWorkload > 0) {
                user.setCurrentWorkload(currentWorkload - 1);
            }
            userRepository.save(user);
        }
    }
}