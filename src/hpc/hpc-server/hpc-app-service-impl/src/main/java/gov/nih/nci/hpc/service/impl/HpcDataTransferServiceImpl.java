/**
 * HpcDataTransferServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;
import gov.nih.nci.hpc.dao.HpcDataObjectDownloadCleanupDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadCleanup;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcDataTransferAuthenticatedToken;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Transfer Service Implementation.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$
 */

public class HpcDataTransferServiceImpl implements HpcDataTransferService
{    
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
    // Data transfer system generated metadata attributes (attach to files in archive)
	private static final String PATH_ATTRIBUTE = "path";
	private static final String USER_ID_ATTRIBUTE = "user_id";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// Map data transfer type to its proxy impl.
	private Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies = 
			new EnumMap<>(HpcDataTransferType.class);
	
	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;
	
	// Data object download cleanup DAO
	@Autowired
	private HpcDataObjectDownloadCleanupDAO dataObjectDownloadCleanupDAO = null;
	
	// The download directory
	private String downloadDirectory = null;
	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param dataTransferProxies The data transfer proxies.
     * @throws HpcException
     */
    public HpcDataTransferServiceImpl(
    		  Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies,
    		  String downloadDirectory) 
    		  throws HpcException
    {
    	if(dataTransferProxies == null || dataTransferProxies.isEmpty() || 
    	   downloadDirectory == null || downloadDirectory.isEmpty()) {
    	   throw new HpcException("Null or empty map of data transfer proxies, or download directory",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.dataTransferProxies.putAll(dataTransferProxies);
    	this.downloadDirectory = downloadDirectory;
    }   
    
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    @SuppressWarnings("unused")
	private HpcDataTransferServiceImpl() throws HpcException
    {
    	throw new HpcException("Default Constructor disabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataTransferService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
	public HpcDataObjectUploadResponse uploadDataObject(HpcFileLocation sourceLocation, 
			                                            File sourceFile, 
			                                            String path, String userId,
			                                            String callerObjectId)
	                                                   throws HpcException
	{
    	// Validate one and only one data source is provided.
    	if(sourceLocation == null && sourceFile == null) {
    	   throw new HpcException("No data transfer source or data attachment provided",
	                              HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	if(sourceLocation != null && sourceFile != null) {
     	   throw new HpcException("Both data transfer source and data attachment provided",
 	                              HpcErrorType.INVALID_REQUEST_INPUT);	
     	}
    	
    	// Create an upload request.
    	HpcDataObjectUploadRequest uploadRequest = new HpcDataObjectUploadRequest();
    	uploadRequest.setPath(path);
    	uploadRequest.setUserId(userId);
    	uploadRequest.setCallerObjectId(callerObjectId);
    	uploadRequest.setSourceLocation(sourceLocation);
    	uploadRequest.setSourceFile(sourceFile);
    	
		// Upload the data object file.
	    return uploadDataObject(uploadRequest);	
	}
	
    @Override
    public HpcDataObjectUploadResponse uploadDataObject(HpcDataObjectUploadRequest uploadRequest) 
                                                       throws HpcException
    {
    	// Input validation.
    	if(uploadRequest.getPath() == null || uploadRequest.getUserId() == null) {
    	   throw new HpcException("Null data object path or user-id", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
        	
    	// Determine the data transfer type.
    	HpcDataTransferType dataTransferType;
    	if(uploadRequest.getSourceLocation() != null) {
    	   dataTransferType = HpcDataTransferType.GLOBUS;
    	   
    	} else if(uploadRequest.getSourceFile() != null) {
    		      dataTransferType = HpcDataTransferType.S_3;
    		      
    	} else {
    		    // Could not determine data transfer type.
    		    throw new HpcException("Could not determine data transfer type",
    		    		               HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
    	// Validate source location exists and accessible.
    	validateFileLocation(dataTransferType, uploadRequest.getSourceLocation(), 
    			             true, true);
    	
    	// Upload the data object using the appropriate data transfer proxy.
  	    return dataTransferProxies.get(dataTransferType).
  	    		   uploadDataObject(getAuthenticatedToken(dataTransferType), 
  	    		                    uploadRequest, 
  	    		                    generateMetadata(uploadRequest.getPath(),
  	    		                    		         uploadRequest.getUserId()));
    }
    
    @Override
	public HpcDataObjectDownloadResponse downloadDataObject(
                                                 HpcFileLocation archiveLocation, 
                                                 HpcFileLocation destinationLocation,
                                                 HpcDataTransferType dataTransferType) 
                                                 throws HpcException
    {
    	HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
    	downloadRequest.setDataTransferType(dataTransferType);
    	downloadRequest.setArchiveLocation(archiveLocation);
    	downloadRequest.setDestinationLocation(destinationLocation);
    	
    	// Create a data object file to download the data if a destination was not provided.
    	if(destinationLocation == null) {
           downloadRequest.setDestinationFile(createFile(downloadDirectory + "/" + 
    	                                                 UUID.randomUUID().toString()));
    	}

    	return downloadDataObject(downloadRequest);
    }
    
    @Override
    public HpcDataObjectDownloadResponse 
              downloadDataObject(HpcDataObjectDownloadRequest downloadRequest) 
                                throws HpcException
    {
    	// Input Validation.
    	HpcDataTransferType dataTransferType = downloadRequest.getDataTransferType();
    	HpcFileLocation destinationLocation = downloadRequest.getDestinationLocation();
    	if(dataTransferType == null || 
    	   !isValidFileLocation(downloadRequest.getArchiveLocation())) {
  	       throw new HpcException("Invalid data transfer request", 
  	    	                      HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// if data is in an archive with S3 data-transfer, and the requested
    	// destination is a GLOBUS endpoint, then we need to perform a 2nd hop. i.e. store 
    	// the data to a local GLOBUS endpoint, and submit a transfer request to the caller's 
    	// destination.
    	HpcDataObjectDownloadRequest secondHopDownloadRequest = null;
    	if(dataTransferType.equals(HpcDataTransferType.S_3) && destinationLocation != null) {
    	   // 2nd Hop needed. Create a request.
    	   secondHopDownloadRequest =
    			 toDownloadRequest(calculateDestination(downloadRequest, 
    			  	            		                HpcDataTransferType.GLOBUS),
    			        		   HpcDataTransferType.GLOBUS,
    			                   downloadRequest.getArchiveLocation().getFileId());
    	   
    	   // Set the first hop file destination to be the source file of the second hop.
    	   downloadRequest.setDestinationFile(
    			   createFile(
    			         dataTransferProxies.get(HpcDataTransferType.GLOBUS).
	                         getFilePath(secondHopDownloadRequest.getArchiveLocation().getFileId(), 
	                        		     false)));
    	   
    	   // Validate the destination location.
    	   validateFileLocation(HpcDataTransferType.GLOBUS, destinationLocation, false, true);
    	}
    	
    	// Download the data object using the appropriate data transfer proxy.
    	HpcDataObjectDownloadResponse downloadResponse =  
    	   dataTransferProxies.get(dataTransferType).
  	    		   downloadDataObject(getAuthenticatedToken(dataTransferType), 
  	                                  downloadRequest);	
    	
    	if(secondHopDownloadRequest != null) {
    		// Perform 2nd hop download.
    		HpcDataObjectDownloadResponse secondHopDownloadResponse = 
    				                      downloadDataObject(secondHopDownloadRequest);
    		
    		// Create an entry to cleanup the file after the async download completes.
    		saveDataObjectDownloadCleanup(secondHopDownloadResponse.getDataTransferRequestId(), 
    		 	                          secondHopDownloadRequest.getDataTransferType(),
    				                      downloadRequest.getDestinationFile().getAbsolutePath());
    		
    		return secondHopDownloadResponse;
    		
    	} else {
    		    return downloadResponse;
    	}
    }
    
	@Override   
	public HpcDataTransferUploadStatus getDataTransferUploadStatus(HpcDataTransferType dataTransferType,
			                                                       String dataTransferRequestId) 
                                                                  throws HpcException
    {	// Input Validation.
		if(dataTransferRequestId == null) {
		   throw new HpcException("Null data transfer request ID", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		return dataTransferProxies.get(dataTransferType).
				   getDataTransferUploadStatus(getAuthenticatedToken(dataTransferType), 
           	                                   dataTransferRequestId);
    }	
	
	@Override   
	public HpcDataTransferDownloadStatus getDataTransferDownloadStatus(HpcDataTransferType dataTransferType,
			                                                           String dataTransferRequestId) 
                                                                      throws HpcException
    {	// Input Validation.
		if(dataTransferRequestId == null) {
		   throw new HpcException("Null data transfer request ID", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		return dataTransferProxies.get(dataTransferType).
				   getDataTransferDownloadStatus(getAuthenticatedToken(dataTransferType), 
           	                                     dataTransferRequestId);
    }	
	
	@Override   
	public long getDataTransferSize(HpcDataTransferType dataTransferType,
			                        String dataTransferRequestId) 
                                   throws HpcException
    {	// Input Validation.
		if(dataTransferRequestId == null) {
		   throw new HpcException("Null data transfer request ID", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		return dataTransferProxies.get(dataTransferType).
				   getDataTransferSize(getAuthenticatedToken(dataTransferType), 
           	                           dataTransferRequestId);
    }	
	
	@Override
	public HpcPathAttributes getPathAttributes(HpcDataTransferType dataTransferType,
			                                   HpcFileLocation fileLocation,
			                                   boolean getSize) 
                                              throws HpcException
    {
    	// Input validation.
    	if(!HpcDomainValidator.isValidFileLocation(fileLocation)) {	
    	   throw new HpcException("Invalid file location", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	return dataTransferProxies.get(dataTransferType).
    			   getPathAttributes(getAuthenticatedToken(dataTransferType), 
    			                     fileLocation, getSize);
    }
	
	@Override
	public File getArchiveFile(HpcDataTransferType dataTransferType,
                               String fileId)  
    	                      throws HpcException
    {
    	// Input validation.
    	if(fileId == null) {	
    	   throw new HpcException("Invalid file id", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	File file = new File(dataTransferProxies.get(dataTransferType).getFilePath(fileId, true));
    	if(!file.exists()) {
    	   throw new HpcException("Archive file could not be found: " + file.getAbsolutePath(),
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return file;
    }
	
	@Override
	public List<HpcDataObjectDownloadCleanup> getDataObjectDownloadCleanups() throws HpcException
	{
		return dataObjectDownloadCleanupDAO.getAll();
	}
	
	@Override
	public void cleanupDataObjectDownloadFile(
	                   HpcDataObjectDownloadCleanup dataObjectDownloadCleanup) 
	                   throws HpcException
	{
		// Input validation
		if(dataObjectDownloadCleanup == null) {
		   throw new HpcException("Invalid data object download cleanup request", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		// Delete the download file.
		if(!FileUtils.deleteQuietly(new File(dataObjectDownloadCleanup.getFilePath()))) {
		   logger.error("Failed to delete file: " + dataObjectDownloadCleanup.getFilePath());
		}
		
		// Cleanup the DB record.
		dataObjectDownloadCleanupDAO.delete(dataObjectDownloadCleanup.getDataTransferRequestId());
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get the data transfer authenticated token from the request context.
     * If it's not in the context, get a token by authenticating.
     * 
     * @param dataTransferType The data transfer type.
     *
     * @throws HpcException If it failed to obtain an authentication token.
     */
    private Object getAuthenticatedToken(HpcDataTransferType dataTransferType) throws HpcException
    {
    	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    	if(invoker == null) {
	       throw new HpcException("Unknown user",
			                      HpcRequestRejectReason.INVALID_DATA_TRANSFER_ACCOUNT);
    	}
    	
    	// Search for an existing token.
    	for(HpcDataTransferAuthenticatedToken authenticatedToken : 
    		invoker.getDataTransferAuthenticatedTokens()) {
    		if(authenticatedToken.getDataTransferType().equals(dataTransferType)) {
    	       return authenticatedToken.getDataTransferAuthenticatedToken();
    		}
    	}
    	
    	// No authenticated token found for this request. Create one.
        Object token = dataTransferProxies.get(dataTransferType).
        		           authenticate(systemAccountLocator.getSystemAccount(dataTransferType));
    	if(token == null) {
    	   throw new HpcException("Invalid data transfer account credentials",
                                  HpcRequestRejectReason.INVALID_DATA_TRANSFER_ACCOUNT);
    	}
    	
    	// Store token on the request context.
    	HpcDataTransferAuthenticatedToken authenticatedToken = new HpcDataTransferAuthenticatedToken();
    	authenticatedToken.setDataTransferAuthenticatedToken(token);
    	authenticatedToken.setDataTransferType(dataTransferType);
    	invoker.getDataTransferAuthenticatedTokens().add(authenticatedToken);
    	
    	return token;
    }
    
    /**
     * Create a download request for a 2nd hop download from local Globus endpoint
     * to caller's destination.
     * 
     * @param destinationLocation The caller's destination.
     * @param dataTransferType The data transfer type to create the request
     * @param The data object logical path.
     *
     * @throws HpcException If it failed to obtain an authentication token.
     */
    private HpcDataObjectDownloadRequest toDownloadRequest(HpcFileLocation destinationLocation,
    		                                               HpcDataTransferType dataTransferType,
    		                                               String path)
    		                                              throws HpcException
    {
    	// Create a source location.
    	HpcFileLocation sourceLocation = 
    	   dataTransferProxies.get(dataTransferType).getDownloadSourceLocation(path);
    	                        
    	// Create and return a download request, from the local GLOBUS endpoint, to the caller's
    	// destination.
    	HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
    	downloadRequest.setArchiveLocation(sourceLocation);
    	downloadRequest.setDestinationLocation(destinationLocation);
    	downloadRequest.setDataTransferType(dataTransferType);
    	
    	return downloadRequest;
    }
    
    /**
     * Calculate data transfer destination
     * 
     * @param downloadRequest The download request.
     * @param dataTransferType The data transfer type to create the request
     *
     * @throws HpcException 
     */    
    private HpcFileLocation calculateDestination(HpcDataObjectDownloadRequest downloadRequest,
    		                                     HpcDataTransferType dataTransferType) 
    		                                    throws HpcException
    {
    	HpcFileLocation destinationLocation = downloadRequest.getDestinationLocation();
	    if(getPathAttributes(dataTransferType, destinationLocation, false).getIsDirectory()) {
           // Caller requested to download to a directory. Append the source file name.
           HpcFileLocation calcDestination = new HpcFileLocation();
           calcDestination.setFileContainerId(destinationLocation.getFileContainerId());
           String sourcePath = downloadRequest.getArchiveLocation().getFileId();
           calcDestination.setFileId(destinationLocation.getFileId() + 
                                     sourcePath.substring(sourcePath.lastIndexOf('/')));
           return calcDestination;
           
	    } else {
	    	    return destinationLocation;
	    }
    }
    
    /**
     * Generate metadata to attach to the data object in the archive:
     * 1. Path - the data object path in the DM system (iRODS)
     * 2. User ID - the user id that registers the data object. 
     * 
     * @param path The data object logical path.
     * @param userId The user-id uploaded the data.
     * 
     * @return a List of the 2 metadata.
     */
    private List<HpcMetadataEntry> generateMetadata(String path, String userId) 
    {
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
       	
       	// Create the user-id metadata.
       	HpcMetadataEntry entry = new HpcMetadataEntry();
    	entry.setAttribute(USER_ID_ATTRIBUTE);
    	entry.setValue(userId);
       	metadataEntries.add(entry);
       	
       	// Create the path metadata.
       	entry = new HpcMetadataEntry();
    	entry.setAttribute(PATH_ATTRIBUTE);
    	entry.setValue(path);
       	metadataEntries.add(entry);
       	
       	return metadataEntries;
    }
    
    /**
     * Create an empty file.
     * 
     * @param filePath The file's path
     * @return File
     */
    private File createFile(String filePath) throws HpcException
    {
    	File file = new File(filePath);
    	
    	try {
    		 FileUtils.touch(file);
  	         
    	} catch(IOException e) {
  		        throw new HpcException("Failed to create a file: " + filePath, 
                                       HpcErrorType.DATA_TRANSFER_ERROR, e);
    	}
    	
    	return file;
  	}
    
    /**
     * Create and store an entry in the DB to cleanup download file after 2nd hop async
     * transfer is complete. 
     * 
     * @param dataTransferRequestId The data transfer request ID.
     * @param dataTransferType The data transfer type.
     * @param filePath The download file path to remove.
     */
    private void saveDataObjectDownloadCleanup(String dataTransferRequestId, 
    		                                   HpcDataTransferType dataTransferType,
    		                                   String filePath)
    {
    	HpcDataObjectDownloadCleanup dataObjectDownloadCleanup = new HpcDataObjectDownloadCleanup();
    	dataObjectDownloadCleanup.setDataTransferRequestId(dataTransferRequestId);
    	dataObjectDownloadCleanup.setDataTransferType(dataTransferType);
    	dataObjectDownloadCleanup.setFilePath(filePath);
    	
    	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    	if(invoker != null && invoker.getNciAccount() != null) {
    	   dataObjectDownloadCleanup.setUserId(invoker.getNciAccount().getUserId());
    	} else {
    		    dataObjectDownloadCleanup.setUserId("Unknown");
    	}
    	
    	try {
    		 dataObjectDownloadCleanupDAO.upsert(dataObjectDownloadCleanup);
    		 
    	} catch(HpcException e) {
    		    logger.error("Failed to persist Data Object Download Cleanup record", e);
    	}
    }
    
    /**
     * Validate file location.
     * 
     * @param dataTransferType The data transfer type.
     * @param fileLocation The file location to validate. If null, no validation is performed.
     * @param validateExists If true, validate the file exists.
     * @param validateAccessible If true, validate file accessible.
     * 
     * @throws HpcException if validation failed.
     */
    private void validateFileLocation(HpcDataTransferType dataTransferType,
                                      HpcFileLocation fileLocation,
                                      boolean validateExists,
                                      boolean validateAccessible) 
                                     throws HpcException
    		                                   
    {
    	if(fileLocation == null) {
    	   return;
    	}
    
	   	HpcPathAttributes pathAttributes = getPathAttributes(dataTransferType, 
	   			                                             fileLocation, false);
		
	   	// Validate file exists.
		if(validateExists && !pathAttributes.getExists()) {
	 	   throw new HpcException("File location doesn't exist: " + 
	 			                  fileLocation.getFileContainerId() + ":" +
	 			                  fileLocation.getFileId(), 
	 	                          HpcErrorType.INVALID_REQUEST_INPUT);	
	 	}
		
	   	// Validate file accessible
		if(validateAccessible && !pathAttributes.getIsAccessible()) {
	 	   throw new HpcException("File location not accessible: " + 
	 			                  fileLocation.getFileContainerId() + ":" +
	 			                  fileLocation.getFileId(), 
	 	                          HpcErrorType.INVALID_REQUEST_INPUT);	
	 	}
    }
}
 