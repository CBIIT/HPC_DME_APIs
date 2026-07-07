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
     * Generate an embedding vector from collection metadata and store it in the
     * vector store with the associated collection path.
     *
     * @param collectionPath HPC collection path.
     * @throws HpcException on service failure.
     */
    public void indexCollection(String collectionPath) throws HpcException;
}
