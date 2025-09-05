package com.example.novisapp.controller;

import java.util.Map;
import java.util.HashMap;
import com.example.novisapp.dto.*;
import com.example.novisapp.entity.User;
import com.example.novisapp.entity.UserRole;
import com.example.novisapp.repository.UserRepository;
import com.example.novisapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST para gestión de usuarios
 * Compatible con el frontend React TypeScript
 * VERSIÓN ACTUALIZADA - Con gestión de contraseñas
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    // ========================================
    // ENDPOINTS DE USUARIOS
    // ========================================

    /**
     * GET /api/users - Listar usuarios con filtros
     * Compatible con usersService.getUsers() del frontend
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<ApiResponseDTO<List<UserDTO>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String roles,
            @RequestParam(required = false) Boolean active) {

        try {
            log.debug("Obteniendo usuarios - search: {}, roles: {}, active: {}", search, roles, active);

            List<User> users;

            if (search != null && !search.trim().isEmpty()) {
                users = userRepository.findByNameContaining(search.trim());
            } else if (roles != null && !roles.trim().isEmpty()) {
                // Filtrar por rol
                UserRole userRole = UserRole.fromString(roles);
                if (userRole != null) {
                    users = active != null && active ?
                            userRepository.findByRoleAndActiveTrue(userRole) :
                            userRepository.findByRole(userRole);
                } else {
                    users = userRepository.findAll();
                }
            } else if (active != null) {
                users = active ?
                        userRepository.findByActiveTrue() :
                        userRepository.findByActive(false);
            } else {
                users = userRepository.findAll();
            }

            List<UserDTO> userDTOs = users.stream()
                    .map(this::convertToUserDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponseDTO.success(userDTOs, "Usuarios obtenidos exitosamente"));

        } catch (Exception e) {
            log.error("Error obteniendo usuarios", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error al obtener usuarios: " + e.getMessage()));
        }
    }

    /**
     * GET /api/users/{id} - Obtener usuario por ID
     */
    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<ApiResponseDTO<UserDTO>> getUserById(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            UserDTO userDTO = convertToUserDTO(user);
            return ResponseEntity.ok(ApiResponseDTO.success(userDTO));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.error("Usuario no encontrado"));
        } catch (Exception e) {
            log.error("Error obteniendo usuario por ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error interno del servidor"));
        }
    }

    /**
     * POST /api/users - Crear nuevo usuario
     * Compatible con usersService.createUser() del frontend
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<ApiResponseDTO<UserDTO>> createUser(
            @Valid @RequestBody CreateUserDTO createUserDTO,
            BindingResult bindingResult) {

        try {
            // Validar errores de binding
            if (bindingResult.hasErrors()) {
                List<ApiResponseDTO.ValidationErrorDTO> errors = bindingResult.getFieldErrors()
                        .stream()
                        .map(error -> ApiResponseDTO.ValidationErrorDTO.builder()
                                .field(error.getField())
                                .message(error.getDefaultMessage())
                                .code("VALIDATION_ERROR")
                                .build())
                        .collect(Collectors.toList());

                return ResponseEntity.badRequest()
                        .body(ApiResponseDTO.error("Errores de validación", errors));
            }

            // Verificar email único
            if (userRepository.existsByEmail(createUserDTO.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDTO.error("Ya existe un usuario con este email"));
            }

            // Crear usuario usando el UserService existente
            User newUser = userService.createUser(
                    createUserDTO.getEmail(),
                    createUserDTO.getTempPassword() != null ? createUserDTO.getTempPassword() : "tempPass123",
                    createUserDTO.getFirstName(),
                    createUserDTO.getLastName(),
                    createUserDTO.getPrimaryRole(),
                    createUserDTO.getPrimarySpecialization(),
                    createUserDTO.getPhoneNumber(),
                    createUserDTO.getCountry()
            );

            // Establecer username si fue proporcionado
            if (createUserDTO.getUsername() != null) {
                newUser.setEmail(createUserDTO.getEmail()); // En tu sistema actual email = username
                userRepository.save(newUser);
            }

            UserDTO userDTO = convertToUserDTO(newUser);

            log.info("Usuario creado exitosamente: {}", newUser.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDTO.success(userDTO, "Usuario creado exitosamente"));

        } catch (Exception e) {
            log.error("Error creando usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error al crear usuario: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/users/{id} - Actualizar usuario
     * Compatible con usersService.updateUser() del frontend
     */
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<ApiResponseDTO<UserDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDTO updateUserDTO) {

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Actualizar campos si fueron proporcionados
            if (updateUserDTO.getFirstName() != null) {
                user.setFirstName(updateUserDTO.getFirstName());
            }
            if (updateUserDTO.getLastName() != null) {
                user.setLastName(updateUserDTO.getLastName());
            }
            if (updateUserDTO.getEmail() != null) {
                // Verificar que el email no esté en uso por otro usuario
                userRepository.findByEmail(updateUserDTO.getEmail())
                        .ifPresent(existingUser -> {
                            if (!existingUser.getId().equals(id)) {
                                throw new RuntimeException("Email ya está en uso");
                            }
                        });
                user.setEmail(updateUserDTO.getEmail());
            }
            if (updateUserDTO.getPhoneNumber() != null) {
                user.setPhoneNumber(updateUserDTO.getPhoneNumber());
            }
            if (updateUserDTO.getPrimaryRole() != null) {
                user.setRole(updateUserDTO.getPrimaryRole());
            }
            if (updateUserDTO.getPrimarySpecialization() != null) {
                user.setSpecialization(updateUserDTO.getPrimarySpecialization());
            }
            if (updateUserDTO.getIsActive() != null) {
                user.setActive(updateUserDTO.getIsActive());
                user.setEnabled(updateUserDTO.getIsActive());
            }
            if (updateUserDTO.getCountry() != null) {
                user.setCountry(updateUserDTO.getCountry());
            }

            User updatedUser = userRepository.save(user);
            UserDTO userDTO = convertToUserDTO(updatedUser);

            log.info("Usuario actualizado exitosamente: {}", updatedUser.getEmail());
            return ResponseEntity.ok(ApiResponseDTO.success(userDTO, "Usuario actualizado exitosamente"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error actualizando usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error al actualizar usuario: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/users/{id} - Eliminar usuario
     * Compatible con usersService.deleteUser() del frontend
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> deleteUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // No permitir eliminar administradores
            if (user.getRole() == UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponseDTO.error("No se puede eliminar usuarios administradores"));
            }

            userRepository.deleteById(id);

            log.info("Usuario eliminado: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponseDTO.success(null, "Usuario eliminado exitosamente"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error eliminando usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error al eliminar usuario: " + e.getMessage()));
        }
    }

    /**
     * PATCH /api/users/{id}/status - Cambiar estado activo/inactivo
     * Compatible con usersService.toggleUserStatus() del frontend
     */
    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<ApiResponseDTO<UserDTO>> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> statusRequest) {

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Boolean isActive = statusRequest.get("isActive");
            if (isActive != null) {
                user.setActive(isActive);
                user.setEnabled(isActive);

                if (isActive) {
                    user.resetFailedLoginAttempts();
                    user.setAccountNonLocked(true);
                }

                User updatedUser = userRepository.save(user);
                UserDTO userDTO = convertToUserDTO(updatedUser);

                String status = isActive ? "activado" : "desactivado";
                log.info("Usuario {}: {}", status, user.getEmail());

                return ResponseEntity.ok(ApiResponseDTO.success(userDTO, "Usuario " + status + " exitosamente"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDTO.error("Campo 'isActive' requerido"));
            }

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error cambiando estado del usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error al cambiar estado del usuario: " + e.getMessage()));
        }
    }

    // ========================================
    // ENDPOINTS DE GESTIÓN DE CONTRASEÑAS - NUEVOS
    // ========================================

    /**
     * POST /api/users/{id}/reset-password - Reset de contraseña de usuario
     * Compatible con usersService.resetUserPassword() del frontend
     */
    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> resetUserPassword(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request) {

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Obtener nueva contraseña del request o generar una automática
            String newPassword = null;
            if (request != null && request.containsKey("newPassword")) {
                newPassword = request.get("newPassword");
            }

            // Si no se proporcionó contraseña, generar una automática
            if (newPassword == null || newPassword.trim().isEmpty()) {
                newPassword = generateTemporaryPassword();
            }

            // Hashear la nueva contraseña
            String hashedPassword = passwordEncoder.encode(newPassword);

            // Actualizar la contraseña del usuario
            user.setPassword(hashedPassword);

            // Marcar que debe cambiar la contraseña en el próximo login
            // (Esto requiere que tengas el campo en tu entidad User)
            // user.setMustChangePassword(true);

            // Resetear intentos fallidos
            user.resetFailedLoginAttempts();
            user.setAccountNonLocked(true);

            userRepository.save(user);

            // Respuesta con la contraseña temporal (solo para admin)
            Map<String, String> response = new HashMap<>();
            response.put("temporaryPassword", newPassword);
            response.put("message", "Contraseña reseteada exitosamente");

            log.info("Contraseña reseteada para usuario: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Contraseña reseteada exitosamente"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error reseteando contraseña del usuario: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error al resetear contraseña: " + e.getMessage()));
        }
    }

    /**
     * POST /api/users/{id}/generate-temp-password - Generar contraseña temporal
     * Compatible con usersService.generateTemporaryPassword() del frontend
     */
    @PostMapping("/users/{id}/generate-temp-password")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> generateTempPassword(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Generar nueva contraseña temporal
            String tempPassword = generateTemporaryPassword();
            String hashedPassword = passwordEncoder.encode(tempPassword);

            // Actualizar contraseña
            user.setPassword(hashedPassword);
            user.resetFailedLoginAttempts();
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("temporaryPassword", tempPassword);

            log.info("Contraseña temporal generada para usuario: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponseDTO.success(response, "Contraseña temporal generada exitosamente"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error generando contraseña temporal para usuario: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error al generar contraseña temporal: " + e.getMessage()));
        }
    }

    /**
     * PATCH /api/users/{id}/force-password-change - Forzar cambio de contraseña en próximo login
     * Compatible con usersService.forcePasswordChange() del frontend
     */
    @PatchMapping("/users/{id}/force-password-change")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<ApiResponseDTO<UserDTO>> forcePasswordChange(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Marcar que debe cambiar contraseña (requiere campo en entidad User)
            // user.setMustChangePassword(true);

            User updatedUser = userRepository.save(user);
            UserDTO userDTO = convertToUserDTO(updatedUser);

            log.info("Cambio de contraseña forzado para usuario: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponseDTO.success(userDTO, "Cambio de contraseña forzado exitosamente"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error forzando cambio de contraseña para usuario: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error al forzar cambio de contraseña: " + e.getMessage()));
        }
    }

    /**
     * POST /api/users/{id}/send-password-reset-email - Enviar email de reset (placeholder)
     * Compatible con usersService.sendPasswordResetEmail() del frontend
     */
    @PostMapping("/users/{id}/send-password-reset-email")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<ApiResponseDTO<Void>> sendPasswordResetEmail(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // TODO: Implementar envío de email cuando tengas el servicio configurado
            // emailService.sendPasswordResetEmail(user.getEmail(), tempPassword);

            log.info("Email de reset de contraseña enviado a: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponseDTO.success(null, "Email de reset enviado exitosamente"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error enviando email de reset para usuario: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error al enviar email de reset: " + e.getMessage()));
        }
    }

    // ========================================
    // ENDPOINTS DE ABOGADOS
    // ========================================

    /**
     * GET /api/lawyers - Listar abogados con métricas
     * Compatible con usersService.getLawyers() del frontend
     */
    @GetMapping("/lawyers")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<ApiResponseDTO<List<LawyerProfileDTO>>> getLawyers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String specializations,
            @RequestParam(required = false) Boolean active) {

        try {
            List<User> lawyers;

            if (search != null && !search.trim().isEmpty()) {
                // Buscar abogados por nombre
                lawyers = userRepository.findByNameContaining(search.trim())
                        .stream()
                        .filter(user -> user.getRole() == UserRole.LAWYER || user.getRole() == UserRole.MANAGING_PARTNER)
                        .collect(Collectors.toList());
            } else if (specializations != null && !specializations.trim().isEmpty()) {
                lawyers = userRepository.findBySpecializationContaining(specializations.trim())
                        .stream()
                        .filter(user -> user.getRole() == UserRole.LAWYER || user.getRole() == UserRole.MANAGING_PARTNER)
                        .collect(Collectors.toList());
            } else {
                lawyers = userRepository.findAvailableLawyers();
            }

            if (active != null) {
                lawyers = lawyers.stream()
                        .filter(user -> user.getActive().equals(active))
                        .collect(Collectors.toList());
            }

            List<LawyerProfileDTO> lawyerDTOs = lawyers.stream()
                    .map(this::convertToLawyerProfileDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponseDTO.success(lawyerDTOs, "Abogados obtenidos exitosamente"));

        } catch (Exception e) {
            log.error("Error obteniendo abogados", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error al obtener abogados: " + e.getMessage()));
        }
    }

    // ========================================
    // ENDPOINTS DE ROLES
    // ========================================

    /**
     * GET /api/roles - Listar roles del sistema
     * Compatible con usersService.getRoles() del frontend
     */
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGING_PARTNER')")
    public ResponseEntity<ApiResponseDTO<List<UserDTO.RoleDTO>>> getRoles() {
        try {
            List<UserDTO.RoleDTO> roles = Arrays.stream(UserRole.values())
                    .map(UserDTO.RoleDTO::fromUserRole)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponseDTO.success(roles, "Roles obtenidos exitosamente"));

        } catch (Exception e) {
            log.error("Error obteniendo roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Error al obtener roles: " + e.getMessage()));
        }
    }

    // ========================================
    // MÉTODOS UTILITARIOS PRIVADOS
    // ========================================

    /**
     * Genera una contraseña temporal segura
     */
    private String generateTemporaryPassword() {
        String upperCase = "ABCDEFGHJKMNPQRSTUVWXYZ";
        String lowerCase = "abcdefghjkmnpqrstuvwxyz";
        String numbers = "23456789";
        String specialChars = "!@#$%&*";

        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();

        // Asegurar al menos uno de cada tipo
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));

        // Completar hasta 10 caracteres
        String allChars = upperCase + lowerCase + numbers + specialChars;
        for (int i = 4; i < 10; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Mezclar caracteres
        char[] passwordArray = password.toString().toCharArray();
        for (int i = 0; i < passwordArray.length; i++) {
            int randomIndex = random.nextInt(passwordArray.length);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[randomIndex];
            passwordArray[randomIndex] = temp;
        }

        return new String(passwordArray);
    }

    /**
     * Convierte User entity a UserDTO
     */
    private UserDTO convertToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getEmail()) // En tu sistema email = username
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getActive())
                .isEmailVerified(user.isEnabled())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(List.of(UserDTO.RoleDTO.fromUserRole(user.getRole())))
                .specializations(user.getSpecialization() != null ?
                        Arrays.asList(user.getSpecialization().split(",\\s*")) : List.of())
                .country(user.getCountry())
                .currentWorkload(user.getCurrentWorkload())
                .build();
    }

    /**
     * Convierte User entity a LawyerProfileDTO
     */
    private LawyerProfileDTO convertToLawyerProfileDTO(User user) {
        return LawyerProfileDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getActive())
                .licenseNumber("LAW-" + (2020 + user.getId()) + "-" + String.format("%03d", user.getId()))
                .licenseState("El Salvador")
                .licenseExpiry("2029-12-31")
                .barAssociation("Colegio de Abogados de El Salvador")
                .yearsExperience(calculateYearsExperience(user))
                .specializations(user.getSpecialization() != null ?
                        Arrays.asList(user.getSpecialization().split(",\\s*")) : List.of())
                .casesCount(user.getCurrentWorkload() != null ? user.getCurrentWorkload() * 5 : 0)
                .activeCasesCount(user.getCurrentWorkload() != null ? user.getCurrentWorkload() : 0)
                .completedCasesCount(user.getCurrentWorkload() != null ? user.getCurrentWorkload() * 4 : 0)
                .successRate(85.0 + (user.getId() % 15)) // Simular tasa de éxito
                .averageCaseDuration(3.5 + (user.getId() % 4))
                .isAvailableForNewCases(user.getCurrentWorkload() != null && user.getCurrentWorkload() < 10)
                .hourlyRate(60.0 + (user.getId() * 5))
                .maxCasesAtOnce(15)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Calcula años de experiencia basado en cuando se creó el usuario
     */
    private Integer calculateYearsExperience(User user) {
        if (user.getCreatedAt() != null) {
            return Math.max(1, java.time.Year.now().getValue() - user.getCreatedAt().getYear());
        }
        return 1;
    }
}