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

import gov.nih.nci.hpc.bus.HpcDataManagementNewBusService;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementNewService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcSecurityService;

import java.io.File;
import java.util.List;

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

public class HpcDataManagementNewBusServiceImpl implements HpcDataManagementNewBusService
{  
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// The Data Management Application Service Instance.
	@Autowired
    private HpcDataManagementNewService dataManagementService = null;
	
	// The Data Transfer Application Service Instance.
	@Autowired
    private HpcDataTransferService dataTransferService = null;
	
	// Security Application Service Instance.
	@Autowired
    private HpcSecurityService securityService = null;
	
	// The Metadata Application Service Instance.
	@Autowired
    private HpcMetadataService metadataService = null;
	
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
    private HpcDataManagementNewBusServiceImpl()
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
    	if(path == null || path.isEmpty() || metadataEntries == null || metadataEntries.isEmpty()) {
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
    	        metadataService.addMetadataToCollection(path, metadataEntries);
    	        
    	        // Generate system metadata and attach to the collection.
       	        metadataService.addSystemGeneratedMetadataToCollection(path);
       	        
       	        // Validate the collection hierarchy.
       	        String doc = metadataService.getCollectionSystemGeneratedMetadata(path).getRegistrarDOC();
       	        dataManagementService.validateHierarchy(path, doc, false);
       	        
       	        registrationCompleted = true;
       	        
    	   } finally {
			          if(!registrationCompleted) {
				         // Collection registration failed. Remove it from Data Management.
				         dataManagementService.delete(path);
			          }
	       }
       	   
    	} else {
    		    metadataService.updateCollectionMetadata(path, metadataEntries);
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
    	
    	HpcCollection collection = dataManagementService.getCollection(path);
    	if(collection == null) {
      	   return null;
      	}
     	
     	// Get the metadata.
     	HpcMetadataEntries metadataEntries = 
     	   metadataService.getCollectionMetadataEntries(collection.getAbsolutePath());
 		
     	HpcCollectionDTO collectionDTO = new HpcCollectionDTO();
     	collectionDTO.setCollection(collection);
        collectionDTO.setMetadataEntries(metadataEntries);
     	
     	return collectionDTO;
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
    		    String collectionPath = path.substring(0, path.lastIndexOf('/'));
    		    String doc = metadataService.getCollectionSystemGeneratedMetadata(collectionPath).getRegistrarDOC();
      	        dataManagementService.validateHierarchy(collectionPath, doc, true);
    		   
    		    // Assign system account as an additional owner of the data-object.
   		        dataManagementService.assignSystemAccountPermission(path);
   		  
		        // Attach the user provided metadata.
		        metadataService.addMetadataToDataObject(
		    	   		           path, 
		    			           dataObjectRegistrationDTO.getMetadataEntries());
		        
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
			    metadataService.addSystemGeneratedMetadataToDataObject(
			        		                   path, uploadResponse.getArchiveLocation(),
			    			                   source, uploadResponse.getDataTransferRequestId(), 
			    			                   uploadResponse.getDataTransferStatus(),
			    			                   uploadResponse.getDataTransferType(),
			    			                   getSourceSize(uploadResponse.getDataTransferRequestId(), source, uploadResponse.getDataTransferType(),
				                                             dataObjectFile), 
			    			                   dataObjectRegistrationDTO.getCallerObjectId()); 
	
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
	    	
	    	    metadataService.updateDataObjectMetadata(path, dataObjectRegistrationDTO.getMetadataEntries()); 
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
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
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
		   return dataTransferService.getDataTransferSize(dataTransferType, requestId);
		}
		
		if(dataObjectFile != null) {
           return dataObjectFile.length();
		}
		
		return null;
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
}

 