package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Map;

public class NotifyUserLambda implements RequestHandler<Map<String, String>, String> {
    public NotifyUserLambda() {

    }

    public String handleRequest(Map<String, String> event, Context context) {
        String email = event.get("email");
        String fullName = event.get("fullName");

        context.getLogger().log("Email: " + email);
        context.getLogger().log("FullName: " + fullName);
        return "";
    }
}
