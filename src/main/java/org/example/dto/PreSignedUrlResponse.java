package org.example.dto;

import lombok.Builder;

@Builder
public record PreSignedUrlResponse(String url) {
}
