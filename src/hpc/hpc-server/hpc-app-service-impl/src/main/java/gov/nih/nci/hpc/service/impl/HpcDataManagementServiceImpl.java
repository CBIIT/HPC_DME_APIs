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
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidIntegratedSystemAccount;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataEntries;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataQueries;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
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
    
    // File Location metadata attributes.
	public final static String FILE_LOCATION_ENDPOINT_ATTRIBUTE = 
			                   "Data Location Globus Endpoint"; 
	public final static String FILE_LOCATION_PATH_ATTRIBUTE = 
			                   "Data Location Globus Path"; 
	public final static String FILE_SOURCE_ENDPOINT_ATTRIBUTE = 
			                   "Data Source Globus Endpoint"; 
	public final static String FILE_SOURCE_PATH_ATTRIBUTE = 
			                   "Data Source Globus Path"; 
	public final static String FILE_REGISTRAR_ID_ATTRIBUTE = 
                               "NCI user-id of the User registering the File"; 
	public final static String FILE_REGISTRAR_NAME_ATTRIBUTE = 
                               "Name of the User registering the File (Registrar)"; 
	public final static String COLLECTION_REGISTRAR_ID_ATTRIBUTE = 
                               "NCI user-id of the User registering the Collectiont"; 
	public final static String COLLECTION_REGISTRAR_NAME_ATTRIBUTE = 
                               "Name of the User registering the Collectiont (Registrar)"; 
	
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
    public boolean createDirectory(String path) 
    		                      throws HpcException
    {
    	HpcIntegratedSystemAccount dataManagementAccount = getDataManagementAccount();
    	boolean created = !dataManagementProxy.getPathAttributes(dataManagementAccount, path).exists;

    	dataManagementProxy.createCollectionDirectory(getDataManagementAccount(), 
    			                                      path);
    	return created;
    }
    
    @Override
    public void createFile(String path, boolean createParentPathDirectory) 
    		              throws HpcException
    {
    	HpcIntegratedSystemAccount dataManagementAccount = getDataManagementAccount();
    	
    	// Validate the path is available.
    	if(dataManagementProxy.getPathAttributes(dataManagementAccount, path).exists) {
    		throw new HpcException("Path already exists: " + path, 
    				               HpcRequestRejectReason.DATA_OBJECT_PATH_ALREADY_EXISTS);
    	}
    	
    	//  Validate the parent directory exists.
    	if(!createParentPathDirectory && 
    	   !dataManagementProxy.isParentPathDirectory(dataManagementAccount, path)) {
    		throw new HpcException("Invalid data object path. Directory doesn't exist: " + path, 
                    HpcRequestRejectReason.INVALID_DATA_OBJECT_PATH);
    	}
    	
    	// Create the parent directory if it doesn't already exist.
    	if(createParentPathDirectory) {
    	   dataManagementProxy.createParentPathDirectory(dataManagementAccount, path);
    	}
    	
    	// Create the data object file.
    	dataManagementProxy.createDataObjectFile(dataManagementAccount, path);
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
       	dataManagementProxy.addMetadataToCollection(getDataManagementAccount(),
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
       	dataManagementProxy.addMetadataToCollection(getDataManagementAccount(), 
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
       	dataManagementProxy.updateCollectionMetadata(getDataManagementAccount(),
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
       	dataManagementProxy.addMetadataToDataObject(getDataManagementAccount(), 
       			                                    path, metadataEntries);
    }
    
    @Override
    public void addSystemGeneratedMetadataToDataObject(String path, 
                                                       HpcFileLocation fileLocation,
    		                                           HpcFileLocation fileSource) 
                                                      throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidFileLocation(fileLocation) ||
       	   !isValidFileLocation(fileSource)) {
       	   throw new HpcException("Null path or Invalid file location", 
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
       	
       	// Add Metadata to the DM system.
       	dataManagementProxy.addMetadataToDataObject(getDataManagementAccount(), 
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
    	HpcIntegratedSystemAccount dataManagementAccount = getDataManagementAccount();
    	if(dataManagementProxy.getPathAttributes(dataManagementAccount, path).isDirectory) {
    	   return dataManagementProxy.getCollection(getDataManagementAccount(), path);
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
       	
    	return dataManagementProxy.getCollections(getDataManagementAccount(),
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
       	
    	return dataManagementProxy.getCollectionMetadata(getDataManagementAccount(),
                                                         path);
    }
    
    @Override
    public HpcDataObject getDataObject(String path) throws HpcException
    {
    	HpcIntegratedSystemAccount dataManagementAccount = getDataManagementAccount();
    	if(dataManagementProxy.getPathAttributes(dataManagementAccount, path).isFile) {
    	   return dataManagementProxy.getDataObject(getDataManagementAccount(), path);
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
       	
    	return dataManagementProxy.getDataObjects(getDataManagementAccount(),
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
       	
    	return dataManagementProxy.getDataObjectMetadata(getDataManagementAccount(),
                                                         path);
    }
    
    @Override
    public String getUserType() throws HpcException
    {
    	return dataManagementProxy.getUserType(getDataManagementAccount());
    }
    
    @Override
    public void closeConnection()
    {
    	try {
    	     dataManagementProxy.closeConnection(getDataManagementAccount());
    	     
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
    	
    	HpcDataManagementPathAttributes pathAttributes = 
    		   dataManagementProxy.getPathAttributes(getDataManagementAccount(), path);
    	if(pathAttributes.isDirectory) {
    	   dataManagementProxy.setCollectionPermission(getDataManagementAccount(), 
    			                                       path, 
    			                                       permissionRequest);
    	} else if(pathAttributes.isFile) {
    		      dataManagementProxy.setDataObjectPermission(getDataManagementAccount(), 
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
     * Get the data management account from the request context.
     *
     * @throws HpcException If the account is not set or invalid.
     */
    private HpcIntegratedSystemAccount getDataManagementAccount() throws HpcException
    {
    	HpcUser user = HpcRequestContext.getRequestInvoker();
    	if(user == null || 
    	   !isValidIntegratedSystemAccount(user.getDataManagementAccount())) {
	       throw new HpcException("Unknown user or invalid data management account",
			                      HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);
    	}
    	
    	return user.getDataManagementAccount();
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
       	HpcUser user = HpcRequestContext.getRequestInvoker();
       	if(user == null) {
       	   throw new HpcException("Unknown service invoker", 
		                          HpcErrorType.UNEXPECTED_ERROR);
       	}	
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<HpcMetadataEntry>();
       	
       	// Create the registrar user-id metadata.
       	HpcMetadataEntry registrarIdMetadata = new HpcMetadataEntry();
       	registrarIdMetadata.setAttribute(registrarIdAttribute);
       	registrarIdMetadata.setValue(user.getNciAccount().getUserId());
       	registrarIdMetadata.setUnit("");
       	metadataEntries.add(registrarIdMetadata);
       	
       	// Create the registrar name metadata.
       	HpcMetadataEntry registrarNameMetadata = new HpcMetadataEntry();
       	registrarNameMetadata.setAttribute(registrarNameAttribute);
       	registrarNameMetadata.setValue(user.getNciAccount().getFirstName() + " " +
       			                       user.getNciAccount().getLastName());
       	registrarNameMetadata.setUnit("");
       	metadataEntries.add(registrarNameMetadata);
       	
       	return metadataEntries;
    }
}