/**
 * HpcVectorSearchServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcTextEmbeddingProxy;
import gov.nih.nci.hpc.integration.HpcVectorStoreProxy;
import gov.nih.nci.hpc.service.HpcVectorSearchService;

/**
 * <p>
 * HPC vector search application service implementation.
 * </p>
 */
public class HpcVectorSearchServiceImpl implements HpcVectorSearchService {

    @Autowired
    private HpcTextEmbeddingProxy hpcTextEmbeddingProxy = null;

    @Autowired
    private HpcVectorStoreProxy hpcVectorStoreProxy = null;

    @Override
    public List<String> queryCollections(String queryText, int maxResults) throws HpcException {
        if (queryText == null || queryText.isBlank()) {
            throw new HpcException("Query text cannot be blank", HpcErrorType.INVALID_REQUEST_INPUT);
        }
        if (maxResults <= 0) {
            throw new HpcException("maxResults must be greater than zero", HpcErrorType.INVALID_REQUEST_INPUT);
        }

        List<Float> queryVector = hpcTextEmbeddingProxy.getEmbeddingVector(queryText);
        return hpcVectorStoreProxy.findCollectionIds(queryVector, maxResults);
    }
}
