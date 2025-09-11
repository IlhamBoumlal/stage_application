package com.projetPFA.backend_pfa.models.springSecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(UserDetailsService userDetailsService, JwtAuthFilter jwtAuthFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // --- Endpoints publics (accessibles sans être connecté) ---
                        .requestMatchers("/api/auth/**").permitAll()  // Autorise TOUS les endpoints auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/error").permitAll()  // Très important pour Spring Boot
                        .requestMatchers(HttpMethod.GET, "/api/pharmacies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/medicaments/**").permitAll()
                        .requestMatchers("/api/reservations/demande-avec-notification").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pharmacies/simuler-stocks").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reservations/*/confirm").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reservations/*/reject").permitAll()                        .requestMatchers(HttpMethod.GET, "/api/reservations/*/reject").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reservations/test-gmail").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/reservations/*/confirm").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reservations/*/reject").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reservations/*/user-choice").permitAll()  // ⭐ ESSENTIEL

                        // --- ENDPOINTS DE TEST EMAIL ---
                        .requestMatchers(HttpMethod.GET, "/api/reservations/test-gmail").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reservations/test-*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reservations/test-*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reservations/email-*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reservations/simulate-*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reservations/force-*").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/cleanup-*").permitAll()



                        // ⭐⭐ AJOUT CRITIQUE ⭐⭐ - Autoriser le webhook WhatsApp
                        .requestMatchers(HttpMethod.POST, "/api/whatsapp/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/whatsapp/webhook").permitAll()
                        .requestMatchers("/api/diagnostic/**").permitAll()  // ⭐⭐ AJOUT IMPORTANT ⭐⭐

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // Pour les requêtes CORS

                        // --- Endpoints protégés (nécessitent une authentification valide) ---
                        .requestMatchers("/api/user/**").authenticated()

                        // --- Règle finale ---
                        .anyRequest().authenticated()
                )

                .authenticationProvider(daoAuthenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:3000"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}