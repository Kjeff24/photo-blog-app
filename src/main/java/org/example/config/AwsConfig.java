package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Bean
    public S3Client getS3Client() {
        return S3Client.create();
    }

    @Bean
    public DynamoDbClient getDynamoDbClient() {
        return DynamoDbClient.create();
    }
}
