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
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
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
	private final static String FILE_LOCATION_ENDPOINT_ATTRIBUTE = 
			                    "Data Location Globus Endpoint"; 
	private final static String FILE_LOCATION_PATH_ATTRIBUTE = 
			                    "Data Location Globus Path"; 
	private final static String FILE_SOURCE_ENDPOINT_ATTRIBUTE = 
			                    "Data Source Globus Endpoint"; 
	private final static String FILE_SOURCE_PATH_ATTRIBUTE = 
			                    "Data Source Globus Path"; 
	private final static String FILE_REGISTRAR_ID_ATTRIBUTE = 
                                "NCI user-id of the User registering the File"; 
	private final static String FILE_REGISTRAR_NAME_ATTRIBUTE = 
                                "Name of the User registering the File (Registrar)"; 
	private final static String FILE_DATA_TRANSFER_STATUS_ATTRIBUTE = 
                                "Data Transfer Status";
	private final static String COLLECTION_REGISTRAR_ID_ATTRIBUTE = 
                                "NCI user-id of the User registering the Collection"; 
	private final static String COLLECTION_REGISTRAR_NAME_ATTRIBUTE = 
                                "Name of the User registering the Collection (Registrar)"; 
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
	// Metadata Validator.
	@Autowired
	private HpcMetadataValidator metadataValidator = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     */
    private HpcDataManagementServiceImpl()
    {
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
       	
       	// Create the registrar ID and name metadata.
       	metadataEntries.addAll(generateRegistrarMetadata(COLLECTION_REGISTRAR_ID_ATTRIBUTE,
       			                                         COLLECTION_REGISTRAR_NAME_ATTRIBUTE));
       	
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
    public void addSystemGeneratedMetadataToDataObject(String path, 
                                                       HpcFileLocation fileLocation,
    		                                           HpcFileLocation fileSource,
    		                                           String dataTransferStatus) 
                                                      throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidFileLocation(fileLocation) ||
       	   !isValidFileLocation(fileSource) || dataTransferStatus == null) {
       	   throw new HpcException("Null path or Invalid file location or null data-transfer-status", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<HpcMetadataEntry>();
       	
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
       	
       	// Create the registrar ID and name metadata.
       	metadataEntries.addAll(generateRegistrarMetadata(FILE_REGISTRAR_ID_ATTRIBUTE,
       			                                         FILE_REGISTRAR_NAME_ATTRIBUTE));
       	
       	// Create the Data Transfer Status metadata.
       	HpcMetadataEntry dataTransferMetadata = new HpcMetadataEntry();
       	dataTransferMetadata.setAttribute(FILE_DATA_TRANSFER_STATUS_ATTRIBUTE);
       	dataTransferMetadata.setValue(dataTransferStatus);
       	dataTransferMetadata.setUnit("");
       	metadataEntries.add(dataTransferMetadata);
       	
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
    		}
    	}
    	
    	if(location.getEndpoint() == null || location.getPath() == null) {
    	   throw new HpcException("File location not found: " + path, 
    			                  HpcRequestRejectReason.FILE_NOT_FOUND);
    	}
    	
    	return location;
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
    public String getUserType(String username) throws HpcException
    {
    	return dataManagementProxy.getUserType(getAuthenticatedToken(), username);
    }
    
    @Override
    public void addUser(HpcNciAccount nciAccount, String userType) throws HpcException
    {
    	// Input validation.
    	if(!isValidNciAccount(nciAccount)) {	
    	   throw new HpcException("Invalid NCI Account", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
       	
    	dataManagementProxy.addUser(getAuthenticatedToken(), 
    			                    nciAccount, userType);
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
     * Generate registrar ID and name metadata.
     * 
     * @param registrarIdAttribute The registrar ID attribute name.
     * @param registrarNameAttribute The registrar-name attribute name.
     *
     * @throws HpcException if the service invoker is unknown.
     */
    private List<HpcMetadataEntry> generateRegistrarMetadata(
    		                                        String registrarIdAttribute,
    		                                        String registrarNameAttribute)
    		                                        throws HpcException
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
       	registrarIdMetadata.setAttribute(registrarIdAttribute);
       	registrarIdMetadata.setValue(invoker.getNciAccount().getUserId());
       	registrarIdMetadata.setUnit("");
       	metadataEntries.add(registrarIdMetadata);
       	
       	// Create the registrar name metadata.
       	HpcMetadataEntry registrarNameMetadata = new HpcMetadataEntry();
       	registrarNameMetadata.setAttribute(registrarNameAttribute);
       	registrarNameMetadata.setValue(invoker.getNciAccount().getFirstName() + " " +
       			                       invoker.getNciAccount().getLastName());
       	registrarNameMetadata.setUnit("");
       	metadataEntries.add(registrarNameMetadata);
       	
       	return metadataEntries;
    }
}