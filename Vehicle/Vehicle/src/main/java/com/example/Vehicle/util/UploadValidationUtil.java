package com.example.Vehicle.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class UploadValidationUtil {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    private UploadValidationUtil() {
    }

    public static void validateImageFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException(fieldName + " is required.");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new RuntimeException(fieldName + " must be 5 MB or smaller.");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        boolean validType = contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
        boolean validExtension = originalFilename != null && ALLOWED_IMAGE_EXTENSIONS.stream()
                .anyMatch(extension -> originalFilename.toLowerCase(Locale.ROOT).endsWith(extension));

        if (!validType && !validExtension) {
            throw new RuntimeException(fieldName + " must be a JPG, PNG, WEBP, or GIF image.");
        }
    }

    public static void validateImageFiles(List<MultipartFile> files, String fieldName, int maxFiles, boolean requireAtLeastOne) {
        if (files == null || files.isEmpty() || files.stream().allMatch(file -> file == null || file.isEmpty())) {
            if (requireAtLeastOne) {
                throw new RuntimeException("At least one " + fieldName + " image is required.");
            }
            return;
        }

        long nonEmptyFileCount = files.stream().filter(file -> file != null && !file.isEmpty()).count();
        if (nonEmptyFileCount > maxFiles) {
            throw new RuntimeException("You can upload a maximum of " + maxFiles + " " + fieldName + " images.");
        }

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                validateImageFile(file, fieldName + " image");
            }
        }
    }
}
