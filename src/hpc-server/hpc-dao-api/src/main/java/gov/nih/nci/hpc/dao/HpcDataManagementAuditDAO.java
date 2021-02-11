/**
 * HpcDataManagementAuditDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.Calendar;

import gov.nih.nci.hpc.domain.datamanagement.HpcAuditRequestType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Management Audit DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcDataManagementAuditDAO 
{    
    /**
     * Store a new audit record.
     *
     * @param userId The user who initiated the request recorded for audit.
     * @param path The data object or collection path.
     * @param requestType The request being recorded for audit.
     * @param metadataBefore The collection or data object metadata before the request.
     * @param metadataAfter The collection or data object metadata after the request.
     * @param archiveLocation The location of the file in the archive (prior to deletion).
     * @param dataManagementStatus Data management (iRODS) request completion status.
     * @param dataTransferStatus Data transfer (archive) request completion status.
     * @param message Error message if the request failed.
     * @param completed The time the request was completed.
     * @throws HpcException on database error.
     */
    public void insert(String userId, String path, HpcAuditRequestType requestType,
    		           HpcMetadataEntries metadataBefore, HpcMetadataEntries metadataAfter,
    		           HpcFileLocation archiveLocation, boolean dataManagementStatus,
    		           Boolean dataTransferStatus, String message, Calendar completed) 
    		          throws HpcException;
}

 