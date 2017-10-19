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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcGroupPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectType;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationStatus;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDeleteResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListRegistrationStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDocDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * <p>
 * HPC Data Management Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
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
    	// Determine the data management configuration to use based on the path.
    	String configurationId = dataManagementService.findDataManagementConfigurationId(path);
    	if(StringUtils.isEmpty(configurationId)) {
    	   throw new HpcException("Failed to determine data management configuration for: " + path,
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();
	    return registerCollection(path, collectionRegistration,
	    		                  invokerNciAccount.getUserId(),
	    		                  invokerNciAccount.getFirstName() + " " + invokerNciAccount.getLastName(),
	    		                  configurationId);
    }
    
    @Override
    public boolean registerCollection(String path,
    		                          HpcCollectionRegistrationDTO collectionRegistration,
    		                          String userId, String userName, String configurationId)  
    		                         throws HpcException
    {
    	// Input validation.
    	if(path == null || path.isEmpty() || collectionRegistration == null || 
    	   collectionRegistration.getMetadataEntries().isEmpty()) {
    	   throw new HpcException("Null path or metadata entries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Create parent collections if requested to.
    	createParentCollections(path, collectionRegistration.getCreateParentCollections(), 
    			                collectionRegistration.getParentCollectionMetadataEntries(),
    			                userId, userName, configurationId);
    	
    	// Create a collection directory.
    	boolean created = dataManagementService.createDirectory(path);
    	
    	// Attach the metadata.
    	if(created) {
    	   boolean registrationCompleted = false; 
    	   try {
    		    // Assign system account as an additional owner of the collection.
    		    dataManagementService.setCoOwnership(path, userId);
    		    
    		    // Add user provided metadata.
    	        metadataService.addMetadataToCollection(path, collectionRegistration.getMetadataEntries(), 
    	        		                                configurationId);
    	        
    	        // Generate system metadata and attach to the collection.
       	        metadataService.addSystemGeneratedMetadataToCollection(path, userId, userName, 
       	        		                                               configurationId);
       	        
       	        // Validate the collection hierarchy.
       	        dataManagementService.validateHierarchy(path, configurationId, false);
       	        
       	        // Add collection update event.
       	        addCollectionUpdatedEvent(path, true, false);
       	        
       	        registrationCompleted = true;
       	        
    	   } finally {
			          if(!registrationCompleted) {
				         // Collection registration failed. Remove it from Data Management.
				         dataManagementService.delete(path, true);
			          }
	       }
       	   
    	} else {
    		    metadataService.updateCollectionMetadata(
    		    		path, collectionRegistration.getMetadataEntries(), 
    		    		metadataService.getCollectionSystemGeneratedMetadata(path).getConfigurationId());
    		    addCollectionUpdatedEvent(path, false, false);
    	}
    	
    	return created;
    }
    
    @Override
    public HpcCollectionDTO getCollection(String path, Boolean list) throws HpcException
    {
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
     	   (!metadataEntries.getParentMetadataEntries().isEmpty() || 
     	    !metadataEntries.getSelfMetadataEntries().isEmpty())) {
           collectionDTO.setMetadataEntries(metadataEntries);
     	}
     	
     	return collectionDTO;
    }
    
    @Override
    public HpcCollectionDTO getCollectionChildren(String path) throws HpcException
    {
    	// Input validation.
    	if(path == null) {
    	   throw new HpcException("Null collection path",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	HpcCollection collection = dataManagementService.getCollectionChildren(path);
    	if(collection == null) {
      	   return null;
      	}
     	
     	HpcCollectionDTO collectionDTO = new HpcCollectionDTO();
     	collectionDTO.setCollection(collection);
     	
     	return collectionDTO;
    }
    
    @Override
    public HpcCollectionDownloadResponseDTO downloadCollection(String path,
                                                               HpcDownloadRequestDTO downloadRequest)
                                                               throws HpcException
    {
    	// Input validation.
    	if(path == null || downloadRequest == null) {
    	   throw new HpcException("Null path or download request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	if(downloadRequest.getDestination() == null) {
     	   throw new HpcException("Null destination in download request",
     			                  HpcErrorType.INVALID_REQUEST_INPUT);	
     	}
    	
    	// Validate collection exists.
    	if(dataManagementService.getCollection(path, true) == null) {
    	   throw new HpcException("Collection doesn't exist: " + path,
	                              HpcErrorType.INVALID_REQUEST_INPUT);	
      	}

    	// Get the System generated metadata.
    	HpcSystemGeneratedMetadata metadata = 
    			 metadataService.getCollectionSystemGeneratedMetadata(path);
    	
    	// Submit a collection download task.
    	HpcCollectionDownloadTask collectionDownloadTask =
    	   dataTransferService.downloadCollection(path, downloadRequest.getDestination(), 
    		   	                                  securityService.getRequestInvoker().getNciAccount().getUserId(),
    		   	                                  metadata.getConfigurationId());
    	
    	// Create and resturn a DAO with the request receipt.
    	HpcCollectionDownloadResponseDTO responseDTO = new HpcCollectionDownloadResponseDTO();
    	responseDTO.setTaskId(collectionDownloadTask.getId());
    	responseDTO.setDestinationLocation(collectionDownloadTask.getDestinationLocation());
    	
    	return responseDTO;
    }
    
    @Override
	public HpcDataObjectListDownloadResponseDTO downloadDataObjects(
			                                            HpcDataObjectListDownloadRequestDTO downloadRequest)
                                                        throws HpcException
    {
    	// Input validation.
    	if(downloadRequest == null) {
    	   throw new HpcException("Null download request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	if(downloadRequest.getDataObjectPaths().isEmpty()) {
      	   throw new HpcException("No data object paths",
      			                  HpcErrorType.INVALID_REQUEST_INPUT);	
      	}
    	
    	if(downloadRequest.getDestination() == null) {
     	   throw new HpcException("Null destination in download request",
     			                  HpcErrorType.INVALID_REQUEST_INPUT);	
     	}
    	
    	// Validate all data object paths requested exist and from the same DOC.
    	String configurationId = null;
    	for(String path : downloadRequest.getDataObjectPaths()) {
    	    if(dataManagementService.getDataObject(path) == null) {
    	       throw new HpcException("Data object doesn't exist: " + path,
	                                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	    }
    	    
    	    // Get the System generated metadata.
        	HpcSystemGeneratedMetadata metadata = 
        			 metadataService.getDataObjectSystemGeneratedMetadata(path);
        	if(configurationId == null) {
        		configurationId = metadata.getConfigurationId();
        	} else {
        		    if(!configurationId.equals(metadata.getConfigurationId())) {
        		       throw new HpcException("Download files from different configuration (base path) not allowed",
                                              HpcErrorType.INVALID_REQUEST_INPUT);	
        		    }
        	}
      	}

    	// Submit a data objects download task.
    	HpcCollectionDownloadTask collectionDownloadTask =
    	   dataTransferService.downloadDataObjects(downloadRequest.getDataObjectPaths(), 
    			                                   downloadRequest.getDestination(), 
    		   	                                   securityService.getRequestInvoker().getNciAccount().getUserId(),
    		   	                                   configurationId);
    	
    	// Create and return a DAO with the request receipt.
    	HpcDataObjectListDownloadResponseDTO responseDTO = new HpcDataObjectListDownloadResponseDTO();
    	responseDTO.setTaskId(collectionDownloadTask.getId());
    	responseDTO.setDestinationLocation(collectionDownloadTask.getDestinationLocation());
    	
    	return responseDTO;
    }
    
    @Override
    public HpcCollectionDownloadStatusDTO getCollectionDownloadStatus(String taskId) 
                                                                     throws HpcException
    {
    	return getCollectionDownloadStatus(taskId, HpcDownloadTaskType.COLLECTION);
    }
    
    @Override
    public HpcCollectionDownloadStatusDTO getDataObjectsDownloadStatus(String taskId) 
                                                                       throws HpcException
    {
    	return getCollectionDownloadStatus(taskId, HpcDownloadTaskType.DATA_OBJECT_LIST);
    }
    
    @Override
    public HpcDownloadSummaryDTO getDownloadSummary(int page, boolean totalCount) throws HpcException
    {
    	// Get the request invoker user-id
    	String userId = securityService.getRequestInvoker().getNciAccount().getUserId();
    	
    	// Populate the DTO with active and completed sownload requests for this user.
    	HpcDownloadSummaryDTO downloadSummary = new HpcDownloadSummaryDTO();
    	downloadSummary.getActiveTasks().addAll(dataTransferService.getDownloadRequests(userId));
    	downloadSummary.getCompletedTasks().addAll(dataTransferService.getDownloadResults(userId, page));
    	
    	int limit = dataTransferService.getDownloadResultsPageSize(); 
    	downloadSummary.setPage(page);
    	downloadSummary.setLimit(limit);
    	
		if(totalCount) {
		   int count = downloadSummary.getCompletedTasks().size();
		   downloadSummary.setTotalCount((page == 1 && count < limit) ? 
					                      count : dataTransferService.getDownloadResultsCount(userId));
		}
    	
    	return downloadSummary;
    }
    
    @Override
	public HpcEntityPermissionsResponseDTO 
	       setCollectionPermissions(String path, HpcEntityPermissionsDTO collectionPermissionsRequest)
                                   throws HpcException
    {
    	// Input validation.
    	if(path == null) {
    	   throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	validatePermissionsRequest(collectionPermissionsRequest);
    	
    	// Validate the collection exists.
    	if(dataManagementService.getCollection(path, false) == null) {
    	   throw new HpcException("Collection doesn't exist: " + path,
                                  HpcErrorType.INVALID_REQUEST_INPUT);	
      	}
    	
    	HpcEntityPermissionsResponseDTO permissionsResponse = new HpcEntityPermissionsResponseDTO();
    	
    	// Execute all user permission requests for this collection.
    	permissionsResponse.getUserPermissionResponses().addAll(
    			   setEntityPermissionForUsers(path, true, collectionPermissionsRequest.getUserPermissions()));
    		
    	// Execute all group permission requests for this collection.
    	permissionsResponse.getGroupPermissionResponses().addAll(
 			   setEntityPermissionForGroups(path, true, collectionPermissionsRequest.getGroupPermissions()));
    		
    	return permissionsResponse;
    }
    
    @Override
    public HpcEntityPermissionsDTO getCollectionPermissions(String path) throws HpcException
    {
    	// Input validation.
    	if(path == null) {
    	   throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Validate the collection exists.
    	if(dataManagementService.getCollection(path, false) == null) {
      	   return null;
      	}
    	
    	return toEntityPermissionsDTO(dataManagementService.getCollectionPermissions(path));
    }
    
    @Override
    public HpcUserPermissionDTO getCollectionPermissionForUser(String path, String userId) throws HpcException
    {
    	// Input validation.
    	if(path == null) {
    	   throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	if(userId == null) {
     	   throw new HpcException("Null userId", HpcErrorType.INVALID_REQUEST_INPUT);	
     	}

    	// Validate the collection exists.
    	if(dataManagementService.getCollection(path, false) == null) {
      	   return null;
      	}
    	HpcSubjectPermission permission = dataManagementService.getCollectionPermissionForUser(path, userId);
    	
    	return toUserPermissionDTO(permission);
    }
    
    @Override
    public boolean registerDataObject(String path,
    		                          HpcDataObjectRegistrationDTO dataObjectRegistration,
    		                          File dataObjectFile)  
    		                         throws HpcException
    {
    	// Determine the data management configuration to use based on the path.
    	String configurationId = dataManagementService.findDataManagementConfigurationId(path);
    	if(StringUtils.isEmpty(configurationId)) {
    	   throw new HpcException("Failed to determine data management configuration for: " + path,
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();
	    return registerDataObject(path, dataObjectRegistration, dataObjectFile, 
	    		                  invokerNciAccount.getUserId(),
	    		                  invokerNciAccount.getFirstName() + " " + invokerNciAccount.getLastName(),
	    		                  configurationId, true);
    }
    
    @Override
    public boolean registerDataObject(String path,
    		                          HpcDataObjectRegistrationDTO dataObjectRegistration,
    		                          File dataObjectFile, String userId, String userName, 
    		                          String configurationId,
    		                          boolean registrationCompletionEvent)  
    		                         throws HpcException
    {
    	// Input validation.
    	if(StringUtils.isEmpty(path) || dataObjectRegistration == null) {
    	   throw new HpcException("Null path or dataObjectRegistrationDTO",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Checksum validation (if requested by caller).
    	validateChecksum(dataObjectFile, dataObjectRegistration.getChecksum());
    	
    	// Create parent collections if requested to.
    	createParentCollections(path, dataObjectRegistration.getCreateParentCollections(), 
    			                dataObjectRegistration.getParentCollectionMetadataEntries(),
    			                userId, userName, configurationId);
    	
    	// Get the collection type containing the data object.
    	String collectionPath = path.substring(0, path.lastIndexOf('/'));
    	String collectionType = dataManagementService.getCollectionType(collectionPath);
    	
    	// Create a data object file (in the data management system).
	    boolean created = dataManagementService.createFile(path);
	    
	    if(created) {
    	   boolean registrationCompleted = false; 
    	   try {
      	        // Validate the new data object meets the hierarchy definition.
      	        dataManagementService.validateHierarchy(collectionPath, configurationId, true);
    		   
    		    // Assign system account as an additional owner of the data-object.
      	        dataManagementService.setCoOwnership(path, userId);
   		  
		        // Attach the user provided metadata.
		        metadataService.addMetadataToDataObject(path, 
		    			                                dataObjectRegistration.getMetadataEntries(), 
		    			                                configurationId, collectionType);
		        
		        // Extract the source location and size.
		        HpcFileLocation source = dataObjectRegistration.getSource();
				if(source != null && 
				   (source.getFileContainerId() == null && source.getFileId() == null)) {
				   source = null;
				}
				
				// Transfer the data file.
		        HpcDataObjectUploadResponse uploadResponse = 
		           dataTransferService.uploadDataObject(source, dataObjectFile, path, userId,
		        	                                    dataObjectRegistration.getCallerObjectId(), 
		        	                                    configurationId);
		        
			    // Generate system metadata and attach to the data object.
			    metadataService.addSystemGeneratedMetadataToDataObject(
			        		                   path, uploadResponse.getArchiveLocation(),
			    			                   source, uploadResponse.getDataTransferRequestId(), 
			    			                   uploadResponse.getChecksum(), 
			    			                   uploadResponse.getDataTransferStatus(),
			    			                   uploadResponse.getDataTransferType(),
			    			                   uploadResponse.getDataTransferStarted(),
			    			                   uploadResponse.getDataTransferCompleted(),
			    			                   getSourceSize(source, uploadResponse.getDataTransferType(),
				                                             dataObjectFile, configurationId), 
			    			                   dataObjectRegistration.getCallerObjectId(),
			    			                   userId, userName, configurationId, 
			    			                   registrationCompletionEvent); 
	
			    // Add collection update event.
       	        addCollectionUpdatedEvent(path, false, true);
       	        
			    registrationCompleted = true;
			     
	    	} finally {
	    			   if(!registrationCompleted) {
	    				  // Data object registration failed. Remove it from Data Management.
	    				  dataManagementService.delete(path, true);
	    			   }
	    	}
    	   
	    } else {
	    	    if(dataObjectFile != null || 
	    	       (dataObjectRegistration.getSource() != null && 
	    	    	dataObjectRegistration.getSource().getFileContainerId() != null && 
	    	    	dataObjectRegistration.getSource().getFileId() != null)) {
	    		   throw new HpcException("Data object cannot be updated. Only updating metadata is allowed.",
			                              HpcErrorType.REQUEST_REJECTED);
	    	    }
	    	
	    	    metadataService.updateDataObjectMetadata(
	    	    		path, dataObjectRegistration.getMetadataEntries(), 
	    	    		metadataService.getDataObjectSystemGeneratedMetadata(path).getConfigurationId(),
	    	    		collectionType); 
	    }
	    
	    return created;
    }
    
    @Override
	public HpcDataObjectListRegistrationResponseDTO 
              registerDataObjects(HpcDataObjectListRegistrationRequestDTO dataObjectListRegistrationRequest)
	                             throws HpcException
	{
    	// Input validation.
    	if(dataObjectListRegistrationRequest == null ||
    	   dataObjectListRegistrationRequest.getDataObjectRegistrationItems().isEmpty()) {
    	   throw new HpcException("Null / Empty registration request",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Break the DTO into a map of registration requests and ensure no duplication of registration paths.
    	Map<String, HpcDataObjectRegistrationRequest> dataObjectRegistrationRequests = new HashMap<>();
    	for(HpcDataObjectListRegistrationItemDTO dataObjectRegistrationItem : 
    		dataObjectListRegistrationRequest.getDataObjectRegistrationItems()) {
    		HpcDataObjectRegistrationRequest dataObjectRegistrationRequest = new HpcDataObjectRegistrationRequest();
    		dataObjectRegistrationRequest.setCallerObjectId(dataObjectRegistrationItem.getCallerObjectId());
    		dataObjectRegistrationRequest.setCreateParentCollections(
    				                         dataObjectRegistrationItem.getCreateParentCollections());
    		dataObjectRegistrationRequest.setSource(dataObjectRegistrationItem.getSource());
    		dataObjectRegistrationRequest.getMetadataEntries().addAll(
    				                         dataObjectRegistrationItem.getMetadataEntries());
    		dataObjectRegistrationRequest.getParentCollectionMetadataEntries().addAll(
    				                         dataObjectRegistrationItem.getParentCollectionMetadataEntries());
    		
    		String path = dataObjectRegistrationItem.getPath();
    		if(StringUtils.isEmpty(path)) {
    		   throw new HpcException("Null / Empty path in registration request",
		                              HpcErrorType.INVALID_REQUEST_INPUT);	
    		}
    		
    		// Validate no multiple registration requests for the same path.
    		if(dataObjectRegistrationRequests.put(path, dataObjectRegistrationRequest) != null) {
    		   throw new HpcException("Duplicated path in registration requests list: " + path,
                                      HpcErrorType.INVALID_REQUEST_INPUT);	
    		}
    	}
    	
    	HpcNciAccount invokerNciAccount = securityService.getRequestInvoker().getNciAccount();

    	// Submit a data objects registration task and return a response DTO.
    	HpcDataObjectListRegistrationResponseDTO responseDTO = new HpcDataObjectListRegistrationResponseDTO();
    	responseDTO.setTaskId(dataManagementService.registerDataObjects(invokerNciAccount.getUserId(), 
    			                                                        dataObjectRegistrationRequests));
    	
    	return responseDTO;
    }
    
    @Override
    public HpcDataObjectListRegistrationStatusDTO getDataObjectsRegistrationStatus(String taskId) 
                                                                                  throws HpcException
	{
		// Input validation.
		if(StringUtils.isEmpty(taskId)) {
		   throw new HpcException("Null / Empty registration task ID",
		                          HpcErrorType.INVALID_REQUEST_INPUT);	
		}
		
		// Get the registration task status.
		HpcDataObjectListRegistrationStatus taskStatus = 
		   dataManagementService.getDataObjectListRegistrationTaskStatus(taskId);
		if(taskStatus == null) {
		   return null;
		}
		
		// Map the task status to DTO.
		HpcDataObjectListRegistrationStatusDTO registrationStatus = new HpcDataObjectListRegistrationStatusDTO();
		registrationStatus.setInProgress(taskStatus.getInProgress());
		if(taskStatus.getInProgress()) {
		   // Registration in progress. Populate the DTO accordingly.
			registrationStatus.setCreated(taskStatus.getTask().getCreated());
			registrationStatus.setTaskStatus(taskStatus.getTask().getStatus());
			populateRegistrationItems(registrationStatus, taskStatus.getTask().getItems());
		
		} else {
				// Download completed or failed. Populate the DTO accordingly. 
			    registrationStatus.setCreated(taskStatus.getResult().getCreated());
			    registrationStatus.setCompleted(taskStatus.getResult().getCompleted());
			    registrationStatus.setMessage(taskStatus.getResult().getMessage());
			    registrationStatus.setResult(taskStatus.getResult().getResult());
			    populateRegistrationItems(registrationStatus, taskStatus.getResult().getItems());
		}
		
		return registrationStatus;
	}
    
    @Override
    public HpcDataObjectDTO getDataObject(String path) throws HpcException
    {
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
    public HpcDataObjectDownloadResponseDTO downloadDataObject(String path,
                                                               HpcDownloadRequestDTO downloadRequest)
                                                              throws HpcException
    {
    	return downloadDataObject(path, downloadRequest, 
    			                  securityService.getRequestInvoker().getNciAccount().getUserId(), 
    			                  true);
    }
    
    /**
     * Download Data Object.
     *
     * @param path The data object path.
     * @param downloadRequest The download request DTO.
     * @param completionEvent If true, an event will be added when async download is complete.
     * @param userId The user submitting the request.
     * @return Download ResponseDTO 
     * @throws HpcException on service failure.
     */
	@Override
    public HpcDataObjectDownloadResponseDTO downloadDataObject(String path,
                                                               HpcDownloadRequestDTO downloadRequest,
                                                               String userId,
                                                               boolean completionEvent)
                                                              throws HpcException
	{
    	// Input validation.
    	if(path == null || downloadRequest == null) {
    	   throw new HpcException("Null path or download request",
    		                      HpcErrorType.INVALID_REQUEST_INPUT);	
    	}

    	// Validate the data object exists.
    	if(dataManagementService.getDataObject(path) == null) {
    	   throw new HpcException("Data object doesn't exist: " + path,
    		                      HpcErrorType.INVALID_REQUEST_INPUT);	
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
    		   dataTransferService.downloadDataObject(path, metadata.getArchiveLocation(), 
    					                              downloadRequest.getDestination(),
    					                              metadata.getDataTransferType(),
    					                              metadata.getConfigurationId(), 
    					                              userId,
    					                              completionEvent);

    	// Construct and return a DTO.
    	return toDownloadResponseDTO(downloadResponse.getDestinationLocation(),
    			                     downloadResponse.getDestinationFile(),
    			                     downloadResponse.getDownloadTaskId()); 
	}
    
    @Override
    public HpcDataObjectDownloadStatusDTO getDataObjectDownloadStatus(String taskId) 
                                                                     throws HpcException
    {
    	// Input validation.
    	if(StringUtils.isEmpty(taskId)) {
    	   throw new HpcException("Null / Empty data object download task ID",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Get the data object download task status.
    	HpcDownloadTaskStatus taskStatus = 
    			   dataTransferService.getDownloadTaskStatus(taskId, HpcDownloadTaskType.DATA_OBJECT);
    	if(taskStatus == null) {
    	   return null;
    	}
    	
    	// Map the task status to DTO.
    	HpcDataObjectDownloadStatusDTO downloadStatus = new HpcDataObjectDownloadStatusDTO();
    	downloadStatus.setInProgress(taskStatus.getInProgress());
    	if(taskStatus.getInProgress()) {
    	   // Download in progress. Populate the DTO accordingly.
    	   downloadStatus.setPath(taskStatus.getDataObjectDownloadTask().getPath());
    	   downloadStatus.setCreated(taskStatus.getDataObjectDownloadTask().getCreated());
    	   downloadStatus.setDataTransferRequestId(taskStatus.getDataObjectDownloadTask().getDataTransferRequestId());
    	   downloadStatus.setDataTransferType(taskStatus.getDataObjectDownloadTask().getDataTransferType());
    	   downloadStatus.setDestinationLocation(taskStatus.getDataObjectDownloadTask().getDestinationLocation());
    	   
    	} else {
    		    // Download completed or failed. Populate the DTO accordingly. 
    		    downloadStatus.setPath(taskStatus.getResult().getPath());
     	        downloadStatus.setCreated(taskStatus.getResult().getCreated());
     	        downloadStatus.setDataTransferRequestId(taskStatus.getResult().getDataTransferRequestId());
     	        downloadStatus.setDataTransferType(taskStatus.getResult().getDataTransferType());
     	        downloadStatus.setDestinationLocation(taskStatus.getResult().getDestinationLocation());
     	        downloadStatus.setCompleted(taskStatus.getResult().getCompleted());
     	        downloadStatus.setMessage(taskStatus.getResult().getMessage());
     	        downloadStatus.setResult(taskStatus.getResult().getResult());
    	}
    	
    	return downloadStatus;
    }
    
    @Override
    public HpcDataObjectDeleteResponseDTO deleteDataObject(String path) throws HpcException
    {
    	// Input validation.
    	if(StringUtils.isEmpty(path)) {
    	   throw new HpcException("Null / empty path",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
        	
		// Validate the data object exists.
		if(dataManagementService.getDataObject(path) == null) {
		   throw new HpcException("Data object doesn't exist: " + path,
	                              HpcErrorType.INVALID_REQUEST_INPUT);	
	  	}
		
    	// Get the metadata for this data object. 
    	HpcMetadataEntries metadataEntries = 
    		               metadataService.getDataObjectMetadataEntries(path);
    	HpcSystemGeneratedMetadata systemGeneratedMetadata =
		   metadataService.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries());
    	if(systemGeneratedMetadata.getDataTransferStatus() == null) {
    	   throw new HpcException("Object is not in archived state yet",
                                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
		
		// Validate the invoker is the owner of the data object.
		HpcPermission permission = dataManagementService.getDataObjectPermission(path).getPermission();
		if(!permission.equals(HpcPermission.OWN)) {
		   throw new HpcException("Data object can only be deleted by its owner. Your permission: " + 
		                          permission.value(), HpcRequestRejectReason.DATA_OBJECT_PERMISSION_DENIED);
		}

		
    	// Instantiate a response DTO
    	HpcDataObjectDeleteResponseDTO dataObjectDeleteResponse = new HpcDataObjectDeleteResponseDTO();
		dataObjectDeleteResponse.setArchiveDeleteStatus(true);
	    dataObjectDeleteResponse.setDataManagementDeleteStatus(true);
	    
		// Delete the file from the archive (if it's archived).
	    switch(systemGeneratedMetadata.getDataTransferStatus()) {
	           case ARCHIVED:
	           case DELETE_REQUESTED:
	           case DELETE_FAILED:
	        	    deleteDataObjectFromArchive(path, systemGeneratedMetadata, dataObjectDeleteResponse);
			        break;
			        
	           case RECEIVED:
	           case IN_PROGRESS_TO_TEMPORARY_ARCHIVE:
	           case IN_TEMPORARY_ARCHIVE:
	           case IN_PROGRESS_TO_ARCHIVE:
	        	    // Data transfer still in progress.
		            throw new HpcException("Object is not in archived state yet. It is in " +
				                           systemGeneratedMetadata.getDataTransferStatus().value() + " state",
				                           HpcRequestRejectReason.FILE_NOT_ARCHIVED);
		            
		       default:
		    	    // The file is not in archive (data transfer failed, or it was deleted).
		    	    break;
		}
    	
		// If the archive removal was successful, then remove the file from data management.
		try {
			 if(dataObjectDeleteResponse.getArchiveDeleteStatus()) {
    	        dataManagementService.delete(path, false);
			 } else {
				     dataObjectDeleteResponse.setDataManagementDeleteStatus(false);
			 }
    	     
		} catch(HpcException e) {
		        logger.error("Failed to delete file from datamanagement", e);
		        dataObjectDeleteResponse.setDataManagementDeleteStatus(false);
		        dataObjectDeleteResponse.setMessage(e.getMessage());
		}
		
		// Keep a record of this data object deletion request and it's result.
		dataManagementService.saveDataObjectDeletionRequest(
				                  path, systemGeneratedMetadata.getArchiveLocation(), 
				                  dataObjectDeleteResponse.getArchiveDeleteStatus(), metadataEntries, 
				                  dataObjectDeleteResponse.getDataManagementDeleteStatus(),
				                  dataObjectDeleteResponse.getMessage());
		
		return dataObjectDeleteResponse;
    }
    
    @Override
	public HpcEntityPermissionsResponseDTO 
	       setDataObjectPermissions(String path, HpcEntityPermissionsDTO dataObjectPermissionsRequest)
                                   throws HpcException
    {
    	// Input Validation.
    	if(path == null) {
     	   throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);	
     	}
    	
    	validatePermissionsRequest(dataObjectPermissionsRequest);
    	
    	// Validate the data object exists.
    	if(dataManagementService.getDataObject(path) == null) {
    	   throw new HpcException("Data object doesn't exist: " + path,
                                  HpcErrorType.INVALID_REQUEST_INPUT);
      	}
    	
    	HpcEntityPermissionsResponseDTO permissionsResponse = new HpcEntityPermissionsResponseDTO();
    	
    	// Execute all user permission requests for this data object.
    	permissionsResponse.getUserPermissionResponses().addAll(
    			   setEntityPermissionForUsers(path, false, dataObjectPermissionsRequest.getUserPermissions()));
    		
    	// Execute all group permission requests for this data object.
    	permissionsResponse.getGroupPermissionResponses().addAll(
 			   setEntityPermissionForGroups(path, false, dataObjectPermissionsRequest.getGroupPermissions()));
    		
    	return permissionsResponse;
    }
    
    @Override
    public HpcEntityPermissionsDTO getDataObjectPermissions(String path) throws HpcException
    {
    	// Input Validation.
    	if(path == null) {
     	   throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);	
     	}
    	
    	// Validate the data object exists.
    	if(dataManagementService.getDataObject(path) == null) {
      	   return null;
      	}
    	
    	return toEntityPermissionsDTO(dataManagementService.getDataObjectPermissions(path));
    }
    
    @Override
    public HpcUserPermissionDTO getDataObjectPermissionForUser(String path, String userId) throws HpcException
    {
    	// Input validation.
    	if(path == null) {
    	   throw new HpcException("Null path", HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	if(userId == null) {
     	   throw new HpcException("Null userId", HpcErrorType.INVALID_REQUEST_INPUT);	
     	}

    	// Validate the collection exists.
    	if(dataManagementService.getDataObject(path) == null) {
      	   return null;
      	}
    	HpcSubjectPermission permission = dataManagementService.getDataObjectPermissionForUser(path, userId);
    	
    	return toUserPermissionDTO(permission);
    }
    
    @Override
    public HpcDataManagementModelDTO getDataManagementModel() throws HpcException
    {
    	// Create a map DOC -> HpcDocDataManagementRulesDTO
    	Map<String, HpcDocDataManagementRulesDTO> docsRules = new HashMap<>();
    	for(HpcDataManagementConfiguration dataManagementConfiguration : 
    		dataManagementService.getDataManagementConfigurations()) {
    		String doc = dataManagementConfiguration.getDoc();
    		HpcDocDataManagementRulesDTO docRules = 
    		   docsRules.containsKey(doc) ? docsRules.get(doc) : new HpcDocDataManagementRulesDTO();
    		
    		HpcDataManagementRulesDTO rules = new HpcDataManagementRulesDTO();
    		rules.setBasePath(dataManagementConfiguration.getBasePath());
    		rules.setDataHierarchy(dataManagementConfiguration.getDataHierarchy());
    		rules.getCollectionMetadataValidationRules().addAll(
    				 dataManagementConfiguration.getCollectionMetadataValidationRules());
    		rules.getDataObjectMetadataValidationRules().addAll(
    				 dataManagementConfiguration.getDataObjectMetadataValidationRules());
    		docRules.setDoc(doc);
    		docRules.getRules().add(rules);
    		docsRules.put(doc, docRules);
    	}
    	
    	// Construct and return the DTO
    	HpcDataManagementModelDTO dataManagementModel = new HpcDataManagementModelDTO();
    	dataManagementModel.getCollectionSystemGeneratedMetadataAttributeNames().addAll(
    			      metadataService.getCollectionSystemMetadataAttributeNames());
    	dataManagementModel.getDataObjectSystemGeneratedMetadataAttributeNames().addAll(
    			      metadataService.getDataObjectSystemMetadataAttributeNames());
    	dataManagementModel.getDocRules().addAll(docsRules.values());
    	
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
     * @param configurationId The data management configuration ID.
     * @return The source size in bytes.
     * @throws HpcException on service failure.
     */
	private Long getSourceSize(HpcFileLocation source, HpcDataTransferType dataTransferType,
			                   File dataObjectFile, String configurationId) throws HpcException
	{
		if(source != null) {
		   return dataTransferService.getPathAttributes(dataTransferType, source, 
				                                        true, configurationId).getSize();
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
		    		            dataTransferRequestId, systemGeneratedMetadata.getConfigurationId());
		} catch(HpcException e) {
			    logger.error("Failed to get data transfer size: " + dataTransferRequestId, e);
			    return "Unknown";
		}
		
		return String.valueOf((transferSize * 100L) / sourceSize) + '%';
	}
	
    /** 
     * Validate permissions request.
     * 
     * @param entityPermissionsRequest The request to validate.
     * @throws HpcException if found an invalid request in the list.
     */
	private void validatePermissionsRequest(HpcEntityPermissionsDTO entityPermissionsRequest)
                                           throws HpcException
    {
		if(entityPermissionsRequest == null || 
		   (entityPermissionsRequest.getUserPermissions().isEmpty() && 
		    entityPermissionsRequest.getGroupPermissions().isEmpty())) {
			throw new HpcException("Null or empty permissions request",
                                   HpcErrorType.INVALID_REQUEST_INPUT);	
		}
		
		Set<String> userIds = new HashSet<String>(); 
		for(HpcUserPermission userPermissionRequest : entityPermissionsRequest.getUserPermissions()) {
			String userId = userPermissionRequest.getUserId();
			HpcPermission permission = userPermissionRequest.getPermission();
			if(StringUtils.isEmpty(userId)) { 
			   throw new HpcException("Null or empty userId in a permission request",
                                      HpcErrorType.INVALID_REQUEST_INPUT);	
			}
			if(!userIds.add(userId)) {
			   throw new HpcException("Duplicate userId in a permission request: " + userId,
                                      HpcErrorType.INVALID_REQUEST_INPUT);	 
			}
	    	if(securityService.getUser(userId) == null) {
	    	   throw new HpcException("User not found: " + userId, 
	    			                  HpcRequestRejectReason.INVALID_NCI_ACCOUNT);	
	    	}
			if(permission == null) { 
			   throw new HpcException("Null or empty permission in a permission request. Valid values are [" +
     			                       Arrays.asList(HpcPermission.values()) + "]",
                                       HpcErrorType.INVALID_REQUEST_INPUT);	
			}
		}
			
		// Validate the group permission requests for this path. 
		Set<String> groupNames = new HashSet<String>(); 
		for(HpcGroupPermission groupPermissionRequest : entityPermissionsRequest.getGroupPermissions()) {
			String groupName = groupPermissionRequest.getGroupName();
			HpcPermission permission = groupPermissionRequest.getPermission();
			if(StringUtils.isEmpty(groupName)) { 
			   throw new HpcException("Null or empty group name in a permission request",
                                      HpcErrorType.INVALID_REQUEST_INPUT);	
			}
			if(!groupNames.add(groupName)) {
			   throw new HpcException("Duplicate group name in a permission request: " + groupName,
                                      HpcErrorType.INVALID_REQUEST_INPUT);	 
			}
			if(permission == null) { 
			   throw new HpcException("Null or empty permission in a permission request. Valid values are [" +
		                              Arrays.asList(HpcPermission.values()) + "]",
                                      HpcErrorType.INVALID_REQUEST_INPUT);	
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
     * Construct a download response DTO object.
     * 
     * @param destinationLocation The destination file location.
     * @param destinationFile The destination file.
     * @param taskId The data object download task ID.
     * @return A download response DTO object
     */
	private HpcDataObjectDownloadResponseDTO toDownloadResponseDTO(
			                                           HpcFileLocation destinationLocation,
			                                           File destinationFile,
			                                           String taskId)
	{
		// Construct and return a DTO
		HpcDataObjectDownloadResponseDTO downloadResponse = new HpcDataObjectDownloadResponseDTO();
		downloadResponse.setDestinationFile(destinationFile);
		downloadResponse.setDestinationLocation(destinationLocation);
		downloadResponse.setTaskId(taskId);
		
		return downloadResponse;
	}
	
    /** 
     * Construct a download response DTO object.
     * 
     * @param path The data object's path.
     * @param createParentCollections The indicator whether to create parent collections.
     * @param parentCollectionMetadataEntries The parent collection metadata entries.
     * @param userId The registrar user-id.
     * @param userName The registrar name.
     * @param configurationId The data management configuration ID.
     * @throws HpcException on service failure.
     */
	private void createParentCollections(String path, 
			                             Boolean createParentCollections, 
			                             List<HpcMetadataEntry> parentCollectionMetadataEntries,
			                             String userId, String userName, String configurationId)
			                            throws HpcException
	{
		// Create parent collections if requested and needed to.
		if(createParentCollections != null && createParentCollections &&
		   !dataManagementService.isPathParentDirectory(path)) {
		   // Validate parent collection metadata provided w/ the request.
		   if(parentCollectionMetadataEntries == null || parentCollectionMetadataEntries.isEmpty()) {
			  throw new HpcException("Null or empty parent metadata entries", 
					                 HpcErrorType.INVALID_REQUEST_INPUT);
		   }
		   
		   // Create a collection registration request DTO. 
		   HpcCollectionRegistrationDTO collectionRegistration = new HpcCollectionRegistrationDTO();
		   collectionRegistration.getMetadataEntries().addAll(parentCollectionMetadataEntries);
		   collectionRegistration.getParentCollectionMetadataEntries().addAll(parentCollectionMetadataEntries);
		   collectionRegistration.setCreateParentCollections(true);
		   
		   // Register the parent collection.
		   registerCollection(path.substring(0, path.lastIndexOf('/')), collectionRegistration,
				              userId, userName, configurationId);
		}
	}
	
    /** 
     * Perform user permission requests on an entity (collection or data object)
     * 
     * @param path The entity path (collection or data object).
     * @param collection True if the path is a collection, False if the path is a data object.
     * @param userPermissionRequests The list of user permissions requests.
     * @return A list of responses to the permission requests.
     */
	private List<HpcUserPermissionResponseDTO> 
	        setEntityPermissionForUsers(String path, boolean collection,
	        		                    List<HpcUserPermission> userPermissionRequests)
	{	
		List<HpcUserPermissionResponseDTO> permissionResponses = new ArrayList<>();
			                            
		// Execute all user permission requests for this entity.
		HpcSubjectPermission subjectPermissionRequest = new HpcSubjectPermission();
		subjectPermissionRequest.setSubjectType(HpcSubjectType.USER);
		for(HpcUserPermission userPermissionRequest : userPermissionRequests) {
			HpcUserPermissionResponseDTO userPermissionResponse = new HpcUserPermissionResponseDTO();
			userPermissionResponse.setUserId(userPermissionRequest.getUserId());
			userPermissionResponse.setResult(true);
			subjectPermissionRequest.setPermission(userPermissionRequest.getPermission());
			subjectPermissionRequest.setSubject(userPermissionRequest.getUserId());
		    try {
		    	 // Set the entity permission.
		    	 if(collection) {
		    	    dataManagementService.setCollectionPermission(path, subjectPermissionRequest);
		    	 } else {
		    		     dataManagementService.setDataObjectPermission(path, subjectPermissionRequest);
		    	 }
			     
		    } catch(HpcException e) {
		    	    // Request failed. Record the message and keep going.
		    		userPermissionResponse.setResult(false);
		    		userPermissionResponse.setMessage(e.getMessage());
		    }
		
		    // Add this user permission response to the list.
		    permissionResponses.add(userPermissionResponse);
		}
		
		return permissionResponses;
	}
	
    /** 
     * Perform group permission requests on an entity (collection or data object)
     * 
     * @param path The entity path (collection or data object).
     * @param collection True if the path is a collection, False if the path is a data object.
     * @param groupPermissionRequests The list of group permissions requests.
     * @return A list of responses to the permission requests.
     */
	private List<HpcGroupPermissionResponseDTO> 
	        setEntityPermissionForGroups(String path, boolean collection,
	        		                     List<HpcGroupPermission> groupPermissionRequests)
	{	
		List<HpcGroupPermissionResponseDTO> permissionResponses = new ArrayList<>();
			                            
		// Execute all user permission requests for this entity.
		HpcSubjectPermission subjectPermissionRequest = new HpcSubjectPermission();
		subjectPermissionRequest.setSubjectType(HpcSubjectType.GROUP);
		for(HpcGroupPermission groupPermissionRequest : groupPermissionRequests) {
			HpcGroupPermissionResponseDTO groupPermissionResponse = new HpcGroupPermissionResponseDTO();
			groupPermissionResponse.setGroupName(groupPermissionRequest.getGroupName());
			groupPermissionResponse.setResult(true);
			subjectPermissionRequest.setPermission(groupPermissionRequest.getPermission());
			subjectPermissionRequest.setSubject(groupPermissionRequest.getGroupName());
		    try {
		    	 // Set the entity permission.
		    	 if(collection) {
		    	    dataManagementService.setCollectionPermission(path, subjectPermissionRequest);
		    	 } else {
		    		     dataManagementService.setDataObjectPermission(path, subjectPermissionRequest);
		    	 }
			     
		    } catch(HpcException e) {
		    	    // Request failed. Record the message and keep going.
		    		groupPermissionResponse.setResult(false);
		    		groupPermissionResponse.setMessage(e.getMessage());
		    }
		
		    // Add this user permission response to the list.
		    permissionResponses.add(groupPermissionResponse);
		}
		
		return permissionResponses;
	}
	
    /** 
     * Construct entity permissions DTO out of a list of subject permissions.
     * 
     * @param subjectPermissions A list of subject permissions.
     * @return Entity permissions DTO
     */
	private HpcEntityPermissionsDTO toEntityPermissionsDTO(List<HpcSubjectPermission> subjectPermissions)
	{
		if(subjectPermissions == null) {
		   return null;
		}
		
		HpcEntityPermissionsDTO entityPermissions = new HpcEntityPermissionsDTO();
		for(HpcSubjectPermission subjectPermission : subjectPermissions) {
			if(subjectPermission.getSubjectType().equals(HpcSubjectType.USER)) {
			   HpcUserPermission userPermission = new HpcUserPermission();
			   userPermission.setPermission(subjectPermission.getPermission());
			   userPermission.setUserId(subjectPermission.getSubject());
			   entityPermissions.getUserPermissions().add(userPermission);
			} else {
				    HpcGroupPermission groupPermission = new HpcGroupPermission();
				    groupPermission.setPermission(subjectPermission.getPermission());
				    groupPermission.setGroupName(subjectPermission.getSubject());
				    entityPermissions.getGroupPermissions().add(groupPermission);
			}
		}
		
		return entityPermissions;
	}
	
    /** 
     * Construct user permission DTO out of subject permission.
     * 
     * @param subjectPermission A subject permission.
     * @return user permission DTO.
     */
	private HpcUserPermissionDTO toUserPermissionDTO(HpcSubjectPermission subjectPermission)
	{
		if(subjectPermission == null) {
		   return null;
		}
		
		HpcUserPermissionDTO userPermission = new HpcUserPermissionDTO();
			   userPermission.setPermission(subjectPermission.getPermission());
			   userPermission.setUserId(subjectPermission.getSubject());
		
		return userPermission;
	}
	
    /**
     * Get collection download task status.
     *
     * @param taskId The collection download task ID.
     * @param taskType COLLECTION or DATA_OBJECT_LIST
     * @return A collection download status DTO. Null if the task could not be found.
     */
    private HpcCollectionDownloadStatusDTO getCollectionDownloadStatus(String taskId,
    		                                                           HpcDownloadTaskType taskType) 
                                                                      throws HpcException
	{
		// Input validation.
		if(taskId == null) {
		   throw new HpcException("Null collection download task ID",
		                          HpcErrorType.INVALID_REQUEST_INPUT);	
		}
		
		// Get the download task status.
		HpcDownloadTaskStatus taskStatus = 
		                      dataTransferService.getDownloadTaskStatus(taskId, taskType);
		if(taskStatus == null) {
		   return null;
		}
		
		// Map the task status to DTO.
		HpcCollectionDownloadStatusDTO downloadStatus = new HpcCollectionDownloadStatusDTO();
		downloadStatus.setInProgress(taskStatus.getInProgress());
		if(taskStatus.getInProgress()) {
			// Download in progress. Populate the DTO accordingly.
			if(taskType.equals(HpcDownloadTaskType.COLLECTION)) {
			   downloadStatus.setPath(taskStatus.getCollectionDownloadTask().getPath());
			} else if(taskType.equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
				downloadStatus.getDataObjectPaths().addAll(taskStatus.getCollectionDownloadTask().getDataObjectPaths());
			}
			downloadStatus.setCreated(taskStatus.getCollectionDownloadTask().getCreated());
			downloadStatus.setTaskStatus(taskStatus.getCollectionDownloadTask().getStatus());
			downloadStatus.setDestinationLocation(taskStatus.getCollectionDownloadTask().getDestinationLocation());
			populateDownloadItems(downloadStatus, taskStatus.getCollectionDownloadTask().getItems());
		
		} else {
				// Download completed or failed. Populate the DTO accordingly. 
			    if(taskType.equals(HpcDownloadTaskType.COLLECTION)) { 
				   downloadStatus.setPath(taskStatus.getResult().getPath());
			    } 
				downloadStatus.setCreated(taskStatus.getResult().getCreated());
				downloadStatus.setDestinationLocation(taskStatus.getResult().getDestinationLocation());
				downloadStatus.setCompleted(taskStatus.getResult().getCompleted());
				downloadStatus.setMessage(taskStatus.getResult().getMessage());
				downloadStatus.setResult(taskStatus.getResult().getResult());
				populateDownloadItems(downloadStatus, taskStatus.getResult().getItems());
		}
		
		return downloadStatus;
	}
    
	/** 
     * Split the list of download items into completed, failed and in-progress buckets.
     * 
     * @param downloadStatus The download status to populate the items into.
     * @param items The collection download items.
     * @return A data management tree .
     * @throws HpcException on service failure.
     */
    private void populateDownloadItems(HpcCollectionDownloadStatusDTO downloadStatus,
    		                           List<HpcCollectionDownloadTaskItem> items)
    {
    	for(HpcCollectionDownloadTaskItem item : items) {
    		Boolean result = item.getResult();
    		if(result == null) {
    		   downloadStatus.getInProgressItems().add(item);
    		} else if(result) {
    			      downloadStatus.getCompletedItems().add(item);
    		} else {
    			    downloadStatus.getFailedItems().add(item);
    		}
    	}
    }
    
	/** 
     * Delete a data object from the archive.
     * 
     * @param path The data object path.
     * @param systemGeneratedMetadata The system generetaed metadata.
     * @param dataObjectDeleteResponse The deletion response DTO.
     */
    private void deleteDataObjectFromArchive(String path, HpcSystemGeneratedMetadata systemGeneratedMetadata,
    		                                 HpcDataObjectDeleteResponseDTO dataObjectDeleteResponse)
    {
       updateDataTransferUploadStatus(path, HpcDataTransferUploadStatus.DELETE_REQUESTED);
 	
       try {
            dataTransferService.deleteDataObject(systemGeneratedMetadata.getArchiveLocation(), 
	     	                                      systemGeneratedMetadata.getDataTransferType(),
	     	                                      systemGeneratedMetadata.getConfigurationId());
    
       } catch(HpcException e) {
	            logger.error("Failed to delete file from archive", e);
	            updateDataTransferUploadStatus(path, HpcDataTransferUploadStatus.DELETE_FAILED);
	            dataObjectDeleteResponse.setArchiveDeleteStatus(false);
	            dataObjectDeleteResponse.setMessage(e.getMessage());
       }
       
       updateDataTransferUploadStatus(path, HpcDataTransferUploadStatus.DELETED);
    }

	/** 
     * Attempt to update data object upload status. No exception thrown is failed.
     * 
     * @param path The data object path.
     * @param dataTransferStatus The data transfer upload system generetaed metadata.
     * @param dataObjectDeleteResponse The deletion response DTO.
     */
    private void updateDataTransferUploadStatus(String path, HpcDataTransferUploadStatus dataTransferStatus)
    {
    	try {
 		     metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null, 
 		    		                                                 dataTransferStatus, null, null);
 		 
    	} catch(HpcException e) {
 		        logger.error("Failed to update system metadata: " + path + ". Data transfer status: " +
    	                     dataTransferStatus, HpcErrorType.UNEXPECTED_ERROR, e);
    	}
    }
    
	/** 
     * Split the list of registration items into completed, failed and in-progress buckets.
     * 
     * @param registrationStatus The registration status to populate the items into.
     * @param items The registration items.
     * @return A data management tree .
     * @throws HpcException on service failure.
     */
    private void populateRegistrationItems(HpcDataObjectListRegistrationStatusDTO registrationStatus,
    		                               List<HpcDataObjectListRegistrationItem> items)
    {
    	for(HpcDataObjectListRegistrationItem item : items) {
    		Boolean result = item.getTask().getResult();
    		if(result == null) {
    		   registrationStatus.getInProgressItems().add(item.getTask());
    		} else if(result) {
    			      registrationStatus.getCompletedItems().add(item.getTask());
    		} else {
    			    registrationStatus.getFailedItems().add(item.getTask());
    		}
    	}
    }
    
	/** 
     * Calculate MD5 checksum of the file and compare it to the value provided.
     * 
     * @param file The file to validate checksum.
     * @param checksum The checksum value provided by the caller.
     * @throws HpcException If the calculated checksum doesn't match the provided value.
     */
    private void validateChecksum(File file, String checksum) throws HpcException
    {
    	if(file == null || StringUtils.isEmpty(checksum)) {
    	   return;
    	}
    	
        FileInputStream fileInputStream = null;
        try {
        	 fileInputStream = new FileInputStream(file);
             if(!checksum.equals(DigestUtils.md5DigestAsHex(IOUtils.toByteArray(fileInputStream)))) {
            	throw new HpcException("Checksum validation failed",
		                                HpcErrorType.INVALID_REQUEST_INPUT);	
             }

        } catch(IOException e) {
        	    throw new HpcException("Failed calculate checksum",
                                       HpcErrorType.UNEXPECTED_ERROR, e);	
        	    
        } finally {
        	       IOUtils.closeQuietly(fileInputStream);
        }
    }
}

 