package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.ImageUploadRequest;
import org.example.dto.PreSignedUrlResponse;
import org.example.model.BlogPost;
import org.example.service.BlogService;
import org.example.service.S3Service;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/blog")
public class BlogController {
    private final S3Service s3Service;
    private final BlogService blogService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<BlogPost> getAllBlogPosts() {
        return blogService.findAllBlogPost();
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.OK)
    public BlogPost uploadImage(
            @RequestBody ImageUploadRequest imageUploadRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userEmail = jwt.getClaimAsString("email");
        return s3Service.uploadImage(imageUploadRequest, userEmail);

    }

    @PutMapping("/generate-url/{objectKey}")
    @ResponseStatus(HttpStatus.OK)
    public PreSignedUrlResponse generatePreSignedUrl(@PathVariable("objectKey") String objectKey, @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        return s3Service.generatePreSignedUrl(objectKey, userEmail);
    }


    @GetMapping("/user")
    @ResponseStatus(HttpStatus.OK)
    public List<BlogPost> getBlogPostByUser(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");

        return blogService.findAllBlogPostByUser(userEmail);
    }

    @DeleteMapping("/delete/{photoId}")
    @ResponseStatus(HttpStatus.OK)
    public void deletePost(@PathVariable("photoId") String photoId, @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        blogService.deleteBlogPost(photoId, userEmail);
    }

    @DeleteMapping("/recycle/{photoId}")
    @ResponseStatus(HttpStatus.OK)
    public void moveToRecycleBin(@PathVariable("photoId") String photoId, @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        blogService.moveToRecycleBin(photoId, userEmail);
    }
}
