package gov.nih.nci.hpc.integration.qdrant.impl;

import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcVectorStoreProxy;

/**
 * Qdrant vector store proxy implementation.
 *
 * <p>
 * Stores embedding vectors paired with an HPC collection ID in Qdrant. The
 * collection ID is stored as the text content of the {@code TextSegment} so
 * that it can be retrieved directly from search results without requiring an
 * extra lookup.
 * </p>
 */
public class HpcVectorStoreProxyImpl implements HpcVectorStoreProxy {

    // Default minimum similarity score to include a result.
    private static final double DEFAULT_MIN_SCORE = 0.7;

    // Metadata key used to carry the HPC collection ID in Qdrant payloads.
    static final String COLLECTION_ID_METADATA_KEY = "hpcCollectionId";

    private final QdrantEmbeddingStore embeddingStore;
    private final double minScore;

    // ---------------------------------------------------------------------//
    // Constructors
    // ---------------------------------------------------------------------//

    /**
     * Spring-injection constructor.
     *
     * @param host           Qdrant server host.
     * @param port           Qdrant gRPC port (default 6334).
     * @param collectionName Name of the Qdrant collection to use.
     * @param useTls         Whether to use TLS for the Qdrant connection.
     * @param apiKey         Qdrant API key (may be empty for local instances).
     * @param minScore       Minimum similarity score threshold (0–1). Values less
     *                       than or equal to zero fall back to the default.
     * @throws HpcException if the Qdrant store cannot be initialised.
     */
    public HpcVectorStoreProxyImpl(String host, int port, String collectionName, boolean useTls, String apiKey,
            double minScore) throws HpcException {
        if (host == null || host.isBlank()) {
            throw new HpcException("Qdrant host must be configured", HpcErrorType.SPRING_CONFIGURATION_ERROR);
        }
        if (collectionName == null || collectionName.isBlank()) {
            throw new HpcException("Qdrant collection name must be configured", HpcErrorType.SPRING_CONFIGURATION_ERROR);
        }

        try {
            QdrantEmbeddingStore.Builder builder = QdrantEmbeddingStore.builder().host(host).port(port)
                    .collectionName(collectionName).useTls(useTls).payloadTextKey(COLLECTION_ID_METADATA_KEY);

            if (apiKey != null && !apiKey.isBlank()) {
                builder = builder.apiKey(apiKey);
            }

            this.embeddingStore = builder.build();
        } catch (Exception e) {
            throw new HpcException("Failed to initialise Qdrant embedding store", HpcErrorType.SPRING_CONFIGURATION_ERROR, e);
        }

        this.minScore = minScore > 0 ? minScore : DEFAULT_MIN_SCORE;
    }

    /**
     * Default constructor is disabled.
     */
    private HpcVectorStoreProxyImpl() throws HpcException {
        throw new HpcException("Default constructor disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }

    // ---------------------------------------------------------------------//
    // HpcVectorStoreProxy Interface Implementation
    // ---------------------------------------------------------------------//

    @Override
    public void storeVector(List<Float> vector, String collectionId) throws HpcException {
        if (vector == null || vector.isEmpty()) {
            throw new HpcException("Embedding vector cannot be empty", HpcErrorType.INVALID_REQUEST_INPUT);
        }
        if (collectionId == null || collectionId.isBlank()) {
            throw new HpcException("Collection ID cannot be blank", HpcErrorType.INVALID_REQUEST_INPUT);
        }

        try {
            float[] floatArray = toFloatArray(vector);
            Embedding embedding = Embedding.from(floatArray);
            // Store the collectionId as the TextSegment text; payloadTextKey maps it to the
            // Qdrant payload field so we can retrieve it on search.
            TextSegment segment = TextSegment.from(collectionId);
            embeddingStore.add(embedding, segment);
        } catch (HpcException e) {
            throw e;
        } catch (Exception e) {
            throw new HpcException("Failed to store vector in Qdrant", HpcErrorType.UNEXPECTED_ERROR, e);
        }
    }

    @Override
    public List<String> findCollectionIds(List<Float> queryVector, int maxResults) throws HpcException {
        if (queryVector == null || queryVector.isEmpty()) {
            throw new HpcException("Query vector cannot be empty", HpcErrorType.INVALID_REQUEST_INPUT);
        }
        if (maxResults <= 0) {
            throw new HpcException("maxResults must be greater than zero", HpcErrorType.INVALID_REQUEST_INPUT);
        }

        try {
            float[] floatArray = toFloatArray(queryVector);
            Embedding queryEmbedding = Embedding.from(floatArray);
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding)
                    .maxResults(maxResults).minScore(minScore).build();
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            List<String> collectionIds = new ArrayList<>(matches.size());
            for (EmbeddingMatch<TextSegment> match : matches) {
                if (match.embedded() != null && match.embedded().text() != null) {
                    collectionIds.add(match.embedded().text());
                }
            }
            return collectionIds;
        } catch (HpcException e) {
            throw e;
        } catch (Exception e) {
            throw new HpcException("Failed to query Qdrant vector store", HpcErrorType.UNEXPECTED_ERROR, e);
        }
    }

    // ---------------------------------------------------------------------//
    // Helper Methods
    // ---------------------------------------------------------------------//

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
