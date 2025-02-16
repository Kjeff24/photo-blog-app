package org.example.dto;

import lombok.Builder;

@Builder
public record BlogPostResponse(
        String pk,
        String sk,
        String type,
        String fullName,
        String imageUrl,
        String uploadDate,
        int deleteStatus
) {
}
