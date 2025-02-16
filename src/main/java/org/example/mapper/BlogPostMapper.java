package org.example.mapper;

import org.example.dto.BlogPostResponse;
import org.example.model.BlogPost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BlogPostMapper {
    @Value("${aws.region}")
    private String region;
    @Value("${aws.s3.bucket.primary}")
    private String bucket;

    public BlogPostResponse toBlogPostResponse(BlogPost blogPost) {
        return BlogPostResponse.builder()
                .pk(blogPost.getPk())
                .sk(blogPost.getSk())
                .type(blogPost.getType())
                .fullName(blogPost.getFullName())
                .imageUrl("https://" + bucket + ".s3." + region + ".amazonaws.com/" + blogPost.getImageKey())
                .deleteStatus(blogPost.getDeleteStatus())
                .uploadDate(blogPost.getUploadDate())
                .build();
    }
}
