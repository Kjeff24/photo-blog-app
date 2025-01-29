package org.example.service;

import org.example.model.BlogPost;

public interface SqsService {
    void sendToSQS(String subject, String sendTo, String message);
}
