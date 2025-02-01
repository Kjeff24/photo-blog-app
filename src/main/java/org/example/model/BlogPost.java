package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;


@DynamoDbBean
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPost {
    private String photoId;
    private String owner;
    private String fullName;
    private String imageUrl;
    private String uploadDate;
    private int deleteStatus;

    @DynamoDbPartitionKey
    public String getPhotoId() {
        return photoId;
    }

    @DynamoDbSortKey
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

    @DynamoDbAttribute(value = "deleteStatus")
    public int getDeleteStatus() {
        return deleteStatus;
    }
}
