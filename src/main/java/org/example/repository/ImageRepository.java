package org.example.repository;

import org.example.model.ImageMetadata;

public interface ImageRepository {
    void save(ImageMetadata metadata);
}
