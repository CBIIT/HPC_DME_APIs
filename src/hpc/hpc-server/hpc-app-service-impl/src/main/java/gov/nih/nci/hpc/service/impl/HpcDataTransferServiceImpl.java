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
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
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
	
	// The download directory
	private String downloadDirectory = null;
	
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
    	   if(!isValidFileLocation(uploadRequest.getSourceLocation())) {
    	      throw new HpcException("Invalid upload file location", 
    	    	                     HpcErrorType.INVALID_REQUEST_INPUT);
    	   }
    	   dataTransferType = HpcDataTransferType.GLOBUS;
    	   
    	} else if(uploadRequest.getSourceFile() != null) {
    		      dataTransferType = HpcDataTransferType.S_3;
    		      
    	} else {
    		    // Could not determine data transfer type.
    		    throw new HpcException("Could not determine data transfer type",
    		    		               HpcErrorType.UNEXPECTED_ERROR);
    	}

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
    	downloadRequest.setTransferType(dataTransferType);
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
    	HpcDataTransferType dataTransferType = downloadRequest.getTransferType();
    	HpcFileLocation destinationLocation = downloadRequest.getDestinationLocation();
    	if(dataTransferType == null || 
    	   !isValidFileLocation(downloadRequest.getArchiveLocation()) ||
    	   (destinationLocation != null && !isValidFileLocation(destinationLocation))) {
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
    			         dataTransferProxies.get(dataTransferType).
	                         getFilePath(secondHopDownloadRequest.getArchiveLocation().getFileId(), 
	                        		     false)));
    	}
    	
    	// Download the data object using the appropriate data transfer proxy.
    	HpcDataObjectDownloadResponse downloadResponse =  
    	   dataTransferProxies.get(dataTransferType).
  	    		   downloadDataObject(getAuthenticatedToken(dataTransferType), 
  	                                  downloadRequest);	
    	
    	// Perform 2nd hop download if needed.
    	if(secondHopDownloadRequest != null) {
    	   return downloadDataObject(secondHopDownloadRequest);
    	} else {
    		    return downloadResponse;
    	}
    }
    
	@Override   
	public HpcDataTransferStatus getDataTransferStatus(HpcDataTransferType dataTransferType,
			                                           String dataTransferRequestId) 
                                                      throws HpcException
    {	// Input Validation.
		if(dataTransferRequestId == null) {
		   throw new HpcException("Null data transfer request ID", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		return dataTransferProxies.get(dataTransferType).
				   getDataTransferStatus(getAuthenticatedToken(dataTransferType), 
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
    HpcDataObjectDownloadRequest toDownloadRequest(HpcFileLocation destinationLocation,
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
    	downloadRequest.setTransferType(dataTransferType);
    	
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
    	String directoryPath = filePath.substring(0, filePath.lastIndexOf('/'));
	
    	File file = null;
    	try {
    		 // Create directory if needed.
  	         File directory = new File(directoryPath);
  	         if(!directory.exists()) {
  	     	    directory.mkdirs();
  	         }
  	
  	         // Create the file.
  	         file = new File(filePath);
  	         if(!file.createNewFile()) {
  	            throw new HpcException("File already exists: " + file.getAbsolutePath(), 
  			                           HpcErrorType.DATA_TRANSFER_ERROR);	
  	         }  	
  	         
    	} catch(IOException e) {
  		        throw new HpcException("Failed to create a file: " + file.getAbsolutePath(), 
                                       HpcErrorType.DATA_TRANSFER_ERROR);
    	}
    	
    	return file;
  	}
}
 