package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.service.CognitoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CognitoServiceImpl implements CognitoService {
    private final CognitoIdentityProviderClient cognitoIdentityProviderClient;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;


    public Optional<String> findUserByEmail(String email) {
        ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                .userPoolId(userPoolId)
                .filter("email = \"" + email + "\"")
                .build();

        ListUsersResponse listUsersResponse = cognitoIdentityProviderClient.listUsers(listUsersRequest);

        if (!listUsersResponse.users().isEmpty()) {
            String username = listUsersResponse.users().get(0).username();
            System.out.println(username);

            AdminGetUserRequest adminGetUserRequest = AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build();

            return cognitoIdentityProviderClient.adminGetUser(adminGetUserRequest)
                    .userAttributes().stream()
                    .filter(attribute -> "name".equals(attribute.name()))
                    .findFirst()
                    .map(AttributeType::value);
        }

        return Optional.empty();
    }
}
