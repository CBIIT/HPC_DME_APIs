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

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidMetadataEntries;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.domain.model.HpcMetadataValidationRules;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcCollectionService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Management Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcCollectionServiceImpl implements HpcCollectionService
{    
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Collection type attribute name.
	private final static String COLLECTION_TYPE_ATTRIBUTE = "Collection type"; 
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
	// A collection of mandatory metadata entries.
	private HpcMetadataValidationRules metadataValidationRules = 
			                           new HpcMetadataValidationRules();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcCollectionServiceImpl()
    {
    	// TODO: move this to external JSON config.

    	// Collection metadata validation rules.
    	HpcMetadataValidationRule collectionType = new HpcMetadataValidationRule();
    	collectionType.setAttribute("Collection type");
    	collectionType.setMandatory(true);
    	collectionType.setRuleEnabled(true);
    	collectionType.setDOC("DOC-NAME");
    	collectionType.getValidValues().addAll(Arrays.asList("project", "dataset"));
    	
    	HpcMetadataValidationRule projectName = new HpcMetadataValidationRule();
    	projectName.setAttribute("Project name");
    	projectName.setMandatory(true);
    	projectName.setRuleEnabled(true);
    	projectName.setDOC("DOC-NAME");
    	projectName.setCollectionType("project");
    	
    	HpcMetadataValidationRule projectType = new HpcMetadataValidationRule();
    	projectType.setAttribute("Project type");
    	projectType.setDefaultValue("Unspecified");
    	projectType.setMandatory(true);
    	projectType.setRuleEnabled(true);
    	projectType.setDOC("DOC-NAME");
    	projectType.setCollectionType("project");
    	
    	HpcMetadataValidationRule projectDescription = new HpcMetadataValidationRule();
    	projectDescription.setAttribute("Project description");
    	projectDescription.setMandatory(true);
    	projectDescription.setRuleEnabled(true);
    	projectDescription.setDOC("DOC-NAME");
    	projectDescription.setCollectionType("project");
    	
    	HpcMetadataValidationRule internalProjectId = new HpcMetadataValidationRule();
    	internalProjectId.setAttribute("Internal Project ID");
    	internalProjectId.setMandatory(true);
    	internalProjectId.setRuleEnabled(true);
    	internalProjectId.setDOC("DOC-NAME");
    	internalProjectId.setCollectionType("project");
    	
    	HpcMetadataValidationRule sourceLabPI = new HpcMetadataValidationRule();
    	sourceLabPI.setAttribute("Source Lab PI");
    	sourceLabPI.setMandatory(true);
    	sourceLabPI.setRuleEnabled(true);
    	sourceLabPI.setDOC("DOC-NAME");
    	sourceLabPI.setCollectionType("project");
    	
    	HpcMetadataValidationRule labBranch = new HpcMetadataValidationRule();
    	labBranch.setAttribute("Lab / Branch Name");
    	labBranch.setMandatory(true);
    	labBranch.setRuleEnabled(true);
    	labBranch.setDOC("DOC-NAME");
    	labBranch.setCollectionType("project");
    	
    	HpcMetadataValidationRule piDOC = new HpcMetadataValidationRule();
    	piDOC.setAttribute("DOC of the PI");
    	piDOC.setMandatory(true);
    	piDOC.setRuleEnabled(true);
    	piDOC.setDOC("DOC-NAME");
    	piDOC.setCollectionType("project");
    	
    	HpcMetadataValidationRule projectDate = new HpcMetadataValidationRule();
    	projectDate.setAttribute("Date the Project was created");
    	projectDate.setMandatory(true);
    	projectDate.setRuleEnabled(true);
    	projectDate.setDOC("DOC-NAME");
    	projectDate.setCollectionType("project");
    	
    	HpcMetadataValidationRule userName = new HpcMetadataValidationRule();
    	userName.setAttribute("Name of the User registering the Project (Registrar)");
    	userName.setMandatory(true);
    	userName.setRuleEnabled(true);
    	userName.setDOC("DOC-NAME");
    	userName.setCollectionType("project");
    	
    	HpcMetadataValidationRule registrarDOC = new HpcMetadataValidationRule();
    	registrarDOC.setAttribute("DOC of the Registrar");
    	registrarDOC.setMandatory(true);
    	registrarDOC.setRuleEnabled(true);
    	registrarDOC.setDOC("DOC-NAME");
    	registrarDOC.setCollectionType("project");
    	
    	HpcMetadataValidationRule otherProjects = new HpcMetadataValidationRule();
    	otherProjects.setAttribute("Link to other projects");
    	otherProjects.setMandatory(true);
    	otherProjects.setRuleEnabled(true);
    	otherProjects.setDOC("DOC-NAME");
    	otherProjects.setCollectionType("project");
    	
    	metadataValidationRules.getCollectionMetadataValidationRules().addAll(
    			Arrays.asList(collectionType, projectName, projectType, projectDescription,
    					      internalProjectId, sourceLabPI, labBranch, piDOC, projectDate, 
    					      userName, registrarDOC, otherProjects));
    	
    	// File metadata validation rules.
    	HpcMetadataValidationRule piiContent = new HpcMetadataValidationRule();
    	piiContent.setAttribute("PII Content");
    	piiContent.setMandatory(true);
    	piiContent.setRuleEnabled(true);
    	piiContent.setDOC("DOC-NAME");
    	piiContent.getValidValues().addAll(Arrays.asList("true", "false"));
    	
    	metadataValidationRules.getFileMetadataValidationRules().addAll(Arrays.asList(piiContent));
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataManagementService Interface Implementation
    //---------------------------------------------------------------------//  

    @Override
    public void createDirectory(String path) throws HpcException
    {
    	dataManagementProxy.createCollectionDirectory(path);
    }

    @Override
    public void addMetadata(String path, List<HpcMetadataEntry> metadataEntries) 
    		               throws HpcException
    {
       	// Input validation.
       	if(path == null || !isValidMetadataEntries(metadataEntries)) {
       	   throw new HpcException("Null path or Invalid metadata entry", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Validate Metadata.
       	validateMetadata(metadataEntries);
       	
       	dataManagementProxy.addMetadataToCollection(path, metadataEntries);
    }

    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  

    /**
     * Validate metadata. Null unit values are converted to empty strings.
     *
     * @param metadataEntries The metadata entries collection to validate.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    private void validateMetadata(List<HpcMetadataEntry> metadataEntries) 
    		                     throws HpcException
    {
    	String collectionType = null;
    	
    	// Crate a metadata <attribute, value> map.
    	Map<String, String> metadataEntriesMap = new HashMap<String, String>();
    	for(HpcMetadataEntry metadataEntry : metadataEntries) {
    		metadataEntriesMap.put(metadataEntry.getAttribute(), metadataEntry.getValue());
    		// Default null unit values to empty string (This is an iRODS expectation).
    		if(metadataEntry.getUnit() == null) {
    		   metadataEntry.setUnit("");	
    		}
    		// Identify the collection type.
    		if(metadataEntry.getAttribute().equals(COLLECTION_TYPE_ATTRIBUTE)) {
    		   collectionType = metadataEntry.getValue();	
    		}
    	}
    	
    	// Validate collection type metadata is provided.
    	if(collectionType == null) {
    	   throw new HpcException("Missing mandataory metadata: " + 
    	                          COLLECTION_TYPE_ATTRIBUTE,
                                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
	    for(HpcMetadataValidationRule metadataValidationRule: 
	    	metadataValidationRules.getCollectionMetadataValidationRules()) {
	    	// Skip disabled rules.
	    	if(!metadataValidationRule.getRuleEnabled()) {
	    	   continue;
	    	}
	    
	        // Skip rules for other collection types.
	    	if(metadataValidationRule.getCollectionType() != null &&
	    	   !metadataValidationRule.getCollectionType().equals(collectionType)) {
	    	   continue;
	    	}
	    	
	    	// Apply default value/unit if default is defined and metadata was not provided.
	    	if(metadataValidationRule.getDefaultValue() != null &&
	    	   !metadataValidationRule.getDefaultValue().isEmpty()) {
	    	   if(!metadataEntriesMap.containsKey(metadataValidationRule.getAttribute())) {
				  HpcMetadataEntry defaultMetadataEntry = new HpcMetadataEntry();
				  defaultMetadataEntry.setAttribute(metadataValidationRule.getAttribute());
				  defaultMetadataEntry.setValue(metadataValidationRule.getDefaultValue());
				  defaultMetadataEntry.setUnit(metadataValidationRule.getDefaultUnit() != null ?
						                       metadataValidationRule.getDefaultUnit() : "");
				  metadataEntries.add(defaultMetadataEntry);
				  metadataEntriesMap.put(defaultMetadataEntry.getAttribute(), defaultMetadataEntry.getValue());
			   }
	    	}
	    	
	    	// Validate a mandatory metadata is provided.
	    	if(metadataValidationRule.getMandatory()) {
	    	   if(!metadataEntriesMap.containsKey(metadataValidationRule.getAttribute())) {	
				  // Metadata entry is missing, but no default is defined.
			      throw new HpcException("Missing mandataory metadata: " + 
			    		                 metadataValidationRule.getAttribute(), 
			                             HpcErrorType.INVALID_REQUEST_INPUT);
			   }
			}
	    	
	    	// Validate the metadata value is valid.
	    	if(metadataValidationRule.getValidValues() != null &&
	    	   !metadataValidationRule.getValidValues().isEmpty()) {
	    	   String value = metadataEntriesMap.get(metadataValidationRule.getAttribute());
	    	   if(metadataEntriesMap.containsKey(metadataValidationRule.getAttribute()) &&
	    		  !metadataValidationRule.getValidValues().contains(value)) {
	    		  throw new HpcException("Invalid Metadata Value: " + 
	    		                         metadataValidationRule.getAttribute() + " = " + 
	    				                 value + ". Valid values: " +
  		                                 metadataValidationRule.getValidValues(), 
                                         HpcErrorType.INVALID_REQUEST_INPUT);
	    	   }
	    	}
		}
    }    	

}

 