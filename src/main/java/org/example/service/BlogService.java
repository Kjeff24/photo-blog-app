package org.example.service;

import org.example.dto.BlogPostResponse;

import java.util.List;

public interface BlogService {
    List<BlogPostResponse> findAllBlogPost();

    List<BlogPostResponse> findAllBlogPostByUser(String userEmail);

    void deleteBlogPost(String photoId, String userEmail);

    void moveToOrRestoreFromRecycleBin(String photoId, String userEmail, boolean isMoveToRecycleBin);

    List<BlogPostResponse> findAllRecycleBlogPost(String userEmail);
}
