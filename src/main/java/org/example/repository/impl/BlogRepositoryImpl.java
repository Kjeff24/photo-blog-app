package org.example.repository.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.BlogPostResponse;
import org.example.mapper.BlogPostMapper;
import org.example.model.BlogPost;
import org.example.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlogRepositoryImpl implements BlogRepository {
    private BlogPostMapper blogPostMapper;
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

    public List<BlogPostResponse> findAll() {
        DynamoDbTable<BlogPost> table = getTable();

        DynamoDbIndex<BlogPost> index = table.index("TypeIndex");

        Expression filterExpression = Expression.builder()
                .expression("deleteStatus <> :deletedStatus")
                .expressionValues(Map.of(":deletedStatus", AttributeValue.builder().n("1").build()))
                .build();

        return index.query(r -> r
                        .queryConditional(QueryConditional.keyEqualTo(
                                Key.builder().partitionValue("photo").build()
                        ))
                        .filterExpression(filterExpression))
                .stream()
                .flatMap(page -> page.items().stream())
                .sorted(Comparator.comparing(BlogPost::getUploadDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(blogPostMapper::toBlogPostResponse)
                .toList();
    }




    public List<BlogPostResponse> findAllByUserEmail(String userEmail) {
        DynamoDbIndex<BlogPost> index = getTable().index("OwnerIndex");
        return index.query(r -> r.queryConditional(
                                QueryConditional.keyEqualTo(k -> k.partitionValue(userEmail)))
                        .filterExpression(Expression.builder()
                                .expression("deleteStatus <> :deletedStatus")
                                .expressionValues(Map.of(":deletedStatus", AttributeValue.builder().n("1").build()))
                                .build()))
                .stream()
                .map(Page::items)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(BlogPost::getUploadDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(blogPostMapper::toBlogPostResponse)
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

    public void updateDeleteStatusAndImageKey(String photoId, String owner, int i, String imageKey) {
        Optional<BlogPost> blogPost = findByPhotoIdAndOwner(photoId, owner);

        if(blogPost.isPresent()) {
            blogPost.get().setDeleteStatus(i);
            blogPost.get().setImageKey(imageKey);
            save(blogPost.get());
        }
    }

    public List<BlogPostResponse> findAllByUserAndDeleteStatus(String owner, int i) {
        DynamoDbIndex<BlogPost> index = getTable().index("OwnerIndex");
        return index.query(r -> r.queryConditional(
                                QueryConditional.keyEqualTo(k -> k.partitionValue(owner)))
                        .filterExpression(Expression.builder()
                                .expression("deleteStatus = :deletedStatus")
                                .expressionValues(Map.of(":deletedStatus", AttributeValue.builder().n("1").build()))
                                .build()))
                .stream()
                .map(Page::items)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(BlogPost::getUploadDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(blogPostMapper::toBlogPostResponse)
                .toList();
    }

    private Key getKey(String photoId, String owner) {
        return Key.builder()
                .partitionValue(photoId)
                .sortValue(owner)
                .build();
    }

}
