package com.example.Vehicle.service;

import com.example.Vehicle.config.JwtUtil;
import com.example.Vehicle.dto.AuthResponseDTO;
import com.example.Vehicle.dto.LoginDTO;
import com.example.Vehicle.dto.RegisterDTO;
import com.example.Vehicle.repository.UserRepository;
import com.example.Vehicle.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AccountDeletionService accountDeletionService;

    @Override
    public String registerUser(RegisterDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email is already registered!");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setContactNumber(dto.getContactNumber());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("CUSTOMER");
        user.setActive(true);

        userRepository.save(user);

        return "User registered successfully!";
    }

    @Override
    public AuthResponseDTO loginUser(LoginDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with this email!"));

        if (!user.isActive()) {
            throw new RuntimeException("This account has been deactivated.");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials!");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return new AuthResponseDTO(token, user.getFullName(), user.getRole());
    }

    @Override
    public String registerSubAdmin(RegisterDTO dto, String role) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email is already registered!");
        }
        if ("MARKETING_MANAGER".equals(role) && userRepository.existsByRoleAndIsActiveTrue("MARKETING_MANAGER")) {
            throw new RuntimeException("Only one active marketing manager is allowed. Deactivate the current marketing manager before adding a new one.");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setContactNumber(dto.getContactNumber());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(role);
        user.setActive(true);

        userRepository.save(user);
        return "Sub-Admin successfully created!";
    }

    @Override
    public void deleteSubAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!"MARKETING_MANAGER".equals(user.getRole())) {
            throw new RuntimeException("Only Sub-Admin accounts can be deleted from this action.");
        }

        accountDeletionService.deleteUserById(id);
    }

    @Override
    public List<Map<String, Object>> getSubAdmins() {
        return userRepository.findByRole("MARKETING_MANAGER").stream()
                .filter(User::isActive)
                .map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("fullName", user.getFullName());
            map.put("email", user.getEmail());
            map.put("contactNumber", user.getContactNumber());
            map.put("role", user.getRole());
            return map;
        }).collect(Collectors.toList());
    }

    // Fetch ALL users for the Admin Hub
    @Override
    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(User::isActive)
                .map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("fullName", user.getFullName());
            map.put("email", user.getEmail());
            map.put("contactNumber", user.getContactNumber());
            map.put("role", user.getRole());
            map.put("premium", user.isPremium()); // Send Premium status for VIP badges!
            return map;
        }).collect(Collectors.toList());
    }

    // Delete ANY user safely
    @Override
    public void deleteUser(Long id) {
        accountDeletionService.deleteUserById(id);
    }

    @Override
    public void updateSubAdmin(Long id, com.example.Vehicle.dto.UpdateUserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // strict security check: Only allow updating Sub-Admins
        if (!"MARKETING_MANAGER".equals(user.getRole())) {
            throw new RuntimeException("Unauthorized: You can only edit Sub-Admin accounts.");
        }

        // Check if the new email is already taken by someone else
        if (!user.getEmail().equals(dto.getEmail()) && userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered by another user.");
        }

        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setContactNumber(dto.getContactNumber());

        userRepository.save(user);
    }
}
