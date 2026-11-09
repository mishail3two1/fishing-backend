package com.fishing.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Кто куда может зайти. Открыто то, что по контракту доступно без входа (CLAUDE.md, раздел 6),
// /api/v1/admin/** — только для ADMIN, всё остальное требует аутентификации.
@Configuration
public class SecurityConfig {

    // фиксированный текст: причину (нет токена, просрочен, подделан) клиенту не уточняем
    private static final String UNAUTHORIZED_BODY =
            "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Требуется вход\"}";
    private static final String FORBIDDEN_BODY =
            "{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,\"detail\":\"Недостаточно прав\"}";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF защищает cookie-сессии; у нас токен в заголовке Authorization, cookie нет
                .csrf(csrf -> csrf.disable())
                // правила CORS Security берёт из CorsConfig (WebMvcConfigurer)
                .cors(Customizer.withDefaults())
                // сервер не хранит сессию: пользователь определяется по токену в каждом запросе
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/water-bodies", "/api/v1/water-bodies/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/fish-species").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // токен из заголовка Authorization: Bearer проверяет Spring Security (подпись и срок) через JwtDecoder
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(SecurityConfig::writeUnauthorized)
                        .accessDeniedHandler(SecurityConfig::writeForbidden))
                // без токена на закрытый адрес — 401, вошёл, но не хватает прав — 403; оба в формате ProblemDetail
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(SecurityConfig::writeUnauthorized)
                        .accessDeniedHandler(SecurityConfig::writeForbidden));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // права берём из claim "role" (его кладёт JwtService) с приставкой ROLE_: "ADMIN" → ROLE_ADMIN для hasRole("ADMIN")
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private static void writeUnauthorized(HttpServletRequest request, HttpServletResponse response,
                                          AuthenticationException exception) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        writeProblem(response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_BODY);
    }

    private static void writeForbidden(HttpServletRequest request, HttpServletResponse response,
                                       AccessDeniedException exception) throws IOException {
        writeProblem(response, HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_BODY);
    }

    // ответ в формате ProblemDetail, как все ошибки API; фильтры Security работают до MVC, GlobalExceptionHandler сюда не дотягивается
    private static void writeProblem(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
