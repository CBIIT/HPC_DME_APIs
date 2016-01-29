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
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
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

import java.util.HashSet;
import java.util.List;
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
    		
    			//Combine collection attributes and metadata into a single DTO.
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
    public void registerDataObject(String path,
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
    	
    	// Calculate the data transfer destination absolute path as the following:
    	// 'base path' / 'caller's data transfer provided path OR account ID' / 'logical path'
    	StringBuffer destinationPath = new StringBuffer();
    	destinationPath.append(dataTransferDestination.getPath());
    	String filePath = dataObjectRegistrationDTO.getFilePath();
    	if(filePath != null && !filePath.isEmpty()) {
    	   if(filePath.charAt(0) != '/') {
    		  destinationPath.append('/'); 
    	   }
    	   destinationPath.append(filePath);
/*    	} else {
    		    // Caller did not provide a path, inject their data-transfer user ID
    		    destinationPath.append('/');
    		    destinationPath.append(userService.getRequestInvoker().
    		    		                           getDataTransferAccount().
    		    		                           getUsername());
 */
    	}
    	destinationPath.append('/');
    	destinationPath.append(path);
    	 
    	HpcFileLocation destination = new HpcFileLocation();
    	destination.setEndpoint(dataTransferDestination.getEndpoint());
    	destination.setPath(destinationPath.toString());
    	
    	// Create a data object file (in the data management system).
    	dataManagementService.createFile(path, false);
    	
		// Transfer the file. 
        //TODO: Persist data transfer report into database
    	dataTransferService.transferData(dataObjectRegistrationDTO.getSource(), 
        		                         destination);				 
    	
    	// Attach the user provided metadata.
    	dataManagementService.addMetadataToDataObject(
    			                 path, 
    			                 dataObjectRegistrationDTO.getMetadataEntries());
    	
    	// Generate system metadata and attach to the data object.
    	dataManagementService.addSystemGeneratedMetadataToDataObject(
    			                 path, destination,
    			                 dataObjectRegistrationDTO.getSource()); 
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
    		
    	return toDTO(dataObject, metadataEntries);
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
    		try
    		{
    		// Get the metadata for this data object.
    		metadataEntries = 
    		dataManagementService.getDataObjectMetadata(dataObject.getAbsolutePath());
    		}
    		catch(HpcException e)
    		{
    			//Unable to find metadata for the object
    			continue;
    		}
    		// Combine data object attributes and metadata into a single DTO.
    		dataObjectsDTO.getDataObjects().add(toDTO(dataObject, metadataEntries));
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
    	
		// Transfer the file.
        dataTransferService.transferData(dataManagementService.getFileLocation(path),
        		                         downloadRequest.getDestination());	
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
     *
     * @return The DTO.
     */
    private HpcDataObjectDTO toDTO(HpcDataObject dataObject, List<HpcMetadataEntry> metadataEntries) 
    {
    	HpcDataObjectDTO dataObjectDTO = new HpcDataObjectDTO();
    	dataObjectDTO.setDataObject(dataObject);
    	if(metadataEntries != null) {
    	   dataObjectDTO.getMetadataEntries().addAll(metadataEntries);
    	}
    	
    	return dataObjectDTO;
    }
    
    /** Validate permissions requests list.
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
}

 