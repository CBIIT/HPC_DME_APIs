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
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy.HpcDataManagementPathAttributes;
import gov.nih.nci.hpc.service.HpcDataManagementService;

import java.util.ArrayList;
import java.util.List;

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
	private final static String FILE_SOURCE_ENDPOINT_ATTRIBUTE = 
                                "source_globus_endpoint"; 
	private final static String FILE_SOURCE_PATH_ATTRIBUTE = 
                                "source_globus_path"; 
	private final static String FILE_LOCATION_ENDPOINT_ATTRIBUTE = 
			                    "data_globus_endpoint"; 
	private final static String FILE_LOCATION_PATH_ATTRIBUTE = 
			                    "data_globus_path"; 
	private final static String FILE_DATA_TRANSFER_ID_ATTRIBUTE = 
                                "data_globus_id";
	private final static String FILE_DATA_TRANSFER_STATUS_ATTRIBUTE = 
                                "data_globus_status";
	
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
    	query.setAttribute(FILE_DATA_TRANSFER_STATUS_ATTRIBUTE);
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
    	boolean created = !dataManagementProxy.getPathAttributes(authenticatedToken, path).exists;
    	dataManagementProxy.createCollectionDirectory(authenticatedToken, path);
    	return created;
    }
    
    @Override
    public void createFile(String path, boolean createParentPathDirectory) 
    		              throws HpcException
    {
    	Object authenticatedToken = getAuthenticatedToken();
    	
    	// Validate the path is available.
    	if(dataManagementProxy.getPathAttributes(authenticatedToken, path).exists) {
    		throw new HpcException("Path already exists: " + path, 
    				               HpcRequestRejectReason.DATA_OBJECT_PATH_ALREADY_EXISTS);
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
    }
    
    @Override
    public void deleteFile(String path) throws HpcException
    {
    	// Delete the data object file.
    	dataManagementProxy.deleteDataObjectFile(getAuthenticatedToken(), path);
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
       	metadataValidator.validateCollectionMetadata(metadataEntries);
       	
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
       	metadataValidator.validateCollectionMetadata(metadataEntries);
       	
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
       	metadataValidator.validateDataObjectMetadata(metadataEntries);
       	
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
       	metadataValidator.validateDataObjectMetadata(metadataEntries);
       	
       	// Update Metadata.
       	dataManagementProxy.updateDataObjectMetadata(getAuthenticatedToken(),
       			                                     path, metadataEntries);
    }
    
    @Override
    public void addSystemGeneratedMetadataToDataObject(String path, 
                                                       HpcFileLocation fileLocation,
    		                                           HpcFileLocation fileSource,
    		                                           String dataTransferRequestId) 
                                                      throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidFileLocation(fileLocation) ||
       	   !isValidFileLocation(fileSource) || dataTransferRequestId == null) {
       	   throw new HpcException("Null path or Invalid file location or null data-transfer-request-id", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<HpcMetadataEntry>();
       	
       	// Generate a data-object ID and add it as metadata.
       	metadataEntries.add(generateIdMetadata());
       	
       	// Create and add the registrar ID, name and DOC metadata.
       	metadataEntries.addAll(generateRegistrarMetadata());
       	
       	// Create the file source endpoint metadata.
       	HpcMetadataEntry sourceEndpointMetadata = new HpcMetadataEntry();
       	sourceEndpointMetadata.setAttribute(FILE_SOURCE_ENDPOINT_ATTRIBUTE);
       	sourceEndpointMetadata.setValue(fileSource.getEndpoint());
       	sourceEndpointMetadata.setUnit("");
       	metadataEntries.add(sourceEndpointMetadata);
       	
       	// Create the file source path metadata.
       	HpcMetadataEntry sourcePathMetadata = new HpcMetadataEntry();
       	sourcePathMetadata.setAttribute(FILE_SOURCE_PATH_ATTRIBUTE);
       	sourcePathMetadata.setValue(fileSource.getPath());
       	sourcePathMetadata.setUnit("");
       	metadataEntries.add(sourcePathMetadata);
       	
       	// Create the file location endpoint metadata.
       	HpcMetadataEntry locationEndpointMetadata = new HpcMetadataEntry();
       	locationEndpointMetadata.setAttribute(FILE_LOCATION_ENDPOINT_ATTRIBUTE);
       	locationEndpointMetadata.setValue(fileLocation.getEndpoint());
       	locationEndpointMetadata.setUnit("");
       	metadataEntries.add(locationEndpointMetadata);
       	
       	// Create the file location path metadata.
       	HpcMetadataEntry locationPathMetadata = new HpcMetadataEntry();
       	locationPathMetadata.setAttribute(FILE_LOCATION_PATH_ATTRIBUTE);
       	locationPathMetadata.setValue(fileLocation.getPath());
       	locationPathMetadata.setUnit("");
       	metadataEntries.add(locationPathMetadata);
       	
       	// Create the Data Transfer ID metadata.
       	HpcMetadataEntry dataTransferIdMetadata = new HpcMetadataEntry();
       	dataTransferIdMetadata.setAttribute(FILE_DATA_TRANSFER_ID_ATTRIBUTE);
       	dataTransferIdMetadata.setValue(dataTransferRequestId);
       	dataTransferIdMetadata.setUnit("");
       	metadataEntries.add(dataTransferIdMetadata);
       	
       	// Create the Data Transfer Status metadata.
       	HpcMetadataEntry dataTransferStatusMetadata = new HpcMetadataEntry();
       	dataTransferStatusMetadata.setAttribute(FILE_DATA_TRANSFER_STATUS_ATTRIBUTE);
       	dataTransferStatusMetadata.setValue(HpcDataTransferStatus.IN_PROGRESS.value());
       	dataTransferStatusMetadata.setUnit("");
       	metadataEntries.add(dataTransferStatusMetadata);
       	
       	// Add Metadata to the DM system.
       	dataManagementProxy.addMetadataToDataObject(getAuthenticatedToken(), 
       			                                    path, metadataEntries);    	
    }
    
    public HpcFileLocation getFileLocation(String path) throws HpcException
    {
    	// Input validation.
    	if(getDataObject(path) == null) {
           throw new HpcException("Data object not found: " + path, 
		                          HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	HpcFileLocation location = new HpcFileLocation();
    	for(HpcMetadataEntry metadata : getDataObjectMetadata(path)) {
    		if(metadata.getAttribute().equals(FILE_LOCATION_ENDPOINT_ATTRIBUTE)) {
    		   location.setEndpoint(metadata.getValue());	
    		   continue;
    		}
    		if(metadata.getAttribute().equals(FILE_LOCATION_PATH_ATTRIBUTE)) {
    		   location.setPath(metadata.getValue());
    		   continue;
    		}
    	}
    	
    	if(location.getEndpoint() == null || location.getPath() == null) {
    	   throw new HpcException("File location not found: " + path, 
    			                  HpcRequestRejectReason.FILE_NOT_FOUND);
    	}
    	
    	return location;
    }
    
    @Override
	public String getDataTransferRequestId(String path) throws HpcException
	{
    	// Input validation.
    	if(getDataObject(path) == null) {
           throw new HpcException("Data object not found: " + path, 
		                          HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
		for(HpcMetadataEntry metadataEntry : getDataObjectMetadata(path)) {
			if(metadataEntry.getAttribute().equals(FILE_DATA_TRANSFER_ID_ATTRIBUTE)) {
			   return metadataEntry.getValue();	
			}
		}
		
		return null;
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
       	dataTransferStatusMetadata.setAttribute(FILE_DATA_TRANSFER_STATUS_ATTRIBUTE);
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
    	if(dataManagementProxy.getPathAttributes(authenticatedToken, path).isDirectory) {
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
    	if(dataManagementProxy.getPathAttributes(authenticatedToken, path).isFile) {
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
    	   throw new HpcException("Invalid NCI Account", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
       	
    	dataManagementProxy.addUser(getAuthenticatedToken(), 
    			                    nciAccount, userRole);
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
    public void setPermission(String path, HpcUserPermission permissionRequest) 
                             throws HpcException
    {
    	// Input validation.
    	if(path == null || permissionRequest == null) {
           throw new HpcException("Null path", 
		                          HpcErrorType.INVALID_REQUEST_INPUT);    	   	
    	}
    	
    	Object authenticatedToken = getAuthenticatedToken();
    	HpcDataManagementPathAttributes pathAttributes = 
    		   dataManagementProxy.getPathAttributes(authenticatedToken, 
    				                                 path);
    	if(pathAttributes.isDirectory) {
    	   dataManagementProxy.setCollectionPermission(authenticatedToken, 
    			                                       path, 
    			                                       permissionRequest);
    	} else if(pathAttributes.isFile) {
    		      dataManagementProxy.setDataObjectPermission(authenticatedToken, 
    		    		                                      path, 
                                                              permissionRequest);
    	} else {
    		    throw new HpcException("Entity path doesn't exist", 
                                       HpcErrorType.INVALID_REQUEST_INPUT);   
    	}
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
    	Object token = dataManagementProxy.authenticate(invoker.getDataManagementAccount());
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
       	
       	return metadataEntries;
    }
}