/**
 * HpcVectorIngestionServiceImpl.java
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
import gov.nih.nci.hpc.service.HpcVectorIngestionService;

/**
 * <p>
 * HPC vector ingestion application service implementation.
 * </p>
 */
public class HpcVectorIngestionServiceImpl implements HpcVectorIngestionService {

    @Autowired
    private HpcTextEmbeddingProxy hpcTextEmbeddingProxy = null;

    @Autowired
    private HpcVectorStoreProxy hpcVectorStoreProxy = null;

    @Override
    public void indexCollection(String collectionId, String text) throws HpcException {
        if (collectionId == null || collectionId.isBlank()) {
            throw new HpcException("Collection ID cannot be blank", HpcErrorType.INVALID_REQUEST_INPUT);
        }
        if (text == null || text.isBlank()) {
            throw new HpcException("Input text cannot be blank", HpcErrorType.INVALID_REQUEST_INPUT);
        }

        List<Float> vector = hpcTextEmbeddingProxy.getEmbeddingVector(text);
        hpcVectorStoreProxy.storeVector(vector, collectionId);
    }
}
