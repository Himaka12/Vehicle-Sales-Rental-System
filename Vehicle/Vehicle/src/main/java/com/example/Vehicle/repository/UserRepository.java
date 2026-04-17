package com.example.Vehicle.repository;

import com.example.Vehicle.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByEmailAndIsActiveTrue(String email);
    boolean existsByRoleAndIsActiveTrue(String role);

    // Fetch users by their specific role
    List<User> findByRole(String role);
}
