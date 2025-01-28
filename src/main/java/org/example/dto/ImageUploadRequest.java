package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.example.util.validator.ImageSize;

public record ImageUploadRequest(
        @Pattern(
                regexp = "^[A-Za-z0-9+/]+={0,2}$",
                message = "Invalid Base64 string.")
        @NotBlank(message = "Base64 string is required")
        @ImageSize(message = "Image limit size is 5MB")
        String imageBase64,
        String fullName,
        String email) {
}
