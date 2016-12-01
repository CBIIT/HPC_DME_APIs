/**
 * HpcMetadataValidator.java
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
import gov.nih.nci.hpc.exception.HpcException;

import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * <p>
 * Validates various metadata provided by the user.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcMetadataValidator.java 1522 2016-10-13 14:56:28Z rosenbergea $
 */

public class HpcDataHierarchyValidator
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Data Hierarchy .definitions per DOC.
	Map<String, HpcDataHierarchy> dataHierarchyDefinitions = new HashMap<>();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    @SuppressWarnings("unused")
	private HpcDataHierarchyValidator() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param dataHierarchyDefinitionsPath The path to the data hierarchy definitions JSON.
     * @throws HpcException If the data hierarchy definition is invalid.
     */
    @SuppressWarnings("unchecked")
    public HpcDataHierarchyValidator(String dataHierarchyDefinitionsPath) throws HpcException
    {
    	// Load the data hierarchy definitions from the JSON config file.
    	JSONArray jsonDataHierarchyDefinitions = 
	     		      getDataHierarchyDefinitionsJSON(dataHierarchyDefinitionsPath);
    	if(jsonDataHierarchyDefinitions == null) {
       	   throw new HpcException("No data hierarchy definitions found",
                                  HpcErrorType.SPRING_CONFIGURATION_ERROR); 
        }
    	
		Iterator<JSONObject> dataHierarchyIterator = jsonDataHierarchyDefinitions.iterator();
	    while(dataHierarchyIterator.hasNext()) {
	          JSONObject jsonDataHierarchy = dataHierarchyIterator.next();
	          
	          // Map Data Hierarchy JSON to a domain object.
	          HpcDataHierarchy dataHierarchy = dataHierarchyFromJSON(jsonDataHierarchy);
	          
	          // Associate data hierarchy to DOC 
	          JSONArray jsonDOC = (JSONArray) jsonDataHierarchy.get("DOC");
	    	  if(jsonDOC != null) {
		    	 Iterator<String> docIterator = jsonDOC.iterator();
		    	 while(docIterator.hasNext()) {
		    		   dataHierarchyDefinitions.put(docIterator.next(), dataHierarchy);
		    	 }
		    	 
	    	  } else {
	    		      throw new HpcException("Invalid Data Hierarchy Definition: " + jsonDataHierarchy,
                                             HpcErrorType.SPRING_CONFIGURATION_ERROR);
	    	  }
	    }
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
    	HpcDataHierarchy dataHierarchy = getDataHierarchy(doc);
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
    		   throw new HpcException("Invalid collection hierarchy", 
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
     * Get a data hierarchy for a DOC.
     *
     * @param doc The DOC.
     * @return HpcDataHierarchy
     */
    public HpcDataHierarchy getDataHierarchy(String doc) 
    {
    	return dataHierarchyDefinitions.get(doc);
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Instantiate a HpcDataHierarchy from JSON.
     *
     * @param jsonDataHierarchy The data hierarchy JSON object. 
     * @return HpcDataHierarchy
     * 
     * @throws HpcException If failed to parse the JSON.
     */
    @SuppressWarnings("unchecked")
	private HpcDataHierarchy dataHierarchyFromJSON(JSONObject jsonDataHierarchy) 
    		                                      throws HpcException
    {
		HpcDataHierarchy dataHierarchy = new HpcDataHierarchy();
		
		if(!jsonDataHierarchy.containsKey("collectionType") ||
		   !jsonDataHierarchy.containsKey("isDataObjectContainer")) {
 	       throw new HpcException("Invalid Data Hierarchy Definition: " + jsonDataHierarchy,
 	                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
		}
		
		dataHierarchy.setCollectionType((String)jsonDataHierarchy.get("collectionType"));
		dataHierarchy.setIsDataObjectContainer((Boolean)jsonDataHierarchy.get("isDataObjectContainer"));
    	
    	// Iterate through the sub collections.
	    JSONArray jsonSubCollections = (JSONArray) jsonDataHierarchy.get("subCollections");
  	    if(jsonSubCollections != null) {
		   Iterator<JSONObject> subCollectionsIterator = jsonSubCollections.iterator();
	       while(subCollectionsIterator.hasNext()) {
	    	     dataHierarchy.getSubCollectionsHierarchies().add(dataHierarchyFromJSON(subCollectionsIterator.next()));
	       }
  	    }
  	    
  	    return dataHierarchy;
    }
    	
    /**
     * Load the data hierarchy definitions from file.
     * 
     * @param dataHierarchyDefinitionsPath The path to the data hierarchy definitions JSON.
     * @return a JSON object with the data hierarchy definitions
     * @throws HpcException If failed to open or parse JSON.
     */
	private JSONArray getDataHierarchyDefinitionsJSON(String dataHierarchyDefinitionsPath) 
			                                         throws HpcException
    {
		try {
	         FileReader reader = new FileReader(dataHierarchyDefinitionsPath);
	         return (JSONArray) ((JSONObject) new JSONParser().parse(reader)).get("HpcDataHierarchyDefinitions");
	         
		} catch(Exception e) {
		        throw new HpcException("Could not open or parse: " + dataHierarchyDefinitionsPath,
                                       HpcErrorType.SPRING_CONFIGURATION_ERROR, e);
		}
    }
}

 