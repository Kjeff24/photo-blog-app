package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.ImageUploadRequest;
import org.example.dto.MessageResponse;
import org.example.service.ImageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponse uploadImage(
            @RequestBody ImageUploadRequest imageUploadRequest
    ) {
        String imageUrl = imageService.uploadImage(imageUploadRequest);
        return MessageResponse.builder().message("Image Upload successful: " + imageUrl).build();

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
