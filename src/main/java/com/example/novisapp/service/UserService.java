package com.example.novisapp.service;

import com.example.novisapp.entity.User;
import com.example.novisapp.entity.UserRole;
import com.example.novisapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        if (!user.getActive() || !user.isEnabled()) {
            throw new UsernameNotFoundException("Usuario deshabilitado: " + email);
        }

        return user;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    public User createUser(String email, String password, String firstName, String lastName,
                           UserRole role, String specialization, String phoneNumber, String country) {

        // Verificar si ya existe el email
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Ya existe un usuario con el email: " + email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setSpecialization(specialization);
        user.setPhoneNumber(phoneNumber);
        user.setCountry(country);
        user.setActive(true);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setCurrentWorkload(0);
        user.setFailedLoginAttempts(0);

        User savedUser = userRepository.save(user);
        log.info("Usuario creado: {} - {}", savedUser.getEmail(), savedUser.getRole());

        return savedUser;
    }

    public User updateUser(Long userId, String firstName, String lastName,
                           String specialization, String phoneNumber, String country) {
        User user = findById(userId);

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setSpecialization(specialization);
        user.setPhoneNumber(phoneNumber);
        user.setCountry(country);

        return userRepository.save(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Contraseña actual incorrecta");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setCredentialsNonExpired(true);
        userRepository.save(user);

        log.info("Contraseña cambiada para usuario: {}", user.getEmail());
    }

    public void deactivateUser(Long userId) {
        User user = findById(userId);
        user.setActive(false);
        user.setEnabled(false);
        userRepository.save(user);

        log.info("Usuario desactivado: {}", user.getEmail());
    }

    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setActive(true);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        log.info("Usuario activado: {}", user.getEmail());
    }

    public void handleLoginSuccess(String email) {
        User user = findByEmail(email);
        user.resetFailedLoginAttempts();
        user.updateLastLogin();
        userRepository.save(user);

        log.debug("Login exitoso para: {}", email);
    }

    public void handleLoginFailure(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.incrementFailedLoginAttempts();

            // Bloquear cuenta después de 5 intentos fallidos
            if (user.getFailedLoginAttempts() >= 5) {
                user.setAccountNonLocked(false);
                log.warn("Cuenta bloqueada por intentos fallidos: {}", email);
            }

            userRepository.save(user);
        }

        log.warn("Login fallido para: {}", email);
    }

    public void unlockUser(Long userId) {
        User user = findById(userId);
        user.setAccountNonLocked(true);
        user.resetFailedLoginAttempts();
        userRepository.save(user);

        log.info("Usuario desbloqueado: {}", user.getEmail());
    }

    public List<User> getAllActiveUsers() {
        return userRepository.findByActiveTrueOrderByLastName();
    }

    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRoleAndActiveTrueOrderByLastName(role);
    }

    public List<User> getAvailableLawyers() {
        return userRepository.findAvailableLawyers();
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.findByEmail(email).isPresent();
    }

    public long getTotalActiveUsers() {
        return userRepository.countByActiveTrue();
    }

    public long getUsersByRoleCount(UserRole role) {
        return userRepository.countByRoleAndActiveTrue(role);
    }

    // Método para crear usuarios por defecto (para testing)
    @Transactional
    public void createDefaultUsers() {
        if (userRepository.count() == 0) {
            log.info("Creando usuarios por defecto...");

            // Admin
            createUser("admin@novis.legal", "admin123", "Sistema", "Administrador",
                    UserRole.ADMIN, "Administración", "+503 7000-0000", "El Salvador");

            // Managing Partner
            createUser("partner@novis.legal", "partner123", "Carlos", "Martínez",
                    UserRole.MANAGING_PARTNER, "Derecho Corporativo", "+503 7000-0001", "El Salvador");

            // Lawyers
            createUser("ana.martinez@novis.legal", "lawyer123", "Ana", "Martínez",
                    UserRole.LAWYER, "Fusiones y Adquisiciones", "+503 7000-0002", "El Salvador");

            createUser("carlos.lopez@novis.legal", "lawyer123", "Carlos", "López",
                    UserRole.LAWYER, "Litigio Comercial", "+503 7000-0003", "El Salvador");

            // Collaborator
            createUser("maria.gonzalez@novis.legal", "collab123", "María", "González",
                    UserRole.COLLABORATOR, "Compliance y Regulatorio", "+503 7000-0004", "El Salvador");

            log.info("Usuarios por defecto creados exitosamente");
        }
    }
}