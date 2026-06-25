/**
 * HpcTextEmbeddingProxy.java
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
 * HPC text embedding proxy interface.
 * </p>
 */
public interface HpcTextEmbeddingProxy {

    /**
     * Convert text into an embedding vector.
     *
     * @param text Input text.
     * @return Embedding vector.
     * @throws HpcException on embedding failures.
     */
    public List<Float> getEmbeddingVector(String text) throws HpcException;
}
