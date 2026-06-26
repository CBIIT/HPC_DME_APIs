/**
 * HpcVectorStoreProxy.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration;

import java.util.List;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC vector store proxy interface. Provides operations to persist embedding
 * vectors paired with an HPC collection ID, and to retrieve matching collection
 * IDs for a given query vector.
 * </p>
 */
public interface HpcVectorStoreProxy {

    /**
     * Store an embedding vector together with its associated collection ID.
     *
     * @param vector       Pre-computed embedding vector.
     * @param collectionId HPC collection ID to associate with the vector.
     * @throws HpcException on vector-store failures.
     */
    public void storeVector(List<Float> vector, String collectionId) throws HpcException;

    /**
     * Find collection IDs whose stored vectors are most similar to the given query
     * vector.
     *
     * @param queryVector Pre-computed embedding of the natural-language query.
     * @param maxResults  Maximum number of results to return.
     * @return Ordered list of collection IDs (most similar first).
     * @throws HpcException on vector-store failures.
     */
    public List<String> findCollectionIds(List<Float> queryVector, int maxResults) throws HpcException;
}
