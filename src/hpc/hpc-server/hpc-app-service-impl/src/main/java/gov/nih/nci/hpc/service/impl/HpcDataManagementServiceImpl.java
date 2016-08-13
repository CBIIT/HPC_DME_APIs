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

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataEntries;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataQueries;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNciAccount;
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
import gov.nih.nci.hpc.domain.model.HpcDataObjectSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.model.HpcGroup;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
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
	
    // System generated metadata attributes.
	private static final String ID_ATTRIBUTE = "uuid";
	private static final String REGISTRAR_ID_ATTRIBUTE = "registered_by";
	private static final String REGISTRAR_NAME_ATTRIBUTE = "registered_by_name";
	private static final String REGISTRAR_DOC_ATTRIBUTE = "registered_by_doc";
	private static final String SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE = 
                                "source_file_container_id"; 
	private static final String SOURCE_LOCATION_FILE_ID_ATTRIBUTE = 
                                "source_file_id"; 
	private static final String ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE = 
			                    "archive_file_container_id"; 
	private static final String ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE = 
			                    "archive_file_id"; 
	private static final String DATA_TRANSFER_REQUEST_ID_ATTRIBUTE = 
                                "data_transfer_request_id";
	private static final String DATA_TRANSFER_STATUS_ATTRIBUTE = 
                                "data_transfer_status";
	private static final String DATA_TRANSFER_TYPE_ATTRIBUTE = 
                                "data_transfer_type";
	private static final String SOURCE_FILE_SIZE_ATTRIBUTE = 
                                "source_file_size";
	private static final String CALLER_OBJECT_ID_ATTRIBUTE = 
                                "archive_caller_object_id";
	
	private static final String EQUAL_OPERATOR = "EQUAL";
	private static final String INVALID_PATH_METADATA_MSG = 
			                    "Invalid path or metadata entry";
	
	// Data Management OWN permission.
	private static final String OWN_PERMISSION = "OWN"; 
	
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
	
	// Prepared query to get data objects that have their data transfer in-progress to archive.
	private List<HpcMetadataQuery> dataTransferInProgressToArchiveQuery = new ArrayList<>();
	
	// Prepared query to get data objects that have their data transfer in-progress to temporary archive.
	private List<HpcMetadataQuery> dataTransferInProgressToTemporaryArchiveQuery = new ArrayList<>();
	
	// Prepared query to get data objects that have their data in temporary archive.
	private List<HpcMetadataQuery> dataTransferInTemporaryArchiveQuery = new ArrayList<>();
	
	// Policy to replicate parent metadata of data objects and collections 
	// (when registering and updating metadata).
	private boolean replicateParentMetadata = false;

    // The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param replicateParentMetadata Policy to replicate parent metadata of data objects
     *                                and collections (when registering and updating metadata)
     */
    private HpcDataManagementServiceImpl(boolean replicateParentMetadata)
    {
    	this.replicateParentMetadata = replicateParentMetadata;
    	
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
    public void addSystemGeneratedMetadataToCollection(String path) 
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
    public HpcMetadataOrigin addParentMetadata(String path) throws HpcException
    {
    	// Check the parent metadata replication policy.
    	if(!replicateParentMetadata) {
    	   return null;
    	}
    	
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
        
        // Prepare a list a metadata entries from the parent to be added. 
        // Note: only parent entries that don't already exist as child entries are added. 
        List<HpcMetadataEntry> addMetadataEntries = new ArrayList<>();
       	for(HpcMetadataEntry parentMetadataEntry : parentMetadataEntries) {
			if(!metadataAttributeSet.contains(parentMetadataEntry.getAttribute())) {
			   if(parentMetadataEntry.getUnit() == null) {
				  parentMetadataEntry.setUnit("");
			   }
			   
			   addMetadataEntries.add(parentMetadataEntry);
			   metadataAttributeSet.add(parentMetadataEntry.getAttribute());
			   metadataOrigin.getParentMetadataAttributes().add(parentMetadataEntry.getAttribute());
			}
       	}
		  
       	// Add Parent Metadata to the data object or collection
    	if(pathAttributes.getIsFile()) {
       	   dataManagementProxy.addMetadataToDataObject(authenticatedToken, 
       		   	                                       path, addMetadataEntries);
    	} else {
    		    dataManagementProxy.addMetadataToCollection(authenticatedToken, 
                                                            path, addMetadataEntries);
    	}
       	
       	return metadataOrigin;
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
    		                                           Long sourceSize, String callerObjectId) 
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
       	
        // Create the Source File Size metadata.
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
                                                        HpcDataTransferType dataTransferType) 
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
       	
		dataManagementProxy.updateDataObjectMetadata(getAuthenticatedToken(),
		                                             path, metadataEntries);
	}
    
    @Override
    public HpcDataObjectSystemGeneratedMetadata 
              getDataObjectSystemGeneratedMetadata(String path) throws HpcException
	{
    	// Input validation.
    	if(getDataObject(path) == null) {
           throw new HpcException("Data object not found: " + path, 
		                          HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	return getDataObjectSystemGeneratedMetadata(getDataObjectMetadata(path));
	}
    
    @Override
    public HpcDataObjectSystemGeneratedMetadata 
              getDataObjectSystemGeneratedMetadata(List<HpcMetadataEntry> dataObjectMetadata) 
            		                              throws HpcException
	{
    	// Extract the system generated data-object metadata entries from the entire set.
    	Map<String, String> metadataMap = toMap(dataObjectMetadata);
    	HpcDataObjectSystemGeneratedMetadata systemGeneratedMetadata = new HpcDataObjectSystemGeneratedMetadata();
    	systemGeneratedMetadata.setDataObjectId(metadataMap.get(ID_ATTRIBUTE));
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
    		  
		return systemGeneratedMetadata;
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
       	
    	return dataManagementProxy.getCollections(getAuthenticatedToken(),
    			                                  metadataQueries);
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
}