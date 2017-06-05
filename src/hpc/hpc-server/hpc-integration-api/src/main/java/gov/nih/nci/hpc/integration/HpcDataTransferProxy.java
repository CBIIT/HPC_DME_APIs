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

import java.util.List;
import java.util.UUID;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
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
     * @throws HpcException on data transfer system failure.
     */
    public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount) 
    		                  throws HpcException;
    
    /**
     * Upload a data object file.
     *
     * @param authenticatedToken An authenticated token.
     * @param uploadRequest The data upload request
     * @param metadataEntries (Optional) a list of metadata to attach to the physical file storage.
     * @param progressListener (Optional) a progress listener for async notification on transfer completion.
     * @return A data object upload response.
     * @throws HpcException on data transfer system failure.
     */
    public HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
    		                                            HpcDataObjectUploadRequest uploadRequest,
    		                                            List<HpcMetadataEntry> metadataEntries,
    		                                            HpcDataTransferProgressListener progressListener) 
    		                                           throws HpcException;
    
    /**
     * Download a data object file.
     *
     * @param authenticatedToken An authenticated token.
     * @param downloadRequest The data object download request.
     * @param progressListener (Optional) a progress listener for async notification on transfer completion.
     * @return A data object download response.
     * @throws HpcException on data transfer system failure.
     */
    public HpcDataObjectDownloadResponse downloadDataObject(Object authenticatedToken,
    		                                                HpcDataObjectDownloadRequest downloadRequest,
    		                                                HpcDataTransferProgressListener progressListener) 
    		                                               throws HpcException;
    
    /**
     * Delete a data object file.
     *
     * @param authenticatedToken An authenticated token.
     * @param fileLocation The file location.
     * @throws HpcException on data transfer system failure.
     */
    default public void deleteDataObject(Object authenticatedToken,
    		                             HpcFileLocation fileLocation)
                                        throws HpcException
    {
    	throw new HpcException("deleteDataObject() not supported",
                                HpcErrorType.UNEXPECTED_ERROR);
                                        }
    
    /**
     * Get a data transfer upload request status.
     *
     * @param authenticatedToken An authenticated token.
     * @param dataTransferRequestId The data transfer request ID.
     * @return The data transfer request status.
     * @throws HpcException on data transfer system failure.
     */
    default public HpcDataTransferUploadStatus 
            getDataTransferUploadStatus(Object authenticatedToken,
    		                            String dataTransferRequestId) 
    		                           throws HpcException
    {
    	throw new HpcException("getDataTransferUploadStatus() not supported",
	                           HpcErrorType.UNEXPECTED_ERROR);
    }
    
    /**
     * Get a data transfer download request status.
     *
     * @param authenticatedToken An authenticated token.
     * @param dataTransferRequestId The data transfer request ID.
     * @return The data transfer request status.
     * @throws HpcException on data transfer system failure.
     */
    default public 
            HpcDataTransferDownloadStatus getDataTransferDownloadStatus(Object authenticatedToken,
    		                                                            String dataTransferRequestId) 
    		                                                           throws HpcException
    {
    	throw new HpcException("getDataTransferDownloadStatus() not supported",
	                           HpcErrorType.UNEXPECTED_ERROR);
    }
    
    /**
     * Get the size of the data transferred of a specific request.
     *
     * @param authenticatedToken An authenticated token.
     * @param dataTransferRequestId The data transfer request ID.
     * @return The size of the data transferred in bytes.
     * @throws HpcException on data transfer system failure.
     */
    default public long getDataTransferSize(Object authenticatedToken,
    		                                String dataTransferRequestId) 
    		                               throws HpcException
    {
    	throw new HpcException("getDataTransferStatus() not supported",
	               HpcErrorType.UNEXPECTED_ERROR);
    }
    
    /**
     * Get attributes of a file/directory.
     *
     * @param authenticatedToken An authenticated token.
     * @param fileLocation The endpoint/path to check.
     * @param getSize If set to true, the file/directory size will be returned. 
     * @return The path attributes.
     * @throws HpcException on data transfer system failure.
     */
    default public HpcPathAttributes getPathAttributes(Object authenticatedToken, 
    		                                           HpcFileLocation fileLocation,
    		                                           boolean getSize) 
    		                                          throws HpcException
    {
    	throw new HpcException("getDataTransferStatus() not supported",
                               HpcErrorType.UNEXPECTED_ERROR);
    }
    
    /**
     * Get a file path for a given file ID
     *
     * @param fileId The file ID.
     * @param archive If true, the archive path is returned, otherwise the download/share path is returned.
     * @return a file path.
     * @throws HpcException on data transfer system failure.
     */
    default public String getFilePath(String fileId, boolean archive) throws HpcException
    {
	  	throw new HpcException("getFilePath not supported",
	                           HpcErrorType.UNEXPECTED_ERROR);
    }
    
    /**
     * Get download source location.
     *
     * @param path The data object logical path.
     * @return The download source location.
     * @throws HpcException on data transfer system failure.
     */
    default public HpcFileLocation getDownloadSourceLocation(String path) throws HpcException
    {
	  	throw new HpcException("getDownloadSourceLocation not supported",
	                           HpcErrorType.UNEXPECTED_ERROR);
    }
    
    /** 
     * Calculate data transfer destination to deposit a data object.
     * 
     * @param baseArchiveDestination The base (archive specific) destination.
     * @param path The data object (logical) path.
     * @param callerObjectId The caller's objectId.
     * @param archiveType The type of the archive.
     * @return The calculated data transfer deposit destination.
     */
	public static HpcFileLocation getArchiveDestinationLocation(
			                         HpcFileLocation baseArchiveDestination,
			                         String path, String callerObjectId,
			                         HpcArchiveType archiveType) 
	{
		// Calculate the data transfer destination absolute path as the following:
		StringBuilder destinationPath = new StringBuilder();
		destinationPath.append(baseArchiveDestination.getFileId());
		
		if(archiveType.equals(HpcArchiveType.ARCHIVE)) {
		   // For Archive destination, destination path is:
		   // 'base path' / 'caller's object-id / 'logical path'
		   if(callerObjectId != null && !callerObjectId.isEmpty()) {
		      if(callerObjectId.charAt(0) != '/') {
			     destinationPath.append('/'); 
		      }
		      destinationPath.append(callerObjectId);
		   }
		
		   if(path.charAt(0) != '/') {
		      destinationPath.append('/');
		   }
		   if(destinationPath.charAt(destinationPath.length() - 1) == '/' && 
			  path.charAt(0) == '/') {
			  destinationPath.append(path.substring(1));
		   } else {
			       destinationPath.append(path);
		   }
		   
		} else {
			    // For Temporary Archive, destination path is:
			    // 'base path' / generated UUID
			    destinationPath.append('/' + UUID.randomUUID().toString());
		}
		 
		HpcFileLocation archiveDestination = new HpcFileLocation();
		archiveDestination.setFileContainerId(baseArchiveDestination.getFileContainerId());
		archiveDestination.setFileId(destinationPath.toString());
		
		return archiveDestination;
	}
}

 