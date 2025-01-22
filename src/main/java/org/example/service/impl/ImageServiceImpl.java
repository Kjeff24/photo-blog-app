package org.example.service.impl;

import jakarta.ws.rs.InternalServerErrorException;
import lombok.RequiredArgsConstructor;
import org.example.dto.ImageUploadRequest;
import org.example.service.ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
    private final S3Client s3Client;
//    private final DynamoDbClient dynamoDbClient;
    @Value("${aws.s3.bucket.staging}")
    private String stagingBucket;
    @Value("${aws.s3.bucket.primary}")
    private String primaryBucket;
    @Value("${aws.dynamodb.table}")
    private String dynamodbTable;
    @Value("${aws.region}")
    private String awsRegion;

    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public String uploadImage(ImageUploadRequest imageUploadRequest) {
        String objectKey = UUID.randomUUID() + "-" + imageUploadRequest.file().getOriginalFilename();

        try {
            // Upload image to S3 staging bucket
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(stagingBucket)
                            .key(objectKey)
                            .build(),
                    RequestBody.fromBytes(imageUploadRequest.file().getBytes()));

            // Process the image
            return processImage(objectKey, imageUploadRequest.user());

        } catch (IOException e) {
            throw new InternalServerErrorException("Image upload failed: " + e.getMessage());
        }
    }

    private String processImage(String objectKey, String user) {
        try {
            // Download the image from S3 staging bucket
            ResponseBytes<GetObjectResponse> s3Object = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(stagingBucket)
                    .key(objectKey).build());

            // Decode bytes into a BufferedImage
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(s3Object.asByteArray()));

            // Add watermark to the image
            String watermarkText = user + " " + user + " - " + dtFormatter.format(LocalDateTime.now());
            BufferedImage watermarkedImage = addWatermark(originalImage, watermarkText);

            // Convert watermarked image back to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(watermarkedImage, "jpg", baos);
            byte[] bytes = baos.toByteArray();

            // Upload the processed image to the primary S3 bucket
            String processedKey = "processed-" + objectKey;
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(primaryBucket)
                            .key(processedKey)
                            .build(),
                    RequestBody.fromBytes(bytes));

            // Save the URL of the processed image in DynamoDB with user-identifiable attributes
            saveImageMetadata(processedKey, user);

            // Delete the original image from the staging bucket
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(stagingBucket)
                    .key(objectKey).build());
            return "https://" + primaryBucket + ".s3." + awsRegion + "amazonaws.com/" + processedKey;
        } catch (IOException | S3Exception e) {
            throw new InternalServerErrorException("Image processing failed: " + e.getMessage());
        }
    }

    private BufferedImage addWatermark(BufferedImage image, String watermarkText) {
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        Font font = new Font("Arial", Font.BOLD, 30);
        g2d.setFont(font);
        g2d.setColor(new Color(255, 0, 0, 40)); // Red with transparency
        g2d.drawString(watermarkText, 10, image.getHeight() - 10);
        g2d.dispose();
        return image;
    }

    private void saveImageMetadata(String imageKey, String user) {
        String imageUrl = "https://" + primaryBucket + ".s3.amazonaws.com/" + imageKey;
        // Use DynamoDB client to save the image URL and user attributes
//        dynamoDbClient.putItem(PutItemRequest.builder()
//                .tableName(dynamodbTable)
//                .item(Map.of(
//                        "userId", AttributeValue.builder().s(user).build(),
//                        "imageUrl", AttributeValue.builder().s(imageUrl).build(),
//                        "uploadDate", AttributeValue.builder().s(LocalDateTime.now().toString()).build()))
//                .build());
    }
}
