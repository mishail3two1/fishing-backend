package com.fishing.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Кто куда может зайти. Открыто то, что по контракту доступно без входа (CLAUDE.md, раздел 6),
// всё остальное требует аутентификации.
@Configuration
public class SecurityConfig {

    // фиксированный текст: причину (нет токена, просрочен, подделан) клиенту не уточняем
    private static final String UNAUTHORIZED_BODY =
            "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Требуется вход\"}";

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
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/fish-species").permitAll()
                        .anyRequest().authenticated())
                // токен из заголовка Authorization: Bearer проверяет Spring Security (подпись и срок) через JwtDecoder
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(SecurityConfig::writeUnauthorized))
                // запрос без токена на закрытый адрес — тоже 401 в формате ProblemDetail
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(SecurityConfig::writeUnauthorized));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 401 в формате ProblemDetail, как все ошибки API; без этого Spring Security отдал бы 401 с пустым телом
    private static void writeUnauthorized(HttpServletRequest request, HttpServletResponse response,
                                          AuthenticationException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType("application/problem+json");
        response.getOutputStream().write(UNAUTHORIZED_BODY.getBytes(StandardCharsets.UTF_8));
    }
}
