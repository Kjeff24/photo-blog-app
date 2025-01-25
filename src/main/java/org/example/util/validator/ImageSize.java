package org.example.util.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = Base64ImageSizeValidator.class)
public @interface ImageSize {
    String message() default "Base64 image exceeds the size limit of 5MB.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
