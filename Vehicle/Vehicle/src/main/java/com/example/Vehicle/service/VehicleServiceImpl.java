package com.example.Vehicle.service;

import com.example.Vehicle.dto.VehicleDTO;
import com.example.Vehicle.dto.VehicleSubmitDTO;
import com.example.Vehicle.entity.Promotion;
import com.example.Vehicle.entity.Vehicle;
import com.example.Vehicle.repository.RefundRepository;
import com.example.Vehicle.repository.RentalBookingRepository;
import com.example.Vehicle.repository.ReviewRepository;
import com.example.Vehicle.repository.SalesInquiryRepository;
import com.example.Vehicle.repository.VehicleRepository;
import com.example.Vehicle.repository.WishlistRepository;
import com.example.Vehicle.util.UploadValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private static final Set<String> VALID_LISTING_TYPES = Set.of("Sale", "Rent");
    private static final Set<String> VALID_CONDITIONS = Set.of("Used", "BrandNew");
    private static final Set<String> VALID_STATUSES = Set.of("Available", "Coming Soon", "Sold");
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "Sedan", "SUV", "Hatchback", "Van", "Truck", "Coupe", "Convertible", "Luxury", "Bike", "Other"
    );

    private final VehicleRepository vehicleRepository;

    // 🔥 NEW: Injecting the child repositories for the Cascade Delete Engine
    private final RentalBookingRepository rentalBookingRepository;
    private final SalesInquiryRepository salesInquiryRepository;
    private final WishlistRepository wishlistRepository;
    private final ReviewRepository reviewRepository;
    private final RefundRepository refundRepository;
    private final PromotionMatchingService promotionMatchingService;

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

    private void validateVehicleRules(VehicleSubmitDTO dto) {
        if (dto.getListingType() == null || !VALID_LISTING_TYPES.contains(dto.getListingType())) {
            throw new RuntimeException("Listing type must be Sale or Rent.");
        }
        if (dto.getVehicleCondition() == null || !VALID_CONDITIONS.contains(dto.getVehicleCondition())) {
            throw new RuntimeException("Vehicle condition must be Used or BrandNew.");
        }
        if (dto.getStatus() == null || !VALID_STATUSES.contains(dto.getStatus())) {
            throw new RuntimeException("Vehicle status must be Available, Coming Soon, or Sold.");
        }
        if (dto.getBrand() == null || dto.getBrand().trim().isEmpty()) {
            throw new RuntimeException("Vehicle brand is required.");
        }
        if (dto.getModel() == null || dto.getModel().trim().isEmpty()) {
            throw new RuntimeException("Vehicle model is required.");
        }
        if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
            throw new RuntimeException("Vehicle category is required.");
        }
        if (!VALID_CATEGORIES.contains(dto.getCategory())) {
            throw new RuntimeException("Vehicle category is invalid.");
        }
        if (dto.getColor() == null || dto.getColor().trim().isEmpty()) {
            throw new RuntimeException("Vehicle color is required.");
        }
        if (dto.getEngineCapacity() == null || dto.getEngineCapacity().trim().isEmpty()) {
            throw new RuntimeException("Engine capacity is required.");
        }
        if (!dto.getEngineCapacity().trim().matches("^[1-9]\\d*$")) {
            throw new RuntimeException("Engine capacity must be greater than 0.");
        }
        if (dto.getFuelType() == null || dto.getFuelType().trim().isEmpty()) {
            throw new RuntimeException("Fuel type is required.");
        }
        if (dto.getTransmission() == null || dto.getTransmission().trim().isEmpty()) {
            throw new RuntimeException("Transmission type is required.");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            throw new RuntimeException("Vehicle description is required.");
        }
        if (dto.getPrice() == null || dto.getPrice() <= 0) throw new RuntimeException("Price must be greater than 0.");
        if (dto.getMileage() == null || dto.getMileage() < 0) throw new RuntimeException("Mileage cannot be a negative value.");
        if (dto.getQuantity() == null || dto.getQuantity() < 0) throw new RuntimeException("Quantity cannot be less than 0.");
        if (dto.getManufactureYear() == null) throw new RuntimeException("Manufacture year is required.");

        int currentYear = Year.now().getValue();
        boolean isComingSoon = "Coming Soon".equalsIgnoreCase(dto.getStatus());

        if (dto.getManufactureYear() > currentYear && !isComingSoon) {
            throw new RuntimeException("Manufacture year cannot be a future year unless the status is 'Coming Soon'.");
        }
        if (dto.getManufactureYear() < 1886) {
            throw new RuntimeException("Manufacture year is invalid.");
        }
        if ("Available".equalsIgnoreCase(dto.getStatus()) && dto.getQuantity() < 1) {
            throw new RuntimeException("Available vehicles must have at least one unit in stock.");
        }
        if ("Coming Soon".equalsIgnoreCase(dto.getStatus()) && dto.getQuantity() != 0) {
            throw new RuntimeException("Coming Soon vehicles must have 0 units in stock.");
        }
        if ("Sold".equalsIgnoreCase(dto.getStatus()) && dto.getQuantity() != 0) {
            throw new RuntimeException("Sold vehicles must have 0 units in stock.");
        }
        if ("Sold".equalsIgnoreCase(dto.getStatus()) && "Rent".equalsIgnoreCase(dto.getListingType())) {
            throw new RuntimeException("Rental vehicles cannot use the Sold status.");
        }
    }

    @Override
    public VehicleDTO addVehicle(VehicleSubmitDTO dto) {
        validateVehicleRules(dto);
        UploadValidationUtil.validateImageFiles(dto.getImages(), "vehicle", 5, true);
        Vehicle vehicle = new Vehicle();
        mapSubmitDtoToEntity(dto, vehicle);
        vehicle.setListedDate(LocalDate.now().toString());
        saveImagesToVehicle(dto.getImages(), vehicle);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return enrichVehicleDto(savedVehicle, promotionMatchingService.getCurrentlyActivePromotions());
    }

    @Override
    public List<VehicleDTO> getAllVehicles() {
        List<Promotion> activePromotions = promotionMatchingService.getCurrentlyActivePromotions();
        return vehicleRepository.findAllByVisibleTrue().stream()
                .map(vehicle -> enrichVehicleDto(vehicle, activePromotions))
                .collect(Collectors.toList());
    }

    @Override
    public VehicleDTO getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findByIdAndVisibleTrue(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found or this listing is hidden."));
        return enrichVehicleDto(vehicle, promotionMatchingService.getCurrentlyActivePromotions());
    }

    @Override
    public List<VehicleDTO> getAdminVehicles() {
        List<Promotion> activePromotions = promotionMatchingService.getCurrentlyActivePromotions();
        return vehicleRepository.findAll().stream()
                .map(vehicle -> enrichVehicleDto(vehicle, activePromotions))
                .collect(Collectors.toList());
    }

    @Override
    public VehicleDTO getAdminVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));
        return enrichVehicleDto(vehicle, promotionMatchingService.getCurrentlyActivePromotions());
    }

    @Override
    public VehicleDTO updateVehicle(Long id, VehicleSubmitDTO dto) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));
        VehicleSubmitDTO mergedDto = mergeVehicleData(vehicle, dto);

        validateVehicleRules(mergedDto);
        UploadValidationUtil.validateImageFiles(mergedDto.getImages(), "vehicle", 5, false);

        vehicle.setCategory(mergedDto.getCategory());
        vehicle.setQuantity(mergedDto.getQuantity());
        vehicle.setMileage(mergedDto.getMileage());
        vehicle.setEngineCapacity(mergedDto.getEngineCapacity());
        vehicle.setDescription(mergedDto.getDescription());
        vehicle.setPrice(mergedDto.getPrice());
        vehicle.setColor(mergedDto.getColor());

        if (mergedDto.getStatus() != null) vehicle.setStatus(mergedDto.getStatus());
        if (mergedDto.getImages() != null && !mergedDto.getImages().isEmpty() && !mergedDto.getImages().get(0).isEmpty()) {
            saveImagesToVehicle(mergedDto.getImages(), vehicle);
        }
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return enrichVehicleDto(savedVehicle, promotionMatchingService.getCurrentlyActivePromotions());
    }

    @Override
    public VehicleDTO updateVehicleVisibility(Long id, boolean visible) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setVisible(visible);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return enrichVehicleDto(savedVehicle, promotionMatchingService.getCurrentlyActivePromotions());
    }

    // 🔥 UPGRADED: The Cascade Delete Engine
    @Override
    @Transactional // Ensures the entire process succeeds, or rolls back if something fails!
    public void deleteVehicle(Long id) {
        // 1. Verify the vehicle actually exists
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (refundRepository.existsByBookingVehicleId(id)) {
            throw new RuntimeException("This vehicle cannot be deleted because refund records still exist for its bookings. Complete and clear the related refund history first, then remove the vehicle.");
        }

        // 2. Wipe out all attached child records safely
        rentalBookingRepository.deleteByVehicleId(id);
        salesInquiryRepository.deleteByVehicleId(id);
        wishlistRepository.deleteByVehicleId(id);
        reviewRepository.deleteByVehicleId(id);

        // 3. Finally, delete the vehicle itself without triggering Foreign Key errors
        vehicleRepository.delete(vehicle);
    }

    private void mapSubmitDtoToEntity(VehicleSubmitDTO dto, Vehicle vehicle) {
        vehicle.setListingType(dto.getListingType());
        vehicle.setVehicleCondition(dto.getVehicleCondition());
        vehicle.setBrand(dto.getBrand());
        vehicle.setModel(dto.getModel());
        vehicle.setCategory(dto.getCategory());
        vehicle.setManufactureYear(dto.getManufactureYear());
        vehicle.setColor(dto.getColor());
        vehicle.setQuantity(dto.getQuantity());
        vehicle.setMileage(dto.getMileage());
        vehicle.setEngineCapacity(dto.getEngineCapacity());
        vehicle.setFuelType(dto.getFuelType());
        vehicle.setTransmission(dto.getTransmission());
        vehicle.setDescription(dto.getDescription());
        vehicle.setPrice(dto.getPrice());
        vehicle.setStatus(dto.getStatus() != null ? dto.getStatus() : "Available");
    }

    private void saveImagesToVehicle(List<MultipartFile> files, Vehicle vehicle) {
        if (files == null || files.isEmpty()) return;
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            for (int i = 0; i < files.size() && i < 5; i++) {
                MultipartFile file = files.get(i);
                if (!file.isEmpty()) {
                    UploadValidationUtil.validateImageFile(file, "Vehicle image");
                    String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                    Path filePath = Paths.get(UPLOAD_DIR + fileName);
                    Files.write(filePath, file.getBytes());

                    String dbPath = "/uploads/" + fileName;
                    if (i == 0) vehicle.setImage1(dbPath);
                    if (i == 1) vehicle.setImage2(dbPath);
                    if (i == 2) vehicle.setImage3(dbPath);
                    if (i == 3) vehicle.setImage4(dbPath);
                    if (i == 4) vehicle.setImage5(dbPath);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save images: " + e.getMessage());
        }
    }

    private VehicleDTO mapToDTO(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setListingType(vehicle.getListingType());
        dto.setVehicleCondition(vehicle.getVehicleCondition());
        dto.setBrand(vehicle.getBrand());
        dto.setModel(vehicle.getModel());
        dto.setCategory(vehicle.getCategory());
        dto.setManufactureYear(vehicle.getManufactureYear());
        dto.setColor(vehicle.getColor());
        dto.setListedDate(vehicle.getListedDate());
        dto.setQuantity(vehicle.getQuantity());
        dto.setMileage(vehicle.getMileage());
        dto.setEngineCapacity(vehicle.getEngineCapacity());
        dto.setFuelType(vehicle.getFuelType());
        dto.setTransmission(vehicle.getTransmission());
        dto.setDescription(vehicle.getDescription());
        dto.setPrice(vehicle.getPrice());
        dto.setStatus(vehicle.getStatus());
        dto.setVisible(vehicle.isVisible());
        dto.setImage1(vehicle.getImage1());
        dto.setImage2(vehicle.getImage2());
        dto.setImage3(vehicle.getImage3());
        dto.setImage4(vehicle.getImage4());
        dto.setImage5(vehicle.getImage5());
        dto.setEffectivePrice(vehicle.getPrice());
        return dto;
    }

    private VehicleDTO enrichVehicleDto(Vehicle vehicle, List<Promotion> activePromotions) {
        VehicleDTO dto = mapToDTO(vehicle);
        dto.setAppliedPromotion(promotionMatchingService.resolveBestPromotion(vehicle, activePromotions));
        if (dto.getAppliedPromotion() != null) {
            dto.setEffectivePrice(dto.getAppliedPromotion().getDiscountedPrice());
        }
        return dto;
    }

    private VehicleSubmitDTO mergeVehicleData(Vehicle vehicle, VehicleSubmitDTO incomingDto) {
        VehicleSubmitDTO mergedDto = new VehicleSubmitDTO();
        mergedDto.setListingType(hasText(incomingDto.getListingType()) ? incomingDto.getListingType() : vehicle.getListingType());
        mergedDto.setVehicleCondition(hasText(incomingDto.getVehicleCondition()) ? incomingDto.getVehicleCondition() : vehicle.getVehicleCondition());
        mergedDto.setBrand(hasText(incomingDto.getBrand()) ? incomingDto.getBrand() : vehicle.getBrand());
        mergedDto.setModel(hasText(incomingDto.getModel()) ? incomingDto.getModel() : vehicle.getModel());
        mergedDto.setCategory(hasText(incomingDto.getCategory()) ? incomingDto.getCategory() : vehicle.getCategory());
        mergedDto.setManufactureYear(incomingDto.getManufactureYear() != null ? incomingDto.getManufactureYear() : vehicle.getManufactureYear());
        mergedDto.setColor(hasText(incomingDto.getColor()) ? incomingDto.getColor() : vehicle.getColor());
        mergedDto.setQuantity(incomingDto.getQuantity() != null ? incomingDto.getQuantity() : vehicle.getQuantity());
        mergedDto.setMileage(incomingDto.getMileage() != null ? incomingDto.getMileage() : vehicle.getMileage());
        mergedDto.setEngineCapacity(hasText(incomingDto.getEngineCapacity()) ? incomingDto.getEngineCapacity() : vehicle.getEngineCapacity());
        mergedDto.setFuelType(hasText(incomingDto.getFuelType()) ? incomingDto.getFuelType() : vehicle.getFuelType());
        mergedDto.setTransmission(hasText(incomingDto.getTransmission()) ? incomingDto.getTransmission() : vehicle.getTransmission());
        mergedDto.setDescription(hasText(incomingDto.getDescription()) ? incomingDto.getDescription() : vehicle.getDescription());
        mergedDto.setPrice(incomingDto.getPrice() != null ? incomingDto.getPrice() : vehicle.getPrice());
        mergedDto.setStatus(hasText(incomingDto.getStatus()) ? incomingDto.getStatus() : vehicle.getStatus());
        mergedDto.setImages(incomingDto.getImages());
        return mergedDto;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
