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
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcGroupPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadReceiptDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadResponseListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcSecurityService;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

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

	// The Data Management Application Service Instance.
	@Autowired
    private HpcDataManagementService dataManagementService = null;
	
	// The Data Transfer Application Service Instance.
	@Autowired
    private HpcDataTransferService dataTransferService = null;
	
	// Security Application Service Instance.
	@Autowired
    private HpcSecurityService securityService = null;
	
	// The Metadata Application Service Instance.
	@Autowired
    private HpcMetadataService metadataService = null;
	
	// TheEvent Application Service Instance.
	@Autowired
    private HpcEventService eventService = null;
	
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
    		                          HpcCollectionRegistrationDTO collectionRegistration)  
    		                         throws HpcException
    {
    	logger.info("Invoking registerCollection(HpcCollectionRegistrationDTO): " + 
    			    collectionRegistration);
    	
    	// Input validation.
    	if(path == null || path.isEmpty() || collectionRegistration == null || 
    	   collectionRegistration.getMetadataEntries().isEmpty()) {
    	   throw new HpcException("Null path or metadata entries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Create parent collections if requested to.
    	Boolean createParentCollections = collectionRegistration.getCreateParentCollections();
    	if(createParentCollections != null && createParentCollections &&
    	   !dataManagementService.isPathParentDirectory(path)) {
    	   registerCollection(path.substring(0, path.lastIndexOf('/')), collectionRegistration);
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
    	        metadataService.addMetadataToCollection(path, collectionRegistration.getMetadataEntries());
    	        
    	        // Generate system metadata and attach to the collection.
       	        metadataService.addSystemGeneratedMetadataToCollection(path);
       	        
       	        // Validate the collection hierarchy.
       	        String doc = metadataService.getCollectionSystemGeneratedMetadata(path).getRegistrarDOC();
       	        dataManagementService.validateHierarchy(path, doc, false);
       	        
       	        // Add collection update event.
       	        addCollectionUpdatedEvent(path, true, false);
       	        
       	        registrationCompleted = true;
       	        
    	   } finally {
			          if(!registrationCompleted) {
				         // Collection registration failed. Remove it from Data Management.
				         dataManagementService.delete(path);
			          }
	       }
       	   
    	} else {
    		    metadataService.updateCollectionMetadata(path, collectionRegistration.getMetadataEntries());
    		    addCollectionUpdatedEvent(path, false, false);
    	}
    	
    	return created;
    }
    
    @Override
    public HpcCollectionDTO getCollection(String path, Boolean list) throws HpcException
    {
    	logger.info("Invoking getCollection(String): " + path);
    	
    	// Input validation.
    	if(path == null) {
    	   throw new HpcException("Null collection path",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	HpcCollection collection = dataManagementService.getCollection(path, list != null ? list : false);
    	if(collection == null) {
      	   return null;
      	}
     	
     	// Get the metadata.
     	HpcMetadataEntries metadataEntries = 
     	   metadataService.getCollectionMetadataEntries(collection.getAbsolutePath());
 		
     	HpcCollectionDTO collectionDTO = new HpcCollectionDTO();
     	collectionDTO.setCollection(collection);
     	if(metadataEntries != null && 
     			(metadataEntries.getParentMetadataEntries() != null && metadataEntries.getParentMetadataEntries().size()>0) || 
     			(metadataEntries.getSelfMetadataEntries() != null && metadataEntries.getSelfMetadataEntries().size()>0))
        collectionDTO.setMetadataEntries(metadataEntries);
     	
     	return collectionDTO;
    }
    
    @Override
    public HpcDownloadResponseListDTO downloadCollection(String path,
                                                         HpcDownloadRequestDTO downloadRequest)
                                                        throws HpcException
    {
    	logger.info("Invoking downloadCollection(path, downloadReqest): " + path + ", " + 
                    downloadRequest);
    	
    	// Input validation.
    	if(path == null || downloadRequest == null) {
    	   throw new HpcException("Null path or download request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	if(downloadRequest.getDestination() == null) {
     	   throw new HpcException("Null destination in download request",
     			                  HpcErrorType.INVALID_REQUEST_INPUT);	
     	}
    	
    	// Get the collection.
    	HpcCollection collection = dataManagementService.getCollection(path, true);
    	if(collection == null) {
      	   return null;
      	}
    	    	
    	// Download all data objects in the collection tree.
    	return downloadDataObjects(collection, downloadRequest.getDestination());
    }
    
    @Override
    public boolean registerDataObject(String path,
    		                          HpcDataObjectRegistrationDTO dataObjectRegistration,
    		                          File dataObjectFile)  
    		                         throws HpcException
    {
    	logger.info("Invoking registerDataObject(HpcDataObjectRegistrationDTO): " + 
    			    dataObjectRegistration);
    	
    	// Input validation.
    	if(path == null || dataObjectRegistration == null) {
    	   throw new HpcException("Null path or dataObjectRegistrationDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Create parent collections if requested to.
    	createParentCollections(path, dataObjectRegistration);
    	
    	// Create a data object file (in the data management system).
	    boolean created = dataManagementService.createFile(path);
	    
	    if(created) {
    	   boolean registrationCompleted = false; 
    	   try {
      	        // Validate the new collection meets the hierarchy definition.
    		    String collectionPath = path.substring(0, path.lastIndexOf('/'));
    		    String doc = securityService.getRequestInvoker().getNciAccount().getDoc();
      	        dataManagementService.validateHierarchy(collectionPath, doc, true);
    		   
    		    // Assign system account as an additional owner of the data-object.
   		        dataManagementService.assignSystemAccountPermission(path);
   		  
		        // Attach the user provided metadata.
		        metadataService.addMetadataToDataObject(
		    	   		           path, 
		    			           dataObjectRegistration.getMetadataEntries());
		        
		        // Extract the source location and size.
		        HpcFileLocation source = dataObjectRegistration.getSource();
				if(source != null && 
				   (source.getFileContainerId() == null && source.getFileId() == null)) {
				   source = null;
				}
				
				// Transfer the data file.
		        HpcDataObjectUploadResponse uploadResponse = 
		           dataTransferService.uploadDataObject(
		        	   source, dataObjectFile, path, 
		        	   securityService.getRequestInvoker().getNciAccount().getUserId(),
		        	   dataObjectRegistration.getCallerObjectId());
		        
			    // Generate system metadata and attach to the data object.
			    metadataService.addSystemGeneratedMetadataToDataObject(
			        		                   path, uploadResponse.getArchiveLocation(),
			    			                   source, uploadResponse.getDataTransferRequestId(), 
			    			                   uploadResponse.getChecksum(), 
			    			                   uploadResponse.getDataTransferStatus(),
			    			                   uploadResponse.getDataTransferType(),
			    			                   getSourceSize(source, uploadResponse.getDataTransferType(),
				                                             dataObjectFile), 
			    			                   dataObjectRegistration.getCallerObjectId()); 
	
			    // Add collection update event.
       	        addCollectionUpdatedEvent(path, false, true);
       	        
			    registrationCompleted = true;
			     
	    	} finally {
	    			   if(!registrationCompleted) {
	    				  // Data object registration failed. Remove it from Data Management.
	    				  dataManagementService.delete(path);
	    			   }
	    	}
    	   
	    } else {
	    	    if(dataObjectFile != null || dataObjectRegistration.getSource() != null) {
	    		   throw new HpcException("Data object cannot be updated. Only updating metadata is allowed.",
			                              HpcErrorType.REQUEST_REJECTED);
	    	    }
	    	
	    	    metadataService.updateDataObjectMetadata(path, dataObjectRegistration.getMetadataEntries()); 
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
    	HpcMetadataEntries metadataEntries = 
    		               metadataService.getDataObjectMetadataEntries(dataObject.getAbsolutePath());
    	String transferPercentCompletion = getDataTransferUploadPercentCompletion(
		               metadataService.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries()));
    		
    	HpcDataObjectDTO dataObjectDTO = new HpcDataObjectDTO();
    	dataObjectDTO.setDataObject(dataObject);
    	dataObjectDTO.setMetadataEntries(metadataEntries);
    	dataObjectDTO.setTransferPercentCompletion(transferPercentCompletion);
    	
    	return dataObjectDTO;
    }
    
    @Override
    public HpcDownloadResponseDTO downloadDataObject(String path,
                                                     HpcDownloadRequestDTO downloadRequest)
                                                    throws HpcException
    {
    	logger.info("Invoking downloadDataObject(path, downloadReqest): " + path + ", " + 
                    downloadRequest);
    	
    	// Input validation.
    	if(path == null || downloadRequest == null) {
    	   throw new HpcException("Null path or download request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Validate the data object exist.
    	if(dataManagementService.getDataObject(path) == null) {
      	   return null;
      	}
    	
    	// Get the System generated metadata.
    	HpcSystemGeneratedMetadata metadata = 
    	   metadataService.getDataObjectSystemGeneratedMetadata(path);
    	
    	// Validate the file is archived.
    	if(!metadata.getDataTransferStatus().equals(HpcDataTransferUploadStatus.ARCHIVED)) {
    	   throw new HpcException("Object is not in archived state yet. It is in " +
    			                  metadata.getDataTransferStatus().value() + " state",
    			                  HpcRequestRejectReason.FILE_NOT_ARCHIVED);
    	}
    	
    	// Download the data object.
    	HpcDataObjectDownloadResponse downloadResponse = 
    		   dataTransferService.downloadDataObject(metadata.getArchiveLocation(), 
                                                      downloadRequest.getDestination(),
                                                      metadata.getDataTransferType());
    	
    	// Construct and return a DTO.
    	return toDownloadResponseDTO(downloadResponse.getDataTransferRequestId(),
    			                     downloadResponse.getDestinationLocation(),
    			                     downloadResponse.getDestinationFile(),
    			                     true, null); 
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
    
    @Override
    public HpcDataManagementModelDTO getDataManagementModel(String doc) throws HpcException
    {
    	logger.info("Invoking getDataManagementMode(String): " + doc);
    	
    	// Input validation.
    	if(doc == null || doc.isEmpty()) {
    	   throw new HpcException("Null or empty DOC", HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	HpcDataManagementModelDTO dataManagementModel = new HpcDataManagementModelDTO();
    	dataManagementModel.getCollectionMetadataValidationRules().addAll(
    			               metadataService.getCollectionMetadataValidationRules(doc));
    	dataManagementModel.getDataObjectMetadataValidationRules().addAll(
	                           metadataService.getDataObjectMetadataValidationRules(doc));
    	dataManagementModel.setDataHierarchy(dataManagementService.getDataHierarchy(doc));
    	dataManagementModel.setBasePath(dataManagementService.getDocBasePath(doc));
    	
    	return dataManagementModel;
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /** 
     * Get the data object source size. (Either source or dataObjectFile are not null)
     * 
     * @param source A data transfer source.
     * @param dataTransferType The data transfer type.
     * @param dataObjectFile The attached data file.
     * @return The source size in bytes.
     * @throws HpcException on service failure.
     */
	private Long getSourceSize(HpcFileLocation source, HpcDataTransferType dataTransferType,
			                   File dataObjectFile) throws HpcException
	{
		if(source != null) {
		   return dataTransferService.getPathAttributes(dataTransferType, source, true).getSize();
		}
		
		if(dataObjectFile != null) {
           return dataObjectFile.length();
		}
		
		return null;
	}
	
    /** 
     * Calculate the data transfer % completion if transfer is in progress
     * 
     * @param systemGeneratedMetadata The system generated metadata of the data object.
     * @return The transfer % completion if transfer is in progress, or null otherwise.
     *         e.g 86%.
     */
	private String getDataTransferUploadPercentCompletion(
			          HpcSystemGeneratedMetadata systemGeneratedMetadata)
	{
		// Get the transfer status, transfer request id and data-object size from the metadata entries.
		HpcDataTransferUploadStatus transferStatus = systemGeneratedMetadata.getDataTransferStatus();
		if(transferStatus == null || 
		   (!transferStatus.equals(HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE) &&
			!transferStatus.equals(HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE))) {
		   // data transfer not in progress.
		   return null;
		} 
		
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
	
    /** 
     * Validate permissions requests list.
     * 
     * @param entityPermissionRequests The requests to validate.
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
     * Add collection update event.
     * 
     * @param path The path of the entity trigger this event.
     * @param collectionRegistered An indicator if a collection was registered.
     * @param dataObjectRegistered An indicator if a data object was registered.
     */
	private void addCollectionUpdatedEvent(String path, boolean collectionRegistered, 
			                               boolean dataObjectRegistered)
	{
		try {
			 if(!collectionRegistered && !dataObjectRegistered) {
				// Add collection metadata updated event.
				eventService.addCollectionUpdatedEvent(path);
				return;
			 }
			 
			 // A collection or data object registered, so we add an event for the parent collection.
			 String parentCollection = StringUtils.trimTrailingCharacter(path, '/');
			 int parentCollectionIndex = parentCollection.lastIndexOf('/');
			 parentCollection = parentCollectionIndex <= 0 ? "/" : 
				                parentCollection.substring(0, parentCollectionIndex);
			 
			 if(collectionRegistered) {
				eventService.addCollectionRegistrationEvent(parentCollection);
			 } else {
				     eventService.addDataObjectRegistrationEvent(parentCollection);
			 }
			 
		} catch(HpcException e) {
			    logger.error("Failed to add collection update event", e);
		}
	}
	
    /** 
     * Download a collection tree.
     * 
     * @param collection The collection to download.
     * @param destination The download destination location.
     * @return A list of files.
     * @throws HpcException on service failure.
     */
	private HpcDownloadResponseListDTO downloadDataObjects(HpcCollection collection, 
			                                               HpcFileLocation destinationLocation) 
			                                              throws HpcException
	{
		// Iterate through the data objects in the collection and download them.
		HpcDownloadResponseListDTO dataObjectDownloadResponses = new HpcDownloadResponseListDTO();
		for(HpcCollectionListingEntry dataObjectEntry : collection.getDataObjects()) {
			// Calculate the destination location for this data object.
			String dataObjectPath = dataObjectEntry.getPath();
			HpcDownloadRequestDTO downloadRequest = new HpcDownloadRequestDTO();
			downloadRequest.setDestination(calculateDownloadDestinationFileLocation(destinationLocation, dataObjectPath));
			
			// Download this data object.
			try {
			     dataObjectDownloadResponses.getDownloadReceipts().add(
				    	   downloadDataObject(dataObjectPath, downloadRequest).getDownloadReceipt());
			     
			} catch(HpcException e) {
				    // Data object download failed. 
				    logger.error("Failed to download data object in a collection" , e); 
				    dataObjectDownloadResponses.getDownloadReceipts().add(
  				        toDownloadResponseDTO(null, downloadRequest.getDestination(), null, false, e.getMessage()).
				        getDownloadReceipt());
			}
		}
		
		// Iterate through the sub-collections and download them.
		for(HpcCollectionListingEntry subCollectionEntry : collection.getSubCollections()) {
			String subCollectionPath = subCollectionEntry.getPath();
			HpcCollection subCollection = dataManagementService.getCollection(subCollectionPath, true);
	    	if(subCollection != null) {
	    	   // Download this sub-collection. 
			   dataObjectDownloadResponses.getDownloadReceipts().addAll(
				   downloadDataObjects(subCollection,
							           calculateDownloadDestinationFileLocation(destinationLocation, 
							        		                                    subCollectionPath)).
							          getDownloadReceipts());
	    	}
		}
		
		return dataObjectDownloadResponses;
	}
	
    /** 
     * Calculate a download destination path for a collection entry under a collection.
     * 
     * @param collectionDestination The collection destination location.
     * @param collectionListingEntryPath The entry path under the collection to calculate the destination location for.
     * @return A calculated destination location.
     */
	
	private HpcFileLocation calculateDownloadDestinationFileLocation(HpcFileLocation collectionDestination,
			                                                         String collectionListingEntryPath)
	{
		HpcFileLocation calcDestination = new HpcFileLocation();
	    calcDestination.setFileContainerId(collectionDestination.getFileContainerId());
	    calcDestination.setFileId(collectionDestination.getFileId() + 
	    		                  collectionListingEntryPath.substring(collectionListingEntryPath.lastIndexOf('/')));
	    return calcDestination;
	}

    /** 
     * Construct a download response DTO object.
     * 
     * @param dataTransferRequestId The data transfer request ID.
     * @param destinationLocation The destination file location.
     * @param destinationFile The destination file.
     * @param result The download request result.
     * @param message The error message.
     * @return A download response DTO object
     */
	private HpcDownloadResponseDTO toDownloadResponseDTO(String dataTransferRequestId,
			                                             HpcFileLocation destinationLocation,
			                                             File destinationFile,
			                                             boolean result, String message)
	{
		// Construct and return a DTO
		HpcDownloadResponseDTO downloadResponse = new HpcDownloadResponseDTO();
		HpcDownloadReceiptDTO downloadReceipt = new HpcDownloadReceiptDTO();
		downloadReceipt.setDataTransferRequestId(dataTransferRequestId);
		downloadReceipt.setDestinationFile(destinationFile);
		downloadReceipt.setDestinationLocation(destinationLocation);
		downloadReceipt.setResult(result);
		downloadReceipt.setMessage(message);
		
		downloadResponse.setDownloadReceipt(downloadReceipt);
		return downloadResponse;
	}
	
    /** 
     * Construct a download response DTO object.
     * 
     * @param path The data object's path.
     * @param dataObjectRegistration A DTO contains the metadata.
     * @throws HpcException on service failure.
     */
	private void createParentCollections(String path, 
			                             HpcDataObjectRegistrationDTO dataObjectRegistration)
			                            throws HpcException
	{
		// Create parent collections if requested and needed to.
		Boolean createParentCollections = dataObjectRegistration.getCreateParentCollections();
		if(createParentCollections != null && createParentCollections &&
		   !dataManagementService.isPathParentDirectory(path)) {
		   HpcCollectionRegistrationDTO collectionRegistration = new HpcCollectionRegistrationDTO();
		   collectionRegistration.getMetadataEntries().addAll(dataObjectRegistration.getMetadataEntries());
		   collectionRegistration.setCreateParentCollections(true);
		   registerCollection(path.substring(0, path.lastIndexOf('/')), collectionRegistration);
		}
	}
}

 