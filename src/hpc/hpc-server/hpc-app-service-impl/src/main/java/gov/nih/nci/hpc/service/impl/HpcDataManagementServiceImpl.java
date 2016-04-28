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
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcDataObjectSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataManagementService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final static String ID_ATTRIBUTE = "uuid";
	private final static String REGISTRAR_ID_ATTRIBUTE = "registered_by";
	private final static String REGISTRAR_NAME_ATTRIBUTE = "registered_by_name";
	private final static String REGISTRAR_DOC_ATTRIBUTE = "registered_by_doc";
	private final static String SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE = 
                                "source_file_container_id"; 
	private final static String SOURCE_LOCATION_FILE_ID_ATTRIBUTE = 
                                "source_file_id"; 
	private final static String ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE = 
			                    "archive_file_container_id"; 
	private final static String ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE = 
			                    "archive_file_id"; 
	private final static String DATA_TRANSFER_REQUEST_ID_ATTRIBUTE = 
                                "data_transfer_request_id";
	private final static String DATA_TRANSFER_STATUS_ATTRIBUTE = 
                                "data_transfer_status";
	private final static String DATA_TRANSFER_TYPE_ATTRIBUTE = 
                                "data_transfer_type";
	private final static String SOURCE_FILE_SIZE_ATTRIBUTE = 
                                "source_file_size";
	
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
	
	// Prepared query to get data objects that have their data transfer in-progress.
	private List<HpcMetadataQuery> dataTransferInProgressQuery = 
			                       new ArrayList<HpcMetadataQuery>();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     */
    private HpcDataManagementServiceImpl()
    {
    	// Populate the query to get data objects in data transfer in-progress state.
    	HpcMetadataQuery query = new HpcMetadataQuery();
    	query.setAttribute(DATA_TRANSFER_STATUS_ATTRIBUTE);
        query.setOperator("EQUAL");
        query.setValue(HpcDataTransferStatus.IN_PROGRESS.value());
        dataTransferInProgressQuery.add(query);
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
       	   throw new HpcException("Null path or Invalid metadata entry", 
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
       	   throw new HpcException("Null path", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<HpcMetadataEntry>();
       	
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
       	   throw new HpcException("Null path or Invalid metadata entry", 
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
       	   throw new HpcException("Null path or Invalid metadata entry", 
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
       	   throw new HpcException("Null path or Invalid metadata entry", 
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
    		                                           HpcDataTransferStatus dataTransferStatus,
    		                                           HpcDataTransferType dataTransferType,
    		                                           Long sourceSize) 
                                                      throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidFileLocation(archiveLocation) ||
       	   (sourceLocation != null && !isValidFileLocation(sourceLocation)) || 
       	   dataTransferStatus == null || dataTransferType == null) {
       	   throw new HpcException("Invalid system generated metadata for data object", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<HpcMetadataEntry>();
       	
       	// Generate a data-object ID and add it as metadata.
       	metadataEntries.add(generateIdMetadata());
       	
       	// Create and add the registrar ID, name and DOC metadata.
       	metadataEntries.addAll(generateRegistrarMetadata());
       	
       	if(sourceLocation != null) {
       	   // Create the source location file-container-id metadata.
       	   HpcMetadataEntry sourceLocationFileContainerIdMetadata = new HpcMetadataEntry();
       	   sourceLocationFileContainerIdMetadata.setAttribute(SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE);
       	   sourceLocationFileContainerIdMetadata.setValue(sourceLocation.getFileContainerId());
       	   sourceLocationFileContainerIdMetadata.setUnit("");
       	   metadataEntries.add(sourceLocationFileContainerIdMetadata);
       	
       	   // Create the source location file-id metadata.
       	   HpcMetadataEntry sourceLocationFileIdMetadata = new HpcMetadataEntry();
       	   sourceLocationFileIdMetadata.setAttribute(SOURCE_LOCATION_FILE_ID_ATTRIBUTE);
       	   sourceLocationFileIdMetadata.setValue(sourceLocation.getFileId());
       	   sourceLocationFileIdMetadata.setUnit("");
       	   metadataEntries.add(sourceLocationFileIdMetadata);
       	}
       	
       	// Create the archive location file-container-id metadata.
       	HpcMetadataEntry archiveLocationFileContainerIdMetadata = new HpcMetadataEntry();
       	archiveLocationFileContainerIdMetadata.setAttribute(ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE);
       	archiveLocationFileContainerIdMetadata.setValue(archiveLocation.getFileContainerId());
       	archiveLocationFileContainerIdMetadata.setUnit("");
       	metadataEntries.add(archiveLocationFileContainerIdMetadata);
       	
       	// Create the archive location file-id metadata.
       	HpcMetadataEntry archiveLocationFileIdMetadata = new HpcMetadataEntry();
       	archiveLocationFileIdMetadata.setAttribute(ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE);
       	archiveLocationFileIdMetadata.setValue(archiveLocation.getFileId());
       	archiveLocationFileIdMetadata.setUnit("");
       	metadataEntries.add(archiveLocationFileIdMetadata);
       	
       	if(dataTransferRequestId != null) {
       	   // Create the Data Transfer Request ID metadata.
       	   HpcMetadataEntry dataTransferRequestIdMetadata = new HpcMetadataEntry();
       	   dataTransferRequestIdMetadata.setAttribute(DATA_TRANSFER_REQUEST_ID_ATTRIBUTE);
       	   dataTransferRequestIdMetadata.setValue(dataTransferRequestId);
       	   dataTransferRequestIdMetadata.setUnit("");
       	   metadataEntries.add(dataTransferRequestIdMetadata);
       	}
       	
       	// Create the Data Transfer Status metadata.
       	HpcMetadataEntry dataTransferStatusMetadata = new HpcMetadataEntry();
       	dataTransferStatusMetadata.setAttribute(DATA_TRANSFER_STATUS_ATTRIBUTE);
       	dataTransferStatusMetadata.setValue(dataTransferStatus.value());
       	dataTransferStatusMetadata.setUnit("");
       	metadataEntries.add(dataTransferStatusMetadata);
       	
       	// Create the Data Transfer Type metadata.
       	HpcMetadataEntry dataTransferTypeMetadata = new HpcMetadataEntry();
       	dataTransferTypeMetadata.setAttribute(DATA_TRANSFER_TYPE_ATTRIBUTE);
       	dataTransferTypeMetadata.setValue(dataTransferType.value());
       	dataTransferTypeMetadata.setUnit("");
       	metadataEntries.add(dataTransferTypeMetadata);
       	
       	if(sourceSize != null) {
       	   // Create the Source File Size metadata.
       	   HpcMetadataEntry sourceSizeMetadata = new HpcMetadataEntry();
       	   sourceSizeMetadata.setAttribute(SOURCE_FILE_SIZE_ATTRIBUTE);
       	   sourceSizeMetadata.setValue(String.valueOf(sourceSize));
       	   sourceSizeMetadata.setUnit("");
       	   metadataEntries.add(sourceSizeMetadata);
       	}
       	// Add Metadata to the DM system.
       	dataManagementProxy.addMetadataToDataObject(getAuthenticatedToken(), 
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
    	systemGeneratedMetadata.setDataTransferStatus(
    		  HpcDataTransferStatus.fromValue(metadataMap.get(DATA_TRANSFER_STATUS_ATTRIBUTE)));
    	systemGeneratedMetadata.setDataTransferType(
      		  HpcDataTransferType.fromValue(metadataMap.get(DATA_TRANSFER_TYPE_ATTRIBUTE)));
    	systemGeneratedMetadata.setSourceSize(
    		  metadataMap.get(SOURCE_FILE_SIZE_ATTRIBUTE) != null ?
    		  Long.valueOf(metadataMap.get(SOURCE_FILE_SIZE_ATTRIBUTE)) : null);
    		  
		return systemGeneratedMetadata;
	}
    
	public void setDataTransferStatus(String path, 
                                      HpcDataTransferStatus dataTransferStatus) 
                                     throws HpcException
    {
    	// Input validation.
    	if(getDataObject(path) == null) {
           throw new HpcException("Data object not found: " + path, 
		                          HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	List<HpcMetadataEntry> metadataEntries = new ArrayList<HpcMetadataEntry>();
       	HpcMetadataEntry dataTransferStatusMetadata = new HpcMetadataEntry();
       	dataTransferStatusMetadata.setAttribute(DATA_TRANSFER_STATUS_ATTRIBUTE);
       	dataTransferStatusMetadata.setValue(dataTransferStatus.value());
       	dataTransferStatusMetadata.setUnit("");
       	metadataEntries.add(dataTransferStatusMetadata);
       	
       	dataManagementProxy.updateDataObjectMetadata(getAuthenticatedToken(),
                                                     path, metadataEntries);
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
       	   throw new HpcException("Null path", 
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
    	return getDataObjects(dataTransferInProgressQuery);
    }
    
    @Override
    public List<HpcMetadataEntry> getDataObjectMetadata(String path) throws HpcException
    {
       	// Input validation.
       	if(path == null) {
       	   throw new HpcException("Null path", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
    	return dataManagementProxy.getDataObjectMetadata(getAuthenticatedToken(),
                                                         path);
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
    public void closeConnection()
    {
    	try {
    	     dataManagementProxy.disconnect(getAuthenticatedToken());
    	     
    	} catch(HpcException e) {
    		    // Ignore.
    	}
    	
    }
    
    @Override
    public HpcPathAttributes setPermission(String path,
    		                               HpcUserPermission permissionRequest) 
                                          throws HpcException
    {
    	// Input validation.
    	if(path == null || permissionRequest == null) {
           throw new HpcException("Null path", 
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
    public Map<String, String> toMap(List<HpcMetadataEntry> metadataEntries)
    {
    	Map<String, String> metadataMap = new HashMap<String, String>();
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
       	HpcMetadataEntry idMetadata = new HpcMetadataEntry();
       	idMetadata.setAttribute(ID_ATTRIBUTE);
       	idMetadata.setValue(keyGenerator.generateKey());
       	idMetadata.setUnit("");
       	return idMetadata;
    }
    
    /**
     * Generate registrar ID, name and DOC metadata.
     * 
     * @return a List of 3 metadata.
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
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<HpcMetadataEntry>();
       	
       	// Create the registrar user-id metadata.
       	HpcMetadataEntry registrarIdMetadata = new HpcMetadataEntry();
       	registrarIdMetadata.setAttribute(REGISTRAR_ID_ATTRIBUTE);
       	registrarIdMetadata.setValue(invoker.getNciAccount().getUserId());
       	registrarIdMetadata.setUnit("");
       	metadataEntries.add(registrarIdMetadata);
       	
       	// Create the registrar name metadata.
       	HpcMetadataEntry registrarNameMetadata = new HpcMetadataEntry();
       	registrarNameMetadata.setAttribute(REGISTRAR_NAME_ATTRIBUTE);
       	registrarNameMetadata.setValue(invoker.getNciAccount().getFirstName() + " " +
       			                       invoker.getNciAccount().getLastName());
       	registrarNameMetadata.setUnit("");
       	metadataEntries.add(registrarNameMetadata);
       	
       	// Create the registrar DOC.
       	HpcMetadataEntry registrarDOCMetadata = new HpcMetadataEntry();
       	registrarDOCMetadata.setAttribute(REGISTRAR_DOC_ATTRIBUTE);
       	registrarDOCMetadata.setValue(invoker.getNciAccount().getDOC());
       	registrarDOCMetadata.setUnit("");
       	metadataEntries.add(registrarDOCMetadata);
       	
       	return metadataEntries;
    }
}