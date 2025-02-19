package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.BlogPostResponse;
import org.example.exception.CustomBadRequestException;
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
    private final String recycleBin = "recycle-bin/";

    public List<BlogPostResponse> findAllBlogPost() {
        return blogRepository.findAll();
    }

    public List<BlogPostResponse> findAllBlogPostByUser(String userEmail) {
        return blogRepository.findAllByUserEmail(userEmail);
    }

    public void deleteBlogPost(String photoId, String userEmail) {
        String objectKey = recycleBin + photoId;
        s3Service.deleteObject(objectKey);
        boolean isDeleted = blogRepository.deleteBlogPost(photoId, userEmail);
        if (!isDeleted) {
            throw new CustomBadRequestException("Blog post doest not exits");
        }
    }

    public List<BlogPostResponse> findAllRecycleBlogPost(String userEmail) {
        return blogRepository.findAllByUserAndDeleteStatus(userEmail, 1);
    }

    public void moveToOrRestoreFromRecycleBin(String photoId, String userEmail, boolean isMoveToRecycleBin) {
        try {
            String imageUrl;
            if (isMoveToRecycleBin) {
                System.out.println("Move to recycle bin");
                // Move to recycle bin
                String destinationKey = recycleBin + photoId;
                s3Service.moveObject(photoId, destinationKey);
                s3Service.deleteObject(photoId);
                blogRepository.updateDeleteStatusAndImageKey(photoId, userEmail, 1, destinationKey);
            } else {
                System.out.println("Move from recycle bin");
                // Restore from recycle bin
                String sourceKey = recycleBin + photoId;
                s3Service.moveObject(sourceKey, photoId);
                s3Service.deleteObject(sourceKey);
                blogRepository.updateDeleteStatusAndImageKey(photoId, userEmail, 0, photoId);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new CustomBadRequestException(isMoveToRecycleBin ? "Failed to move to recycle bin" : "Failed to restore from recycle bin");
        }
    }
}
