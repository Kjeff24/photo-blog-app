package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.exception.CustomBadRequestException;
import org.example.model.BlogPost;
import org.example.repository.BlogRepository;
import org.example.service.BlogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final BlogRepository blogRepository;

    public List<BlogPost> findAllBlogPost() {
        return blogRepository.findAll();
    }

    public List<BlogPost> findAllBlogPostByUser(String userEmail) {
        return blogRepository.findAllByUserEmail(userEmail);
    }

    public void deleteBlogPost(String photoId, String userEmail) {
        boolean isDeleted = blogRepository.deleteBlogPost(photoId, userEmail);
        if (!isDeleted) {
            throw new CustomBadRequestException("Blog post doest not exits");
        }
    }
}
