/**
 * HpcDomainValidator.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Helper class to validate domain objects.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

class HpcDomainValidator 
{   
    //---------------------------------------------------------------------//
    // Class members
    //---------------------------------------------------------------------//

    // The logger instance.
	private static final Logger logger = 
			LoggerFactory.getLogger(HpcDomainValidator.class.getName());
	
    //---------------------------------------------------------------------//
    // User Domain Object Types Validators
    //---------------------------------------------------------------------//
	
    /**
     * Validate User object.
     *
     * @param user The object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidUser(HpcUser user) 
    {
    	if(user == null || 
    	   !isValidNciAccount(user.getNciAccount()) ||
    	   !isValidDataTransferAccount(user.getDataTransferAccount())) {
    	   logger.info("Invalid User: " + user);
    	   return false;
    	}
    	return true;
    }  
    
    /**
     * Validate NCI Account object.
     *
     * @param nciAccount the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidNciAccount(HpcNciAccount nciAccount) 
    {
    	if(nciAccount == null || nciAccount.getUserId() == null || 
 	       nciAccount.getFirstName() == null || 
 	       nciAccount.getLastName() == null) {
    	   logger.info("Invalid NCI Account: " + nciAccount);
    	   return false;
    	}
    	return true;
    }  
    
    /**
     * Validate Data Transfer Account object.
     *
     * @param dataTransferAccount the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidDataTransferAccount(
    		                HpcDataTransferAccount dataTransferAccount) 
    {
    	if(dataTransferAccount == null || 
    	   dataTransferAccount.getUsername() == null || 
    	   dataTransferAccount.getPassword() == null ||
    	   dataTransferAccount.getAccountType() == null) {
    	   logger.info("Invalid Data Transfer Account: " + dataTransferAccount);
    	   return false;
    	}
    	return true;
    }  
	 
    //---------------------------------------------------------------------//
    // Dataset Domain Object Types Validators
    //---------------------------------------------------------------------//
    /**
     * Validate a file upload request object.
     *
     * @param file the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidFileUploadRequest(HpcFileUploadRequest request) 
    {
    	if(request.getType() == null || request.getLocations() == null ||
    	   !isValidFileLocation(request.getLocations().getSource()) ||
    	   !isValidFileLocation(request.getLocations().getDestination()) ||
    	   !isValidFilePrimaryMetadata(request.getMetadata())) {
    	   logger.info("Invalid File Upload Request: " + request);
    	   return false;
    	}
    	return true;
    }  
    
    /**
     * Validate a file location object.
     *
     * @param location the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidFileLocation(HpcFileLocation location) 
    {
    	if(location == null || location.getEndpoint() == null ||
    	   location.getPath() == null) {
     	   logger.info("Invalid File Location: " + location);
     	   return false;
    	}
    	return true;
    }  
    
    /**
     * Validate a Data Transfer Locations object.
     *
     * @param locations the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidDataTransferLocations(
    		                         HpcDataTransferLocations locations) 
    {
    	if(locations == null || !isValidFileLocation(locations.getSource()) ||
    	   !isValidFileLocation(locations.getDestination())) {
     	   logger.info("Invalid Data Transfer Locations: " + locations);
     	   return false;
    	}
    	return true;
    }  
    
    //---------------------------------------------------------------------//
    // Metadata Domain Object Types Validators
    //---------------------------------------------------------------------//
    
    /**
     * Validate a file primary metadata object.
     *
     * @param metadata the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidFilePrimaryMetadata(HpcFilePrimaryMetadata metadata) 
    {
    	// All fields are mandatory except 'funding organization'.
    	if(metadata == null ||
    	   metadata.getDataContainsPII() == null || 	
    	   metadata.getDataContainsPHI() == null ||
    	   metadata.getDataEncrypted() == null ||
    	   metadata.getDataCompressed() == null ||
    	   metadata.getPrincipalInvestigatorNciUserId() == null ||
    	   metadata.getCreatorName() == null ||
    	   metadata.getRegistrarNciUserId() == null ||
    	   metadata.getDescription() == null ||
    	   metadata.getLabBranch() == null ||
    	   metadata.getPrincipalInvestigatorDOC() == null ||
    	   metadata.getRegistrarDOC() == null ||
    	   metadata.getOriginallyCreated() == null ||
    	   (metadata.getMetadataItems() != null && 
  	   	    !isValidMetadataItems(metadata.getMetadataItems()))) {
    	   logger.info("Invalid Dataset Primary Metadata");
     	   return false;
    	}
    	return true;
    }  
    
    /**
     * Check if a file primary metadata object is 'empty'.
     *
     * @param metadata the object to be checked.
     * @return true if empty, false otherwise.
     */
    public static boolean isEmptyFilePrimaryMetadata(HpcFilePrimaryMetadata metadata) 
    {    
    	if(metadata == null ||
	       (metadata.getDataContainsPII() == null && 	
	    	metadata.getDataContainsPHI() == null &&
	    	metadata.getDataEncrypted() == null &&
	    	metadata.getDataCompressed() == null &&
	    	metadata.getFundingOrganization() == null && 
	    	metadata.getPrincipalInvestigatorNciUserId() == null &&
	    	metadata.getCreatorName() == null &&
	    	metadata.getRegistrarNciUserId() == null &&
	    	metadata.getDescription() == null &&
	    	metadata.getLabBranch() == null &&
	    	metadata.getPrincipalInvestigatorDOC() == null &&
	    	metadata.getRegistrarDOC() == null &&
	    	metadata.getOriginallyCreated() == null &&
	    	(metadata.getMetadataItems() == null ||
	    	 metadata.getMetadataItems().isEmpty()))) {
	    	 return true;
	    	}
    		
    		return false;
    }
    
    /**
     * Validate a project metadata object
     *
     * @param metadata the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidProjectMetadata(HpcProjectMetadata metadata) 
    {
    	if(metadata == null ||
    	   metadata.getName() == null ||
    	   metadata.getType() == null ||
    	   metadata.getInternalProjectId() == null ||
    	   metadata.getPrincipalInvestigatorNciUserId() == null ||
    	   metadata.getRegistrarNciUserId() == null ||
    	   metadata.getLabBranch() == null ||
    	   metadata.getPrincipalInvestigatorDOC() == null ||
   	   	   metadata.getRegistrarDOC() == null ||
   	   	   metadata.getDescription() == null ||
   	   	   (metadata.getMetadataItems() != null && 
   	   	    !isValidMetadataItems(metadata.getMetadataItems()))) { 
    	   logger.info("Invalid Project Metadata");
      	   return false;
     	}
     	return true;
    }
    
    /**
     * Check if a roject metadata object is 'empty'.
     *
     * @param metadata the object to be checked.
     * @return true if empty, false otherwise.
     */
    public static boolean isEmptyProjectMetadata(HpcProjectMetadata metadata) 
    {    
    	if(metadata == null ||
    	   (metadata.getName() == null &&
    	    metadata.getType() == null &&
    		metadata.getInternalProjectId() == null &&
    		metadata.getPrincipalInvestigatorNciUserId() == null &&
    	    metadata.getRegistrarNciUserId() == null &&
    	    metadata.getLabBranch() == null &&
    	    metadata.getPrincipalInvestigatorDOC() == null &&
    	    metadata.getCreated() == null &&
    	    metadata.getRegistrarDOC() == null && 
    		metadata.getDescription() == null &&
    		(metadata.getMetadataItems() == null || 
    		 metadata.getMetadataItems().isEmpty()))) {
    		return true;
	    }
    	
    	return false;
    }
    
    /**
     * Validate metadata items collection
     *
     * @param metadataItems Metadata items collection
     * @return true if valid, false otherwise.
     */
    public static boolean isValidMetadataItems(List<HpcMetadataItem> metadataItems) 
    {
    	if(metadataItems == null) {
    	   return false;
     	}
    	for(HpcMetadataItem metadataItem : metadataItems) {
    		if(metadataItem.getKey() == null ||
    		   metadataItem.getValue() == null) {
    		   return false;
    		}
    	}
     	return true;
    }
    
    /**
     * Check if 2 lists of metadata items overlaps
     *
     * @param listA List of metadata items.
     * @param listB List of metadata items.
     * @return true if the list overlaps (i.e. has at least one common key).
     */
    public static boolean isOverlapping(List<HpcMetadataItem> listA, 
    		                            List<HpcMetadataItem> listB)
    {
    	for(HpcMetadataItem itemA : listA) {
    		for(HpcMetadataItem itemB : listB) {
    			if(itemB.getKey().equals(itemA.getKey())) {
    			   return true;
    			}
    		}
    	}
    	
    	return false;
    }  
}

 