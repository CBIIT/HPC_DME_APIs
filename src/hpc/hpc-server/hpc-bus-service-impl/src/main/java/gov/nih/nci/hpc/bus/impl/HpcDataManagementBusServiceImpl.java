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
import gov.nih.nci.hpc.domain.datamanagement.HpcGroupPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcDataObjectSystemGeneratedMetadata;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcSecurityService;

import java.io.File;
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
    private HpcDataTransferService dataTransferService = null;
	
	@Autowired
    private HpcDataManagementService dataManagementService = null;
	
	@Autowired
    private HpcSecurityService securityService = null;
	
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
    // HpcDataManagementBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public boolean registerCollection(String path,
    				                  List<HpcMetadataEntry> metadataEntries)  
    		                         throws HpcException
    {
    	logger.info("Invoking registerCollection(List<HpcMetadataEntry>): " + 
    			    metadataEntries);
    	
    	// Input validation.
    	if(path == null || path.isEmpty() || metadataEntries == null || metadataEntries.size() == 0) {
    	   throw new HpcException("Null path or metadata entries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Create a collection directory.
    	boolean created = dataManagementService.createDirectory(path);
    	
    	// Attach the metadata.
    	if(created) {
    	   boolean registrationCompleted = false; 
    	   try {
    		    // Assign system account as an additional owner of the collection.
    		    dataManagementService.assignSystemAccountPermission(path);
    		    
    		    // Add user provided metadata.
    	        dataManagementService.addMetadataToCollection(path, metadataEntries);
       	   
    	        // Generate system metadata and attach to the collection.
       	        dataManagementService.addSystemGeneratedMetadataToCollection(path);
       	        
       	        registrationCompleted = true;
       	        
    	   } finally {
			          if(!registrationCompleted) {
				         // Collection registration failed. Remove it from Data Management.
				         dataManagementService.delete(path);
			          }
	       }
       	   
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
    			// Failed to get metadata.
    			logger.error("Failed to fetch metadata for "+ collection.getAbsolutePath(), e);
    			continue;
    		}
    	}
    	
    	return collectionsDTO;
    }
    
    @Override
    public boolean registerDataObject(String path,
    		                          HpcDataObjectRegistrationDTO dataObjectRegistrationDTO,
    		                          File dataObjectFile)  
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
    		    // Assign system account as an additional owner of the data-object.
   		        dataManagementService.assignSystemAccountPermission(path);
   		  
		        // Attach the user provided metadata.
		        dataManagementService.addMetadataToDataObject(
		    	   		                 path, 
		    			                 dataObjectRegistrationDTO.getMetadataEntries());
		       
		        // Extract the source location.
		        HpcFileLocation source = dataObjectRegistrationDTO.getSource();
				if(source != null && 
				   (source.getFileContainerId() == null && source.getFileId() == null)) {
				   source = null;
				}
				
				// Transfer the data file.
		        HpcDataObjectUploadResponse uploadResponse = 
		           dataTransferService.uploadDataObject(
		        	   source, dataObjectFile, path, 
		        	   securityService.getRequestInvoker().getNciAccount().getUserId(),
		        	   dataObjectRegistrationDTO.getCallerObjectId());
		        Long sourceSize = source != null ? 
		        		          dataTransferService.getPathAttributes(uploadResponse.getDataTransferType(), 
		        		        		                                source, true).getSize() : 
		        		          null;
		        		        		                                
		   		if(dataObjectFile != null)
					sourceSize = dataObjectFile.length();
		     
			    // Generate system metadata and attach to the data object.
			    dataManagementService.addSystemGeneratedMetadataToDataObject(
			        		                   path, uploadResponse.getArchiveLocation(),
			    			                   source, uploadResponse.getDataTransferRequestId(), 
			    			                   uploadResponse.getDataTransferStatus(),
			    			                   uploadResponse.getDataTransferType(),
			    			                   sourceSize, 
			    			                   dataObjectRegistrationDTO.getCallerObjectId()); 
	
			     registrationCompleted = true;
			     
	    	} finally {
	    			   if(!registrationCompleted) {
	    				  // Data object registration failed. Remove it from Data Management.
	    				  dataManagementService.delete(path);
	    			   }
	    	}
    	   
	    } else {
	    	if(dataObjectFile != null)
	    		   throw new HpcException("Data object cannot be updated. Only updating metadata is allowed.",
			                  HpcErrorType.REQUEST_REJECTED);	
	    	
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
    			     getDataTransferUploadPercentCompletion(
    			        dataManagementService.getDataObjectSystemGeneratedMetadata(metadataEntries)));
    }
    
    @Override
    public HpcDataObjectListDTO getDataObjects(List<HpcMetadataQuery> metadataQueries,
    		                                   List<HpcMetadataQuery> collectionMetadataQueries) 
                                           throws HpcException
    {
    	logger.info("Invoking getDataObjects(List<HpcMetadataQuery>, List<HpcMetadataQuery>): " + 
    			    metadataQueries + "***" + collectionMetadataQueries);
    	
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
    		dataObjectsDTO.getDataObjects().add(
    			toDTO(dataObject, metadataEntries,
    			      getDataTransferUploadPercentCompletion(
    			       	 dataManagementService.getDataObjectSystemGeneratedMetadata(metadataEntries))));
    	}
    	
    	return dataObjectsDTO;
    }
    @Override
    public HpcDataObjectDownloadResponseDTO 
              downloadDataObject(String path,
                                 HpcDataObjectDownloadRequestDTO downloadRequestDTO)
                                throws HpcException
    {
    	logger.info("Invoking downloadDataObject(path, downloadReqest, dataObjectFile): " + path + ", " + 
                    downloadRequestDTO);
    	
    	// Input validation.
    	if(path == null || downloadRequestDTO == null) {
    	   throw new HpcException("Null path or download request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Get the System generated metadata.
    	HpcDataObjectSystemGeneratedMetadata metadata = 
    	   dataManagementService.getDataObjectSystemGeneratedMetadata(path);
    	
    	// Validate the file is archived.
    	if(!metadata.getDataTransferStatus().equals(HpcDataTransferUploadStatus.ARCHIVED)) {
    	   throw new HpcException("Object is not in archived state yet. It is in " +
    			                  metadata.getDataTransferStatus().value() + " state",
    			                  HpcRequestRejectReason.FILE_NOT_ARCHIVED);
    	}
    	
    	// Download the data object.
    	HpcDataObjectDownloadResponse downloadResponse = 
    	   dataTransferService.downloadDataObject(metadata.getArchiveLocation(), 
                                                  downloadRequestDTO.getDestination(),
                                                  metadata.getDataTransferType());
        
        // Construct and return download response DTO.
        HpcDataObjectDownloadResponseDTO downloadResponseDTO = new HpcDataObjectDownloadResponseDTO();
        downloadResponseDTO.setDestinationLocation(downloadResponse.getDestinationLocation());
        downloadResponseDTO.setRequestId(downloadResponse.getDataTransferRequestId());
        downloadResponseDTO.setDestinationFile(downloadResponse.getDestinationFile());
        return downloadResponseDTO;
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
    		    	 // Set the data management permission.
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
    		
       		for(HpcGroupPermission groupPermissionRequest : entityPermissionRequest.getGroupPermissions()) {
    			HpcGroupPermissionResponseDTO groupPermissionResponse = new HpcGroupPermissionResponseDTO();
    			groupPermissionResponse.setGroupId(groupPermissionRequest.getGroupId());
    			groupPermissionResponse.setResult(true);
    		    try {
    		    	 // Set the data management permission.
    		    	 //HpcPathAttributes pathAttributes =
    		    		dataManagementService.setPermission(entityPermissionRequest.getPath(), 
    		    				groupPermissionRequest);
    		    } catch(HpcException e) {
    		    	    // Request failed. Record the message and keep going.
    		    		groupPermissionResponse.setResult(false);
    		    		groupPermissionResponse.setMessage(e.getMessage());
    		    }
     		
    		    // Add this user permission response to the list.
    		    entityPermissionResponse.getGroupPermissionResponses().add(groupPermissionResponse);
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
			List<HpcGroupPermission> groupPermissionRequests = entityPermissionRequest.getGroupPermissions();
			
			if((userPermissionRequests == null || userPermissionRequests.isEmpty()) && (groupPermissionRequests == null || groupPermissionRequests.isEmpty())) {
			   throw new HpcException("Null or empty user and group permission requests for path: " + path,
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
			
			// Validate the group permission requests for this path. 
			Set<String> groupIds = new HashSet<String>(); 
			for(HpcGroupPermission groupPermissionRequest : groupPermissionRequests) {
				String groupId = groupPermissionRequest.getGroupId();
				String permission = groupPermissionRequest.getPermission();
				if(groupId == null || groupId.isEmpty()) { 
				   throw new HpcException("Null or empty groupId in a permission request for path: " + path,
                                          HpcErrorType.INVALID_REQUEST_INPUT);	
				}
				if(!groupIds.add(groupId)) {
				   throw new HpcException("Duplicate groupId in a permission request for path: " + path +
						                  ", groupId: " + groupId,
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
     * Calculate the data transfer % completion if transfer is in progress
     * 
     * @param dataObject The data object to check the timeout for.
     * 
     * @return The transfer % completion if transfer is in progress, or null otherwise.
     *         e.g 86%.
     */
	private String getDataTransferUploadPercentCompletion(
			          HpcDataObjectSystemGeneratedMetadata systemGeneratedMetadata)
	{
		// Get the transfer status, transfer request id and data-object size from the metadata entries.
		HpcDataTransferUploadStatus transferStatus = systemGeneratedMetadata.getDataTransferStatus();
		logger.info("getDataTransferUploadPercentCompletion "+transferStatus.value());
		if(transferStatus == null || 
		   (!transferStatus.equals(HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE) &&
			!transferStatus.equals(HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE))) {
		   // data transfer not in progress.
		   return null;
		} else if(transferStatus.equals(HpcDataTransferUploadStatus.ARCHIVED))
			return "100%";
		
		
		String dataTransferRequestId = systemGeneratedMetadata.getDataTransferRequestId();
		Long sourceSize = systemGeneratedMetadata.getSourceSize();
		
		if(dataTransferRequestId == null || sourceSize == null || sourceSize <= 0) {
		   return "Unknown";	
		}
		
		// Get the size of the data transferred so far.
		long transferSize = 0;
		try {
		     transferSize = dataTransferService.getDataTransferSize(
		    		                    systemGeneratedMetadata.getDataTransferType(),
		    		                    dataTransferRequestId);
		} catch(HpcException e) {
			    logger.error("Failed to get data transfer size: " + dataTransferRequestId);
			    return "Unknown";
		}
		
		return String.valueOf((transferSize * 100L) / sourceSize) + '%';
	}
}

 