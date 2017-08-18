/**
 * HpcDataTransferQueueDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Globus transfer (upload/download) queues DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcDataTransferQueueDAO 
{    
    /**
     * Store a new data object upload request.
     *
     * @param uploadRequest The data object upload request to persist.
     * @param dataTransferType The data transfer type to queue the transfer request for.
     * @throws HpcException on database error.
     */
    public void upsertUploadQueue(HpcDataObjectUploadRequest uploadRequest,
    		                      HpcDataTransferType dataTransferType) throws HpcException;
}

 