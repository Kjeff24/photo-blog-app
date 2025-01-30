package org.example.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.model.BlogPost;
import org.example.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlogRepositoryImpl implements BlogRepository {
    @Value("${aws.dynamodb.table}")
    private String tableName;

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    public static final TableSchema<BlogPost> TABLE_SCHEMA = TableSchema
            .fromBean(BlogPost.class);

    private DynamoDbTable<BlogPost> getTable() {
        return dynamoDbEnhancedClient.table(tableName, TABLE_SCHEMA);
    }

    public void save(BlogPost metadata) {
        getTable().putItem(metadata);
    }

    public Optional<BlogPost> findByPhotoIdAndOwner(String photoId, String owner) {
        Key key = getKey(photoId, owner);
        return Optional.ofNullable(getTable().getItem(r -> r.key(key)));
    }

    public List<BlogPost> findAll() {
        return getTable().scan().items().stream()
                .sorted(Comparator.comparing(BlogPost::getUploadDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<BlogPost> findAllByUserEmail(String userEmail) {
        DynamoDbIndex<BlogPost> index = getTable().index("OwnerIndex");
        return index.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(k -> k.partitionValue(userEmail))))
                .stream()
                .map(Page::items)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(BlogPost::getUploadDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public boolean deleteBlogPost(String photoId, String owner) {
        Optional<BlogPost> blogPost = findByPhotoIdAndOwner(photoId, owner);

        if (blogPost.isEmpty()) {
            return false;
        }

        getTable().deleteItem(getKey(photoId, owner));
        return true;
    }

    private Key getKey(String photoId, String owner) {
        return Key.builder()
                .partitionValue(photoId)
                .sortValue(owner)
                .build();
    }

}
