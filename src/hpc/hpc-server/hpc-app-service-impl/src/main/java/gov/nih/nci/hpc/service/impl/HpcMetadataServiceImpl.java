/**
 * HpcMetadataServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataEntries;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.CALLER_OBJECT_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_REQUEST_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_STATUS_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_TYPE_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.REGISTRAR_DOC_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.REGISTRAR_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.REGISTRAR_NAME_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_SIZE_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_LOCATION_FILE_ID_ATTRIBUTE;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcMetadataService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class HpcMetadataServiceImpl implements HpcMetadataService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
	private static final String INVALID_PATH_METADATA_MSG = 
                                "Invalid path or metadata entry";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
    // The Data Management Authenticator.
	@Autowired
    private HpcDataManagementAuthenticator dataManagementAuthenticator = null;
	
	// Metadata Validator.
	@Autowired
	private HpcMetadataValidator metadataValidator = null;
	
	// Key Generator.
	@Autowired
	private HpcKeyGenerator keyGenerator = null;
	
    // The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @throws HpcException Constructor is disabled.
     */
	private HpcMetadataServiceImpl() throws HpcException
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcMetadataService Interface Implementation
    //---------------------------------------------------------------------//  

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
       	dataManagementProxy.addMetadataToCollection(dataManagementAuthenticator.getAuthenticatedToken(),
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
       	dataManagementProxy.updateCollectionMetadata(dataManagementAuthenticator.getAuthenticatedToken(),
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
       	dataManagementProxy.addMetadataToCollection(dataManagementAuthenticator.getAuthenticatedToken(), 
       			                                    path, metadataEntries);    	
    }
    
    @Override
    public HpcSystemGeneratedMetadata 
              getCollectionSystemGeneratedMetadata(String path) throws HpcException
	{
       	// Input validation.
       	if(path == null) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
    	
    	return toSystemGeneratedMetadata(getCollectionMetadata(path));
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
    		  
		return systemGeneratedMetadata;
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
     * Get metadata of a collection.
     *
     * @param path The collection's path.
     * @return HpcMetadataEntries The collection's metadata entries.
     * 
     * @throws HpcException
     */
    private List<HpcMetadataEntry> getCollectionMetadata(String path) throws HpcException
    {
       	// Input validation.
       	if(path == null) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
    	return dataManagementProxy.getCollectionMetadata(
    			                      dataManagementAuthenticator.getAuthenticatedToken(),
                                      path);
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
       			                         invoker.getNciAccount().getDoc()));
       	
       	return metadataEntries;
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
}