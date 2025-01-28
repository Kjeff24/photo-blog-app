package org.example.service;

import org.example.dto.ImageUploadRequest;
import org.example.model.BlogPost;

public interface ImageService {;
    BlogPost uploadImage(ImageUploadRequest imageUploadRequest);

    BlogPost generatePreSignedUrl(String objectKey);

}
