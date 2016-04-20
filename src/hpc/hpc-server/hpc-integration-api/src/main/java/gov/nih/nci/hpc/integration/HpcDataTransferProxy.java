/**
 * HpcDataTransferProxy.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Transfer Proxy Interface.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$ 
 */

public interface HpcDataTransferProxy 
{    
    /**
     * Authenticate the invoker w/ the data transfer system.
     *
     * @param dataTransferAccount The Data Transfer account to authenticate.
     * @return An authenticated token, to be used in subsequent calls to data transfer.
     *         It returns null if the account is not authenticated.
     * 
     * @throws HpcException
     */
    public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount) 
    		                  throws HpcException;
    
    /**
     * Upload a data object file.
     *
      *@param authenticatedToken An authenticated token.
     * @param dataUploadRequest The data upload request
     * @return HpcDataObjectUploadResponse A data object upload response.
     * 
     * @throws HpcException
     */
    public HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
    		                                            HpcDataObjectUploadRequest uploadRequest) 
    		                                           throws HpcException;
    
    /**
     * Download a data object file.
     *
     * @param authenticatedToken An authenticated token.
     * @param dataDownloadRequest The data object download request.
     * 
     * @throws HpcException
     */
    public void downloadDataObject(Object authenticatedToken,
    		                       HpcDataObjectDownloadRequest downloadRequest) 
    		                      throws HpcException;
    
    /**
     * Get a data transfer request status.
     *
     * @param authenticatedToken An authenticated token.
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @return HpcDataTransferStatus the data transfer request status.
     * 
     * @throws HpcException
     */
    public HpcDataTransferStatus getDataTransferStatus(Object authenticatedToken,
    		                                           String dataTransferRequestId) 
    		                                          throws HpcException;
    
    /**
     * Get the size of the data transferred of a specific request.
     *
     * @param authenticatedToken An authenticated token.
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @return The size of the data transferred in bytes.
     * 
     * @throws HpcException
     */
    public long getDataTransferSize(Object authenticatedToken,
    		                        String dataTransferRequestId) 
    		                       throws HpcException;
    
    /**
     * Get attributes of a file/directory.
     *
     * @param authenticatedToken An authenticated token.
     * @param fileLocation The endpoint/path to check.
     * @param getSize If set to true, the file/directory size will be returned. 
     * @return HpcDataTransferPathAttributes The path attributes.
     * 
     * @throws HpcException
     */
    public HpcPathAttributes getPathAttributes(Object authenticatedToken, 
    		                                   HpcFileLocation fileLocation,
    		                                   boolean getSize) 
    		                                  throws HpcException;
    
    /**
     * Set file permission.
     *
     * @param authenticatedToken An authenticated token.
     * @param fileLocation The endpoint/path to set permission
     * @param permissionRequest The user permission request.
     * 
     * @throws HpcException
     */
    public void setPermission(Object authenticatedToken,
    		                  HpcFileLocation fileLocation,
    		                  HpcUserPermission permissionRequest) 
    		                 throws HpcException; 
    
    /** 
     * Calculate data transfer destination to deposit a data object
     * 
     * @param baseArchiveDestination The base (archive specific) destination.
     * @param path The data object (logical) path.
     * @param callerObjectId The caller's objectId.
     * 
     * @return HpcFileLocation The calculated data transfer deposit destination.
     */
	public static HpcFileLocation getArchiveDestination(HpcFileLocation baseArchiveDestination,
			                                            String path, String callerObjectId) 
	{
		// Calculate the data transfer destination absolute path as the following:
		// 'base path' / 'caller's data transfer destination path/ 'logical path'
		StringBuffer destinationPath = new StringBuffer();
		destinationPath.append(baseArchiveDestination.getFileId());
		
		if(callerObjectId != null && !callerObjectId.isEmpty()) {
		   if(callerObjectId.charAt(0) != '/') {
			  destinationPath.append('/'); 
		   }
		   destinationPath.append(callerObjectId);
		}
		
		destinationPath.append('/');
		destinationPath.append(path);
		 
		HpcFileLocation archiveDestination = new HpcFileLocation();
		archiveDestination.setFileContainerId(baseArchiveDestination.getFileContainerId());
		archiveDestination.setFileId(destinationPath.toString());
		
		return archiveDestination;
	}
}

 