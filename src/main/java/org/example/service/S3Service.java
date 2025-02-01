package org.example.service;

import org.example.dto.ImageUploadRequest;
import org.example.model.BlogPost;
import org.springframework.security.oauth2.jwt.Jwt;

public interface ImageService {;
    BlogPost uploadImage(ImageUploadRequest imageUploadRequest, String userEmail);

    BlogPost generatePreSignedUrl(String objectKey, String userEmail);

}
