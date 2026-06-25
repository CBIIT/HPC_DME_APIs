package gov.nih.nci.hpc.integration.huggingface.impl;

import java.time.Duration;
import java.util.List;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.output.Response;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcTextEmbeddingProxy;

/**
 * Hugging Face text embedding proxy implementation.
 */
public class HpcTextEmbeddingProxyImpl implements HpcTextEmbeddingProxy {

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    private final EmbeddingModel embeddingModel;

    public HpcTextEmbeddingProxyImpl(String accessToken, String modelId, Integer timeoutSeconds) {
        int resolvedTimeout = timeoutSeconds == null || timeoutSeconds.intValue() <= 0 ? DEFAULT_TIMEOUT_SECONDS
                : timeoutSeconds.intValue();

        this.embeddingModel = HuggingFaceEmbeddingModel.builder().accessToken(accessToken).modelId(modelId)
                .waitForModel(Boolean.TRUE).timeout(Duration.ofSeconds(resolvedTimeout)).build();
    }

    @Override
    public List<Float> getEmbeddingVector(String text) throws HpcException {
        if (text == null || text.isBlank()) {
            throw new HpcException("Input text cannot be blank", HpcErrorType.INVALID_REQUEST_INPUT_PARAMETER);
        }

        try {
            Response<Embedding> response = embeddingModel.embed(text);
            return response.content().vectorAsList();
        } catch (Exception e) {
            throw new HpcException("Failed to generate embedding vector", HpcErrorType.UNEXPECTED_ERROR, e);
        }
    }
}
