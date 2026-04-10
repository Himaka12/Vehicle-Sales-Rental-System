package com.example.Vehicle.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Find the exact absolute path to your uploads folder
        String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

        // Convert it to a Spring-friendly file URL
        String uploadPath = new File(uploadDir).toURI().toString();

        // Map the /uploads/** URL to that physical folder
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}