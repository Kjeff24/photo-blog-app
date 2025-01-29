package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.ImageUploadRequest;
import org.example.model.BlogPost;
import org.example.service.ImageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/blog")
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.OK)
    public BlogPost uploadImage(
            @RequestBody ImageUploadRequest imageUploadRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userEmail = jwt.getClaimAsString("email");
        return imageService.uploadImage(imageUploadRequest, userEmail);

    }

    @PostMapping("/generate-url/{objectKey}")
    @ResponseStatus(HttpStatus.OK)
    public BlogPost generatePreSignedUrl(@PathVariable("objectKey") String objectKey) {
        return imageService.generatePreSignedUrl(objectKey);
    }

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> getUserImages(@PathVariable("userId") String userId) {

//        QueryRequest queryRequest = QueryRequest.builder()
//                .tableName(tableName)
//                .keyConditionExpression("UserId = :userId")
//                .expressionAttributeValues(Map.of(":userId", AttributeValue.builder().s(userId).build()))
//                .build();
//
//        Map<String, String> imageUrls = new HashMap<>();
//        dynamoDbClient.query(queryRequest).items().forEach(item -> {
//            String imageId = item.get("ImageId").s();
//            String url = item.get("ImageUrl").s();
//            imageUrls.put(imageId, url);
//        });

        return new HashMap<>();
    }
}
