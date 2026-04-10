package com.example.Vehicle.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless REST APIs
                .authorizeHttpRequests(auth -> auth
                        // Public authentication endpoints
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()

                        // Main admin only
                        .requestMatchers("/api/auth/add-subadmin").hasAuthority("MAIN_ADMIN")
                        .requestMatchers("/api/auth/subadmins").hasAuthority("MAIN_ADMIN")
                        .requestMatchers("/api/auth/delete-subadmin/**").hasAuthority("MAIN_ADMIN")
                        .requestMatchers("/api/auth/users").hasAuthority("MAIN_ADMIN")
                        .requestMatchers("/api/auth/delete-user/**").hasAuthority("MAIN_ADMIN")
                        .requestMatchers("/api/auth/update-subadmin/**").hasAuthority("MAIN_ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyAuthority("MAIN_ADMIN", "MARKETING_MANAGER")

                        .requestMatchers(HttpMethod.POST, "/api/vehicles/add").hasAuthority("MAIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/vehicles/update/**").hasAuthority("MAIN_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/vehicles/delete/**").hasAuthority("MAIN_ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/bookings/all").hasAuthority("MAIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/bookings/status/**").hasAuthority("MAIN_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/bookings/admin-delete/**").hasAuthority("MAIN_ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/inquiries/all").hasAuthority("MAIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/inquiries/update-status/**").hasAuthority("MAIN_ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/reviews/all").hasAuthority("MAIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/admin-delete/**").hasAuthority("MAIN_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/admin-purge/**").hasAuthority("MAIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/admin-respond/**").hasAuthority("MAIN_ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/refunds/pending").hasAuthority("MAIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/refunds/all").hasAuthority("MAIN_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/refunds/process/**").hasAuthority("MAIN_ADMIN")

                        // Marketing manager and main admin
                        .requestMatchers(HttpMethod.POST, "/api/promotions/add").hasAnyAuthority("MARKETING_MANAGER", "MAIN_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/promotions/all").hasAnyAuthority("MARKETING_MANAGER", "MAIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/promotions/status/**").hasAnyAuthority("MARKETING_MANAGER", "MAIN_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/promotions/update/**").hasAnyAuthority("MARKETING_MANAGER", "MAIN_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/promotions/**").hasAnyAuthority("MARKETING_MANAGER", "MAIN_ADMIN")

                        // Customer-only flows
                        .requestMatchers("/api/customer/**").hasAuthority("CUSTOMER")
                        .requestMatchers("/api/wishlist/**").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/bookings/rent").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/bookings/update/**").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/bookings/delete/**").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/bookings/my-bookings").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/inquiries/add").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/inquiries/my-inquiries").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/inquiries/check/**").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/reviews/add").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/update/**").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/delete/**").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/reviews/my-reviews").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/reviews/can-review/**").hasAuthority("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/refunds/claim/**").hasAuthority("CUSTOMER")

                        // Public read-only APIs
                        .requestMatchers(HttpMethod.GET, "/api/vehicles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/promotions/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/promotions/showcase").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/vehicle/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/bookings/check-availability").permitAll()

                        // Static resources and public pages
                        .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/images/**", "/uploads/**", "/error").permitAll()

                        // Any endpoint not mapped above still requires authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Add our custom JWT filter before the standard Spring Security filter
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
