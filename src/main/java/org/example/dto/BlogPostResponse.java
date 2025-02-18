package org.example.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BlogPostResponse {
    private String pk;
    private String sk;
    private String fullName;
    private String imageUrl;
    private String uploadDate;
}
