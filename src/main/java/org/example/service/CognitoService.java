package org.example.service;

import java.util.Optional;

public interface CognitoService {
    Optional<String> findUserByEmail(String email);
}
