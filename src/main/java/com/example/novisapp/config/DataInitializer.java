package com.example.novisapp.config;

import com.example.novisapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Crear usuarios por defecto solo si no existen
            userService.createDefaultUsers();
            log.info("✅ Inicialización de datos completada");
        } catch (Exception e) {
            log.error("❌ Error durante la inicialización de datos: {}", e.getMessage());
        }
    }
}