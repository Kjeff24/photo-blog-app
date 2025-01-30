package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.service.SqsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SqsServiceImpl implements SqsService {
    private final SqsClient sqsClient;
    @Value("${aws.sqs.task}")
    private String taskQueue;


    public void sendToSQS(String subject, String sendTo, String message) {
        String htmlMessage = "<html>" +
                "<body>" +
                "<h1>" + subject + "</h1>" +
                "<p>" + message + "</p>" +
                "</body>" +
                "</html>";

        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        attributes.put("owner", MessageAttributeValue.builder().dataType("String").stringValue(sendTo).build());
        attributes.put("subject", MessageAttributeValue.builder().dataType("String").stringValue(subject).build());
        attributes.put("workflowType", MessageAttributeValue.builder().dataType("String").stringValue("publishSNS").build());

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(taskQueue)
                .messageBody(htmlMessage)
                .messageAttributes(attributes)
                .build();

        sqsClient.sendMessage(sendMessageRequest);
    }
}
