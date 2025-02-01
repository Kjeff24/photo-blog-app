package org.example.service.impl;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.example.dto.ImageUploadRequest;
import org.example.dto.PreSignedUrlResponse;
import org.example.exception.CustomBadRequestException;
import org.example.exception.CustomNotFoundException;
import org.example.model.BlogPost;
import org.example.repository.BlogRepository;
import org.example.service.CognitoService;
import org.example.service.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
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
public class S3ServiceImpl implements S3Service {
    private final S3Client s3Client;
    private final LambdaClient lambdaClient;
    private final S3Presigner s3Presigner;
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

    public PreSignedUrlResponse generatePreSignedUrl(String objectKey, String userEmail) {
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
        return PreSignedUrlResponse.builder()
                .url(url.toString())
                .build();
    }

    public void moveObject(String sourceKey, String destinationKey) {
        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(primaryBucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(primaryBucket)
                    .destinationKey(destinationKey)
                    .build();
            s3Client.copyObject(copyRequest);
        } catch (S3Exception e) {
            throw new CustomBadRequestException("Failed to move object");
        }
    }

    public void deleteObject(String objectKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(primaryBucket)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
        } catch (S3Exception e) {
            throw new CustomBadRequestException("Failed to move object");
        }
    }

    private BlogPost invokeLambda(Map<String, String> lambdaEvent) {

        String eventJson = new Gson().toJson(lambdaEvent);

        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(imageProcessingLambda)
                .payload(SdkBytes.fromUtf8String(eventJson))
                .build();

        InvokeResponse invokeResult = lambdaClient.invoke(invokeRequest);

        String responsePayload = invokeResult.payload().asUtf8String();
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
