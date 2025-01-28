package org.example.exception;

public class CustomBadRequestException extends RuntimeException{
    public CustomBadRequestException(){}
    public CustomBadRequestException(String message) {
        super(message);
    }
}
