package com.travel.ai.config;

import com.travel.ai.store.InMemoryVectorStoreImpl;
import com.travel.ai.store.MilvusVectorStoreImpl;
import com.travel.ai.store.VectorStore;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for Milvus vector database integration.
 * Creates VectorStore beans with automatic fallback from Milvus to in-memory.
 */
@Slf4j
@Configuration
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Value("${milvus.collection-name:travel_knowledge}")
    private String collectionName;

    @Value("${milvus.dimension:768}")
    private int dimension;

    /**
     * Create a MilvusClientV2 bean when milvus.enabled=true.
     * Uses URI format: http://host:port
     */
    @Bean
    @ConditionalOnProperty(name = "milvus.enabled", havingValue = "true")
    public MilvusClientV2 milvusClient() {
        String uri = "http://" + host + ":" + port;
        ConnectConfig config = ConnectConfig.builder()
                .uri(uri)
                .build();

        MilvusClientV2 client = new MilvusClientV2(config);
        log.info("Milvus client created: {}", uri);
        return client;
    }

    /**
     * Create a Milvus-backed VectorStore when milvus.enabled=true.
     */
    @Bean
    @ConditionalOnProperty(name = "milvus.enabled", havingValue = "true")
    public VectorStore milvusVectorStore(MilvusClientV2 milvusClient) {
        ensureCollectionExists(milvusClient);
        return new MilvusVectorStoreImpl(milvusClient, collectionName, dimension);
    }

    /**
     * Create an in-memory VectorStore as fallback when Milvus is not enabled.
     */
    @Bean
    @ConditionalOnProperty(name = "milvus.enabled", havingValue = "false",
            matchIfMissing = true)
    public VectorStore inMemoryVectorStore() {
        log.info("Milvus not enabled, using in-memory vector store");
        return new InMemoryVectorStoreImpl();
    }

    /**
     * Ensure the Milvus collection exists with the correct schema.
     * Creates it if it does not already exist.
     */
    private void ensureCollectionExists(MilvusClientV2 client) {
        try {
            HasCollectionReq hasReq = HasCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();

            Boolean exists = client.hasCollection(hasReq);

            if (exists == null || !exists) {
                createCollection(client);
            } else {
                log.info("Milvus collection '{}' already exists", collectionName);
            }
        } catch (Exception e) {
            log.warn("Failed to check/create Milvus collection: {}", e.getMessage());
        }
    }

    private void createCollection(MilvusClientV2 client) {
        String vectorFieldName = "vector";

        CreateCollectionReq.CollectionSchema schema = client.createSchema();

        // Primary key field
        schema.addField(AddFieldReq.builder()
                .fieldName("chunk_id")
                .dataType(DataType.VarChar)
                .maxLength(256)
                .isPrimaryKey(true)
                .build());

        // Vector field
        schema.addField(AddFieldReq.builder()
                .fieldName(vectorFieldName)
                .dataType(DataType.FloatVector)
                .dimension(dimension)
                .build());

        // Metadata fields
        schema.addField(AddFieldReq.builder()
                .fieldName("doc_id").dataType(DataType.VarChar).maxLength(256).build());
        schema.addField(AddFieldReq.builder()
                .fieldName("doc_title").dataType(DataType.VarChar).maxLength(512).build());
        schema.addField(AddFieldReq.builder()
                .fieldName("content").dataType(DataType.VarChar).maxLength(65535).build());
        schema.addField(AddFieldReq.builder()
                .fieldName("category").dataType(DataType.VarChar).maxLength(128).build());
        schema.addField(AddFieldReq.builder()
                .fieldName("chunk_index").dataType(DataType.Int32).build());

        // Index for vector field
        IndexParam indexParam = IndexParam.builder()
                .fieldName(vectorFieldName)
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .metricType(IndexParam.MetricType.COSINE)
                .build();

        List<IndexParam> indexParams = new ArrayList<>();
        indexParams.add(indexParam);

        CreateCollectionReq createReq = CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .indexParams(indexParams)
                .build();

        client.createCollection(createReq);
        log.info("Created Milvus collection '{}' with dimension={}", collectionName, dimension);
    }
}
