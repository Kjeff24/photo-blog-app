package org.example.service;

import org.example.model.BlogPost;

import java.util.List;

public interface BlogService {
    List<BlogPost> findAllBlogPost();

    List<BlogPost> findAllBlogPostByUser(String userEmail);

    void deleteBlogPost(String photoId, String userEmail);

}
