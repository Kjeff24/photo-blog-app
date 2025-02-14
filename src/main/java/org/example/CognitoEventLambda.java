package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostAuthenticationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class CognitoEventLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final String dynamodbTable;
    private final String topicArn;
    private final String taskQueue;
    private final SnsClient snsClient;
    private final SqsClient sqsClient;
    private final DynamoDbClient dynamoDbClient;
    private final ObjectMapper objectMapper;

    public CognitoEventLambda() {
        snsClient = SnsClient.create();
        sqsClient = SqsClient.create();
        dynamoDbClient = DynamoDbClient.create();
        topicArn = System.getenv("NOTIFICATION_TOPIC_ARN");
        taskQueue = System.getenv("TASK_QUEUE");
        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        dynamodbTable = System.getenv("DYNAMODB_TABLE");
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Event: " + event);
        String triggerSource = (String) event.get("triggerSource");

        try {
            if ("PostAuthentication_Authentication".equals(triggerSource)) {
                CognitoUserPoolPostAuthenticationEvent authEvent = objectMapper.convertValue(event, CognitoUserPoolPostAuthenticationEvent.class);
                sendLoginNotification(authEvent);
            } else if ("PostConfirmation_ConfirmSignUp".equals(triggerSource)) {
                CognitoUserPoolPostConfirmationEvent postConfirmationEvent = objectMapper.convertValue(event, CognitoUserPoolPostConfirmationEvent.class);
                subscribeUserToSNS(postConfirmationEvent);
                saveUser(postConfirmationEvent);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return event;
    }

    private void sendLoginNotification(CognitoUserPoolPostAuthenticationEvent event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        String userEmail = event.getRequest().getUserAttributes().get("email");
        String subject = "LOGIN ALERT";
        String message = "A login was detected for your account on " + LocalDateTime.now().format(formatter);

        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        attributes.put("email", MessageAttributeValue.builder().dataType("String").stringValue(userEmail).build());
        attributes.put("subject", MessageAttributeValue.builder().dataType("String").stringValue(subject).build());
        attributes.put("workflowType", MessageAttributeValue.builder().dataType("String").stringValue("publishSNS").build());

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(taskQueue)
                .messageBody(message)
                .messageAttributes(attributes)
                .build();

        sqsClient.sendMessage(sendMessageRequest);
    }

    private void subscribeUserToSNS(CognitoUserPoolPostConfirmationEvent event) {
        String userEmail = event.getRequest().getUserAttributes().get("email");
        SubscribeRequest request = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("email")
                .endpoint(userEmail)
                .build();

        snsClient.subscribe(request);
    }

    private void saveUser(CognitoUserPoolPostConfirmationEvent event) {
        String email = event.getRequest().getUserAttributes().get("email");
        String fullName = event.getRequest().getUserAttributes().get("name");
        try {
            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(dynamodbTable)
                    .item(Map.of(
                            "pk", AttributeValue.builder().s(email).build(),
                            "sk", AttributeValue.builder().s(fullName).build(),
                            "type", AttributeValue.builder().s("user").build())
                    )
                    .conditionExpression("attribute_not_exists(pk)")
                    .build();

            dynamoDbClient.putItem(putItemRequest);
        } catch (ConditionalCheckFailedException e) {
            System.err.println("User already exists: " + email);
        } catch (DynamoDbException e) {
            System.err.println("Error saving user: " + e.getMessage());
        }
    }

}
