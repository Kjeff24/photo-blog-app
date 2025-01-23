package org.example.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ImageUploadRequest(
        @Pattern(
                regexp = "^[A-Za-z0-9+/]+={0,2}$",
                message = "Invalid Base64 string."
        )
        String image,
        @Pattern(
                regexp = "^(image/(jpeg|png|gif|bmp|tiff|webp))$",
                message = "Invalid MIME type. Allowed types: image/jpeg, image/png, image/gif, image/bmp, image/tiff, image/webp."
        )
//        @Size(max = 10000, message = "Image size exceeds the allowed limit.")
        String imageType,
        String firstName,
        String lastName,
        String user) {
}
