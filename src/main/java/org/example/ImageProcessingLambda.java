package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class ImageProcessingLambda implements RequestHandler<Map<String, String>, String> {

    private final SfnClient sfnClient;
    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    //    private final String stagingBucket;
    private final String primaryBucket;
    private final String awsRegion;
    private final String stepFunctionArn;
    private final String dynamodbTable;

    public ImageProcessingLambda() {
        sfnClient = SfnClient.create();
        s3Client = S3Client.create();
        dynamoDbClient = DynamoDbClient.create();
//        stagingBucket = System.getenv("STAGING_BUCKET");
        primaryBucket = System.getenv("S3_BUCKET_PRIMARY");
        stepFunctionArn = System.getenv("STEP_FUNCTION_ARN");
        awsRegion = System.getenv("AWS_REGION");
        dynamodbTable = System.getenv("DYNAMODB_TABLE");
    }

    public String handleRequest(Map<String, String> event, Context context) {
        String bucketName = event.get("bucketName");
        String objectKey = event.get("objectKey");
        String userId = event.get("userId");
        String fullName = event.get("fullName");

        context.getLogger().log("BucketName : " + bucketName);
        context.getLogger().log("ObjectKey : " + objectKey);
        context.getLogger().log("UserId : " + userId);
        context.getLogger().log("Full name : " + fullName);

        try {

            ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey).build());
            context.getLogger().log("Get input stream " + inputStream.toString());

            HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            context.getLogger().log(headObjectResponse.toString());
            String mimeType = headObjectResponse.contentType();
            context.getLogger().log("mimeType : " + mimeType);
            String imageFormat = getImageFormatFromMimeType(mimeType);
            context.getLogger().log("imageFormat : " + imageFormat);

            InputStream processedImage = processImageWithWatermark(inputStream, fullName, imageFormat);

            String processedObjectKey = UUID.randomUUID() + imageFormat;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(primaryBucket)
                    .key(processedObjectKey)
                    .contentType(mimeType)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(processedImage, processedImage.available()));
            context.getLogger().log("https://" + primaryBucket + ".s3." + awsRegion + ".amazonaws.com/" + processedObjectKey);
            saveImageUrlToDynamoDb(processedObjectKey, userId, fullName);

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());

        } catch (Exception e) {
            context.getLogger().log("Error occurred while processing image" + e.getMessage());
//            invokeStepFunction(event);
        }
        return "";
    }

    private String getImageFormatFromMimeType(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg", "imageBase64/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/bmp" -> "bmp";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("Unsupported imageBase64 MIME type: " + mimeType);
        };
    }

    public InputStream processImageWithWatermark(InputStream inputStream, String fullName, String imageFormat) throws IOException, IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);
        int fontSize = originalImage.getWidth() / 10;

        Graphics2D graphics = (Graphics2D) originalImage.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Font font = new Font("Arial", Font.PLAIN, fontSize);
        graphics.setFont(font);

        FontMetrics fontMetrics = graphics.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(fullName);
        int textHeight = fontMetrics.getHeight();

        int x = (originalImage.getWidth() - textWidth) / 2;
        int y = (originalImage.getHeight() + textHeight) / 2;

        Color backgroundColor = new Color(originalImage.getRGB(Math.max(0, x), Math.max(0, y - textHeight / 2)));
        Color fontColor = getWatermarkColor(backgroundColor);

        graphics.setColor(fontColor);
        graphics.drawString(fullName, x, y);
        graphics.dispose();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(originalImage, imageFormat, byteArrayOutputStream);

        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }

    private static Color getWatermarkColor(Color backgroundColor) {
        int brightness = (int) Math.sqrt(0.299 * Math.pow(backgroundColor.getRed(), 2) +
                0.587 * Math.pow(backgroundColor.getGreen(), 2) +
                0.114 * Math.pow(backgroundColor.getBlue(), 2));
        return brightness > 128 ? new Color(0, 0, 0, 75) : new Color(255, 255, 255, 75);
    }

    private void saveImageUrlToDynamoDb(String imageKey, String userId, String fullName) {
        String imageUrl = "https://" + primaryBucket + ".s3." + awsRegion + ".amazonaws.com/" + imageKey;

        System.out.println("Saving imageBase64 metadata:");

        System.out.println("User: " + userId);
        System.out.println("Image URL: " + imageUrl);
        System.out.println("Upload Time: " + LocalDateTime.now());

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(dynamodbTable)
                .item(Map.of(
                        "photoId", AttributeValue.builder().s(imageKey).build(),
                        "owner", AttributeValue.builder().s(userId).build(),
                        "imageUrl", AttributeValue.builder().s(imageUrl).build(),
                        "uploadDate", AttributeValue.builder().s(LocalDateTime.now().toString()).build()))
                .build());
    }

    private void invokeStepFunction(Map<String, String> events) {
        events.put("workflowType", "image-processing-retry");

        String payload = new Gson().toJson(events);
        StartExecutionRequest startExecutionRequest = StartExecutionRequest.builder()
                .stateMachineArn(stepFunctionArn)
                .input(payload)
                .build();

        StartExecutionResponse result = sfnClient.startExecution(startExecutionRequest);
        System.out.println("Step Function started with Execution ARN: " + result.executionArn());
        System.out.println("Started step function");
    }
}
