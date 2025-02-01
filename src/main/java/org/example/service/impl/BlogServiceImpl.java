package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.exception.CustomBadRequestException;
import org.example.model.BlogPost;
import org.example.repository.BlogRepository;
import org.example.service.BlogService;
import org.example.service.S3Service;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final BlogRepository blogRepository;
    private final S3Service s3Service;

    public List<BlogPost> findAllBlogPost() {
        return blogRepository.findAll();
    }

    public List<BlogPost> findAllBlogPostByUser(String userEmail) {
        return blogRepository.findAllByUserEmail(userEmail);
    }

    public void deleteBlogPost(String photoId, String userEmail) {
        s3Service.deleteObject(photoId);
        boolean isDeleted = blogRepository.deleteBlogPost(photoId, userEmail);
        if (!isDeleted) {
            throw new CustomBadRequestException("Blog post doest not exits");
        }
    }

    public void moveToRecycleBin(String photoId, String userEmail) {
        try {
            s3Service.moveToRecycleBin(photoId);
            s3Service.deleteObject(photoId);
            blogRepository.updateDeleteStatus(photoId, userEmail, 1);
        } catch (Exception e) {
            throw new CustomBadRequestException("Failed to move to recycle bin");
        }
    }
}
