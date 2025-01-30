package org.example.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;


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
    private String temporaryImageUrl;
    private String uploadDate;

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

    @DynamoDbAttribute(value = "temporaryImageUrl")
    public String getTemporaryImageUrl() {
        return temporaryImageUrl;
    }

    @DynamoDbAttribute(value = "uploadDate")
    public String getUploadDate() {
        return uploadDate;
    }
}
