package com.example.Vehicle.service;

import com.example.Vehicle.dto.VehicleDTO;
import com.example.Vehicle.dto.VehicleSubmitDTO;
import java.util.List;

public interface VehicleService {
    VehicleDTO addVehicle(VehicleSubmitDTO submitDTO);
    List<VehicleDTO> getAllVehicles();
    VehicleDTO getVehicleById(Long id);
    List<VehicleDTO> getAdminVehicles();
    VehicleDTO getAdminVehicleById(Long id);
    VehicleDTO updateVehicle(Long id, VehicleSubmitDTO submitDTO);
    VehicleDTO updateVehicleVisibility(Long id, boolean visible);
    void deleteVehicle(Long id);
}
