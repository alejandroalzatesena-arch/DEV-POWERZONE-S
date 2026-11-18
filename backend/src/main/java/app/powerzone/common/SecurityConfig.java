package app.powerzone.common;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad de la API: cadena stateless con JWT HS256, reglas de acceso por
 * ruta y autorizacion por rol en los controladores.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final RateLimitFilter rateLimitFilter;
    private final SecurityErrorWriter securityErrorWriter;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final String allowedOrigins;

    public SecurityConfig(RateLimitFilter rateLimitFilter,
                          SecurityErrorWriter securityErrorWriter,
                          JwtAuthenticationConverter jwtAuthenticationConverter,
                          @Value("${powerzone.cors-origins:http://localhost:4200}") String allowedOrigins) {
        this.rateLimitFilter = rateLimitFilter;
        this.securityErrorWriter = securityErrorWriter;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/health", "/actuator/**", "/api/docs/**", "/api/docs-json/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password").permitAll()
                        // El webhook se autentica con la firma de la pasarela, no con JWT.
                        .requestMatchers(HttpMethod.POST, "/api/v1/billing/webhook").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/trainer/**").hasAnyRole("ADMIN", "TRAINER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(securityErrorWriter)
                        .accessDeniedHandler(securityErrorWriter))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityErrorWriter)
                        .accessDeniedHandler(securityErrorWriter))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Gym-Id", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Location", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
