package com.packedgo.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Permite cualquier solicitud a /users y sus subrutas sin autenticación
                        .requestMatchers("/users/**").permitAll()
                        // Cualquier otra solicitud requiere autenticación básica
                        .anyRequest().authenticated()
                )
                // Desactiva la protección CSRF para peticiones POST, PUT, DELETE, etc.
                .csrf(csrf -> csrf.disable())
                // Habilita la autenticación básica
                .httpBasic(withDefaults());
        return http.build();
    }
}