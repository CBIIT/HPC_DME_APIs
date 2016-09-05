/**
 * HpcDataManagementServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.*;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataEntries;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataQueries;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNciAccount;
import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcEntityPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataOrigin;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcDataManagementAccount;
import gov.nih.nci.hpc.domain.model.HpcGroup;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.user.HpcGroupResponse;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataManagementService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Management Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataManagementServiceImpl implements HpcDataManagementService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
	private static final String EQUAL_OPERATOR = "EQUAL";
	private static final String INVALID_PATH_METADATA_MSG = 
			                    "Invalid path or metadata entry";
	
	// Data Management OWN permission.
	private static final String OWN_PERMISSION = "OWN"; 
	
	// JSON attributes.
	private static final String JSON_METADATA_ATTRIBUTES = "metadataAttributes"; 
	private static final String JSON_PARENT_METADATA_ATTRIBUTES = "parentMetadataAttributes"; 
	
	// Hierarchical metadata policies. 
	private static final String METADATA_REPLICATION_POLICY = "replication";
	private static final String METADATA_VIEWS_POLICY = "views";
	private static final String NO_HIERARCHICAL_METADATA_POLICY = "none";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
	// Metadata Validator.
	@Autowired
	private HpcMetadataValidator metadataValidator = null;
	
	// Key Generator.
	@Autowired
	private HpcKeyGenerator keyGenerator = null;
	
	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;
	
	// Metadata DAO.
	@Autowired
	private HpcMetadataDAO metadataDAO = null;
	
	// Prepared query to get data objects that have their data transfer in-progress to archive.
	private List<HpcMetadataQuery> dataTransferInProgressToArchiveQuery = new ArrayList<>();
	
	// Prepared query to get data objects that have their data transfer in-progress to temporary archive.
	private List<HpcMetadataQuery> dataTransferInProgressToTemporaryArchiveQuery = new ArrayList<>();
	
	// Prepared query to get data objects that have their data in temporary archive.
	private List<HpcMetadataQuery> dataTransferInTemporaryArchiveQuery = new ArrayList<>();
	
	// Policy to support hierarchical metadata search. Policies are:
	// 1. 'replication' - metadata at a collection level are propagated through the hierarchy.
	// 2. 'views' - Custom DB views (on top of iRODS DB) are used to support hierarchical metadata search.
	// 3. 'none' - Hierarchical metadata search not supported.
	private String hierarchicalMetadataPolicy = null;

    // The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param hierarchicalMetadataPolicy The desired hierarchical metadata policy.
     * @throws HpcException
     */
    private HpcDataManagementServiceImpl(String hierarchicalMetadataPolicy) throws HpcException
    {
    	if(hierarchicalMetadataPolicy == null ||
    	   (!hierarchicalMetadataPolicy.equals(METADATA_REPLICATION_POLICY) &&
    	    !hierarchicalMetadataPolicy.equals(METADATA_VIEWS_POLICY) &&
    	    !hierarchicalMetadataPolicy.equals(NO_HIERARCHICAL_METADATA_POLICY))) {
    	   throw new HpcException("Invalid hierarchical metadata policy: " + hierarchicalMetadataPolicy,
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	this.hierarchicalMetadataPolicy = hierarchicalMetadataPolicy;
    	
    	// Prepare the query to get data objects in data transfer in-progress to archive.
        dataTransferInProgressToArchiveQuery.add(
            toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, 
            		        EQUAL_OPERATOR, 
        	                HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE.value()));
        
        // Prepare the query to get data objects in data transfer in-progress to temporary archive.
        dataTransferInProgressToTemporaryArchiveQuery.add(
        	toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, 
        			        EQUAL_OPERATOR, 
        			        HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE.value()));
        
        // Prepare the query to get data objects in temporary archive.
        dataTransferInTemporaryArchiveQuery.add(
        	toMetadataQuery(DATA_TRANSFER_STATUS_ATTRIBUTE, 
        			        EQUAL_OPERATOR, 
        			        HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE.value()));
    }   
    
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
	private HpcDataManagementServiceImpl() throws HpcException
    {
    	throw new HpcException("Default Constructor disabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataManagementService Interface Implementation
    //---------------------------------------------------------------------//  

    @Override
    public boolean createDirectory(String path) throws HpcException
    {
    	Object authenticatedToken = getAuthenticatedToken();
    	
    	// Validate the directory path.
    	HpcPathAttributes pathAttributes = 
    	   dataManagementProxy.getPathAttributes(authenticatedToken, path);
    	if(pathAttributes.getExists()) {
    	   if(pathAttributes.getIsDirectory()) {
    		  // Directory already exists.
    		  return false;
    	   }
    	   if(pathAttributes.getIsFile()) {
    		  throw new HpcException("Path already exists as a file: " + path, 
    				                 HpcErrorType.INVALID_REQUEST_INPUT); 
    	   }
    	}
    	
    	// Create the directory.
    	dataManagementProxy.createCollectionDirectory(authenticatedToken, path);
    	return true;
    }
    
    @Override
    public boolean createFile(String path, boolean createParentPathDirectory) 
    		                 throws HpcException
    {
    	Object authenticatedToken = getAuthenticatedToken();
    	
    	// Validate the file path.
    	HpcPathAttributes pathAttributes = 
    	   dataManagementProxy.getPathAttributes(authenticatedToken, path);
    	if(pathAttributes.getExists()) {
    	   if(pathAttributes.getIsFile()) {
    		  // File already exists.
    		  return false;
    	   }
    	   if(pathAttributes.getIsDirectory()) {
    		  throw new HpcException("Path already exists as a directory: " + path, 
    				                 HpcErrorType.INVALID_REQUEST_INPUT); 
    	   }
    	}
    	
    	//  Validate the parent directory exists.
    	if(!createParentPathDirectory && 
    	   !dataManagementProxy.isParentPathDirectory(authenticatedToken, path)) {
    		throw new HpcException("Invalid data object path. Directory doesn't exist: " + path, 
                                   HpcRequestRejectReason.INVALID_DATA_OBJECT_PATH);
    	}
    	
    	// Create the parent directory if it doesn't already exist.
    	if(createParentPathDirectory) {
    	   dataManagementProxy.createParentPathDirectory(authenticatedToken, path);
    	}
    	
    	// Create the data object file.
    	dataManagementProxy.createDataObjectFile(authenticatedToken, path);
    	return true;
    }
    
    @Override
    public void delete(String path) throws HpcException
    {
    	// Delete the data object file.
    	dataManagementProxy.delete(getAuthenticatedToken(), path);
    }

    @Override
    public void addMetadataToCollection(String path, 
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidMetadataEntries(metadataEntries)) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Validate Metadata.
       	metadataValidator.validateCollectionMetadata(null, metadataEntries);
       	
       	// Add Metadata to the DM system.
       	dataManagementProxy.addMetadataToCollection(getAuthenticatedToken(),
       			                                    path, metadataEntries);
    }
    
    @Override
    public void addSystemGeneratedMetadataToCollection(String path,
    		                                           HpcMetadataOrigin metadataOrigin) 
                                                      throws HpcException
    {
       	// Input validation.
       	if(path == null) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
       	
       	// Generate a collection ID and add it as metadata.
       	metadataEntries.add(generateIdMetadata());
       	
       	// Create and add the registrar ID, name and DOC metadata.
       	metadataEntries.addAll(generateRegistrarMetadata());
       	
       	// Add the metadata origin metadata.
       	addMetadataEntry(metadataEntries,
       			         generateMetadataOriginMetadata(metadataOrigin));
       	
       	// Add Metadata to the DM system.
       	dataManagementProxy.addMetadataToCollection(getAuthenticatedToken(), 
       			                                    path, metadataEntries);    	
    }
    
    @Override
    public void updateCollectionMetadata(String path, 
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidMetadataEntries(metadataEntries)) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Validate Metadata.
       	metadataValidator.validateCollectionMetadata(getCollectionMetadata(path),
       			                                     metadataEntries);
       	
       	// Add Metadata to the DM system.
       	dataManagementProxy.updateCollectionMetadata(getAuthenticatedToken(),
       			                                     path, metadataEntries);
    }
    
    @Override
    public void addMetadataToDataObject(String path, 
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidMetadataEntries(metadataEntries)) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Validate Metadata.
       	metadataValidator.validateDataObjectMetadata(null, metadataEntries);
       	
       	
       	// Add Metadata to the DM system.
       	dataManagementProxy.addMetadataToDataObject(getAuthenticatedToken(), 
       			                                    path, metadataEntries);
    }
    
    @Override
    public void updateDataObjectMetadata(String path, 
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidMetadataEntries(metadataEntries)) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Validate Metadata.
       	metadataValidator.validateDataObjectMetadata(getDataObjectMetadata(path),
       			                                     metadataEntries);
       	
       	// Update Metadata.
       	dataManagementProxy.updateDataObjectMetadata(getAuthenticatedToken(),
       			                                     path, metadataEntries);
    }
    
    @Override
    public void addSystemGeneratedMetadataToDataObject(String path, 
                                                       HpcFileLocation archiveLocation,
    		                                           HpcFileLocation sourceLocation,
    		                                           String dataTransferRequestId,
    		                                           HpcDataTransferUploadStatus dataTransferStatus,
    		                                           HpcDataTransferType dataTransferType,
    		                                           Long sourceSize, String callerObjectId,
    		                                           HpcMetadataOrigin metadataOrigin) 
                                                      throws HpcException
    {
       	// Input validation.
       	if(path == null || dataTransferStatus == null || dataTransferType == null) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
		                          HpcErrorType.INVALID_REQUEST_INPUT);	
       	}
       	if(!isValidFileLocation(archiveLocation) ||
       	   (sourceLocation != null && !isValidFileLocation(sourceLocation))) {
       	   throw new HpcException("Invalid source/archive location", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
       	
       	// Generate a data-object ID and add it as metadata.
       	metadataEntries.add(generateIdMetadata());
       	
       	// Create and add the registrar ID, name and DOC metadata.
       	metadataEntries.addAll(generateRegistrarMetadata());
       	
       	// Add the metadata origin metadata.
       	addMetadataEntry(metadataEntries,
       	                 generateMetadataOriginMetadata(metadataOrigin));
       	
       	if(sourceLocation != null) {
       	   // Create the source location file-container-id metadata.
       	   addMetadataEntry(metadataEntries,
       			            toMetadataEntry(SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE, 
       			                            sourceLocation.getFileContainerId()));
       	
       	   // Create the source location file-id metadata.
       	   addMetadataEntry(metadataEntries,
       			            toMetadataEntry(SOURCE_LOCATION_FILE_ID_ATTRIBUTE, 
       			                            sourceLocation.getFileId()));
       	}
       	
       	// Create the archive location file-container-id metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE, 
       			                         archiveLocation.getFileContainerId()));
       	
       	// Create the archive location file-id metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE, 
       			                         archiveLocation.getFileId()));
       	
   	    // Create the Data Transfer Request ID metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(DATA_TRANSFER_REQUEST_ID_ATTRIBUTE, 
       			                         dataTransferRequestId));
       	
       	// Create the Data Transfer Status metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(DATA_TRANSFER_STATUS_ATTRIBUTE, 
       		                             dataTransferStatus.value()));
       	
       	// Create the Data Transfer Type metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(DATA_TRANSFER_TYPE_ATTRIBUTE, 
       		                             dataTransferType.value()));
       	
       	// Create the Source File Size metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(SOURCE_FILE_SIZE_ATTRIBUTE, 
       			                         sourceSize));
       	
        // Create the Caller Object ID metadata.
        addMetadataEntry(metadataEntries,
        		         toMetadataEntry(CALLER_OBJECT_ID_ATTRIBUTE, 
        	                             callerObjectId));
        
       	// Add Metadata to the DM system.
       	dataManagementProxy.addMetadataToDataObject(getAuthenticatedToken(), 
       			                                    path, metadataEntries);    	
    }
    
    @Override
    public void updateDataObjectSystemGeneratedMetadata(String path, 
                                                        HpcFileLocation archiveLocation,
                                                        String dataTransferRequestId,
                                                        HpcDataTransferUploadStatus dataTransferStatus,
                                                        HpcDataTransferType dataTransferType,
                                                        HpcMetadataOrigin metadataOrigin) 
                                                        throws HpcException
	{
       	// Input validation.
       	if(path == null || 
       	   (archiveLocation != null && !isValidFileLocation(archiveLocation))) {
       	   throw new HpcException("Invalid updated system generated metadata for data object", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
       	
       	if(archiveLocation != null) {
       	   // Update the archive location file-container-id metadata.
       	   addMetadataEntry(metadataEntries,
       			            toMetadataEntry(ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE, 
       		                                archiveLocation.getFileContainerId()));
       	
       	   // Update the archive location file-id metadata.
       	   addMetadataEntry(metadataEntries,
       		   	            toMetadataEntry(ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE, 
       		   	                            archiveLocation.getFileId()));
       	}
       	
       	if(dataTransferRequestId != null) {
       	   // Update the Data Transfer Request ID metadata.
       	   addMetadataEntry(metadataEntries,
       		                toMetadataEntry(DATA_TRANSFER_REQUEST_ID_ATTRIBUTE, 
       			                            dataTransferRequestId));
       	}
       	
       	if(dataTransferStatus != null) {
       	   // Update the Data Transfer Status metadata.
       	   addMetadataEntry(metadataEntries,
       			            toMetadataEntry(DATA_TRANSFER_STATUS_ATTRIBUTE, 
       		   	                            dataTransferStatus.value()));
       	}
       	
       	if(dataTransferType != null) {
       	   // Update the Data Transfer Type metadata.
       	   addMetadataEntry(metadataEntries,
       		                toMetadataEntry(DATA_TRANSFER_TYPE_ATTRIBUTE, 
       		  	                            dataTransferType.value()));
       	}
       	
       	if(metadataOrigin != null) {
           // Update the Metadata Origin.
       	   addMetadataEntry(metadataEntries,	
       		                generateMetadataOriginMetadata(metadataOrigin));
       	}
       	
       	if(!metadataEntries.isEmpty()) {
		   dataManagementProxy.updateDataObjectMetadata(getAuthenticatedToken(),
		                                                path, metadataEntries);
       	}
	}
    
    @Override
    public HpcSystemGeneratedMetadata 
              getDataObjectSystemGeneratedMetadata(String path) throws HpcException
	{
    	// Input validation.
    	if(getDataObject(path) == null) {
           throw new HpcException("Data object not found: " + path, 
		                          HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	return toSystemGeneratedMetadata(getDataObjectMetadata(path));
	}
    
    @Override
    public HpcSystemGeneratedMetadata 
              getCollectionSystemGeneratedMetadata(String path) throws HpcException
	{
    	// Input validation.
    	if(getCollection(path) == null) {
           throw new HpcException("Collection not found: " + path, 
		                          HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	return toSystemGeneratedMetadata(getCollectionMetadata(path));
	}
    
    @Override
    public void updateCollectionSystemGeneratedMetadata(String path, 
                                                        HpcMetadataOrigin metadataOrigin) 
                                                        throws HpcException
	{
       	// Input validation.
       	if(path == null) {
       	   throw new HpcException("Invalid updated system generated metadata for collectiont", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
       	
       	if(metadataOrigin != null) {
           // Update the Metadata Origin.
       	   addMetadataEntry(metadataEntries,
       	                    generateMetadataOriginMetadata(metadataOrigin));
       	}
       	
       	if(!metadataEntries.isEmpty()) {
		   dataManagementProxy.updateCollectionMetadata(getAuthenticatedToken(),
		                                                path, metadataEntries);
       	}
	}
    
    @Override
    public HpcSystemGeneratedMetadata 
              toSystemGeneratedMetadata(List<HpcMetadataEntry> systemGeneratedMetadataEntries) 
            	                       throws HpcException
	{
    	// Extract the system generated data-object metadata entries from the entire set.
    	Map<String, String> metadataMap = toMap(systemGeneratedMetadataEntries);
    	HpcSystemGeneratedMetadata systemGeneratedMetadata = new HpcSystemGeneratedMetadata();
    	systemGeneratedMetadata.setObjectId(metadataMap.get(ID_ATTRIBUTE));
    	systemGeneratedMetadata.setRegistrarId(metadataMap.get(REGISTRAR_ID_ATTRIBUTE));
    	systemGeneratedMetadata.setRegistrarName(metadataMap.get(REGISTRAR_NAME_ATTRIBUTE));
    	systemGeneratedMetadata.setRegistrarDOC(metadataMap.get(REGISTRAR_DOC_ATTRIBUTE));
    	
    	HpcFileLocation archiveLocation = new HpcFileLocation();
    	archiveLocation.setFileContainerId(metadataMap.get(ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE));
    	archiveLocation.setFileId(metadataMap.get(ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE));
    	if(archiveLocation.getFileContainerId() != null || archiveLocation.getFileId() != null) {
    		systemGeneratedMetadata.setArchiveLocation(archiveLocation);
    	}
    	
    	HpcFileLocation sourceLocation = new HpcFileLocation();
    	sourceLocation.setFileContainerId(metadataMap.get(SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE));
    	sourceLocation.setFileId(metadataMap.get(SOURCE_LOCATION_FILE_ID_ATTRIBUTE));
    	if(sourceLocation.getFileContainerId() != null || sourceLocation.getFileId() != null) {
    		systemGeneratedMetadata.setSourceLocation(sourceLocation);
    	}
    	
    	systemGeneratedMetadata.setDataTransferRequestId(metadataMap.get(DATA_TRANSFER_REQUEST_ID_ATTRIBUTE));
    	if(metadataMap.get(DATA_TRANSFER_STATUS_ATTRIBUTE) != null) {
    	   try {
    		    systemGeneratedMetadata.setDataTransferStatus(
    			      HpcDataTransferUploadStatus.fromValue(
    						               metadataMap.get(DATA_TRANSFER_STATUS_ATTRIBUTE)));
    		     
    	   } catch(Exception e) {
    			   logger.error("Unable to determine data transfer status: "+ 
    		                    metadataMap.get(DATA_TRANSFER_STATUS_ATTRIBUTE), e);
    			   systemGeneratedMetadata.setDataTransferStatus(HpcDataTransferUploadStatus.UNKNOWN);
    		}
    	}
    	
    	if(metadataMap.get(DATA_TRANSFER_TYPE_ATTRIBUTE) != null) {
    	   try {
    			systemGeneratedMetadata.setDataTransferType(
    				  HpcDataTransferType.fromValue(metadataMap.get(DATA_TRANSFER_TYPE_ATTRIBUTE)));
    			
    		} catch(Exception e) {
    			    logger.error("Unable to determine data transfer type: "+ 
    		                     metadataMap.get(DATA_TRANSFER_TYPE_ATTRIBUTE), e);
    			    systemGeneratedMetadata.setDataTransferType(null);
    		}
    	}
    	
    	systemGeneratedMetadata.setSourceSize(
    		  metadataMap.get(SOURCE_FILE_SIZE_ATTRIBUTE) != null ?
    		  Long.valueOf(metadataMap.get(SOURCE_FILE_SIZE_ATTRIBUTE)) : null);
    	systemGeneratedMetadata.setCallerObjectId(
      		  metadataMap.get(CALLER_OBJECT_ID_ATTRIBUTE));
    	systemGeneratedMetadata.setMetadataOrigin(fromJSON(
    		  metadataMap.get(METADATA_ORIGIN_ATTRIBUTE)));
    		  
		return systemGeneratedMetadata;
	}
    
    @Override
    public HpcMetadataOrigin addParentMetadata(String path) throws HpcException
    {
    	// Only applicable for hierarchical metadata replication policy.
        if(hierarchicalMetadataPolicy.equals(METADATA_REPLICATION_POLICY)) {
    	   return replicateParentMetadata(path);
    	}

    	return null;
    }
    
    @Override
    public void updateMetadataTree(String path) throws HpcException
    {
    	// Only applicable for hierarchical metadata replication policy.
    	if(!hierarchicalMetadataPolicy.equals(METADATA_REPLICATION_POLICY)) {
    	   return;
    	}
    	
    	// Get the path attributes to determine if this is a collection or data object.
    	Object authenticatedToken = getAuthenticatedToken();
    	HpcPathAttributes pathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken, path);
    
    	// Update parent metadata for this path.
    	HpcMetadataOrigin metadataOrigin = updateParentMetadata(authenticatedToken, path, pathAttributes);
    	if(pathAttributes.getIsDirectory()) {
    	   updateCollectionSystemGeneratedMetadata(path, metadataOrigin);
    	   if(pathAttributes.getFiles() != null) {
    	      for(String childFile : pathAttributes.getFiles()) {
    		      updateMetadataTree(path + "/" + childFile);
    	      }
    	   }
    	} else if(pathAttributes.getIsFile()) {
    		      updateDataObjectSystemGeneratedMetadata(path, null, null, null, null, metadataOrigin);
    	}
    }
    
    @Override
    public HpcMetadataOrigin updateMetadataOrigin(HpcMetadataOrigin metadataOrigin, 
    		                                      List<HpcMetadataEntry> metadataEntries) 
    {
    	// Only applicable for hierarchical metadata replication policy.
    	if(!hierarchicalMetadataPolicy.equals(METADATA_REPLICATION_POLICY)) {
    	   return null;
    	}
    	
    	Set<String> metadataAttributesSet = new HashSet<>();
    	metadataAttributesSet.addAll(metadataOrigin.getMetadataAttributes());
    	
    	Set<String> parentMetadataAttributesSet = new HashSet<>();
    	parentMetadataAttributesSet.addAll(metadataOrigin.getParentMetadataAttributes());
    	
    	// Update the origin for the entries on the list.
    	for(HpcMetadataEntry metadataEntry : metadataEntries) {
    	    metadataAttributesSet.add(metadataEntry.getAttribute());
    		parentMetadataAttributesSet.remove(metadataEntry.getAttribute());
    	}

    	// Return an updated metadata origin object.
    	HpcMetadataOrigin updatedMetadataOrigin = new HpcMetadataOrigin();
    	updatedMetadataOrigin.getMetadataAttributes().addAll(metadataAttributesSet);
    	updatedMetadataOrigin.getParentMetadataAttributes().addAll(parentMetadataAttributesSet);
    	return updatedMetadataOrigin;
    }
    
    @Override
    public HpcCollection getCollection(String path) throws HpcException
    {
    	Object authenticatedToken = getAuthenticatedToken();
    	if(dataManagementProxy.getPathAttributes(authenticatedToken, path).getIsDirectory()) {
    	   return dataManagementProxy.getCollection(authenticatedToken, path);
    	}
    	
    	return null;
    }
    
    @Override
    public List<HpcCollection> getCollections(
    		    List<HpcMetadataQuery> metadataQueries) throws HpcException
    {
       	if(!isValidMetadataQueries(metadataQueries) || metadataQueries.isEmpty()) {
           throw new HpcException("Invalid or empty metadata queries", 
        			              HpcErrorType.INVALID_REQUEST_INPUT);
        }
       	
       	if(hierarchicalMetadataPolicy.equals(METADATA_VIEWS_POLICY)) {
       	   // Use the hierarchical metadata views to perform the search.
       	   return getCollectionsByIds(metadataDAO.getCollectionIds(metadataQueries));
       		
       	} else {
    	        return dataManagementProxy.getCollections(getAuthenticatedToken(),
    			                                          metadataQueries);
       	}
    }
    
    @Override
    public List<HpcMetadataEntry> getCollectionMetadata(String path) throws HpcException
    {
       	// Input validation.
       	if(path == null) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
    	return dataManagementProxy.getCollectionMetadata(getAuthenticatedToken(),
                                                         path);
    }
    
    @Override
    public HpcDataObject getDataObject(String path) throws HpcException
    {
    	Object authenticatedToken = getAuthenticatedToken();
    	if(dataManagementProxy.getPathAttributes(authenticatedToken, path).getIsFile()) {
    	   return dataManagementProxy.getDataObject(authenticatedToken, path);
    	}
    	
    	return null;
    }
    
    @Override
    public List<HpcDataObject> getDataObjects(
    		    List<HpcMetadataQuery> metadataQueries) throws HpcException
    {
       	if(!isValidMetadataQueries(metadataQueries) || metadataQueries.isEmpty()) {
           throw new HpcException("Invalid or empty metadata queries", 
        			              HpcErrorType.INVALID_REQUEST_INPUT);
        }
       	
    	return dataManagementProxy.getDataObjects(getAuthenticatedToken(),
    			                                  metadataQueries);
    }
    
    @Override
    public List<HpcDataObject> getDataObjectsInProgress() throws HpcException
    {
    	List<HpcDataObject> objectsInProgress = new ArrayList<>();
    	objectsInProgress.addAll(
    		   getDataObjects(dataTransferInProgressToArchiveQuery));
    	objectsInProgress.addAll(
    		   getDataObjects(dataTransferInProgressToTemporaryArchiveQuery));
    	
    	return objectsInProgress;
    }
    
    @Override
    public List<HpcDataObject> getDataObjectsInTemporaryArchive() throws HpcException
    {
    	return getDataObjects(dataTransferInTemporaryArchiveQuery);
    }
    
    @Override
    public List<HpcMetadataEntry> getDataObjectMetadata(String path) throws HpcException
    {
       	// Input validation.
       	if(path == null) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
    	return dataManagementProxy.getDataObjectMetadata(getAuthenticatedToken(),
                                                         path);
    }
    
    @Override
    public HpcDataManagementAccount getHpcDataManagementAccount(Object irodsAccount) throws HpcException
    {
   		return dataManagementProxy.getHpcDataManagementAccount(irodsAccount);
    }
    
    @Override
    public Object getProxyManagementAccount(HpcDataManagementAccount irodsAccount) throws HpcException
    {
   		return dataManagementProxy.getProxyManagementAccount(irodsAccount);
    }  
    
    @Override
    public HpcUserRole getUserRole(String username) throws HpcException
    {
    	return dataManagementProxy.getUserRole(getAuthenticatedToken(), username);
    }
    
    @Override
    public void addUser(HpcNciAccount nciAccount, HpcUserRole userRole) throws HpcException
    {
    	// Input validation.
    	if(!isValidNciAccount(nciAccount)) {	
    	   throw new HpcException("Invalid NCI Account: Null user ID or name or DOC", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
       	
    	dataManagementProxy.addUser(getAuthenticatedToken(), 
    			                    nciAccount, userRole);
    }
     
    @Override
    public void updateUser(String nciUserId, String firstName, String lastName,
                           HpcUserRole userRole) throws HpcException
    {
    	// Input validation.
    	if(nciUserId == null || firstName == null || lastName == null ||
    	   userRole == null) {	
    	   throw new HpcException("Invalid update user input", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	dataManagementProxy.updateUser(getAuthenticatedToken(), nciUserId,
    			                       firstName, lastName, userRole);
    }    
    
    @Override
    public void deleteUser(String nciUserId) throws HpcException
    {
    	// Input validation.
    	if(nciUserId == null) {	
    	   throw new HpcException("Invalid NCI user ID", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
       	
    	dataManagementProxy.deleteUser(getAuthenticatedToken(), nciUserId);
    }

    @Override
    public HpcGroupResponse setGroup(HpcGroup hpcGroup, List<String> addUserIds, List<String> removeUserIds) throws HpcException
    {
    	// Input validation.
    	if(hpcGroup == null) {	
    	   throw new HpcException("Null group name", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
       	
    	return dataManagementProxy.addGroup(getAuthenticatedToken(), hpcGroup, addUserIds, removeUserIds);
    }
    
    @Override
    public void closeConnection()
    {
    	try {
    	     dataManagementProxy.disconnect(getAuthenticatedToken());
    	     
    	} catch(HpcException e) {
    		    // Ignore.
    		    logger.error("Failed to close data management connection", e);
    	}
    }
    
    @Override
    public HpcPathAttributes setPermission(String path,
    		                               HpcEntityPermission permissionRequest) 
                                          throws HpcException
    {
    	// Input validation.
    	if(path == null || permissionRequest == null) {
           throw new HpcException("Null path or permission request", 
		                          HpcErrorType.INVALID_REQUEST_INPUT);    	   	
    	}
    	
    	// Get the path attributes.
    	Object authenticatedToken = getAuthenticatedToken();
    	HpcPathAttributes pathAttributes = 
    		              dataManagementProxy.getPathAttributes(authenticatedToken, 
    				                                            path);
    	
    	// Set collection or data-object permission.
    	if(pathAttributes.getIsDirectory()) {
    	   dataManagementProxy.setCollectionPermission(authenticatedToken, 
    			                                       path, 
    			                                       permissionRequest);
    	} else if(pathAttributes.getIsFile()) {
    		      dataManagementProxy.setDataObjectPermission(authenticatedToken, 
    		    		                                      path, 
                                                              permissionRequest);
    	} else {
    		    throw new HpcException("Entity path doesn't exist", 
                                       HpcErrorType.INVALID_REQUEST_INPUT);   
    	}
    	
    	return pathAttributes;
    }    
    
    @Override
    public HpcPathAttributes assignSystemAccountPermission(String path) 
                                                          throws HpcException
    {
    	HpcIntegratedSystemAccount dataManagementAccount = 
    	    	     systemAccountLocator.getSystemAccount(HpcIntegratedSystem.IRODS);
    	if(dataManagementAccount == null) {
    	   throw new HpcException("System Data Management Account not configured",
    	      	                  HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
    	HpcUserPermission permissionRequest = new HpcUserPermission();
    	permissionRequest.setPermission(OWN_PERMISSION);
    	permissionRequest.setUserId(dataManagementAccount.getUsername());
    	
    	return setPermission(path, permissionRequest);
    }
    
    @Override
    public Map<String, String> toMap(List<HpcMetadataEntry> metadataEntries)
    {
    	Map<String, String> metadataMap = new HashMap<>();
    	for(HpcMetadataEntry metadataEntry : metadataEntries) {
    		metadataMap.put(metadataEntry.getAttribute(), metadataEntry.getValue());
    	}
    	
    	return metadataMap;
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get the data management authenticated token from the request context.
     * If it's not in the context, get a token by authenticating.
     *
     * @throws HpcException If it failed to obtain an authentication token.
     */
    private Object getAuthenticatedToken() throws HpcException
    {
    	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    	if(invoker == null) {
	       throw new HpcException("Unknown user",
			                      HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);
    	}
    	
    	if(invoker.getDataManagementAuthenticatedToken() != null) {
    	   return invoker.getDataManagementAuthenticatedToken();
    	}
    	
    	// No authenticated token found in the request token. Authenticate the invoker.
    	if(invoker.getDataManagementAccount() == null) {
    		throw new HpcException("Unknown data management account",
                                   HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);
    	}
    	
    	// Authenticate w/ data management
    	Object token = dataManagementProxy.authenticate(invoker.getDataManagementAccount(),
    			                                        invoker.getLdapAuthenticated());
    	if(token == null) {
    	   throw new HpcException("Invalid data management account credentials",
                                  HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);
    	}
    	
    	// Store token on the request context.
    	invoker.setDataManagementAuthenticatedToken(token);
    	return token;
    }
    
    /**
     * Generate ID Metadata.
     *
     * @return The Generated ID metadata. 
     */
    private HpcMetadataEntry generateIdMetadata()
    {
    	return toMetadataEntry(ID_ATTRIBUTE, keyGenerator.generateKey());
    }
    
    /**
     * Generate registrar ID, name and DOC metadata.
     * 
     * @return a List of the 3 metadata.
     * @throws HpcException if the service invoker is unknown.
     */
    private List<HpcMetadataEntry> generateRegistrarMetadata() throws HpcException
    {
       	// Get the service invoker.
       	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
       	if(invoker == null) {
       	   throw new HpcException("Unknown service invoker", 
		                          HpcErrorType.UNEXPECTED_ERROR);
       	}	
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
       	
       	// Create the registrar user-id metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(REGISTRAR_ID_ATTRIBUTE, 
       			                         invoker.getNciAccount().getUserId()));
       	
       	// Create the registrar name metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(REGISTRAR_NAME_ATTRIBUTE, 
       			                         invoker.getNciAccount().getFirstName() + " " +
                                         invoker.getNciAccount().getLastName()));
       	
       	// Create the registrar DOC.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(REGISTRAR_DOC_ATTRIBUTE, 
       			                         invoker.getNciAccount().getDOC()));
       	
       	return metadataEntries;
    }
    
    /** 
     * Generate 'Metadata Origin' Metadata
     * 
     * @param metadataOrigin the metadata origin object
     * @return A JSON representation of object.
     */
	@SuppressWarnings("unchecked")
	private HpcMetadataEntry generateMetadataOriginMetadata(HpcMetadataOrigin metadataOrigin)
	{

		// Generate a JSON string from the metadataOrigin object.
		JSONObject jsonMetadataOrigin = new JSONObject();
		if(metadataOrigin != null) {
		   JSONArray jsonMetadataAttributes = new JSONArray();
		   if(metadataOrigin.getMetadataAttributes() != null) {
			  jsonMetadataAttributes.addAll(metadataOrigin.getMetadataAttributes());
		   }
		   JSONArray jsonParentMetadataAttributes = new JSONArray();
		   if(metadataOrigin.getParentMetadataAttributes() != null) {
			  jsonParentMetadataAttributes.addAll(metadataOrigin.getParentMetadataAttributes());
		   }
		   
		   jsonMetadataOrigin.put(JSON_METADATA_ATTRIBUTES, jsonMetadataAttributes);
		   jsonMetadataOrigin.put(JSON_PARENT_METADATA_ATTRIBUTES, jsonParentMetadataAttributes);
		}
		
		return toMetadataEntry(METADATA_ORIGIN_ATTRIBUTE, metadataOrigin != null ? 
				                                          jsonMetadataOrigin.toJSONString() : null);
	}
    
    /**
     * Generate a metadata entry from attribute/value pair.
     * 
     * @param attribute The metadata entry attribute.
     * @param value The metadata entry value.
     * @return HpcMetadataEntry instance
     */
    private HpcMetadataEntry toMetadataEntry(String attribute, String value)
    {
    	HpcMetadataEntry entry = new HpcMetadataEntry();
    	entry.setAttribute(attribute);
	    entry.setValue(value);
	    entry.setUnit("");
	    return entry;
    }
    
    /**
     * Generate a metadata entry from attribute/value pair.
     * 
     * @param attribute The metadata entry attribute.
     * @param value The metadata entry value.
     * @return HpcMetadataEntry instance
     */
    private HpcMetadataEntry toMetadataEntry(String attribute, Long value)
    {
    	return toMetadataEntry(attribute, value != null ? String.valueOf(value) : null);
    }
    
    /**
     * Add a metadata entry to a list. 
     * 
     * @param metadataEntries list of metadata entries. 
     * @param entry A metadata entry.
     */
    private void addMetadataEntry(List<HpcMetadataEntry> metadataEntries,
    		                      HpcMetadataEntry entry)
    {
    	if(entry.getAttribute() != null && entry.getValue() != null) {
    	   metadataEntries.add(entry);
    	}
    }
    
    /**
     * Generate a metadata query.
     * 
     * @param attribute The metadata entry attribute.
     * @param operator The query operator.
     * @param value The metadata entry value.
     * @return HpcMetadataEntry instance
     */
    private HpcMetadataQuery toMetadataQuery(String attribute, String operator, String value)
    {
		HpcMetadataQuery query = new HpcMetadataQuery();
		query.setAttribute(attribute);
	    query.setOperator(operator);
	    query.setValue(value);
	    
	    return query;
    }
    
    /** 
     * Convert JSON string to HpcMetadataOrigin object.
     * 
     * @param jsonMetadataOriginStr The metadata origin JSON String.
     * @return HpcMetadataOrigin
     */
	@SuppressWarnings("unchecked")
	private HpcMetadataOrigin fromJSON(String jsonMetadataOriginStr)
	{
		HpcMetadataOrigin metadataOrigin = new HpcMetadataOrigin();
		if(jsonMetadataOriginStr == null || jsonMetadataOriginStr.isEmpty()) {
		   return metadataOrigin;
		}
		
		// Parse the JSON string.
		JSONObject jsonMetadataOrigin = null;
		try {
			 jsonMetadataOrigin = (JSONObject) (new JSONParser().parse(jsonMetadataOriginStr));
			 
		} catch(ParseException e) {
			    return metadataOrigin;
		}
		
		JSONArray jsonMetadataAttributes = 
				  (JSONArray) jsonMetadataOrigin.get(JSON_METADATA_ATTRIBUTES);
		if(jsonMetadataAttributes != null) {
			metadataOrigin.getMetadataAttributes().addAll(jsonMetadataAttributes);	
		}
		JSONArray jsonParentMetadataAttributes = 
				  (JSONArray) jsonMetadataOrigin.get(JSON_PARENT_METADATA_ATTRIBUTES);
		if(jsonParentMetadataAttributes != null) {
			metadataOrigin.getParentMetadataAttributes().addAll(jsonParentMetadataAttributes);	
		}

		return metadataOrigin;
	}	
	
    /**
     * Update parent metadata to a either a collection or a data object.
     *
     * @param authenticatedToken DM authenticated token
     * @param path The collection or data object path.
     * @param pathAttributes The data object or collection path attributes.
     * @return HpcMetadataOrigin An object listing the origin of the collection / data object
     *                           metadata after the change.
     * @throws HpcException
     */
    private HpcMetadataOrigin updateParentMetadata(Object authenticatedToken, String path,
    		                                       HpcPathAttributes pathAttributes) throws HpcException
    {
    	// Get the current metadata origin object.
       	HpcMetadataOrigin metadataOrigin;
    	if(pathAttributes.getIsFile()) {
    	   metadataOrigin = getDataObjectSystemGeneratedMetadata(path).getMetadataOrigin();
    	} else if(pathAttributes.getIsDirectory()) {
    		      metadataOrigin = getCollectionSystemGeneratedMetadata(path).getMetadataOrigin();
    	} else {
    		    return null;
    	}
    	
    	Set<String> metadataAttributesSet = new HashSet<>();
    	metadataAttributesSet.addAll(metadataOrigin.getMetadataAttributes());
    	
    	Set<String> parentMetadataAttributesSet = new HashSet<>();
    	parentMetadataAttributesSet.addAll(metadataOrigin.getParentMetadataAttributes());
       	
       	// Get the parent metadata.
        List<HpcMetadataEntry> parentMetadataEntries = 
        	dataManagementProxy.getParentPathMetadata(authenticatedToken, path);
        
        // Get the set of system generated metadata attributes.
        Set<String> systemGeneratedMetadataAttributes = 
        	        metadataValidator.getSystemGeneratedMetadataAttributes();
        
        // Prepare a list a metadata entries from the parent to be updated. 
        // Note: only parent entries that don't already exist as child entries are updated. 
        //       Also - system generated metadata are not replicated.
        List<HpcMetadataEntry> updateMetadataEntries = new ArrayList<>();
       	for(HpcMetadataEntry parentMetadataEntry : parentMetadataEntries) {
			if(!metadataAttributesSet.contains(parentMetadataEntry.getAttribute()) &&
			   !systemGeneratedMetadataAttributes.contains(parentMetadataEntry.getAttribute())) {
			   if(parentMetadataEntry.getUnit() == null) {
				  parentMetadataEntry.setUnit("");
			   }
			   
			   updateMetadataEntries.add(parentMetadataEntry);
			   parentMetadataAttributesSet.add(parentMetadataEntry.getAttribute());
			}
       	}
		  
        // Update Parent Metadata to the data object or collection. 
       	if(!updateMetadataEntries.isEmpty()) {
    	   if(pathAttributes.getIsFile()) {
       	      dataManagementProxy.updateDataObjectMetadata(authenticatedToken, 
       		     	                                       path, updateMetadataEntries);
    	   } else {
    		       dataManagementProxy.updateCollectionMetadata(authenticatedToken, 
                                                                path, updateMetadataEntries);
    	   }
       	}
       	
    	// Return an updated metadata origin object.
    	HpcMetadataOrigin updatedMetadataOrigin = new HpcMetadataOrigin();
    	updatedMetadataOrigin.getMetadataAttributes().addAll(metadataAttributesSet);
    	updatedMetadataOrigin.getParentMetadataAttributes().addAll(parentMetadataAttributesSet);
    	return updatedMetadataOrigin;
    }
    
    /**
     * Replicate parent metadata to a either a collection or a data object.
     *
     * @param path The collection or data object path.
     * @return HpcMetadataOrigin An object listing the origin of the collection / data object
     *                           metadata after the change.
     * @throws HpcException
     */
    private HpcMetadataOrigin replicateParentMetadata(String path) throws HpcException
    {
    	// Get the path attributes to determine if this is a collection or data object.
    	Object authenticatedToken = getAuthenticatedToken();
       	HpcMetadataOrigin metadataOrigin = new HpcMetadataOrigin();
    	HpcPathAttributes pathAttributes = dataManagementProxy.getPathAttributes(authenticatedToken, path);
    	
    	// Get the existing metadata on the collection or data object.
    	List<HpcMetadataEntry> metadataEntries = null;
    	if(pathAttributes.getIsFile()) {
    	   metadataEntries = getDataObjectMetadata(path);	
    	} else if(pathAttributes.getIsDirectory()) {
    		      metadataEntries = getCollectionMetadata(path);
    	} else {
    		    return null;
    	}
    	
    	// Create a metadata attribute set of the collection or data object.
    	Set<String> metadataAttributeSet = new HashSet<>();
    	if(metadataEntries != null) {
    	   for(HpcMetadataEntry metadataEntry : metadataEntries) {
    		   metadataAttributeSet.add(metadataEntry.getAttribute());
    		   metadataOrigin.getMetadataAttributes().add(metadataEntry.getAttribute());
    	   }
    	}
       	
       	// Get the parent metadata.
        List<HpcMetadataEntry> parentMetadataEntries = 
        	dataManagementProxy.getParentPathMetadata(authenticatedToken, path);
        
        // Get the set of system generated metadata attributes.
        Set<String> systemGeneratedMetadataAttributes = 
        	        metadataValidator.getSystemGeneratedMetadataAttributes();
        
        // Prepare a list a metadata entries from the parent to be added. 
        // Note: only parent entries that don't already exist as child entries are added. 
        //       Also - system generated metadata are not replicated.
        List<HpcMetadataEntry> addMetadataEntries = new ArrayList<>();
       	for(HpcMetadataEntry parentMetadataEntry : parentMetadataEntries) {
			if(!metadataAttributeSet.contains(parentMetadataEntry.getAttribute()) &&
			   !systemGeneratedMetadataAttributes.contains(parentMetadataEntry.getAttribute())) {
			   if(parentMetadataEntry.getUnit() == null) {
				  parentMetadataEntry.setUnit("");
			   }
			   
			   addMetadataEntries.add(parentMetadataEntry);
			   metadataAttributeSet.add(parentMetadataEntry.getAttribute());
			   metadataOrigin.getParentMetadataAttributes().add(parentMetadataEntry.getAttribute());
			}
       	}
		  
        // Add Parent Metadata to the data object or collection. 
       	if(!addMetadataEntries.isEmpty()) {
    	   if(pathAttributes.getIsFile()) {
       	      dataManagementProxy.addMetadataToDataObject(authenticatedToken, 
       		     	                                      path, addMetadataEntries);
    	   } else {
    		       dataManagementProxy.addMetadataToCollection(authenticatedToken, 
                                                               path, addMetadataEntries);
    	   }
       	}
       	
       	return metadataOrigin;
    }
    
    /**
     * Get collections by IDs.
     *
     * @param ids The list of collection IDs.
     * @return List<HpcCollection>
     * @throws HpcException
     */
    private List<HpcCollection> getCollectionsByIds(List<Integer> ids) throws HpcException
    {
    	Object authenticatedToken = getAuthenticatedToken();
    	List<HpcCollection> collections = new ArrayList<>();
    	for(int id : ids) {
    		collections.add(dataManagementProxy.getCollection(authenticatedToken, id));
    	}
    	
    	return collections;
    }
}