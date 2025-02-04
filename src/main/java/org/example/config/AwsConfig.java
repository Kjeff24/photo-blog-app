package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsClient;


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

//    @Bean
//    public SfnClient stepFunctionClient() {
//        return SfnClient.create();
//    }

    @Bean
    public LambdaClient getLambdaClient() {
        return LambdaClient.create();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public S3Presigner getPreSigner() {
        return S3Presigner.create();
    }

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.create();
    }


}
