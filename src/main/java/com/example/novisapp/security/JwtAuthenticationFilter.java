package com.example.novisapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");
        final String jwtToken;
        final String username;

        // Verificar si el header Authorization está presente y tiene el formato correcto
        if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extraer el token JWT del header
        jwtToken = requestTokenHeader.substring(7);

        try {
            // Extraer username del token
            username = jwtTokenUtil.getUsernameFromToken(jwtToken);
        } catch (Exception e) {
            log.error("Unable to get JWT Token or JWT Token has expired: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Validar el token y configurar el contexto de seguridad
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Si el token es válido, configurar Spring Security para establecer manualmente la autenticación
            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Después de establecer la Authentication en el contexto, especificamos
                // que el usuario actual está autenticado. Así pasa las verificaciones de Spring Security.
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Successfully authenticated user: {}", username);
            } else {
                log.warn("JWT Token validation failed for user: {}", username);
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // No filtrar estas rutas públicas
        return path.startsWith("/api/auth/") ||
                path.startsWith("/api/public/") ||
                path.equals("/api/health") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/") ||
                path.startsWith("/swagger-resources/") ||
                path.startsWith("/webjars/") ||
                path.equals("/favicon.ico");
    }
}