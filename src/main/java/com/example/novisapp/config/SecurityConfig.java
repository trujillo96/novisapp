package com.example.novisapp.config;

import com.example.novisapp.security.JwtAuthenticationFilter;
import com.example.novisapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // ✅ RUTAS PÚBLICAS - NO REQUIEREN AUTENTICACIÓN
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/public/**",
                                "/api/legal-cases/**",      // ✅ AGREGADO - Casos legales públicos
                                "/api/health",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()

                        // ✅ RUTAS DE ADMINISTRACIÓN - SOLO ADMIN
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // ✅ RUTAS DE GESTIÓN - ADMIN Y MANAGING_PARTNER
                        .requestMatchers("/api/management/**")
                        .hasAnyRole("ADMIN", "MANAGING_PARTNER")

                        // ✅ RUTAS DE ABOGADOS - ADMIN, MANAGING_PARTNER, LAWYER
                        .requestMatchers("/api/lawyers/**")
                        .hasAnyRole("ADMIN", "MANAGING_PARTNER", "LAWYER")

                        // ✅ RUTAS ESPECÍFICAS POR FUNCIONALIDAD
                        .requestMatchers("/api/cases/create", "/api/cases/*/update", "/api/cases/*/delete")
                        .hasAnyRole("ADMIN", "MANAGING_PARTNER", "LAWYER")

                        .requestMatchers("/api/documents/*/delete")
                        .hasAnyRole("ADMIN", "MANAGING_PARTNER")

                        .requestMatchers("/api/teams/assign/**")
                        .hasAnyRole("ADMIN", "MANAGING_PARTNER")

                        .requestMatchers("/api/financial/**")
                        .hasAnyRole("ADMIN", "MANAGING_PARTNER")

                        // ✅ TODAS LAS DEMÁS RUTAS REQUIEREN AUTENTICACIÓN
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ PERMITIR ORÍGENES ESPECÍFICOS
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",     // React development
                "http://localhost:3001",     // React alternative port
                "https://*.vercel.app",      // Vercel deployments
                "https://*.netlify.app",     // Netlify deployments
                "https://novis.legal",       // Production domain
                "https://*.novis.legal"      // Subdomains
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Cache-Control",
                "X-File-Name"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "X-File-Name"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}