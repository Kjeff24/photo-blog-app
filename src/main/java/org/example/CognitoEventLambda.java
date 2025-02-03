package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostAuthenticationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPreTokenGenerationEventV2;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final String topicArn;
    private final String taskQueue;
    private final SnsClient snsClient;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    public CognitoEventLambda() {
        snsClient = SnsClient.create();
        sqsClient = SqsClient.create();
        topicArn = System.getenv("NOTIFICATION_TOPIC_ARN");
        taskQueue = System.getenv("TASK_QUEUE");
        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
            } else if ("TokenGeneration_HostedAuth".equals(triggerSource)) {
                CognitoUserPoolPreTokenGenerationEventV2 preTokenEvent = objectMapper.convertValue(event, CognitoUserPoolPreTokenGenerationEventV2.class);
                return objectMapper.convertValue(modifyToken(preTokenEvent), new TypeReference<>() {
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return event;
    }

    private CognitoUserPoolPreTokenGenerationEventV2 modifyToken(CognitoUserPoolPreTokenGenerationEventV2 event) {
        System.out.println("CognitoUserPoolPreTokenGenerationEventV2: " + event);
        Map<String, String> userAttributes = event.getRequest().getUserAttributes();

        if (userAttributes != null && userAttributes.containsKey("name")) {
            String name = userAttributes.get("name");
            Map<String, String> claimsToAddOrOverride = new HashMap<>();
            claimsToAddOrOverride.put("name", name);

            if (event.getResponse() == null) {
                event.setResponse(new CognitoUserPoolPreTokenGenerationEventV2.Response());
            }

            if (event.getResponse().getClaimsAndScopeOverrideDetails() == null) {
                event.getResponse().setClaimsAndScopeOverrideDetails(new CognitoUserPoolPreTokenGenerationEventV2.ClaimsAndScopeOverrideDetails());
            }

            if (event.getResponse().getClaimsAndScopeOverrideDetails().getIdTokenGeneration() == null) {
                event.getResponse().getClaimsAndScopeOverrideDetails().setIdTokenGeneration(
                        new CognitoUserPoolPreTokenGenerationEventV2.IdTokenGeneration()
                );
            }

            event.getResponse().getClaimsAndScopeOverrideDetails().getIdTokenGeneration().setClaimsToAddOrOverride(claimsToAddOrOverride);
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

}
