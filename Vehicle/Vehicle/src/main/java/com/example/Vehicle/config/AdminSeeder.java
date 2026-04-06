package com.example.Vehicle.config;

import com.example.Vehicle.entity.User;
import com.example.Vehicle.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminFullName;
    private final String adminContactNumber;
    private final String adminPassword;

    public AdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.full-name}") String adminFullName,
            @Value("${app.admin.contact-number}") String adminContactNumber,
            @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminFullName = adminFullName;
        this.adminContactNumber = adminContactNumber;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(adminEmail)) {
            User mainAdmin = new User();
            mainAdmin.setFullName(adminFullName);
            mainAdmin.setEmail(adminEmail);
            mainAdmin.setContactNumber(adminContactNumber);
            mainAdmin.setPassword(passwordEncoder.encode(adminPassword));
            mainAdmin.setRole("MAIN_ADMIN");
            mainAdmin.setActive(true);

            userRepository.save(mainAdmin);
            System.out.println("Main Admin account seeded successfully into the database.");
        } else {
            System.out.println("Main Admin account already exists. Skipping seeding.");
        }
    }
}
