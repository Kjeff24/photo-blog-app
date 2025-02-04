package org.example.service;

import org.example.dto.ImageUploadRequest;
import org.example.dto.PreSignedUrlResponse;
import org.example.model.BlogPost;

public interface S3Service {;
    BlogPost uploadImage(ImageUploadRequest imageUploadRequest, String userEmail, String fullName);

    PreSignedUrlResponse generatePreSignedUrl(String objectKey, String userEmail);

    void moveObject(String sourceKey, String destinationKey);

    void deleteObject(String objectKey);

}
