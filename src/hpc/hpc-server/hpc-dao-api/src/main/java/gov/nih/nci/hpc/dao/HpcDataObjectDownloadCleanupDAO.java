/**
 * HpcDataObjectDownloadCleanupDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadCleanup;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Data Object Download Cleanup DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDataObjectDownloadCleanupDAO 
{    
    /**
     * Store a new data object download cleanup it if it exists, or update one if it exists.
     *
     * @param dataObjectDownloadCleanup The data object download cleanup object to persist.
     * 
     * @throws HpcException
     */
    public void upsert(HpcDataObjectDownloadCleanup dataObjectDownloadCleanup) 
    		          throws HpcException;
    
    /**
     * Delete a data object download cleanup.
     *
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @throws HpcException
     */
    public void delete(String dataTransferRequestId) throws HpcException;
    
    /**
     * Get all data object download cleanup entries. 
     *
     * @return List<HpcDataObjectDownloadCleanup>
     * 
     * @throws HpcException
     */
    public List<HpcDataObjectDownloadCleanup> getAll() throws HpcException;
}

 