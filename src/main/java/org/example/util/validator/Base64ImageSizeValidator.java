package org.example.util.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Base64;

public class Base64ImageSizeValidator implements ConstraintValidator<ImageSize, String> {
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB in bytes

    @Override
    public void initialize(ImageSize constraintAnnotation) {
        // Initialization if needed
    }

    @Override
    public boolean isValid(String base64, ConstraintValidatorContext context) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64);

            return decodedBytes.length <= MAX_SIZE_BYTES;

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 encoded string: " + base64);
        }
    }
}
