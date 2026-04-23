package com.example.Vehicle.service;

import com.example.Vehicle.dto.WishlistDTO;
import com.example.Vehicle.entity.User;
import com.example.Vehicle.entity.Vehicle;
import com.example.Vehicle.entity.Wishlist;
import com.example.Vehicle.repository.UserRepository;
import com.example.Vehicle.repository.VehicleRepository;
import com.example.Vehicle.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public String toggleWishlist(String email, Long vehicleId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(() -> new RuntimeException("Vehicle not found"));

        Optional<Wishlist> existing = wishlistRepository.findByUserAndVehicle(user, vehicle);
        if (existing.isPresent()) {
            wishlistRepository.delete(existing.get());
            return "removed";
        }

        if (!vehicle.isVisible()) {
            throw new RuntimeException("This listing is currently hidden and cannot be added to the wishlist.");
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setVehicle(vehicle);
        wishlistRepository.save(wishlist);
        return "added";
    }

    @Override
    public List<WishlistDTO> getUserWishlist(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return wishlistRepository.findByUser(user).stream()
                .filter(w -> w.getVehicle() != null && w.getVehicle().isVisible())
                .map(w -> {
            WishlistDTO dto = new WishlistDTO();
            dto.setWishlistId(w.getId());
            dto.setVehicleId(w.getVehicle().getId());
            dto.setBrand(w.getVehicle().getBrand());
            dto.setModel(w.getVehicle().getModel());
            dto.setPrice(w.getVehicle().getPrice());
            dto.setImage1(w.getVehicle().getImage1());
            dto.setListingType(w.getVehicle().getListingType());
            dto.setAddedDate(w.getAddedDate());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Long> getUserWishlistVehicleIds(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return wishlistRepository.findByUser(user).stream()
                .filter(w -> w.getVehicle() != null && w.getVehicle().isVisible())
                .map(w -> w.getVehicle().getId())
                .collect(Collectors.toList());
    }
}
