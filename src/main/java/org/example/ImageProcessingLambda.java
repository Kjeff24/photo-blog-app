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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ImageProcessingLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final SfnClient sfnClient;
    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private final SqsClient sqsClient;
    private final String primaryBucket;
    private final String awsRegion;
    private final String stepFunctionArn;
    private final String dynamodbTable;
    private final String taskQueue;

    public ImageProcessingLambda() {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("user.fontconfig.cache", "/tmp/.fontconfig");
        sfnClient = SfnClient.create();
        s3Client = S3Client.create();
        dynamoDbClient = DynamoDbClient.create();
        sqsClient = SqsClient.create();
        primaryBucket = System.getenv("S3_BUCKET_PRIMARY");
        stepFunctionArn = System.getenv("STEP_FUNCTION_ARN");
        awsRegion = System.getenv("AWS_REGION");
        dynamodbTable = System.getenv("DYNAMODB_TABLE");
        taskQueue = System.getenv("TASK_QUEUE");
    }

    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Events: " + event.toString());
        String bucketName = (String) event.get("bucketName");
        String objectKey = (String) event.get("objectKey");
        String email = (String) event.get("email");
        String fullName = (String) event.get("fullName");
        int retryAttempt = event.get("retryAttempt") != null ? Integer.parseInt(event.get("retryAttempt").toString()) + 1 : 1;

        Map<String, Object> response;

        try {
            ResponseInputStream<GetObjectResponse> inputStream = fetchInputStream(bucketName, objectKey);

            String mimeType = fetchMimeType(bucketName, objectKey);

            String imageFormat = getImageFormatFromMimeType(mimeType);
            InputStream processedImage = processImageWithWatermark(inputStream, fullName, imageFormat);

            uploadProcessedImage(objectKey, mimeType, processedImage);

            response = saveImageUrlToDynamoDb(objectKey, email, fullName);

            deleteOriginalImage(bucketName, objectKey);

        } catch (Exception e) {
            context.getLogger().log("Error occurred while processing image" + e.getMessage());
            handleRetryOrFailure(event, bucketName, objectKey, fullName, email, retryAttempt);
            throw new RuntimeException("ImageProcessingFailed: " + e.getMessage());
        }
        return response;
    }

    private void sendToSQS(String fullName, String email) {
        String subject = "IMAGE UPLOAD FAILED";
        String message = "Hi " +
                fullName +
                ", " +
                "\nThe image you tried to upload failed.";

        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        attributes.put("email", MessageAttributeValue.builder().dataType("String").stringValue(email).build());
        attributes.put("subject", MessageAttributeValue.builder().dataType("String").stringValue(subject).build());
        attributes.put("workflowType", MessageAttributeValue.builder().dataType("String").stringValue("publishSNS").build());

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(taskQueue)
                .messageBody(message)
                .messageAttributes(attributes)
                .build();

        sqsClient.sendMessage(sendMessageRequest);
    }

    private ResponseInputStream<GetObjectResponse> fetchInputStream(String bucketName, String objectKey) {
        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());
    }

    private String fetchMimeType(String bucketName, String objectKey) {
        HeadObjectResponse headResponse = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());
        return headResponse.contentType();
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

    public InputStream processImageWithWatermark(InputStream inputStream, String fullName, String imageFormat) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);

        Graphics2D graphics = (Graphics2D) originalImage.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int fontSize = originalImage.getWidth() / 20;
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
        graphics.setFont(font);

        FontMetrics fontMetrics = graphics.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(fullName);
        int textHeight = fontMetrics.getHeight();

        int x = originalImage.getWidth() - textWidth - 10;
        int y = originalImage.getHeight() - textHeight + fontMetrics.getAscent();

        Color fontColor = new Color(255, 255, 255, 128);
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

    private void uploadProcessedImage(String objectKey, String mimeType, InputStream processedImage) throws IOException {
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(primaryBucket)
                        .key(objectKey)
                        .contentType(mimeType)
                        .build(),
                RequestBody.fromInputStream(processedImage, processedImage.available()));
    }

    private Map<String, Object> saveImageUrlToDynamoDb(String imageKey, String email, String fullName) {
        String imageUrl = "https://" + primaryBucket + ".s3." + awsRegion + ".amazonaws.com/" + imageKey;

        String uploadDate = LocalDateTime.now().toString();

        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(dynamodbTable)
                .item(Map.of(
                        "photoId", AttributeValue.builder().s(imageKey).build(),
                        "owner", AttributeValue.builder().s(email).build(),
                        "fullName", AttributeValue.builder().s(fullName).build(),
                        "imageUrl", AttributeValue.builder().s(imageUrl).build(),
                        "uploadDate", AttributeValue.builder().s(uploadDate).build(),
                        "deleteStatus", AttributeValue.builder().n("0").build())
                )
                .build();

        dynamoDbClient.putItem(putItemRequest);

        Map<String, Object> imageMetadata = new HashMap<>();
        imageMetadata.put("photoId", imageKey);
        imageMetadata.put("owner", email);
        imageMetadata.put("fullName", fullName);
        imageMetadata.put("imageUrl", imageUrl);
        imageMetadata.put("uploadDate", uploadDate);

        return imageMetadata;
    }

    private void deleteOriginalImage(String bucketName, String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());
    }

    private void handleRetryOrFailure(Map<String, Object> event, String bucketName, String objectKey, String fullName, String email, int retryAttempt) {
        if (retryAttempt == 1) {
            sendToSQS(fullName, email);
        }

        if (retryAttempt <= 2) {
            invokeStepFunction(event, retryAttempt);
        }

        if (retryAttempt == 3) {
            deleteOriginalImage(bucketName, objectKey);
        }
    }

    private void invokeStepFunction(Map<String, Object> events, int retryAttempt) {
        events.put("workflowType", "image-processing-retry");
        events.put("retryAttempt", retryAttempt);
        String payload = new Gson().toJson(events);
        StartExecutionRequest startExecutionRequest = StartExecutionRequest.builder()
                .stateMachineArn(stepFunctionArn)
                .input(payload)
                .build();

        sfnClient.startExecution(startExecutionRequest);
    }
}
