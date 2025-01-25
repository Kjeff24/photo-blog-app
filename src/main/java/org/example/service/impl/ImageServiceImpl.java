package org.example.service.impl;

import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import org.example.dto.ImageUploadRequest;
import org.example.service.ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
    private final S3Client s3Client;
    //    private final DynamoDbClient dynamoDbClient;
    @Value("${aws.s3.bucket.staging}")
    private String stagingBucket;
    @Value("${aws.s3.bucket.primary}")
    private String primaryBucket;
    //    @Value("${aws.dynamodb.table}")
//    private String dynamodbTable;
    @Value("${aws.region}")
    private String awsRegion;

    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public String uploadImage(ImageUploadRequest request) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(request.image());
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

            return processImage(objectKey, imageBytes, imageFormat, request.user());
        } catch (IllegalArgumentException e) {
            throw new InternalServerErrorException("Image upload failed: " + e.getMessage());
        }
    }

    private String processImage(String objectKey, byte[] imageBytes, String fileFormat, String user) {
        try {
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

            String watermarkText = user + " - " + dtFormatter.format(LocalDateTime.now());
            BufferedImage watermarkedImage = addWatermark(originalImage, watermarkText);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(watermarkedImage, fileFormat, outputStream);
            byte[] watermarkedBytes = outputStream.toByteArray();

            String processedKey = "processed-" + objectKey;
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(primaryBucket)
                            .key(processedKey)
                            .build(),
                    RequestBody.fromBytes(watermarkedBytes));

            saveImageMetadata(processedKey, user);

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(stagingBucket)
                    .key(objectKey).build());

            return generateS3Url(primaryBucket, processedKey);
        } catch (IOException e) {
            throw new InternalServerErrorException("Image processing failed: " + e.getMessage());
        }
    }


    private BufferedImage addWatermark(BufferedImage image, String watermarkText) {
        Graphics2D g2d = image.createGraphics();
        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        g2d.setColor(new Color(255, 0, 0, 40)); // Red with transparency
        g2d.drawString(watermarkText, 10, image.getHeight() - 10);
        g2d.dispose();
        return image;
    }

    private String detectMimeType(byte[] imageBytes) {
        try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            return URLConnection.guessContentTypeFromStream(inputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to detect MIME type of the image.");
        }
    }

    private void validateImageType(String mimeType) {
        if (!mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("Invalid file type. Only images are allowed.");
        }
    }

    private String getImageFormat(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("Unsupported image type: " + mimeType);
        };
    }

    private String generateS3Url(String bucket, String key) {
        return "https://" + bucket + ".s3." + awsRegion + ".amazonaws.com/" + key;
    }

    private void saveImageMetadata(String imageKey, String user) {
        String imageUrl = "https://" + primaryBucket + ".s3." + awsRegion + ".amazonaws.com/" + imageKey;

        // Example: Print metadata to logs (replace with actual database saving logic)
        System.out.println("Saving image metadata:");
        System.out.println("User: " + user);
        System.out.println("Image URL: " + imageUrl);
        System.out.println("Upload Time: " + LocalDateTime.now());

        // If using DynamoDB or another database:
    /*
    dynamoDbClient.putItem(PutItemRequest.builder()
            .tableName(dynamodbTable)
            .item(Map.of(
                    "userId", AttributeValue.builder().s(user).build(),
                    "imageUrl", AttributeValue.builder().s(imageUrl).build(),
                    "uploadDate", AttributeValue.builder().s(LocalDateTime.now().toString()).build()))
            .build());
    */
    }
}
