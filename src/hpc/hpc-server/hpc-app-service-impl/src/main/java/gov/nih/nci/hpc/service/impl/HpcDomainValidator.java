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

import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
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

public class HpcDomainValidator 
{   
    //---------------------------------------------------------------------//
    // Class members
    //---------------------------------------------------------------------//

    // The logger instance.
	private static final Logger logger = 
			LoggerFactory.getLogger(HpcDomainValidator.class.getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default constructor is disabled.
     * 
     */
    private HpcDomainValidator()
    {
    }
    
    //---------------------------------------------------------------------//
    // Security Domain Object Types Validators
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
    	   !isValidIntegratedSystemAccount(user.getDataManagementAccount())) {
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
    	if(nciAccount == null || nciAccount.getUserId() == null) {
    	   return false;	
    	}
    	
    	if(nciAccount.getFirstName() == null || 
 	       nciAccount.getLastName() == null ||
 	       nciAccount.getDOC() == null) {
    	   return false;
    	}
    	
    	return true;
    }  
    
    /**
     * Validate Integrated System Account object.
     *
     * @param dataTransferAccount the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidIntegratedSystemAccount(
    		                HpcIntegratedSystemAccount integratedSystemAccount) 
    {
    	if(integratedSystemAccount == null || 
    	   integratedSystemAccount.getUsername() == null || 
    	   integratedSystemAccount.getPassword() == null ||
    	   integratedSystemAccount.getIntegratedSystem() == null) {
    	   logger.info("Invalid Integrated System Account: " + integratedSystemAccount);
    	   return false;
    	}
    	return true;
    }  
	 
    //---------------------------------------------------------------------//
    // Data Management Domain Object Types Validators
    //---------------------------------------------------------------------//
    
    /**
     * Validate a file location object.
     *
     * @param location the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidFileLocation(HpcFileLocation location) 
    {
    	if(location == null || location.getFileContainerId() == null ||
    	   location.getFileId() == null) {
     	   logger.info("Invalid File Location: " + location);
     	   return false;
    	}
    	return true;
    }  
    
    //---------------------------------------------------------------------//
    // Metadata Domain Object Types Validators
    //---------------------------------------------------------------------//
    
    /**
     * Validate metadata entry collection.
     *
     * @param metadataEntries Metadata entry collection.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidMetadataEntries(List<HpcMetadataEntry> metadataEntries) 
    {
    	if(metadataEntries == null) {
    	   return false;
     	}
    	for(HpcMetadataEntry metadataEntry : metadataEntries) {
    		if(isEmpty(metadataEntry.getAttribute()) || 
    		   isEmpty(metadataEntry.getValue())) {
    		   return false;
    		}
    	}
     	return true;
    }
    
    /**
     * Validate metadata query collection.
     *
     * @param metadataQueries Metadata query collection.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidMetadataQueries(List<HpcMetadataQuery> metadataQueries) 
    {
    	if(metadataQueries == null) {
    	   return false;
     	}
    	for(HpcMetadataQuery metadataQuery : metadataQueries) {
    		if(isEmpty(metadataQuery.getAttribute()) || 
    		   isEmpty(metadataQuery.getValue()) ||
    		   isEmpty(metadataQuery.getOperator())) {
    		   return false;
    		}
    	}
     	return true;
    }
    
    //---------------------------------------------------------------------//
    // Notification Domain Object Types Validators
    //---------------------------------------------------------------------//
    
    /**
     * Validate a notification subscription object.
     *
     * @param notificationSubscription the object to be validated.
     * @return true if valid, false otherwise.
     */
    public static boolean isValidNotificationSubscription(HpcNotificationSubscription notificationSubscription) 
    {
    	if(notificationSubscription == null || 
    	   notificationSubscription.getNotificationType() == null ||
    	   notificationSubscription.getNotificationDeliveryMethods() == null ||
    	   notificationSubscription.getNotificationDeliveryMethods().isEmpty() ||
    	   notificationSubscription.getNotificationDeliveryMethods().contains(null)) {
     	   return false;
    	}
    	
    	
    
    	return true;
    }  
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Check is a string is empty.
     *
     * @param value The string to check
     * @return true if not null and not empty, false otherwise.
     */
    private static boolean isEmpty(String value) 
    {
    	return value == null ? false : value.isEmpty();
    }
}

 