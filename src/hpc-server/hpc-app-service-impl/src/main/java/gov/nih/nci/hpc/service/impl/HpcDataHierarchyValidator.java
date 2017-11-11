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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * Validates data registration path against defined hierarchy for a given data management configuration.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataHierarchyValidator
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Data management configuration locator.
	@Autowired
	private HpcDataManagementConfigurationLocator dataManagementConfigurationLocator = null;
	
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
     * Validate a collection path against a hierarchy definition for a given data management configuration.
     *
     * @param configurationId The data management configuration ID.
     * @param collectionPathTypes The collection types on the path. e.g /Project/Dataset/Folder.
     * @param dataObjectRegistration If set to true, the validation will check if a data object is allowed
     *                               to be registered to this path.
     * 
     * @throws HpcException If the hierarchy is invalid.
     */
    public void validateHierarchy(String configurationId, List<String> collectionPathTypes, 
    		                      boolean dataObjectRegistration) 
    		                     throws HpcException
    {
    	HpcDataManagementConfiguration dataManagementConfiguration = 
    	                               dataManagementConfigurationLocator.get(configurationId);
    	if(dataManagementConfiguration == null) {
    	   throw new HpcException("Invalid Data Management Configuration: " + configurationId, 
    			                  HpcRequestRejectReason.INVALID_DOC);
    	}
    	
    	HpcDataHierarchy dataHierarchy = dataManagementConfiguration.getDataHierarchy();
  		if(dataHierarchy == null) {
    	   // No hierarchy definition is configured, so validation is not needed.
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
    		   throw new HpcException("Invalid collection hierarchy for: " +
    				                  dataManagementConfiguration.getBasePath() + 
    				                  ". collection hirarchy: " 
    		                          + toString(collectionPathTypes) +
    				                  ". hierarchy definition: " + toString(dataHierarchy), 
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
	
    /**
     * Return a string from data hierarchy. 
     * 
     * @param dataHierarchy The data hierarchy.
     * @return a pretty string. 
     */
	private String toString(HpcDataHierarchy dataHierarchy) 
	{
		List<String> dataHierarchyPaths = new ArrayList<>();
		String path = "/";
		
		// Traverse the data hierarchy model and create a list of hierarchy paths.
		toDataHierarchyPaths(dataHierarchyPaths, path, dataHierarchy);

		return dataHierarchyPaths.toString();
	}
	
    /**
     * create a list of data hierarchy paths.
     * 
     * @param dataHierarchyPaths The list of paths to populate.
     * @param path The current path traversed in the hierarchy.
     * @param dataHierarchy The data hierarchy
     */
	private void toDataHierarchyPaths(List<String> dataHierarchyPaths, String path,  
			                          HpcDataHierarchy dataHierarchy) 
	{
		String visitedPath = path + "/" + dataHierarchy.getCollectionType();
		if(dataHierarchy.getSubCollectionsHierarchies().isEmpty()) {
		   // This is a leaf in the data hierarchy - add this path
		   dataHierarchyPaths.add(visitedPath);
		   return;
		   
		} else {
			    // This is not a leaf in the hierarchy. Traverse the childs.
			    dataHierarchy.getSubCollectionsHierarchies().forEach(
			    	subCollectionDataHierarchy -> toDataHierarchyPaths(dataHierarchyPaths, 
			    			                                           visitedPath, 
			    			                                           subCollectionDataHierarchy));
		}
	}
}

 
