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
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataOrigin;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupPermissionResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcNamedCompoundMetadataQueryListDTO;
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
    	        
			    // Attach the parent metadata.
		        HpcMetadataOrigin metadataOrigin = dataManagementService.addParentMetadata(path);
       	   
    	        // Generate system metadata and attach to the collection.
       	        dataManagementService.addSystemGeneratedMetadataToCollection(path,
       	        		                                                     metadataOrigin);
       	        
       	        // Validate the collection hierarchy.
       	        dataManagementService.validateHierarchy(path, false);
       	        
       	        registrationCompleted = true;
       	        
    	   } finally {
			          if(!registrationCompleted) {
				         // Collection registration failed. Remove it from Data Management.
				         dataManagementService.delete(path);
			          }
	       }
       	   
    	} else {
    		    updateCollection(path, metadataEntries);
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
    	return getCollection(dataManagementService.getCollection(path));
    }
    
    @Override
    public HpcCollectionListDTO getCollections(List<HpcMetadataQuery> metadataQueries,
    		                                   boolean detailedResponse, int page) 
                                              throws HpcException
    {
    	logger.info("Invoking getCollections(List<HpcMetadataQuery>, boolean): " + 
    			    metadataQueries);
    	
    	// Input validation.
    	if(metadataQueries == null) {
    	   throw new HpcException("Null metadata queries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	if(detailedResponse) {
    	   return toCollectionListDTO(
    			    dataManagementService.getCollections(metadataQueries, page), null);
    	} else {
       		    return toCollectionListDTO(
       		    		 null, dataManagementService.getCollectionPaths(metadataQueries, page));
    	}
    }
    
    @Override
    public HpcCollectionListDTO getCollections(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
                                              throws HpcException
    {
    	logger.info("Invoking getCollections(HpcCompoundMetadataQueryDTO): " + compoundMetadataQueryDTO);
    	
    	// Input validation.
    	if(compoundMetadataQueryDTO == null) {
    	   throw new HpcException("Null compound metadata query",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
      	boolean detailedResponse = compoundMetadataQueryDTO.getDetailedResponse() != null && 
                                   compoundMetadataQueryDTO.getDetailedResponse();
      	int page = compoundMetadataQueryDTO.getPage() != null ? compoundMetadataQueryDTO.getPage() : 1;
      	
    	if(detailedResponse) {
    	   return toCollectionListDTO(
    			    dataManagementService.getCollections(compoundMetadataQueryDTO.getQuery(), page), 
    			    null);
    	} else {
       		    return toCollectionListDTO(
       		    		 null, 
       		    		 dataManagementService.getCollectionPaths(compoundMetadataQueryDTO.getQuery(), page));
    	}
    }
    
    @Override
    public HpcCollectionListDTO getCollections(String queryName, boolean detailedResponse, int page) 
                                              throws HpcException
    {
    	logger.info("Invoking getCollections(string,boolean): " + queryName);
    	
    	return getCollections(toCompoundMetadataQueryDTO(queryName, detailedResponse, page));
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
	    boolean created = dataManagementService.createFile(path);
	    
	    if(created) {
    	   boolean registrationCompleted = false; 
    	   try {
      	        // Validate the new collection meets the hierarchy definition.
      	        dataManagementService.validateHierarchy(path, true);
    		   
    		    // Assign system account as an additional owner of the data-object.
   		        dataManagementService.assignSystemAccountPermission(path);
   		  
		        // Attach the user provided metadata.
		        dataManagementService.addMetadataToDataObject(
		    	   		                 path, 
		    			                 dataObjectRegistrationDTO.getMetadataEntries());
		        
			    // Attach the parent metadata.
		        HpcMetadataOrigin metadataOrigin = dataManagementService.addParentMetadata(path);
		        
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
		        
			    // Generate system metadata and attach to the data object.
			    dataManagementService.addSystemGeneratedMetadataToDataObject(
			        		                   path, uploadResponse.getArchiveLocation(),
			    			                   source, uploadResponse.getDataTransferRequestId(), 
			    			                   uploadResponse.getDataTransferStatus(),
			    			                   uploadResponse.getDataTransferType(),
			    			                   getSourceSize(uploadResponse.getDataTransferRequestId(), source, uploadResponse.getDataTransferType(),
				                                             dataObjectFile), 
			    			                   dataObjectRegistrationDTO.getCallerObjectId(),
			    			                   metadataOrigin); 
	
			    registrationCompleted = true;
			     
	    	} finally {
	    			   if(!registrationCompleted) {
	    				  // Data object registration failed. Remove it from Data Management.
	    				  dataManagementService.delete(path);
	    			   }
	    	}
    	   
	    } else {
	    	    if(dataObjectFile != null) {
	    		   throw new HpcException("Data object cannot be updated. Only updating metadata is allowed.",
			                              HpcErrorType.REQUEST_REJECTED);
	    	    }
	    	
	            updateDataObject(path, dataObjectRegistrationDTO.getMetadataEntries());
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
    	return getDataObject(dataManagementService.getDataObject(path));
    }
    
    @Override
    public HpcDataObjectListDTO getDataObjects(List<HpcMetadataQuery> metadataQueries, 
    		                                   boolean detailedResponse, int page) 
                                              throws HpcException
    {
    	logger.info("Invoking getDataObjects(List<HpcMetadataQuery>, boolean): " + metadataQueries);
    	
    	// Input validation.
    	if(metadataQueries == null) {
    	   throw new HpcException("Null metadata queries",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	if(detailedResponse) {
    	   return toDataObjectListDTO(
    			    dataManagementService.getDataObjects(metadataQueries, page), null);
    	} else {
    		    return toDataObjectListDTO(
    		    		 null, dataManagementService.getDataObjectPaths(metadataQueries, page));
    	}
    }
    
    @Override
    public HpcDataObjectListDTO getDataObjects(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
                                              throws HpcException
    {
    	logger.info("Invoking getDataObjects(HpcCompoundMetadataQueryDTO): " + compoundMetadataQueryDTO);
    	
    	// Input validation.
    	if(compoundMetadataQueryDTO == null) {
    	   throw new HpcException("Null compound metadata query",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	boolean detailedResponse = compoundMetadataQueryDTO.getDetailedResponse() != null && 
    			                   compoundMetadataQueryDTO.getDetailedResponse();
    	int page = compoundMetadataQueryDTO.getPage() != null ? compoundMetadataQueryDTO.getPage() : 1;
    	
    	if(detailedResponse) {
    	   return toDataObjectListDTO(
    			    dataManagementService.getDataObjects(compoundMetadataQueryDTO.getQuery(), page), 
    			    null);
    	} else {
    		    return toDataObjectListDTO(
    		    		 null, 
    		    		 dataManagementService.getDataObjectPaths(compoundMetadataQueryDTO.getQuery(), page));
    	}
    }
    
    @Override
    public HpcDataObjectListDTO getDataObjects(String queryName, boolean detailedResponse, 
    		                                   int page) 
                                              throws HpcException
    {
    	logger.info("Invoking getDataObjects(string,boolean): " + queryName);
    	
    	return getDataObjects(toCompoundMetadataQueryDTO(queryName, detailedResponse, page));
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
    	HpcSystemGeneratedMetadata metadata = 
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
    			               dataManagementService.getCollectionMetadataValidationRules(doc));
    	dataManagementModel.getDataObjectMetadataValidationRules().addAll(
	                           dataManagementService.getDataObjectMetadataValidationRules(doc));
    	dataManagementModel.setDataHierarchy(dataManagementService.getDataHierarchy(doc));
    	
    	return dataManagementModel;
    }
    
    @Override
	public HpcMetadataAttributesListDTO getMetadataAttributes(String collectionType) 
                                                             throws HpcException
    {
    	logger.info("Invoking getDataManagementMode(String): " + collectionType);
    	
    	HpcMetadataAttributesListDTO metadataAttributes = new HpcMetadataAttributesListDTO();
    	metadataAttributes.getMetadataAttributes().addAll(
    			dataManagementService.getMetadataAttributes(collectionType));
    	
    	return metadataAttributes;
    }
    
    @Override
    public void saveQuery(String queryName,
    		              HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
    		             throws HpcException
    {
    	logger.info("Invoking saveQuery(String, HpcCompoundMetadataQueryDTO)");
    	
    	// Input validation.
    	if(queryName == null || queryName.isEmpty() ||
           compoundMetadataQueryDTO == null) {
    	   throw new HpcException("Null or empty queryName / compoundMetadataQueryDTO", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery = new HpcNamedCompoundMetadataQuery();
    	namedCompoundMetadataQuery.setName(queryName);
    	namedCompoundMetadataQuery.setCompoundQuery(compoundMetadataQueryDTO.getQuery());
    	
    	// Save the query.
    	dataManagementService.saveQuery(securityService.getRequestInvoker().getNciAccount().getUserId(), 
    			                        namedCompoundMetadataQuery);
    }
    
    @Override
    public void deleteQuery(String queryName) throws HpcException
    {
    	logger.info("Invoking deleteQuery(String)");
    	
    	// Input validation.
    	if(queryName == null || queryName.isEmpty())  {
    	   throw new HpcException("Null or empty nciUserId / queryName", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
    	// Delete the query.
    	dataManagementService.deleteQuery(securityService.getRequestInvoker().getNciAccount().getUserId(), 
    			                          queryName);
    }

    @Override
    public HpcNamedCompoundMetadataQueryListDTO getQueries() throws HpcException
    {
    	logger.info("Invoking getQueries()");
    	
    	HpcNamedCompoundMetadataQueryListDTO queriesList = new HpcNamedCompoundMetadataQueryListDTO();
    	queriesList.getQueries().addAll(dataManagementService.getQueries(
    			                        securityService.getRequestInvoker().getNciAccount().getUserId()));
    	
    	return queriesList;
    }
    	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Create a collection DTO from a domain object.
     * 
     * @param collection The collection domain object.
     *
     * @return The DTO.
     * @throws HpcException If it failed to get the metadata
     */
    private HpcCollectionDTO getCollection(HpcCollection collection) throws HpcException
    {
    	if(collection == null) {
     	   return null;
     	}
    	
    	// Get the metadata.
    	HpcMetadataEntries metadataEntries = 
    	   dataManagementService.getCollectionMetadataEntries(collection.getAbsolutePath());
		
    	HpcCollectionDTO collectionDTO = new HpcCollectionDTO();
    	collectionDTO.setCollection(collection);
        collectionDTO.setMetadataEntries(metadataEntries);
    	
    	return collectionDTO;
    }
    
    /**
     * Create a data object DTO from domain object.
     * 
     * @param data object The data object domain object.
     *
     * @return The DTO.
     */
    private HpcDataObjectDTO getDataObject(HpcDataObject dataObject) throws HpcException
    {
    	if(dataObject == null) {
     	   return null;
     	}
    	
    	// Get the metadata for this data object.
    	HpcMetadataEntries metadataEntries = 
    		               dataManagementService.getDataObjectMetadataEntries(dataObject.getAbsolutePath());
    	String transferPercentCompletion = getDataTransferUploadPercentCompletion(
		               dataManagementService.toSystemGeneratedMetadata(metadataEntries.getSelfMetadataEntries()));
    		
    	HpcDataObjectDTO dataObjectDTO = new HpcDataObjectDTO();
    	dataObjectDTO.setDataObject(dataObject);
    	dataObjectDTO.setMetadataEntries(metadataEntries);
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
     * Get the data object source size. (Either source or dataObjectFile are not null)
     * 
     * @param source A data transfer source.
     * @param dataTransferType The data transfer type.
     * @param dataObjectFile The attached data file.
     * 
     * @return The source size in bytes.
     */
	private Long getSourceSize(String requestId, HpcFileLocation source, HpcDataTransferType dataTransferType,
			                   File dataObjectFile) throws HpcException
	{
		if(source != null) {
		     return dataTransferService.getDataTransferSize(
		    		 dataTransferType,
		    		 requestId);
		}
		if(dataObjectFile != null) {
           return dataObjectFile.length();
		}
		return null;
	}
	
    /**
     * Update a Collection.
     *
     * @param path The collection's path.
     * @param metadataEntries A list of metadata entries to update.
     * 
     * @throws HpcException
     */
    private void updateCollection(String path,
                                  List<HpcMetadataEntry> metadataEntries)  
                                 throws HpcException
    {
    	dataManagementService.updateCollectionMetadata(path, metadataEntries);
    	
       	// Update the metadata origin system metadata.
       	HpcMetadataOrigin metadataOrigin =
       	   dataManagementService.updateMetadataOrigin(
       		   dataManagementService.getCollectionSystemGeneratedMetadata(path).getMetadataOrigin(), 
       		   metadataEntries);
       	dataManagementService.updateCollectionSystemGeneratedMetadata(path, metadataOrigin);
       	
       	// Propagate the metadata update to the tree below.
       	dataManagementService.updateMetadataTree(path);
    }
    
    /**
     * Update a Data object.
     *
     * @param path The data object's path.
     * @param metadataEntries A list of metadata entries to update.
     * 
     * @throws HpcException
     */
    private void updateDataObject(String path,
                                  List<HpcMetadataEntry> metadataEntries)  
                                 throws HpcException
    {
        dataManagementService.updateDataObjectMetadata(path, metadataEntries); 
        
       	// Update the metadata origin system metadata.
       	HpcMetadataOrigin metadataOrigin =
       	   dataManagementService.updateMetadataOrigin(
       		   dataManagementService.getDataObjectSystemGeneratedMetadata(path).getMetadataOrigin(), 
       		   metadataEntries);
       	dataManagementService.updateDataObjectSystemGeneratedMetadata(path, null, null, null, 
       			                                                      null, metadataOrigin);
    }
    
    
    /**
     * Construct a collection list DTO.
     *
     * @param collections A list of collection domain objects.
     * @param collectionPaths A list of collection paths.
     */
    private HpcCollectionListDTO toCollectionListDTO(List<HpcCollection> collections,
    		                                         List<String> collectionPaths)
    {
		HpcCollectionListDTO collectionsDTO = new HpcCollectionListDTO();
		
		if(collections != null) {
		   for(HpcCollection collection : collections) {
			   // Get the metadata for this collection.
			   try {
				    // Combine collection attributes and metadata into a single DTO.
				    collectionsDTO.getCollections().add(getCollection(collection));
				 
	    	   } catch(HpcException e) {
	    			   // Failed to get metadata.
	    			   logger.error("Failed to fetch metadata for "+ collection.getAbsolutePath(), e);
	    			   continue;
	    	   }
		   }
		} 
		
		if(collectionPaths != null) {
		   String basePath = dataManagementService.getBasePath();
		   for(String collectionPath : collectionPaths) {
		       collectionsDTO.getCollectionPaths().add(getRelativePath(collectionPath, basePath));
		   }
		}
		
		return collectionsDTO;
    }
    
    /**
     * Construct a data object list DTO.
     *
     * @param dataObjects A list of data object domain objects.
     * @param dataObjectPaths A list of data object paths.
     */
    private HpcDataObjectListDTO toDataObjectListDTO(List<HpcDataObject> dataObjects,
    		                                         List<String> dataObjectPaths)
    {
		// Construct the DTO.
		HpcDataObjectListDTO dataObjectsDTO = new HpcDataObjectListDTO();
		
		if(dataObjects != null) {
		   for(HpcDataObject dataObject : dataObjects) {
			   try {
					// Combine data object attributes and metadata into a single DTO.
	    		    dataObjectsDTO.getDataObjects().add(getDataObject(dataObject));
	
			   } catch(HpcException e) {
				       // Failed to get metadata for data object.
				       logger.error("Failed to fetch metadata for "+ dataObject.getAbsolutePath(), e);
				       continue;
			   }
		   }
		}
		
		if(dataObjectPaths != null) {
		   String basePath = dataManagementService.getBasePath();
		   for(String dataObjectPath : dataObjectPaths) {
		       dataObjectsDTO.getDataObjectPaths().add(getRelativePath(dataObjectPath, basePath));
		   }
		}
		
		return dataObjectsDTO;
    }
    
    /**
     * Construct a HpcCompoundMetadataQueryDTO from a named query.
     *
     * @param queryName The user query.
     * @param detailedResponse The detailed response indicator.
     * @param The requested results page
     * @return HpcCompoundMetadataQueryDTO
     * 
     * @throws HpcException If the user query was not found
     */
    private HpcCompoundMetadataQueryDTO 
               toCompoundMetadataQueryDTO(String queryName, boolean detailedResponse, 
            		                      int page)
                                         throws HpcException
    {
    	// Input validation.
    	if(queryName == null || queryName.isEmpty()) {
    	   throw new HpcException("Null or empty query name",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	
		// Get the user query.
		HpcNamedCompoundMetadataQuery namedCompoundQuery = 
		   dataManagementService.getQuery(
	                     securityService.getRequestInvoker().getNciAccount().getUserId(), 
	                     queryName);
		if(namedCompoundQuery == null || namedCompoundQuery.getCompoundQuery() == null) {
		   throw new HpcException("User query not found: " + queryName,
				                  HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		// Invoke the query.
		HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO = new HpcCompoundMetadataQueryDTO();
		compoundMetadataQueryDTO.setQuery(namedCompoundQuery.getCompoundQuery());
		compoundMetadataQueryDTO.setDetailedResponse(detailedResponse);
		compoundMetadataQueryDTO.setPage(page);
		
		return compoundMetadataQueryDTO;
    }
    
    private String getRelativePath(String absolutePath, String basePath)
    {
    	if(absolutePath == null)
    		return absolutePath;
    	
    	if(absolutePath.startsWith(basePath))
    		return absolutePath.substring(basePath.length());
    	else
    		return absolutePath;
    	
    }
}

 