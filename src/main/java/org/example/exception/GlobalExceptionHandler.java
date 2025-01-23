package org.example.exception;


import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import org.example.dto.MessageResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InternalServerErrorException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public MessageResponse notAuthorizedExceptionHandler(InternalServerErrorException e) {
        return MessageResponse.builder().message(e.getMessage()).build();
    }

    @ExceptionHandler(CustomBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponse customBadRequestExceptionHandler(CustomBadRequestException e) {
        return MessageResponse.builder().message(e.getMessage()).build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponse illegalArgumentExceptionHandler(IllegalArgumentException e) {
        return MessageResponse.builder().message(e.getMessage()).build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<String>> validationErrorsHandler(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage).collect(Collectors.toList());
        return new ResponseEntity<>(errors, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }
}
