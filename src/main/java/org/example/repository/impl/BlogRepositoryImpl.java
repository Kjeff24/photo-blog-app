package org.example.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.model.BlogPost;
import org.example.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.net.URL;
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

    public Optional<BlogPost> findByPhotoId(String photoId) {
        Key key = Key.builder().partitionValue(photoId).build();
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

        Key key = Key.builder()
                .partitionValue(photoId)
                .sortValue(owner)
                .build();

        Optional<BlogPost> blogPost = findByPhotoId(photoId);

        if (blogPost.isEmpty()) {
            return false;
        }

        getTable().deleteItem(key);
        return true;
    }

}
