package org.example.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.model.ImageMetadata;
import org.example.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Service
@RequiredArgsConstructor
public class ImageRepositoryImpl implements ImageRepository {
    @Value("${aws.dynamodb.table}")
    private String tableName;

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    public static final TableSchema<ImageMetadata> TABLE_SCHEMA = TableSchema
            .fromBean(ImageMetadata.class);

    private DynamoDbTable<ImageMetadata> getTable() {
        return dynamoDbEnhancedClient.table(tableName, TABLE_SCHEMA);
    }

    public void save(ImageMetadata metadata) {
        getTable().putItem(metadata);
    }

}
