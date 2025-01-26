package org.example.service;

import org.example.dto.ImageUploadRequest;
import org.example.model.ImageMetadata;

public interface ImageService {;
    ImageMetadata uploadImage(ImageUploadRequest imageUploadRequest);
}
