/**
 * HpcDataHierarchyValidator.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcDocConfiguration;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Validates data registration path against defined hierarchy for DOC.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataHierarchyValidator
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// DOC configuration locator.
	@Autowired
	private HpcDocConfigurationLocator docConfigurationLocator = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     */
	private HpcDataHierarchyValidator()
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Validate a collection path against a hierarchy definition for the DOC.
     *
     * @param doc The DOC.
     * @param collectionPathTypes The collection types on the path. e.g /Project/Dataset/Folder.
     * @param dataObjectRegistration If set to true, the validation will check if a data object is allowed
     *                               to be registered to this path.
     * 
     * @throws HpcException If the hierarchy is invalid.
     */
    public void validateHierarchy(String doc, List<String> collectionPathTypes, 
    		                      boolean dataObjectRegistration) 
    		                     throws HpcException
    {
    	HpcDocConfiguration docConfiguration = docConfigurationLocator.get(doc);
    	if(docConfiguration == null) {
    	   throw new HpcException("Invalid DOC: " + doc, HpcRequestRejectReason.INVALID_DOC);
    	}
    	
    	HpcDataHierarchy dataHierarchy = docConfiguration.getDataHierarchy();
  		if(dataHierarchy == null) {
    	   // No hierarchy definition found for the DOC, so validation is not needed.
    	   return;
    	}
    	
    	List<HpcDataHierarchy> subCollectionsHierarchies = Arrays.asList(dataHierarchy);
    	boolean isDataObjectContainer = false;
    	
    	// Iterate through the collection path types, and validate against the hierarchy definition.
    	for(String collectionType : collectionPathTypes) {
    		boolean collectionTypeValidated = false;
    		for(HpcDataHierarchy collectionHierarchy : subCollectionsHierarchies) {
    			if(collectionHierarchy.getCollectionType().equals(collectionType)) {
    			   collectionTypeValidated = true;
    			   subCollectionsHierarchies = collectionHierarchy.getSubCollectionsHierarchies();
    			   isDataObjectContainer = collectionHierarchy.getIsDataObjectContainer();
    			   break;
    			}
    		}
    		
    		if(!collectionTypeValidated) {
    		   throw new HpcException("Invalid collection hierarchy for DOC: " + doc +
    				                  ". collection hirarchy: " + toString(collectionPathTypes) +
    				                  ". hierarchy definition: " + dataHierarchy, 
    				                  HpcErrorType.INVALID_REQUEST_INPUT);
    		}
    	}   
    	
    	// Validate if data object registration is allowed to this path.
    	if(dataObjectRegistration && !isDataObjectContainer) {
    	   throw new HpcException("Data object is not allowed in this collection", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    }
    
    /**
     * Return a string from path types.
     * 
     * @param collectionPathTypes The path types.
     * @return a pretty string. 
     */
	private String toString(List<String> collectionPathTypes) 
	{
		StringBuilder collectionPathTypesStr = new StringBuilder();
		for(String pathType : collectionPathTypes) {
			collectionPathTypesStr.append("/" + pathType);
		}
		
		return collectionPathTypesStr.toString();
	}
}

 
