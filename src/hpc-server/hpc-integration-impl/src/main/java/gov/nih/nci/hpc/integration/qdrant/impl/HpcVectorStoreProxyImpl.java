package gov.nih.nci.hpc.integration.qdrant.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
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

    // The Qdrant embedding-store connection (injected).
    @Autowired
    private HpcEmbeddingStore hpcEmbeddingStore = null;

    @Value("${hpc.integration.qdrant.minScore}")
    private double minScore = DEFAULT_MIN_SCORE;

    // ---------------------------------------------------------------------//
    // Constructors
    // ---------------------------------------------------------------------//

    /** Default Constructor. */
    private HpcVectorStoreProxyImpl() {
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
            hpcEmbeddingStore.getEmbeddingStore().add(embedding, segment);
        } catch (Exception e) {
            throw new HpcException("Failed to store vector for collection " + collectionId + " in Qdrant", HpcErrorType.UNEXPECTED_ERROR, e);
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
                    .maxResults(maxResults).minScore(minScore > 0 ? minScore : DEFAULT_MIN_SCORE).build();
            EmbeddingSearchResult<TextSegment> searchResult = hpcEmbeddingStore.getEmbeddingStore().search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            List<String> collectionIds = new ArrayList<>(matches.size());
            for (EmbeddingMatch<TextSegment> match : matches) {
                if (match.embedded() != null && match.embedded().text() != null) {
                    collectionIds.add(match.embedded().text());
                }
            }
            return collectionIds;
        } catch (Exception e) {
            throw new HpcException("Failed to query Qdrant vector store " + queryVector, HpcErrorType.UNEXPECTED_ERROR, e);
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
