package org.example.service;

import org.example.dto.ImageUploadRequest;
import org.example.dto.PreSignedUrlResponse;
import org.example.model.BlogPost;

public interface S3Service {;
    BlogPost uploadImage(ImageUploadRequest imageUploadRequest, String userEmail);

    PreSignedUrlResponse generatePreSignedUrl(String objectKey, String userEmail);

    void moveToRecycleBin(String objectKey);

    void deleteObject(String objectKey);

    void deleteFromRecycleBin(String objectKey);
}
