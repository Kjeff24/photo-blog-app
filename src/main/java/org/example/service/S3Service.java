package org.example.service;

import org.example.dto.BlogPostResponse;
import org.example.dto.ImageUploadRequest;
import org.example.dto.PreSignedUrlResponse;

public interface S3Service {
    BlogPostResponse uploadImage(ImageUploadRequest imageUploadRequest, String userEmail, String fullName);

    PreSignedUrlResponse generatePreSignedUrl(String objectKey, String userEmail);

    void moveObject(String sourceKey, String destinationKey);

    void deleteObject(String objectKey);

}
