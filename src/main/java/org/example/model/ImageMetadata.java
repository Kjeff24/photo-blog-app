package org.example.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;


@DynamoDbBean
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageMetadata {
    private String photoId;
    private String owner;
    private String fullName;
    private String imageUrl;
    private String uploadDate;

    @DynamoDbPartitionKey
    public String getPhotoId() {
        return photoId;
    }

    @DynamoDbAttribute(value = "owner")
    @DynamoDbSecondaryPartitionKey(indexNames = "OwnerIndex")
    public String getOwner() {
        return owner;
    }

    @DynamoDbAttribute(value = "fullName")
    public String getFullName() {
        return fullName;
    }

    @DynamoDbAttribute(value = "imageUrl")
    public String getImageUrl() {
        return imageUrl;
    }

    @DynamoDbAttribute(value = "uploadDate")
    public String getUploadDate() {
        return uploadDate;
    }
}
