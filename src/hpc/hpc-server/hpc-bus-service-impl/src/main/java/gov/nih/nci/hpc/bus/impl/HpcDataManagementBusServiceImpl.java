/**
 * HpcDataManagementBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferRequestInfo;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcUserService;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Management Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataManagementBusServiceImpl implements HpcDataManagementBusService
{  
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Data transfer status check timeout, in days. If these many days pass 
	// after the data registration date, and we still can't get a data transfer 
	// status, then the state will move to UNKNOWN.
	private final static int DATA_TRANSFER_STATUS_CHECK_TIMEOUT_PERIOD = 1;
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
	
	@Autowired
    private HpcUserService userService = null;
	
	@Autowired
    private HpcDataTransferService dataTransferService = null;
	
	@Autowired
    private HpcDataManagementService dataManagementService = null;
	
	@Autowired
	HpcFileLocation dataTransferDestination = null;
	
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcDataManagementBusServiceImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcCollectionBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public boolean registerCollection(String path,
    				                  List<HpcMetadataEntry> metadataEntries)  
    		                         throws HpcException
    {
    	logger.info("Invoking registerCollection(List<HpcMetadataEntry>): " + 
    			    metadataEntries);
    	
    	// Input validation.
    	if(path == null || metadataEntries == null) {
    	   throw new HpcException("Null path or metadata entries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Create a collection directory.
    	boolean created = dataManagementService.createDirectory(path);
    	
    	// Attach the metadata.
    	if(created) {
    	   dataManagementService.addMetadataToCollection(path, metadataEntries);
       	   
    	   // Generate system metadata and attach to the collection.
       	   dataManagementService.addSystemGeneratedMetadataToCollection(path);
       	   
    	} else {
    		    dataManagementService.updateCollectionMetadata(path, metadataEntries);
    	}
    	
    	return created;
    }
    
    @Override
    public HpcCollectionDTO getCollection(String path) throws HpcException
    {
    	logger.info("Invoking getCollection(String): " + path);
    	
    	// Input validation.
    	if(path == null) {
    	   throw new HpcException("Null collection path",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Get the collection.
    	HpcCollection collection = dataManagementService.getCollection(path);
    	if(collection == null) {
    	   return null;
    	}
    		
    	// Get the metadata for this collection.
    	List<HpcMetadataEntry> metadataEntries = 
    		                   dataManagementService.getCollectionMetadata(path);
    		
    	return toDTO(collection, metadataEntries);
    }
    
    @Override
    public HpcCollectionListDTO getCollections(List<HpcMetadataQuery> metadataQueries) 
                                              throws HpcException
    {
    	logger.info("Invoking getCollections(List<HpcMetadataQuery>): " + 
    			    metadataQueries);
    	
    	// Input validation.
    	if(metadataQueries == null) {
    	   throw new HpcException("Null metadata queries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Construct the DTO.
    	HpcCollectionListDTO collectionsDTO = new HpcCollectionListDTO();
    	for(HpcCollection collection : dataManagementService.getCollections(metadataQueries)) {
    		// Get the metadata for this collection.
    		try
    		{
    			List<HpcMetadataEntry> metadataEntries = 
    					dataManagementService.getCollectionMetadata(collection.getAbsolutePath());
    		
    			// Combine collection attributes and metadata into a single DTO.
    			collectionsDTO.getCollections().add(toDTO(collection, metadataEntries));
    		}
    		catch(HpcException e)
    		{
    			//Failed to get metadata
    			logger.error("Failed to fetch metadata for "+collection.getAbsolutePath(), e);
    			continue;
    		}
    	}
    	
    	return collectionsDTO;
    }
    
    @Override
    public boolean registerDataObject(String path,
    		                          HpcDataObjectRegistrationDTO dataObjectRegistrationDTO)  
    		                         throws HpcException
    {
    	logger.info("Invoking registerDataObject(HpcDataObjectRegistrationDTO): " + 
    			    dataObjectRegistrationDTO);
    	
    	// Input validation.
    	if(path == null || dataObjectRegistrationDTO == null) {
    	   throw new HpcException("Null path or dataObjectRegistrationDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Create a data object file (in the data management system).
	    boolean created = dataManagementService.createFile(path, false);
	    
	    if(created) {
    	   boolean registrationCompleted = false; 
    	   try {
		        // Attach the user provided metadata.
		        dataManagementService.addMetadataToDataObject(
		    	   		                 path, 
		    			                 dataObjectRegistrationDTO.getMetadataEntries());
		     
			     // Calculate the data transfer destination to deposit the data object.
			     HpcFileLocation destination = 
			        getDataObjectRegistrationDestination(path, 
			        		                             dataObjectRegistrationDTO.getFilePath());
			     HpcFileLocation source = dataObjectRegistrationDTO.getSource(); 
			     
				 // Submit a request to transfer the file (this is performed async). 
			     String dataTransferRequestId = 
			                dataTransferService.transferData(source, destination);	
			     
			     // Get the data object file(s) size.
			     long size = dataTransferService.getPathAttributes(source, true).getSize();
			     
			     // Generate system metadata and attach to the data object.
			     dataManagementService.addSystemGeneratedMetadataToDataObject(
			        		                    path, destination,
			    			                    dataObjectRegistrationDTO.getSource(),
			    			                    dataTransferRequestId, size); 
	
			     registrationCompleted = true;
			     
	    	} finally {
	    			   if(!registrationCompleted) {
	    				  // Data object registration failed. Remove it from Data Management.
	    				  dataManagementService.deleteFile(path);
	    			   }
	    	}
    	   
	    } else {
	    	    // Attach the user provided metadata.
	            dataManagementService.updateDataObjectMetadata(
	    	   	    	                    path, 
	    			                        dataObjectRegistrationDTO.getMetadataEntries());
	    }
	    
	    return created;
    }
    
    @Override
    public HpcDataObjectDTO getDataObject(String path) throws HpcException
    {
    	logger.info("Invoking getDataObject(String): " + path);
    	
    	// Input validation.
    	if(path == null) {
    	   throw new HpcException("Null data object path",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Get the data object.
    	HpcDataObject dataObject = dataManagementService.getDataObject(path);
    	if(dataObject == null) {
    	   return null;
    	}
    		
    	// Get the metadata for this data object.
    	List<HpcMetadataEntry> metadataEntries = 
    		                   dataManagementService.getDataObjectMetadata(path);
    		
    	return toDTO(dataObject, metadataEntries, 
    			     getDataTransferPercentCompletion(metadataEntries));
    }
    
    @Override
    public HpcDataObjectListDTO getDataObjects(List<HpcMetadataQuery> metadataQueries) 
                                           throws HpcException
    {
    	logger.info("Invoking getDataObjects(List<HpcMetadataQuery>): " + 
    			    metadataQueries);
    	
    	// Input validation.
    	if(metadataQueries == null) {
    	   throw new HpcException("Null metadata queries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Construct the DTO.
    	HpcDataObjectListDTO dataObjectsDTO = new HpcDataObjectListDTO();
    	for(HpcDataObject dataObject : dataManagementService.getDataObjects(metadataQueries)) {
    		List<HpcMetadataEntry> metadataEntries = null;
    		try {
    			 // Get the metadata for this data object.
    		     metadataEntries = 
    		             dataManagementService.getDataObjectMetadata(dataObject.getAbsolutePath());
    		} catch(HpcException e) {
    			    // Unable to find metadata for the object
    			    continue;
    		}
    		
    		// Combine data object attributes and metadata into a single DTO.
    		dataObjectsDTO.getDataObjects().add(toDTO(dataObject, metadataEntries,
    				                                  getDataTransferPercentCompletion(metadataEntries)));
    	}
    	
    	return dataObjectsDTO;
    }
    @Override
    public void downloadDataObject(String path,
                                   HpcDataObjectDownloadDTO downloadRequest)
                                  throws HpcException
    {
    	logger.info("Invoking downloadDataObject(path, downloadReqest): " + path + ", " + 
                    downloadRequest);
    	
    	// Input validation.
    	if(path == null || downloadRequest == null || 
    	   downloadRequest.getDestination() == null) {
    	   throw new HpcException("Null path or download request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Calculate the data object download destination.
    	HpcFileLocation source = dataManagementService.getFileLocation(path);
    	HpcFileLocation destination = 
    	   getDataObjectDownloadDestination(source, downloadRequest.getDestination());
    	
		// Transfer the file.
        dataTransferService.transferData(source, destination);	
    }
    
    @Override
    public void closeConnection()
    {
    	dataManagementService.closeConnection();
    }
    
    @Override
	public HpcEntityPermissionResponseListDTO setPermissions(
                    List<HpcEntityPermissionRequestDTO> entityPermissionRequests)
                    throws HpcException
    {
    	// Input Validation.
    	validatePermissionRequests(entityPermissionRequests);
    	
    	HpcEntityPermissionResponseListDTO responses = new HpcEntityPermissionResponseListDTO();
    	
    	// Iterate through and execute the entity permission requests.
    	for(HpcEntityPermissionRequestDTO entityPermissionRequest : entityPermissionRequests) {
    		HpcEntityPermissionResponseDTO entityPermissionResponse = new HpcEntityPermissionResponseDTO();
    		entityPermissionResponse.setPath(entityPermissionRequest.getPath());
    		// Execute all user permission requests for this entity (collection or data object).
    		for(HpcUserPermission userPermissionRequest : entityPermissionRequest.getUserPermissions()) {
    			HpcUserPermissionResponseDTO userPermissionResponse = new HpcUserPermissionResponseDTO();
    			userPermissionResponse.setUserId(userPermissionRequest.getUserId());
    			userPermissionResponse.setResult(true);
    		    try {
    			     dataManagementService.setPermission(entityPermissionRequest.getPath(), 
    			    		                             userPermissionRequest);
    			     
    		    } catch(HpcException e) {
    		    	    // Request failed. Record the message and keep going.
    		    		userPermissionResponse.setResult(false);
    		    		userPermissionResponse.setMessage(e.getMessage());
    		    }
     		
    		    // Add this user permission response to the list.
    		    entityPermissionResponse.getUserPermissionResponses().add(userPermissionResponse);
    		}
    		
    		// Add this entity permission response to the list
    		responses.getEntityPermissionResponses().add(entityPermissionResponse);
    	}
    	
    	
    	return responses;
    }
    
    @Override
    public void updateDataTransferStatus() throws HpcException
    {
    	// Iterate through the data objects that their data transfer is in-progress
    	for(HpcDataObject dataObject : dataManagementService.getDataObjectsInProgress()) {
    		String path = dataObject.getAbsolutePath();
    		try {
    		     // Get current data transfer Request Info.
    			 HpcDataTransferRequestInfo dataTransferRequestInfo = 
    			    dataManagementService.getDataTransferRequestInfo(path);
    			 
    			 // Use the registrar data transfer account to check for transfer status.
    			 HpcUser registrar = userService.getUser(dataTransferRequestInfo.getRegistrarId());
    			 if(registrar != null) {
    			    userService.getRequestInvoker().setDataTransferAuthenticatedToken(null);
    			    userService.getRequestInvoker().setDataTransferAccount(registrar.getDataTransferAccount());
    			 }
    			 
    			 // Get the data transfer request status.
    			 HpcDataTransferStatus dataTransferStatus =
    		        dataTransferService.getDataTransferStatus(dataTransferRequestInfo.getRequestId());
    			 
    		     if(dataTransferStatus.equals(HpcDataTransferStatus.ARCHIVED)) {
    		    	// Data transfer completed successfully. Update the metadata.
    		    	dataManagementService.setDataTransferStatus(path, dataTransferStatus);
    		    	logger.info("Data transfer completed: " + path);
    		     } else if(dataTransferStatus.equals(HpcDataTransferStatus.FAILED)) {
     		    	       // Data transfer failed. Remove the data object
     		    	       dataManagementService.deleteFile(path);
     		    	       logger.info("Data transfer failed: " + path);
    		     }
    		     
    		} catch(HpcException e) {
    			    logger.error("Failed to process data transfer update:" + path, e);
    			    
    			    // If timeout occurred, move the status to unknown.
    			    if(isDataTransferStatusCheckTimedOut(dataObject)) {
    			       try {
    			            dataManagementService.setDataTransferStatus(
    			    	                             path, 
    			    	                             HpcDataTransferStatus.UNKNOWN);
    			       } catch(Exception ex) {
    			    	       logger.error("failed to set data transfer status to unknown: " + 
    			                            path, ex);
    			       }
        		       logger.error("Unknown data transfer status: " + path);
    			    }
    		}
    	}
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Create a collection DTO from domain objects.
     * 
     * @param collection The collection domain object.
     * @param metadataEntries The list of metadata domain objects.
     *
     * @return The DTO.
     */
    private HpcCollectionDTO toDTO(HpcCollection collection, List<HpcMetadataEntry> metadataEntries) 
    {
    	HpcCollectionDTO collectionDTO = new HpcCollectionDTO();
    	collectionDTO.setCollection(collection);
    	if(metadataEntries != null) {
    	   collectionDTO.getMetadataEntries().addAll(metadataEntries);
    	}
    	
    	return collectionDTO;
    }
    
    /**
     * Create a data object DTO from domain objects.
     * 
     * @param data object The data object domain object.
     * @param metadataEntries The list of metadata domain objects.
     * transferPercentCompletion The data transfer % completion.
     *
     * @return The DTO.
     */
    private HpcDataObjectDTO toDTO(HpcDataObject dataObject, 
    		                       List<HpcMetadataEntry> metadataEntries,
    		                       String transferPercentCompletion) 
    {
    	HpcDataObjectDTO dataObjectDTO = new HpcDataObjectDTO();
    	dataObjectDTO.setDataObject(dataObject);
    	if(metadataEntries != null) {
    	   dataObjectDTO.getMetadataEntries().addAll(metadataEntries);
    	}
    	dataObjectDTO.setTransferPercentCompletion(transferPercentCompletion);
    	
    	return dataObjectDTO;
    }
    
    /** 
     * Validate permissions requests list.
     * 
     * @param entityPermissionRequests The requests to validate.
     *
     * @throws HpcException if found an invalid request in the list.
     */
	private void validatePermissionRequests(
                         List<HpcEntityPermissionRequestDTO> entityPermissionRequests)
                         throws HpcException
    {
		if(entityPermissionRequests == null || entityPermissionRequests.isEmpty()) {
		   throw new HpcException("Null or empty permission requests",
	                              HpcErrorType.INVALID_REQUEST_INPUT);	
		}
		
		Set<String> paths = new HashSet<String>(); 
		for(HpcEntityPermissionRequestDTO entityPermissionRequest : entityPermissionRequests) {
			// Validate the path is not empty and not a dup.
			String path = entityPermissionRequest.getPath();
			if(path == null || path.isEmpty()) {
			   throw new HpcException("Null or empty path in a permission request",
                                       HpcErrorType.INVALID_REQUEST_INPUT);					
			}
			if(!paths.add(path)) {
			   throw new HpcException("Duplicate path in a permission request: " + path,
                                      HpcErrorType.INVALID_REQUEST_INPUT);	
			}
			
			// Validate the user permission requests for this path. 
			List<HpcUserPermission> userPermissionRequests = entityPermissionRequest.getUserPermissions();
			if(userPermissionRequests == null || userPermissionRequests.isEmpty()) {
			   throw new HpcException("Null or empty user permission requests for path: " + path,
                                      HpcErrorType.INVALID_REQUEST_INPUT);						
			}
			
			Set<String> userIds = new HashSet<String>(); 
			for(HpcUserPermission userPermissionRequest : userPermissionRequests) {
				String userId = userPermissionRequest.getUserId();
				String permission = userPermissionRequest.getPermission();
				if(userId == null || userId.isEmpty()) { 
				   throw new HpcException("Null or empty userId in a permission request for path: " + path,
                                          HpcErrorType.INVALID_REQUEST_INPUT);	
				}
				if(!userIds.add(userId)) {
				   throw new HpcException("Duplicate userId in a permission request for path: " + path +
						                  ", userId: " + userId,
                                          HpcErrorType.INVALID_REQUEST_INPUT);	 
				}
				if(permission == null || permission.isEmpty()) { 
				   throw new HpcException("Null or empty permission in a permission request for path: " + path,
	                                       HpcErrorType.INVALID_REQUEST_INPUT);	
				}
			}
		}
    }
	
    /** 
     * Calculate data transfer destination to deposit a data object
     * 
     * @param path The data object (logical) path.
     * @param dataTransferPath The caller's provided data transfer path.
     * 
     * @return HpcFileLocation The data transfer deposit destination.
     */
	private HpcFileLocation getDataObjectRegistrationDestination(String path, 
			                                                     String dataTransferPath) 
	{
		// Calculate the data transfer destination absolute path as the following:
		// 'base path' / 'caller's data transfer destination path/ 'logical path'
		StringBuffer destinationPath = new StringBuffer();
		destinationPath.append(dataTransferDestination.getPath());
		
		if(dataTransferPath != null && !dataTransferPath.isEmpty()) {
		   if(dataTransferPath.charAt(0) != '/') {
			  destinationPath.append('/'); 
		   }
		   destinationPath.append(dataTransferPath);
		}
		
		destinationPath.append('/');
		destinationPath.append(path);
		 
		HpcFileLocation destination = new HpcFileLocation();
		destination.setEndpoint(dataTransferDestination.getEndpoint());
		destination.setPath(destinationPath.toString());
		
		return destination;
	}
	
    /** 
     * Calculate data transfer destination to download a data object.
     * 
     * @param source The source file location
     * @param destination The user requested file destination
     * 
     * @return HpcFileLocation The data transfer download destination.
     * @throws HpcException
     */
	private HpcFileLocation getDataObjectDownloadDestination(HpcFileLocation source, 
			                                                 HpcFileLocation destination) 
			                                                throws HpcException
	{
		if(!dataTransferService.getPathAttributes(destination, false).getIsDirectory()) {
		   // The user requested destination is NOT a directory, transfer to it.
		   return destination;
		}
		
		// User requested to download to a directory. Append the source file name.
		HpcFileLocation downloadDestination = new HpcFileLocation();
		downloadDestination.setEndpoint(destination.getEndpoint());
		String sourcePath = source.getPath();
		downloadDestination.setPath(destination.getPath() + 
				                    sourcePath.substring(sourcePath.lastIndexOf('/')));
		
		return downloadDestination;
	}
	
    /** 
     * Determine if data transfer status check timed out.
     * 
     * @param dataObject The data object to check the timeout for.
     * 
     * @return true if status check timeout occurred. 
     */
	private boolean isDataTransferStatusCheckTimedOut(HpcDataObject dataObject) 
	{
		if(dataObject.getCreatedAt() == null) {
		   // Creation time unknown.
		   return true;	
		}
		
		// Calculate the timeout.
		Calendar timeout = Calendar.getInstance();
	    timeout.setTime(dataObject.getCreatedAt().getTime());
	    timeout.add(Calendar.DATE, DATA_TRANSFER_STATUS_CHECK_TIMEOUT_PERIOD);
	    
	    // Compare to now.
	    return Calendar.getInstance().after(timeout);
	}
	
    /** 
     * Calculate the data transfer % completion if transfer is in progress
     * 
     * @param dataObject The data object to check the timeout for.
     * 
     * @return The transfer % completion if transfer is in progress, or null otherwise.
     *         e.g 86%.
     */
	private String getDataTransferPercentCompletion(List<HpcMetadataEntry> metadataEntries)
	{
		// Get the transfer status, transfer request id and data-object size from the metadata entries.
		Map<String, String> metadataMap = dataManagementService.toMap(metadataEntries);
		HpcDataTransferStatus transferStatus = 
		   HpcDataTransferStatus.fromValue(metadataMap.get(
				  HpcDataManagementService.FILE_DATA_TRANSFER_STATUS_ATTRIBUTE));
		String dataTransferRequestId = metadataMap.get(
				                       HpcDataManagementService.FILE_DATA_TRANSFER_ID_ATTRIBUTE);
		Long dataObjectSize = Long.valueOf(metadataMap.get(
				                           HpcDataManagementService.FILE_SIZE_ATTRIBUTE));
		
		if(transferStatus == null || !transferStatus.equals(HpcDataTransferStatus.IN_PROGRESS)) {
		   // data transfer not in progress.
		   return null;
		}
		
		if(dataTransferRequestId == null ||
		   dataObjectSize == null || dataObjectSize == 0) {
		   return "Unknown";	
		}
		
		// Get the size of the data transferred so far.
		long transferSize = 0;
		try {
		     transferSize = dataTransferService.getDataTransferSize(dataTransferRequestId);
		} catch(HpcException e) {
			    logger.error("Failed to get data transfer size: " + dataTransferRequestId);
			    return "Unknown";
		}
		
		return String.valueOf((transferSize * 100L) / dataObjectSize) + '%';
	}
}

 