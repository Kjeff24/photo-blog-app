package org.example.repository;

import org.example.model.BlogPost;

import java.util.List;
import java.util.Optional;

public interface BlogRepository {
    void save(BlogPost metadata);

    Optional<BlogPost> findByPhotoIdAndOwner(String photoId, String owner);

    List<BlogPost> findAll();

    List<BlogPost> findAllByUserEmail(String owner);

    boolean deleteBlogPost(String photoId, String owner);

    void updateDeleteStatusAndImageUrl(String photoId, String owner, int i, String imageUrl);

    List<BlogPost> findAllByUserAndDeleteStatus(String owner, int i);
}
