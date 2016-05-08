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
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

import java.io.File;

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
     * @param authenticatedToken An authenticated token.
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
     * @return HpcDataObjectDownloadResponse A data object download response.
     * 
     * @throws HpcException
     */
    public HpcDataObjectDownloadResponse downloadDataObject(Object authenticatedToken,
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
    default HpcDataTransferStatus getDataTransferStatus(Object authenticatedToken,
    		                                            String dataTransferRequestId) 
    		                                           throws HpcException
    {
    	throw new HpcException("getDataTransferStatus() not supported by S3",
	               HpcErrorType.UNEXPECTED_ERROR);
    }
    
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
    default long getDataTransferSize(Object authenticatedToken,
    		                         String dataTransferRequestId) 
    		                        throws HpcException
    {
    	throw new HpcException("getDataTransferStatus() not supported by S3",
	               HpcErrorType.UNEXPECTED_ERROR);
    }
    
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
    default HpcPathAttributes getPathAttributes(Object authenticatedToken, 
    		                                    HpcFileLocation fileLocation,
    		                                    boolean getSize) 
    		                                   throws HpcException
    {
    	throw new HpcException("getDataTransferStatus() not supported by S3",
                               HpcErrorType.UNEXPECTED_ERROR);
    }
    
    /**
     * Get upload file.
     *
     * @param fileId The file ID.
     * 
     * @throws HpcException
     */
    default File getUploadFile(String fileId) throws HpcException
    {
	  	throw new HpcException("getUploadFile not supported by S3",
	                           HpcErrorType.UNEXPECTED_ERROR);
    }
    
    /**
     * Get download file.
     *
     * @param fileId The file ID.
     * 
     * @throws HpcException
     */
    default File getDownloadFile(String fileId) throws HpcException
    {
	  	throw new HpcException("getDownloadFile not supported by S3",
	                           HpcErrorType.UNEXPECTED_ERROR);
    }
    
    /**
     * Get download source location.
     *
     * @param path The data object logical path.
     * 
     * @throws HpcException
     */
    default HpcFileLocation getDownloadSourceLocation(String path) throws HpcException
    {
	  	throw new HpcException("getDownloadSourceLocation not supported by S3",
	                           HpcErrorType.UNEXPECTED_ERROR);
    }
    
    /** 
     * Calculate data transfer destination to deposit a data object
     * 
     * @param baseArchiveDestination The base (archive specific) destination.
     * @param path The data object (logical) path.
     * @param callerObjectId The caller's objectId.
     * 
     * @return HpcFileLocation The calculated data transfer deposit destination.
     */
	public static HpcFileLocation getArchiveDestinationLocation(
			                         HpcFileLocation baseArchiveDestination,
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
		
		if(path.charAt(0) != '/') {
		   destinationPath.append('/');
		}
		destinationPath.append(path);
		 
		HpcFileLocation archiveDestination = new HpcFileLocation();
		archiveDestination.setFileContainerId(baseArchiveDestination.getFileContainerId());
		archiveDestination.setFileId(destinationPath.toString());
		
		return archiveDestination;
	}
}

 