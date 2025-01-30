package org.example.service.impl;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.example.dto.ImageUploadRequest;
import org.example.exception.CustomBadRequestException;
import org.example.exception.CustomNotFoundException;
import org.example.model.BlogPost;
import org.example.repository.BlogRepository;
import org.example.service.CognitoService;
import org.example.service.ImageService;
import org.example.service.SqsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
    private final S3Client s3Client;
    private final LambdaClient lambdaClient;
    private final S3Presigner s3Presigner;
    private final BlogRepository blogRepository;
    private final CognitoService cognitoService;
    @Value("${aws.s3.bucket.staging}")
    private String stagingBucket;
    @Value("${aws.s3.bucket.primary}")
    private String primaryBucket;
    @Value("${aws.lambda.function.image-processing-lambda}")
    private String imageProcessingLambda;

    public BlogPost uploadImage(ImageUploadRequest request, String userEmail) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(request.imageBase64());
            String mimeType = detectMimeType(imageBytes);
            validateImageType(mimeType);

            String objectKey = String.valueOf(UUID.randomUUID());
            String fullName = cognitoService.findUserByEmail(userEmail).orElse(userEmail);

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(stagingBucket)
                            .key(objectKey)
                            .contentType(mimeType)
                            .build(),
                    RequestBody.fromBytes(imageBytes)
            );

            Map<String, String> lambdaEvent = Map.of(
                    "objectKey", objectKey,
                    "bucketName", stagingBucket,
                    "email", userEmail,
                    "fullName", fullName
            );

            return invokeLambda(lambdaEvent);
        } catch (Exception e) {
            throw new CustomBadRequestException("Image upload failed");
        }
    }

    public BlogPost generatePreSignedUrl(String objectKey, String userEmail) {
        BlogPost blogPost = blogRepository.findByPhotoId(objectKey, userEmail).orElseThrow(() -> new CustomNotFoundException("Photo not found"));

        if(LocalDateTime.now().isBefore(LocalDateTime.parse(blogPost.getUploadDate()))) {
            throw new CustomBadRequestException("Temporary Image Url hasn't expired");
        }
        Duration expiration = Duration.ofHours(3);

        GetObjectPresignRequest objectRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(primaryBucket)
                        .key(objectKey)
                        .build())
                .build();

        PresignedGetObjectRequest temporaryAccessUrl = s3Presigner.presignGetObject(objectRequest);
        URL url = temporaryAccessUrl.url();
        blogPost.setTemporaryImageUrl(url.toString());
        blogRepository.save(blogPost);
        return blogPost;
    }


    private BlogPost invokeLambda(Map<String, String> lambdaEvent) {

        String eventJson = new Gson().toJson(lambdaEvent);

        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(imageProcessingLambda)
                .payload(SdkBytes.fromUtf8String(eventJson))
                .build();

        InvokeResponse invokeResult = lambdaClient.invoke(invokeRequest);

        String responsePayload = invokeResult.payload().asUtf8String();
        System.out.println("Lambda invocation response: " + responsePayload);
        if (invokeResult.functionError() != null && !invokeResult.functionError().isEmpty()) {
            throw new CustomBadRequestException();
        }

        return new Gson().fromJson(responsePayload, BlogPost.class);
    }


    private String detectMimeType(byte[] imageBytes) {
        try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            return URLConnection.guessContentTypeFromStream(inputStream);
        } catch (IOException e) {
            throw new CustomBadRequestException("Unable to detect MIME type of the imageBase64.");
        }
    }

    private void validateImageType(String mimeType) {
        if (!mimeType.startsWith("image/")) {
            throw new CustomBadRequestException("Invalid file type. Only images are allowed.");
        }
    }

}
