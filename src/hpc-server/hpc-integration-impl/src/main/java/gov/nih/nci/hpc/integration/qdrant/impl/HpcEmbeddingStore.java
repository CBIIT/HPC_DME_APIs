/**
 * HpcEmbeddingStore.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.qdrant.impl;

import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Qdrant embedding-store connection.
 *
 * <p>
 * Encapsulates Qdrant connection configuration and owns the lifecycle of the
 * {@link QdrantEmbeddingStore} singleton. Analogous to
 * {@code HpcIRODSConnection}: its sole purpose is to validate config, build the
 * store once, and expose it to callers that perform business operations.
 * </p>
 *
 * @see HpcVectorStoreProxyImpl
 */
public class HpcEmbeddingStore {

    // Metadata key used to carry the HPC collection ID in Qdrant payloads.
    static final String COLLECTION_ID_METADATA_KEY = "hpcCollectionId";

    // ---------------------------------------------------------------------//
    // Instance members
    // ---------------------------------------------------------------------//

    private final QdrantEmbeddingStore embeddingStore;

    // ---------------------------------------------------------------------//
    // Constructors
    // ---------------------------------------------------------------------//

    /**
     * Constructor for Spring Dependency Injection.
     *
     * @param host           Qdrant server host name or IP address.
     * @param port           Qdrant gRPC port (default 6334).
     * @param collectionName Name of the Qdrant collection to use.
     * @param useTls         Whether to use TLS for the gRPC connection.
     * @param apiKey         Qdrant API key; may be {@code null} or blank for
     *                       unauthenticated (local) instances.
     * @throws HpcException if any required parameter is missing or the store
     *                      cannot be initialised.
     */
    public HpcEmbeddingStore(String host, int port, String collectionName, boolean useTls, String apiKey)
            throws HpcException {
        if (host == null || host.isBlank()) {
            throw new HpcException("Qdrant host must be configured", HpcErrorType.SPRING_CONFIGURATION_ERROR);
        }
        if (collectionName == null || collectionName.isBlank()) {
            throw new HpcException("Qdrant collection name must be configured",
                    HpcErrorType.SPRING_CONFIGURATION_ERROR);
        }

        try {
            QdrantEmbeddingStore.Builder builder = QdrantEmbeddingStore.builder()
                    .host(host)
                    .port(port)
                    .collectionName(collectionName)
                    .useTls(useTls)
                    .payloadTextKey(COLLECTION_ID_METADATA_KEY);

            if (apiKey != null && !apiKey.isBlank()) {
                builder = builder.apiKey(apiKey);
            }

            this.embeddingStore = builder.build();

        } catch (Exception e) {
            throw new HpcException("Failed to initialise Qdrant embedding store",
                    HpcErrorType.SPRING_CONFIGURATION_ERROR, e);
        }
    }

    /**
     * Default constructor disabled.
     *
     * @throws HpcException always; default construction is not supported.
     */
    private HpcEmbeddingStore() throws HpcException {
        throw new HpcException("HpcEmbeddingStore default constructor disabled",
                HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }

    // ---------------------------------------------------------------------//
    // Methods
    // ---------------------------------------------------------------------//

    /**
     * Returns the initialised {@link QdrantEmbeddingStore}.
     *
     * @return the Qdrant embedding store.
     */
    public QdrantEmbeddingStore getEmbeddingStore() {
        return embeddingStore;
    }
}
