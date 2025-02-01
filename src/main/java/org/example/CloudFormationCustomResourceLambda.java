package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CloudFormationCustomResourceLambda implements RequestHandler<CloudFormationCustomResourceEvent, Void> {
    private final S3Client s3Client;

    public CloudFormationCustomResourceLambda() {
        s3Client = S3Client.create();
    }

    @Override
    public Void handleRequest(CloudFormationCustomResourceEvent event, Context context) {
        String responseUrl = event.getResponseUrl();
        String status = "SUCCESS";
        Map<String, Object> responseData = new HashMap<>();
        try {
            if ("Delete".equalsIgnoreCase(event.getRequestType())) {
                sendResponse(responseUrl, event, context, status, responseData);
                return null;
            }
            String primaryBucket = event.getResourceProperties().get("S3_BUCKET_PRIMARY").toString();
            String folderName = event.getResourceProperties().get("RECYCLE_BIN").toString();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(primaryBucket)
                    .key(folderName)
                    .build();

            s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.empty());

            context.getLogger().log("Recycle bin folder created in bucket: " + primaryBucket);

        } catch (Exception e) {
            status = "FAILED";
            context.getLogger().log("Error: " + e.getMessage());
            responseData.put("Error", e.getMessage());
        }
        sendResponse(responseUrl, event, context, status, responseData);
        return null;
    }

    private void sendResponse(String url, CloudFormationCustomResourceEvent event, Context context, String status, Map<String, Object> data) {
        LambdaLogger logger = context.getLogger();
        ObjectMapper objectMapper = new ObjectMapper();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            Map<String, Object> responseBody = Map.of(
                    "Status", status,
                    "Reason", "See the details in CloudWatch Log Stream: " + context.getLogStreamName(),
                    "PhysicalResourceId", event.getPhysicalResourceId() != null ? event.getPhysicalResourceId() : context.getLogStreamName(),
                    "StackId", event.getStackId(),
                    "RequestId", event.getRequestId(),
                    "LogicalResourceId", event.getLogicalResourceId(),
                    "Data", data
            );

            try {
                StringEntity entity = new StringEntity(objectMapper.writeValueAsString(responseBody));
                HttpPut request = new HttpPut(url);
                request.setEntity(entity);
                request.setHeader("Content-Type", "application/json");

                httpClient.execute(request, response -> {
                    EntityUtils.consume(response.getEntity());
                    logger.log("Response sent to CloudFormation successfully.");
                    return null;
                });
                logger.log("Response sent to CloudFormation successfully.");
            } catch (IOException e) {
                logger.log("Failed to send response to CloudFormation: " + e.getMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
