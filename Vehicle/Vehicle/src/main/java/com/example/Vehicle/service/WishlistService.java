package com.example.Vehicle.service;

import com.example.Vehicle.dto.WishlistDTO;
import java.util.List;

public interface WishlistService {
    String toggleWishlist(String email, Long vehicleId);
    List<WishlistDTO> getUserWishlist(String email);
    List<Long> getUserWishlistVehicleIds(String email);
}