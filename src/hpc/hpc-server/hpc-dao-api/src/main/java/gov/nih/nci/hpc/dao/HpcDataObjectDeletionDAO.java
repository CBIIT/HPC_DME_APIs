/**
 * HpcDataObjectDeletionDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.Calendar;

import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Object Deletion DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcDataObjectDeletionDAO 
{    
    /**
     * Store a new data object deletion record.
     *
     * @param userId The user who deleted the data object.
     * @param path The data object path.
     * @param metadataEntries The data object metadata.
     * @param archiveLocation The location of the file in the archive (prior to deletion).
     * @param archiveDeleteStatus True if the physical file was successfully removed from archive.
     * @param dataManagementDeleteStatus True if the data object record was successfully removed from 
     *        the data management system (iRODS). 
     * @param deleted The time the data object was deleted.
     * @param message (Optional) Error message if the deletion failed.
     * 
     * @throws HpcException on database error.
     */
    public void insert(String userId, String path, HpcMetadataEntries metadataEntries,
    		           HpcFileLocation archiveLocation, boolean archiveDeleteStatus,
    		           boolean dataManagementDeleteStatus, Calendar deleted, String message) 
    		          throws HpcException;
}

 