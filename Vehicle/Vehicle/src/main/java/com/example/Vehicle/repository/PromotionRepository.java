package com.example.Vehicle.repository;

import com.example.Vehicle.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByStatusOrderByIdDesc(String status);
    List<Promotion> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByIdDesc(
            String status, LocalDate startDate, LocalDate endDate);
}
