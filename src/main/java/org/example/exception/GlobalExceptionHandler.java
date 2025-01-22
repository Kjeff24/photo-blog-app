package org.example.exception;


import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import org.example.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InternalServerErrorException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public MessageResponse notAuthorizedExceptionHandler(NotAuthorizedException e) {
        return MessageResponse.builder().message(e.getMessage()).build();
    }
}
