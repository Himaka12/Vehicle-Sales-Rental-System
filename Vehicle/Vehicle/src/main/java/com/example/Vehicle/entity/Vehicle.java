package com.example.Vehicle.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vehicles")
@Data
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String listingType;

    @Column(nullable = false)
    private String vehicleCondition;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    private String category;

    @Column(nullable = false)
    private int manufactureYear;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private String listedDate;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false)
    private int mileage;

    @Column(nullable = false)
    private String engineCapacity;

    @Column(nullable = false)
    private String fuelType;

    @Column(nullable = false)
    private String transmission;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private String status = "Available";

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean visible = true;

    private String image1;
    private String image2;
    private String image3;
    private String image4;
    private String image5;
}
