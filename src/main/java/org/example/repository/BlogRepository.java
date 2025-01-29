package org.example.repository;

import org.example.model.BlogPost;

import java.util.List;
import java.util.Optional;

public interface BlogRepository {
    void save(BlogPost metadata);

    Optional<BlogPost> findByPhotoId(String photoId);

    List<BlogPost> findAll();

    List<BlogPost> findAllByUserEmail(String userEmail);

    boolean deleteBlogPost(String photoId, String userEmail);
}
