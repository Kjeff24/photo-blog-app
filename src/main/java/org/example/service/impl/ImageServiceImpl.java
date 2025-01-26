package org.example.service.impl;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.example.dto.ImageUploadRequest;
import org.example.exception.CustomBadRequestException;
import org.example.model.ImageMetadata;
import org.example.service.ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
    private final S3Client s3Client;
    private final LambdaClient lambdaClient;
    @Value("${aws.s3.bucket.staging}")
    private String stagingBucket;
    @Value("${aws.lambda.function.image-processing-lambda}")
    private String imageProcessingLambda;

    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ImageMetadata uploadImage(ImageUploadRequest request) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(request.imageBase64());
            String mimeType = detectMimeType(imageBytes);
            validateImageType(mimeType);

            String imageFormat = getImageFormat(mimeType);
            String objectKey = UUID.randomUUID() + "." + imageFormat;

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
                    "userId", request.userId(),
                    "fullName", request.fullName()
            );

            return invokeLambda(lambdaEvent);
        } catch (CustomBadRequestException e) {
            throw new CustomBadRequestException("Image upload failed: " + e.getMessage());
        }
    }

    private ImageMetadata invokeLambda(Map<String, String> lambdaEvent) {

        String eventJson = new Gson().toJson(lambdaEvent);

        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(imageProcessingLambda)
                .payload(SdkBytes.fromUtf8String(eventJson))
                .build();

        InvokeResponse invokeResult = lambdaClient.invoke(invokeRequest);

        String responsePayload = invokeResult.payload().asUtf8String();
        System.out.println("Lambda invocation response: " + responsePayload);

        return new Gson().fromJson(responsePayload, ImageMetadata.class);
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

    private String getImageFormat(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg", "imageBase64/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> throw new CustomBadRequestException("Unsupported imageBase64 type: " + mimeType);
        };
    }

}
