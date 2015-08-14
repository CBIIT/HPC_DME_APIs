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
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcNihAccount;
import gov.nih.nci.hpc.exception.HpcException;

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
    	   !isValidNihAccount(user.getNihAccount()) ||
    	   !isValidDataTransferAccount(user.getDataTransferAccount())) {
    	   logger.info("Invalid User: " + user);
    	   return false;
    	}
    	return true;
    }  
    
    /**
     * Validate NIH Account object.
     *
     * @param nihAccount the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidNihAccount(HpcNihAccount nihAccount) 
    {
    	if(nihAccount == null || nihAccount.getUserId() == null || 
 	       nihAccount.getFirstName() == null || 
 	       nihAccount.getLastName() == null) {
    	   logger.info("Invalid NIH Account: " + nihAccount);
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
    	   !isValidDatasetPrimaryMetadata(request.getMetadata())) {
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
     * Validate a dataset primary metadata object.
     *
     * @param metadata the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidDatasetPrimaryMetadata(HpcFilePrimaryMetadata metadata) 
    {
    	if(metadata == null ||
    	   metadata.getDataContainsPII() == null || 	
    	  // metadata.getDataContainsPHI() == null ||
    	   metadata.getDataEncrypted() == null ||
    	   //metadata.getDataCompressed() == null ||
    	   metadata.getFundingOrganization() == null || 
    	   metadata.getPrimaryInvestigatorNihUserId() == null ||
    	   metadata.getCreatorName() == null ||
    	   metadata.getRegistrarNihUserId() == null ||
    	   metadata.getDescription() == null ||
    	   metadata.getLabBranch() == null) {
    	   logger.info("Invalid Dataset Primary Metadata");
     	   return false;
    	}
    	return true;
    }  
    
    /**
     * Validate a project metadata object
     *
     * @param metadata the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidProjectMetadata(HpcProjectMetadata metadata) throws HpcException
    {
    	if(metadata == null ||
    	   metadata.getName() == null ||
    	   metadata.getPrimaryInvestigatorNihUserId() == null ||
    	   metadata.getRegistrarNihUserId() == null ||
    	   metadata.getLabBranch() == null ||
    	   metadata.getDoc() == null ||
    	   metadata.getInternalProjectId() == null ||
   	   	   metadata.getFundingOrganization() == null ||
   	   	   metadata.getExperimentId() == null) { 
    	   logger.info("Invalid Project Metadata");
      	   return false;
     	}
     	return true;
    }
}

 