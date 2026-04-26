package com.example.Vehicle.repository;

import com.example.Vehicle.entity.User;
import com.example.Vehicle.entity.Vehicle;
import com.example.Vehicle.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUser(User user);
    Optional<Wishlist> findByUserAndVehicle(User user, Vehicle vehicle);

    // Deletes all saved cars linked to this user
    void deleteByUser(User user);

    void deleteByVehicleId(Long vehicleId);
}