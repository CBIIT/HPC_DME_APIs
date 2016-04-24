/**
 * HpcDataTransferService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Transfer Service Interface.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$
 */

public interface HpcDataTransferService 
{         
    /**
     * Upload a data object file.
     *
     * @param dataUploadRequest The data upload request
     * @return HpcDataObjectUploadResponse A data object upload response.
     * 
     * @throws HpcException
     */
    public HpcDataObjectUploadResponse uploadDataObject(HpcDataObjectUploadRequest uploadRequest) 
    		                                           throws HpcException;
    
    /**
     * Download a data object file.
     *
     * @param dataDownloadRequest The data object download request.
     * @return HpcDataObjectDownloadResponse A data object download response.
     * 
     * @throws HpcException
     */
    public HpcDataObjectDownloadResponse downloadDataObject(HpcDataObjectDownloadRequest downloadRequest) 
    		                                               throws HpcException;

    /**
     * Get a data transfer request status.
     *
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @return HpcDataTransferStatus the data transfer request status.
     * 
     * @throws HpcException
     */
    public HpcDataTransferStatus getDataTransferStatus(String dataTransferRequestId) 
    		                                          throws HpcException;
    
    /**
     * Get the size of the data transferred of a specific request.
     *
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @return The size of the data transferred in bytes.
     * 
     * @throws HpcException
     */
    public long getDataTransferSize(String dataTransferRequestId) 
    		                       throws HpcException;
    
    /**
     * Validate a data transfer account.
     *
     * @param dataTransferAccount The account to validate.
     * @return True if the account is valid, or false otherwise.
     */
    public boolean validateDataTransferAccount(
    		               HpcIntegratedSystemAccount dataTransferAccount)
    		               throws HpcException;
    
    /**
     * Get endpoint/path attributes .
     *
     * @param fileLocation The endpoint/path to get attributes for.
     * @param getSize If set to true, the file/directory size will be returned. 
     * @return HpcPathAttributes 
     * 
     * @throws HpcException
     */
    public HpcPathAttributes getPathAttributes(HpcFileLocation fileLocation,
    		                                   boolean getSize) 
    		                                  throws HpcException;
    
    /**
     * Set file permission.
     *
     * @param fileLocation The endpoint/path to set permission
     * @param permissionRequest The user permission request.
     * @param dataTransferAccount Optional (can be null). If specified, the user ID to set 
     *                            the permission for is taken from this account instead of the 
     *                            permission request.
     * 
     * @throws HpcException
     */
    public void setPermission(HpcFileLocation fileLocation,
    		                  HpcUserPermission permissionRequest,
    		                  HpcIntegratedSystemAccount dataTransferAccount) 
    		                 throws HpcException; 
}

 