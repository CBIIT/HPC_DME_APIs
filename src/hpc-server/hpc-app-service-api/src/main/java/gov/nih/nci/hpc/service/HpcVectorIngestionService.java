/**
 * HpcVectorIngestionService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC vector ingestion application service interface.
 * </p>
 */
public interface HpcVectorIngestionService {

    /**
     * Generate an embedding vector from the provided text and store it in the
     * vector store with the associated collection ID.
     *
     * @param collectionId HPC collection ID.
     * @param text         Source text to embed.
     * @throws HpcException on service failure.
     */
    public void indexCollection(String collectionId, String text) throws HpcException;
}
