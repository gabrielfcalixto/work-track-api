package br.com.gfctech.project_manager.secury;

import br.com.gfctech.project_manager.secury.jwt.AuthEntryPointJwt;
import br.com.gfctech.project_manager.secury.jwt.AuthFilterToken;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    FilterRegistrationBean<AuthFilterToken> jwtServletRegistration(AuthFilterToken filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AuthEntryPointJwt unauthorizedHandler,
                                    AuthFilterToken jwt, UrlBasedCorsConfigurationSource cors) throws Exception {
        return http
                .cors(config -> config.configurationSource(cors))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(config -> config.authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler((request, response, error) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"Acesso negado.\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/generate-reset-code",
                                "/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/user", "/user/").hasAnyAuthority("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/user/*/change-password").authenticated()
                        .requestMatchers("/user/**").hasAuthority("ADMIN")
                        .requestMatchers("/clients/**", "/project/**").hasAnyAuthority("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/task", "/task/").hasAnyAuthority("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/task/**").authenticated()
                        .requestMatchers("/task/**").hasAnyAuthority("MANAGER", "ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource(@Value("${worktrack.cors.origins}") String origins) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
