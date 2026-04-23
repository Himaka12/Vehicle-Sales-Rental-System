package com.example.Vehicle.service;

import com.example.Vehicle.dto.SalesInquiryDTO;
import com.example.Vehicle.entity.SalesInquiry;
import com.example.Vehicle.entity.User;
import com.example.Vehicle.entity.Vehicle;
import com.example.Vehicle.repository.SalesInquiryRepository;
import com.example.Vehicle.repository.UserRepository;
import com.example.Vehicle.repository.VehicleRepository;
import com.example.Vehicle.util.StatusRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesInquiryService {

    private final SalesInquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public SalesInquiryDTO submitInquiry(String email, SalesInquiryDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (!vehicle.isVisible()) {
            throw new RuntimeException("This listing is currently hidden and cannot accept new inquiries.");
        }

        if (inquiryRepository.existsByUserIdAndVehicleId(user.getId(), dto.getVehicleId())
                || inquiryRepository.existsByEmailAndVehicleId(user.getEmail(), dto.getVehicleId())) {
            throw new RuntimeException("You have already submitted an inquiry for this vehicle. Our team will contact you soon!");
        }

        String phone = dto.getPhone() != null && !dto.getPhone().trim().isEmpty()
                ? dto.getPhone().trim()
                : user.getContactNumber();

        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Please add a contact number before submitting an inquiry.");
        }

        SalesInquiry inquiry = new SalesInquiry();
        inquiry.setVehicleId(dto.getVehicleId());
        inquiry.setUserId(user.getId());
        inquiry.setCustomerName(user.getFullName());
        inquiry.setEmail(user.getEmail());
        inquiry.setPhone(phone);
        inquiry.setPreferredContactTime(dto.getPreferredContactTime());
        inquiry.setMessage(dto.getMessage());
        inquiry.setStatus(StatusRules.INQUIRY_PENDING);
        inquiry.setInquiryDate(LocalDate.now().toString());

        SalesInquiry savedInquiry = inquiryRepository.save(inquiry);
        return mapToDTO(savedInquiry);
    }

    public List<SalesInquiryDTO> getAllInquiries() {
        return inquiryRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<SalesInquiryDTO> getMyInquiries(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<Long, SalesInquiry> inquiriesById = new LinkedHashMap<>();
        inquiryRepository.findAllByUserIdOrderByIdDesc(user.getId())
                .forEach(inquiry -> inquiriesById.put(inquiry.getId(), inquiry));
        inquiryRepository.findAllByEmailOrderByIdDesc(email)
                .forEach(inquiry -> inquiriesById.putIfAbsent(inquiry.getId(), inquiry));

        return inquiriesById.values().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public SalesInquiryDTO updateStatus(Long id, String newStatus) {
        SalesInquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inquiry not found"));
        String normalizedStatus = StatusRules.normalizeInquiryStatus(newStatus);
        StatusRules.validateInquiryAdminTransition(inquiry.getStatus(), normalizedStatus);
        inquiry.setStatus(normalizedStatus);
        return mapToDTO(inquiryRepository.save(inquiry));
    }

    private SalesInquiryDTO mapToDTO(SalesInquiry inquiry) {
        SalesInquiryDTO dto = new SalesInquiryDTO();
        dto.setId(inquiry.getId());
        dto.setVehicleId(inquiry.getVehicleId());
        dto.setCustomerName(inquiry.getCustomerName());
        dto.setEmail(inquiry.getEmail());
        dto.setPhone(inquiry.getPhone());
        dto.setPreferredContactTime(inquiry.getPreferredContactTime());
        dto.setMessage(inquiry.getMessage());
        dto.setStatus(StatusRules.normalizeInquiryStatus(inquiry.getStatus()));
        dto.setInquiryDate(inquiry.getInquiryDate());

        vehicleRepository.findById(inquiry.getVehicleId()).ifPresent(vehicle -> applyVehicleSummary(dto, vehicle));

        Optional<User> user = inquiry.getUserId() != null
                ? userRepository.findById(inquiry.getUserId())
                : userRepository.findByEmail(inquiry.getEmail());
        if (user.isPresent()) {
            dto.setPremiumCustomer(user.get().isPremium());
        } else {
            dto.setPremiumCustomer(false);
        }

        return dto;
    }

    private void applyVehicleSummary(SalesInquiryDTO dto, Vehicle vehicle) {
        dto.setVehicleName(vehicle.getBrand() + " " + vehicle.getModel());
        dto.setVehicleBrand(vehicle.getBrand());
        dto.setVehicleModel(vehicle.getModel());
        dto.setVehicleImageUrl(vehicle.getImage1());
        dto.setVehicleListingType(vehicle.getListingType());
        dto.setVehicleCondition(vehicle.getVehicleCondition());
        dto.setVehicleColor(vehicle.getColor());
        dto.setVehicleManufactureYear(vehicle.getManufactureYear());
        dto.setVehicleFuelType(vehicle.getFuelType());
        dto.setVehicleTransmission(vehicle.getTransmission());
        dto.setVehiclePrice(vehicle.getPrice());
    }

    public boolean hasInquired(String email, Long vehicleId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return inquiryRepository.existsByUserIdAndVehicleId(user.getId(), vehicleId)
                || inquiryRepository.existsByEmailAndVehicleId(email, vehicleId);
    }
}
