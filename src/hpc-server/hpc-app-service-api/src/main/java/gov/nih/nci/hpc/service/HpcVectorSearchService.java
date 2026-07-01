/**
 * HpcVectorSearchService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import java.util.List;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC vector search application service interface.
 * </p>
 */
public interface HpcVectorSearchService {

    /**
     * Generate an embedding vector from the query text and return the matching
     * collection IDs from the vector store.
     *
     * @param queryText  Natural-language query text.
     * @param maxResults Maximum number of results to return.
     * @return Ordered list of matching collection IDs.
     * @throws HpcException on service failure.
     */
    public List<String> queryCollections(String queryText, int maxResults) throws HpcException;
}
