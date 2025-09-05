package com.example.novisapp.controller;

import com.example.novisapp.entity.User;
import com.example.novisapp.entity.UserRole;
import com.example.novisapp.security.JwtTokenUtil;
import com.example.novisapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder; // ✅ AGREGADO PARA DEBUG

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Intento de login para: {}", loginRequest.getEmail());

            // Autenticar usuario
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Obtener usuario autenticado
            User user = (User) authentication.getPrincipal();

            // Generar token JWT
            String token = jwtTokenUtil.generateToken(user);

            // Actualizar último login
            userService.handleLoginSuccess(user.getEmail());

            // Preparar respuesta
            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenUtil.getTokenRemainingTime(token))
                    .user(UserInfoResponse.from(user))
                    .build();

            log.info("Login exitoso para: {} - Rol: {}", user.getEmail(), user.getRole());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Credenciales incorrectas para: {}", loginRequest.getEmail());
            userService.handleLoginFailure(loginRequest.getEmail());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "INVALID_CREDENTIALS",
                            "message", "Email o contraseña incorrectos"
                    ));

        } catch (DisabledException e) {
            log.warn("Usuario deshabilitado: {}", loginRequest.getEmail());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "ACCOUNT_DISABLED",
                            "message", "Cuenta deshabilitada. Contacte al administrador"
                    ));

        } catch (Exception e) {
            log.error("Error durante login para: {} - {}", loginRequest.getEmail(), e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "LOGIN_FAILED",
                            "message", "Error interno del servidor"
                    ));
        }
    }

    /**
     * ⚠️ ENDPOINT TEMPORAL - Solo para debugging - ELIMINAR después del testing
     */
    @GetMapping("/debug/generate-hash/{password}")
    public ResponseEntity<?> generateHash(@PathVariable String password) {
        try {
            String hash = passwordEncoder.encode(password);

            log.info("Hash generado para debugging - Password: {}", password);

            return ResponseEntity.ok(Map.of(
                    "password", password,
                    "hash", hash,
                    "encoder", passwordEncoder.getClass().getSimpleName(),
                    "hashLength", hash.length(),
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Error generando hash: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "HASH_GENERATION_FAILED", "message", e.getMessage()));
        }
    }

    /**
     * ⚠️ ENDPOINT TEMPORAL - Verificar hash - ELIMINAR después del testing
     */
    @PostMapping("/debug/verify-hash")
    public ResponseEntity<?> verifyHash(@RequestBody Map<String, String> request) {
        try {
            String password = request.get("password");
            String hash = request.get("hash");

            boolean matches = passwordEncoder.matches(password, hash);

            return ResponseEntity.ok(Map.of(
                    "password", password,
                    "hash", hash,
                    "matches", matches,
                    "encoder", passwordEncoder.getClass().getSimpleName()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "VERIFICATION_FAILED", "message", e.getMessage()));
        }
    }

    /**
     * Logout endpoint
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            // En un sistema más avanzado, aquí agregaríamos el token a una blacklist
            // Por ahora, solo limpiamos el contexto de seguridad
            SecurityContextHolder.clearContext();

            log.info("Usuario deslogueado exitosamente");
            return ResponseEntity.ok(Map.of(
                    "message", "Logout exitoso",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error durante logout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "LOGOUT_FAILED", "message", "Error durante logout"));
        }
    }

    /**
     * Obtener información del usuario actual
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "NOT_AUTHENTICATED", "message", "Usuario no autenticado"));
            }

            User user = (User) authentication.getPrincipal();
            UserInfoResponse userInfo = UserInfoResponse.from(user);

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            log.error("Error obteniendo información del usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "USER_INFO_FAILED", "message", "Error obteniendo información del usuario"));
        }
    }

    /**
     * Refrescar token JWT
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "INVALID_TOKEN", "message", "Token no válido"));
            }

            String token = authHeader.substring(7);

            if (!jwtTokenUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "EXPIRED_TOKEN", "message", "Token expirado"));
            }

            String newToken = jwtTokenUtil.refreshToken(token);

            return ResponseEntity.ok(Map.of(
                    "token", newToken,
                    "tokenType", "Bearer",
                    "expiresIn", jwtTokenUtil.getTokenRemainingTime(newToken)
            ));

        } catch (Exception e) {
            log.error("Error refrescando token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "REFRESH_FAILED", "message", "Error refrescando token"));
        }
    }

    /**
     * Registrar nuevo usuario (solo para ADMIN)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest,
                                      Authentication authentication) {
        try {
            // Verificar permisos (solo ADMIN puede registrar usuarios)
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "NOT_AUTHENTICATED", "message", "Requiere autenticación"));
            }

            User currentUser = (User) authentication.getPrincipal();
            if (currentUser.getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "INSUFFICIENT_PERMISSIONS", "message", "Solo administradores pueden registrar usuarios"));
            }

            // Verificar si el email ya existe
            if (!userService.isEmailAvailable(registerRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "EMAIL_EXISTS", "message", "El email ya está registrado"));
            }

            // Crear usuario
            User newUser = userService.createUser(
                    registerRequest.getEmail(),
                    registerRequest.getPassword(),
                    registerRequest.getFirstName(),
                    registerRequest.getLastName(),
                    registerRequest.getRole(),
                    registerRequest.getSpecialization(),
                    registerRequest.getPhoneNumber(),
                    registerRequest.getCountry()
            );

            UserInfoResponse userInfo = UserInfoResponse.from(newUser);

            log.info("Usuario registrado exitosamente: {} por {}", newUser.getEmail(), currentUser.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Usuario registrado exitosamente",
                    "user", userInfo
            ));

        } catch (Exception e) {
            log.error("Error registrando usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "REGISTRATION_FAILED", "message", "Error registrando usuario"));
        }
    }

    /**
     * Cambiar contraseña
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "NOT_AUTHENTICATED", "message", "Requiere autenticación"));
            }

            User user = (User) authentication.getPrincipal();

            userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());

            log.info("Contraseña cambiada exitosamente para: {}", user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "Contraseña cambiada exitosamente",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (RuntimeException e) {
            log.warn("Error cambiando contraseña: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "CHANGE_PASSWORD_FAILED", "message", e.getMessage()));

        } catch (Exception e) {
            log.error("Error interno cambiando contraseña: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Error interno del servidor"));
        }
    }

    /**
     * Validar token
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "message", "Token no válido"));
            }

            String token = authHeader.substring(7);
            boolean isValid = jwtTokenUtil.validateToken(token);

            if (isValid) {
                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "expiresIn", jwtTokenUtil.getTokenRemainingTime(token),
                        "email", jwtTokenUtil.getUsernameFromToken(token)
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "message", "Token expirado o inválido"));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "Token inválido"));
        }
    }

    // ========================================
    // DTOs INTERNOS
    // ========================================

    public static class LoginRequest {
        private String email;
        private String password;

        // Getters y Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private UserRole role;
        private String specialization;
        private String phoneNumber;
        private String country;

        // Getters y Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
        public String getSpecialization() { return specialization; }
        public void setSpecialization(String specialization) { this.specialization = specialization; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class LoginResponse {
        private String token;
        private String tokenType;
        private Long expiresIn;
        private UserInfoResponse user;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private LoginResponse response = new LoginResponse();
            public Builder token(String token) { response.token = token; return this; }
            public Builder tokenType(String tokenType) { response.tokenType = tokenType; return this; }
            public Builder expiresIn(Long expiresIn) { response.expiresIn = expiresIn; return this; }
            public Builder user(UserInfoResponse user) { response.user = user; return this; }
            public LoginResponse build() { return response; }
        }

        // Getters
        public String getToken() { return token; }
        public String getTokenType() { return tokenType; }
        public Long getExpiresIn() { return expiresIn; }
        public UserInfoResponse getUser() { return user; }
    }

    public static class UserInfoResponse {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String fullName;
        private UserRole role;
        private String specialization;
        private String phoneNumber;
        private String country;
        private Integer currentWorkload;
        private LocalDateTime lastLogin;

        public static UserInfoResponse from(User user) {
            UserInfoResponse response = new UserInfoResponse();
            response.id = user.getId();
            response.email = user.getEmail();
            response.firstName = user.getFirstName();
            response.lastName = user.getLastName();
            response.fullName = user.getFullName();
            response.role = user.getRole();
            response.specialization = user.getSpecialization();
            response.phoneNumber = user.getPhoneNumber();
            response.country = user.getCountry();
            response.currentWorkload = user.getCurrentWorkload();
            response.lastLogin = user.getLastLogin();
            return response;
        }

        // Getters
        public Long getId() { return id; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getFullName() { return fullName; }
        public UserRole getRole() { return role; }
        public String getSpecialization() { return specialization; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getCountry() { return country; }
        public Integer getCurrentWorkload() { return currentWorkload; }
        public LocalDateTime getLastLogin() { return lastLogin; }
    }
}