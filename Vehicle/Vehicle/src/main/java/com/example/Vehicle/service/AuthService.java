package com.example.Vehicle.service;

import com.example.Vehicle.dto.AuthResponseDTO;
import com.example.Vehicle.dto.LoginDTO;
import com.example.Vehicle.dto.RegisterDTO;

import java.util.List;
import java.util.Map;

public interface AuthService {
    String registerUser(RegisterDTO dto);
    AuthResponseDTO loginUser(LoginDTO dto);

    // Admin tools for Sub-Admins
    String registerSubAdmin(RegisterDTO dto, String role);
    List<Map<String, Object>> getSubAdmins();
    void deleteSubAdmin(Long id);

    // Fetch everyone and delete anyone!
    List<Map<String, Object>> getAllUsers();
    void deleteUser(Long id);

    void updateSubAdmin(Long id, com.example.Vehicle.dto.UpdateUserDTO dto);
}