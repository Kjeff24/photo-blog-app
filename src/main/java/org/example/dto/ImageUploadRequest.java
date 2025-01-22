package org.example.dto;

import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

@Builder
public record ImageUploadRequest(
        MultipartFile file,
        String firstName,
        String lastName,
        String user
) {
}
