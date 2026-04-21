package com.example.Vehicle.controller;

import com.example.Vehicle.dto.WishlistDTO;
import com.example.Vehicle.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/toggle/{vehicleId}")
    public ResponseEntity<String> toggleWishlist(Authentication authentication, @PathVariable Long vehicleId) {
        String result = wishlistService.toggleWishlist(authentication.getName(), vehicleId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-wishlist")
    public ResponseEntity<List<WishlistDTO>> getMyWishlist(Authentication authentication) {
        return ResponseEntity.ok(wishlistService.getUserWishlist(authentication.getName()));
    }

    @GetMapping("/my-wishlist-ids")
    public ResponseEntity<List<Long>> getMyWishlistIds(Authentication authentication) {
        return ResponseEntity.ok(wishlistService.getUserWishlistVehicleIds(authentication.getName()));
    }
}
