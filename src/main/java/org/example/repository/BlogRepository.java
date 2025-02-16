package org.example.repository;

import org.example.dto.BlogPostResponse;
import org.example.model.BlogPost;

import java.util.List;
import java.util.Optional;

public interface BlogRepository {
    void save(BlogPost metadata);

    Optional<BlogPost> findByPhotoIdAndOwner(String photoId, String owner);

    List<BlogPostResponse> findAll();

    List<BlogPostResponse> findAllByUserEmail(String owner);

    boolean deleteBlogPost(String photoId, String owner);

    void updateDeleteStatusAndImageKey(String photoId, String owner, int i, String imageKey);

    List<BlogPostResponse> findAllByUserAndDeleteStatus(String owner, int i);
}
