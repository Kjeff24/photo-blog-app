package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.exception.CustomBadRequestException;
import org.example.model.BlogPost;
import org.example.repository.BlogRepository;
import org.example.service.BlogService;
import org.example.service.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final BlogRepository blogRepository;
    private final S3Service s3Service;
    private final String recycleBin = "recycle-bin/";
    @Value("${aws.region}")
    private String awsRegion;
    @Value("${aws.s3.bucket.primary}")
    private String primaryBucket;

    public List<BlogPost> findAllBlogPost() {
        return blogRepository.findAll();
    }

    public List<BlogPost> findAllBlogPostByUser(String userEmail) {
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

    public List<BlogPost> findAllRecycleBlogPost(String userEmail) {
        return blogRepository.findAllByUserAndDeleteStatus(userEmail, 1);
    }

    public void moveToOrRestoreFromRecycleBin(String photoId, String userEmail, boolean isMoveToRecycleBin) {
        try {
            String imageUrl;
            if (isMoveToRecycleBin) {
                // Move to recycle bin
                String destinationKey = recycleBin + photoId;
                imageUrl = "https://" + primaryBucket + ".s3." + awsRegion + ".amazonaws.com/" + destinationKey;
                s3Service.moveObject(photoId, destinationKey);
                s3Service.deleteObject(photoId);
                blogRepository.updateDeleteStatusAndImageUrl(photoId, userEmail, 1, imageUrl);
            } else {
                // Restore from recycle bin
                String sourceKey = recycleBin + photoId;
                imageUrl = "https://" + primaryBucket + ".s3." + awsRegion + ".amazonaws.com/" + sourceKey;
                s3Service.moveObject(sourceKey, photoId);
                s3Service.deleteObject(sourceKey);
                blogRepository.updateDeleteStatusAndImageUrl(photoId, userEmail, 0, imageUrl);
            }
        } catch (Exception e) {
            throw new CustomBadRequestException(isMoveToRecycleBin ? "Failed to move to recycle bin" : "Failed to restore from recycle bin");
        }
    }
}
