package com.example.Vehicle.service;

import com.example.Vehicle.dto.VehicleDTO;
import com.example.Vehicle.dto.VehicleSubmitDTO;
import java.util.List;

public interface VehicleService {
    VehicleDTO addVehicle(VehicleSubmitDTO submitDTO);
    List<VehicleDTO> getAllVehicles();
    VehicleDTO getVehicleById(Long id);
    VehicleDTO updateVehicle(Long id, VehicleSubmitDTO submitDTO);
    void deleteVehicle(Long id);
}