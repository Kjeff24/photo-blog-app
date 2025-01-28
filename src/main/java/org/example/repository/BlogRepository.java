package org.example.repository;

import org.example.model.BlogPost;

import java.net.URL;
import java.util.Optional;

public interface BlogRepository {
    void save(BlogPost metadata);

    Optional<BlogPost> findByPhotoId(String photoId);
}
