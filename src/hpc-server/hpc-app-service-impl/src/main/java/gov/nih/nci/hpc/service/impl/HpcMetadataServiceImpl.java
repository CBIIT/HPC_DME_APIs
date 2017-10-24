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

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataEntries;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.CALLER_OBJECT_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.CHECKSUM_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_COMPLETED_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_REQUEST_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_STARTED_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_STATUS_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.DATA_TRANSFER_TYPE_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.METADATA_UPDATED_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.REGISTRAR_DOC_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.REGISTRAR_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.REGISTRAR_NAME_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_FILE_SIZE_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcMetadataValidator.SOURCE_LOCATION_FILE_ID_ATTRIBUTE;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcMetadataService;

/**
 * <p>
 * HPC Data Management Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
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
	
	// Metadata DAO.
	@Autowired
	private HpcMetadataDAO metadataDAO = null;
	
	// Date formatter to format metadata entries of type Calendar (like data transfer start/completion time). 
	private DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
	
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
    		                            List<HpcMetadataEntry> metadataEntries,
    		                            String doc) 
    		                           throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidMetadataEntries(metadataEntries)) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Validate Metadata.
       	metadataValidator.validateCollectionMetadata(doc, null, metadataEntries);
       	
       	// Add Metadata to the DM system.
       	dataManagementProxy.addMetadataToCollection(dataManagementAuthenticator.getAuthenticatedToken(),
       			                                    path, metadataEntries);
    }
    
    @Override
    public void updateCollectionMetadata(String path, 
    		                             List<HpcMetadataEntry> metadataEntries,
    		                             String doc) 
    		                            throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidMetadataEntries(metadataEntries)) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Validate collection type is not in the update request.
       	List<HpcMetadataEntry> existingMetadataEntries = dataManagementProxy.getCollectionMetadata(
                                                             dataManagementAuthenticator.getAuthenticatedToken(),
                                                             path);
       	validateCollectionTypeUpdate(existingMetadataEntries, metadataEntries);
       	
       	// Validate the metadata.
       	metadataValidator.validateCollectionMetadata(doc, existingMetadataEntries,
       			                                     metadataEntries);
       	
        // Update the 'metadata updated' system-metadata to record the time of this metadata update.
       	metadataEntries.add(generateMetadataUpdatedMetadata());
       	
       	// Update the metadata.
       	dataManagementProxy.updateCollectionMetadata(dataManagementAuthenticator.getAuthenticatedToken(),
       			                                     path, metadataEntries);
    }
    
    @Override
    public void addSystemGeneratedMetadataToCollection(String path, String userId, String userName,
    		                                           String doc) 
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
       	
       	// Create the Metadata-Updated metadata.
       	metadataEntries.add(generateMetadataUpdatedMetadata());
       	
       	// Create and add the registrar ID, name and DOC metadata.
       	metadataEntries.addAll(generateRegistrarMetadata(userId, userName, doc));
       	
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
    	
    	return toSystemGeneratedMetadata(dataManagementProxy.getCollectionMetadata(
                                             dataManagementAuthenticator.getAuthenticatedToken(),
                                             path));
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
    			   systemGeneratedMetadata.setDataTransferStatus(HpcDataTransferUploadStatus.FAILED);
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
    	
    	if(metadataMap.get(DATA_TRANSFER_STARTED_ATTRIBUTE) != null) {
    	   systemGeneratedMetadata.setDataTransferStarted(toCalendar(metadataMap.get(DATA_TRANSFER_STARTED_ATTRIBUTE)));
    	}
    	if(metadataMap.get(DATA_TRANSFER_COMPLETED_ATTRIBUTE) != null) {
     	   systemGeneratedMetadata.setDataTransferCompleted(toCalendar(metadataMap.get(DATA_TRANSFER_COMPLETED_ATTRIBUTE)));
     	}
    	
    	systemGeneratedMetadata.setSourceSize(
    		  metadataMap.get(SOURCE_FILE_SIZE_ATTRIBUTE) != null ?
    		  Long.valueOf(metadataMap.get(SOURCE_FILE_SIZE_ATTRIBUTE)) : null);
    	systemGeneratedMetadata.setCallerObjectId(
      		  metadataMap.get(CALLER_OBJECT_ID_ATTRIBUTE));
    	systemGeneratedMetadata.setChecksum(metadataMap.get(CHECKSUM_ATTRIBUTE));
    	
    	if(metadataMap.get(METADATA_UPDATED_ATTRIBUTE) != null) {
     	   systemGeneratedMetadata.setMetadataUpdated(toCalendar(metadataMap.get(METADATA_UPDATED_ATTRIBUTE)));
     	}
    		  
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
    
    @Override
    public HpcMetadataEntries getCollectionMetadataEntries(String path) throws HpcException
    {
    	HpcMetadataEntries metadataEntries = new HpcMetadataEntries();
    	
    	// Get the metadata associated with the collection itself.
    	metadataEntries.getSelfMetadataEntries().addAll(
    			dataManagementProxy.getCollectionMetadata(
    					               dataManagementAuthenticator.getAuthenticatedToken(), path));
    	
    	// Get the hierarchical metadata.
    	metadataEntries.getParentMetadataEntries().addAll(
    			metadataDAO.getCollectionMetadata(dataManagementProxy.getAbsolutePath(path), 2));
    	
    	return metadataEntries;
    }
    
    @Override
    public void addMetadataToDataObject(String path, 
    		                            List<HpcMetadataEntry> metadataEntries,
    		                            String doc, String collectionType) 
    		                           throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidMetadataEntries(metadataEntries)) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Validate Metadata.
       	metadataValidator.validateDataObjectMetadata(doc, null, metadataEntries, collectionType);
       	
       	// Add Metadata to the DM system.
       	dataManagementProxy.addMetadataToDataObject(dataManagementAuthenticator.getAuthenticatedToken(), 
       			                                    path, metadataEntries);
    }
    
    @Override
    public void addSystemGeneratedMetadataToDataObject(String path, 
                                                       HpcFileLocation archiveLocation,
    		                                           HpcFileLocation sourceLocation,
    		                                           String dataTransferRequestId,
    		                                           String checksum,
    		                                           HpcDataTransferUploadStatus dataTransferStatus,
    		                                           HpcDataTransferType dataTransferType,
    		                                           Calendar dataTransferStarted,
    		                                           Calendar dataTransferCompleted,
    		                                           Long sourceSize, String callerObjectId,
    		                                           String userId, String userName, String doc) 
                                                      throws HpcException
    {
       	// Input validation.
       	if(path == null || dataTransferStatus == null || dataTransferType == null || 
       	   dataTransferStarted == null) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
		                          HpcErrorType.INVALID_REQUEST_INPUT);	
       	}
       	if((archiveLocation != null &&!isValidFileLocation(archiveLocation)) ||
       	   (sourceLocation != null && !isValidFileLocation(sourceLocation))) {
       	   throw new HpcException("Invalid source/archive location", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
       	
       	// Generate a data-object ID and add it as metadata.
       	metadataEntries.add(generateIdMetadata());
       	
       	// Create the Metadata-Updated metadata.
       	metadataEntries.add(generateMetadataUpdatedMetadata());
       	
       	// Create and add the registrar ID, name and DOC metadata.
       	metadataEntries.addAll(generateRegistrarMetadata(userId, userName, doc));
       	
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
       	
       	if(archiveLocation != null) {
           // Create the archive location file-container-id metadata.
       	   addMetadataEntry(metadataEntries,
       		   	            toMetadataEntry(ARCHIVE_LOCATION_FILE_CONTAINER_ID_ATTRIBUTE, 
       			                            archiveLocation.getFileContainerId()));
       	
       	   // Create the archive location file-id metadata.
       	   addMetadataEntry(metadataEntries,
       		  	            toMetadataEntry(ARCHIVE_LOCATION_FILE_ID_ATTRIBUTE, 
       			                            archiveLocation.getFileId()));
       	}
       	
   	    // Create the Data Transfer Request ID metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(DATA_TRANSFER_REQUEST_ID_ATTRIBUTE, 
       			                         dataTransferRequestId));
       	
   	    // Create the Data Transfer Request ID metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(CHECKSUM_ATTRIBUTE, checksum));
       	
       	// Create the Data Transfer Status metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(DATA_TRANSFER_STATUS_ATTRIBUTE, 
       		                             dataTransferStatus.value()));
       	
       	// Create the Data Transfer Type metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(DATA_TRANSFER_TYPE_ATTRIBUTE, 
       		                             dataTransferType.value()));
       	
       	// Create the Data Transfer Started metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(DATA_TRANSFER_STARTED_ATTRIBUTE, 
       			        		         dateFormat.format(dataTransferStarted.getTime())));
       	
       	// Create the Data Transfer Completed metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(DATA_TRANSFER_COMPLETED_ATTRIBUTE, 
       			        		         dataTransferCompleted != null ? 
       			        		        	 dateFormat.format(dataTransferCompleted.getTime()) : null));
       	
       	// Create the Source File Size metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(SOURCE_FILE_SIZE_ATTRIBUTE, 
       			                         sourceSize));
       	
        // Create the Caller Object ID metadata.
        addMetadataEntry(metadataEntries,
        		         toMetadataEntry(CALLER_OBJECT_ID_ATTRIBUTE, 
        	                             callerObjectId));
        
       	// Add Metadata to the DM system.
       	dataManagementProxy.addMetadataToDataObject(dataManagementAuthenticator.getAuthenticatedToken(), 
       			                                    path, metadataEntries);    	
    }
    
    @Override
    public HpcSystemGeneratedMetadata 
              getDataObjectSystemGeneratedMetadata(String path) throws HpcException
	{
    	// Input validation.
       	if(path == null) {
           throw new HpcException(INVALID_PATH_METADATA_MSG, 
        			                  HpcErrorType.INVALID_REQUEST_INPUT);
        }	
    	
    	return toSystemGeneratedMetadata(dataManagementProxy.getDataObjectMetadata(
    			                             dataManagementAuthenticator.getAuthenticatedToken(),
                                             path));
	}
    
    @Override
    public void updateDataObjectSystemGeneratedMetadata(String path, 
                                                        HpcFileLocation archiveLocation,
                                                        String dataTransferRequestId,
                                                        String checksum,
                                                        HpcDataTransferUploadStatus dataTransferStatus,
                                                        HpcDataTransferType dataTransferType,
                                                        Calendar dataTransferCompleted) 
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
       	
       	if(checksum != null) {
           // Update the Checksum metadata.
           addMetadataEntry(metadataEntries,
        	                toMetadataEntry(CHECKSUM_ATTRIBUTE, checksum));
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
       	
       	if(dataTransferCompleted != null) {
           // Update the Data Transfer Completed metadata.
           addMetadataEntry(metadataEntries,
             		        toMetadataEntry(DATA_TRANSFER_COMPLETED_ATTRIBUTE, 
           			            	        dateFormat.format(dataTransferCompleted.getTime())));
       	}
       	
       	if(!metadataEntries.isEmpty()) {
		   dataManagementProxy.updateDataObjectMetadata(dataManagementAuthenticator.getAuthenticatedToken(),
		                                                path, metadataEntries);
       	}
	}
    
    @Override
    public void updateDataObjectMetadata(String path, 
    		                             List<HpcMetadataEntry> metadataEntries, 
    		                             String doc, String collectionType) 
    		                            throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidMetadataEntries(metadataEntries)) {
       	   throw new HpcException(INVALID_PATH_METADATA_MSG, 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Validate the metadata.
       	metadataValidator.validateDataObjectMetadata(doc,
       			                                     dataManagementProxy.getDataObjectMetadata(
       			                                         dataManagementAuthenticator.getAuthenticatedToken(),
                                                         path),
       			                                     metadataEntries, collectionType);
       	
       	// Update the 'metadata updated' system-metadata to record the time of this metadata update.
       	metadataEntries.add(generateMetadataUpdatedMetadata());
       	
       	// Update the metadata.
       	dataManagementProxy.updateDataObjectMetadata(dataManagementAuthenticator.getAuthenticatedToken(),
       			                                     path, metadataEntries);
    }
    
    @Override
    public HpcMetadataEntries getDataObjectMetadataEntries(String path) throws HpcException
    {
    	HpcMetadataEntries metadataEntries = new HpcMetadataEntries();
    	
    	// Get the metadata associated with the data object itself.
    	metadataEntries.getSelfMetadataEntries().addAll(
    			dataManagementProxy.getDataObjectMetadata(
    					               dataManagementAuthenticator.getAuthenticatedToken(), path));
    	
    	// Get the hierarchical metadata.
    	metadataEntries.getParentMetadataEntries().addAll(
    			metadataDAO.getDataObjectMetadata(dataManagementProxy.getAbsolutePath(path), 2));
    	
    	return metadataEntries;
    }
    
    @Override
    public void refreshViews() throws HpcException
    {
    	metadataDAO.refreshViews();
    }
    
    @Override
    public List<String> getCollectionSystemMetadataAttributeNames() throws HpcException
    {
    	return metadataValidator.getCollectionSystemGeneratedMetadataAttributeNames();
    }

    @Override
    public List<String> getDataObjectSystemMetadataAttributeNames() throws HpcException
    {
    	return metadataValidator.getDataObjectSystemGeneratedMetadataAttributeNames();
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
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
     * Generate Metadata Updated Metadata.
     *
     * @return The Generated ID metadata. 
     */
    private HpcMetadataEntry generateMetadataUpdatedMetadata()
    {
    	return toMetadataEntry(METADATA_UPDATED_ATTRIBUTE, 
    			               dateFormat.format(Calendar.getInstance().getTime()));
    }
    
    /**
     * Generate registrar ID, name and DOC metadata.
     * 
     * @param userId The user ID.
     * @param userName The user name.
     * @param doc The DOC.
     * @return A List of the 3 metadata.
     * @throws HpcException if the service invoker is unknown.
     */
    private List<HpcMetadataEntry> generateRegistrarMetadata(String userId, String userName, String doc) 
    		                                                throws HpcException
    {
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
       	
       	// Create the registrar user-id metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(REGISTRAR_ID_ATTRIBUTE, userId));
       	
       	// Create the registrar name metadata.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(REGISTRAR_NAME_ATTRIBUTE, userName));
       	
       	// Create the registrar DOC.
       	addMetadataEntry(metadataEntries,
       			         toMetadataEntry(REGISTRAR_DOC_ATTRIBUTE, doc));
       	
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
    	if(entry.getAttribute() != null && (entry.getValue() != null && !entry.getValue().isEmpty())) {
    	   metadataEntries.add(entry);
    	}
    }
    
    /**
     * Generate a metadata entry from attribute/value pair.
     * 
     * @param attribute The metadata entry attribute.
     * @param value The metadata entry value.
     * @return The metadata entry.
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
     * Validate that collection type is not updated
     *
     * @param existingMetadataEntries Existing collection metadata entries.
     * @param metadataEntries Updated collection metadata entries.
     * @throws HpcException If the user tries to update the collection type metadata.
     */
    private void validateCollectionTypeUpdate(List<HpcMetadataEntry> existingMetadataEntries,
    		                                  List<HpcMetadataEntry> metadataEntries) 
    		                                 throws HpcException
    {
    	// Get the current collection type.
	   	String collectionType = null;
	   	for(HpcMetadataEntry metadataEntry : existingMetadataEntries) {
			if(metadataEntry.getAttribute().equals(HpcMetadataValidator.COLLECTION_TYPE_ATTRIBUTE)) {
			   collectionType = metadataEntry.getValue();
			   break;
	 		}
	   	}
	   	
	   	// Validate it's not getting updated.
	   	if(collectionType == null) {
	   	   return;
	   	}
	   	
	   for(HpcMetadataEntry metadataEntry : metadataEntries) {
		   if(metadataEntry.getAttribute().equals(HpcMetadataValidator.COLLECTION_TYPE_ATTRIBUTE)) {
		      if(!metadataEntry.getValue().equals(collectionType)) {
			     throw new HpcException("Collection type can't updated", 
		                                HpcErrorType.INVALID_REQUEST_INPUT);
		      }
			      break;
		   }
	   }
    }
    
    /**
     * Instantiate a Calendar object from string.
     *
     * @param calendarStr The calendar as a string.
     * @return The Calendar instance.
     */
    private Calendar toCalendar(String calendarStr) 
    {
    	Calendar cal = Calendar.getInstance();
    	try {
     	     cal.setTime(dateFormat.parse(calendarStr));
     	     
    	} catch(ParseException e) {
    		    logger.error("Failed to parse calendar string: " + calendarStr);
    		    return null;
    	}
    	
    	return cal;
    }
}