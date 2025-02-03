package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.Subscription;

import java.util.HashMap;
import java.util.Map;

public class SQSEventLambda implements RequestHandler<SQSEvent, Void> {
    private final SnsClient snsClient;
    private final String notificationTopicArn;

    public SQSEventLambda() {
        snsClient = SnsClient.create();
        notificationTopicArn = System.getenv("NOTIFICATION_TOPIC_ARN");
    }

    @Override
    public Void handleRequest(SQSEvent sqsEvent, Context context) {
        for (SQSEvent.SQSMessage message : sqsEvent.getRecords()) {
            String workflowType = message.getMessageAttributes().get("workflowType").getStringValue();

            if ("publishSNS".equals(workflowType)) {
                handlePublishSNS(message, context);
            } else {
                context.getLogger().log("Unknown workflowType: " + workflowType);
            }
        }
        return null;
    }

    private void handlePublishSNS(SQSEvent.SQSMessage message, Context context) {
        context.getLogger().log("Processing PublishToSNS event: " + message);

        String userEmail = message.getMessageAttributes().get("email").getStringValue();
        String subject = message.getMessageAttributes().get("subject").getStringValue();
        String messageBody = message.getBody();

        sendSNSNotification(userEmail, messageBody, subject);
    }

    private void sendSNSNotification(String userEmail, String message, String subject) {
        Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();

        if (userEmail != null && !userEmail.isEmpty()) {
            messageAttributes.put("userEmail", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(userEmail)
                    .build());
        }

        addSubscriptionFilter(userEmail);

        PublishRequest publishRequest = PublishRequest.builder()
                .subject(subject)
                .topicArn(notificationTopicArn)
                .message(message)
                .messageAttributes(messageAttributes)
                .build();

        snsClient.publish(publishRequest);
    }

    private void addSubscriptionFilter(String userEmail) {
        ListSubscriptionsByTopicResponse subscriptionsResponse = snsClient.listSubscriptionsByTopic(
                ListSubscriptionsByTopicRequest.builder()
                        .topicArn(notificationTopicArn)
                        .build()
        );

        for (Subscription subscription : subscriptionsResponse.subscriptions()) {
            String subscriptionArn = subscription.subscriptionArn();

            if ("PendingConfirmation".equals(subscriptionArn) || "Deleted".equals(subscriptionArn)) {
                continue;
            }

            String endpoint = snsClient.getSubscriptionAttributes(
                    GetSubscriptionAttributesRequest.builder()
                            .subscriptionArn(subscriptionArn)
                            .build()
            ).attributes().get("Endpoint");

            String filterPolicy;

            if (userEmail.equals(endpoint)) {
                filterPolicy = String.format(String.format("{\"userEmail\": [\"%s\"]}", userEmail));
            } else {
                filterPolicy = "{\"userEmail\": [\"none\"]}";
            }

            snsClient.setSubscriptionAttributes(
                    SetSubscriptionAttributesRequest.builder()
                            .subscriptionArn(subscriptionArn)
                            .attributeName("FilterPolicy")
                            .attributeValue(filterPolicy)
                            .build()
            );

        }
    }
}
