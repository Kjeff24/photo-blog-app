package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;

public class CognitoPostConfirmationLambda implements RequestHandler<CognitoUserPoolPostConfirmationEvent, CognitoUserPoolPostConfirmationEvent> {
    private final String topicArn;
    private final SnsClient snsClient;

    public CognitoPostConfirmationLambda() {
        snsClient = SnsClient.create();
        topicArn = System.getenv("NOTIFICATION_TOPIC_ARN");
    }

    @Override
    public CognitoUserPoolPostConfirmationEvent handleRequest(CognitoUserPoolPostConfirmationEvent cognitoUserPoolPostConfirmationEvent, Context context) {
        String userEmail = cognitoUserPoolPostConfirmationEvent.getRequest().getUserAttributes().get("email");

        SubscribeRequest request = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("email")
                .endpoint(userEmail)
                .build();

        snsClient.subscribe(request);

        return cognitoUserPoolPostConfirmationEvent;
    }
}
