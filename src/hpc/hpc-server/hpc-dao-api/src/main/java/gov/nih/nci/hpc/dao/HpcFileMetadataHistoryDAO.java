/**
 * HpcFileMetadataHistoryDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.metadata.HpcFileMetadataHistory;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC File Metadata History DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcFileMetadataHistoryDAO 
{         
    /**
     * Store a new file metadata history to the repository or update it if it exists.
     *
     * @param metadataHistory The file metadata history to be added/updated.
     * 
     * @throws HpcException
     */
    public void upsert(HpcFileMetadataHistory metadataHistory) throws HpcException;
    
    /**
     * Get file metadata history from the repository by file ID.
     *
     * @param fileId The file ID.
     * @return The file metadata history object if found, or null otherwise.
     * 
     * @throws HpcException
     */
    public HpcFileMetadataHistory getFileMetadataHistory(String fileId) throws HpcException;
}

 