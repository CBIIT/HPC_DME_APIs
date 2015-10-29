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

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.domain.model.HpcMetadataValidationRules;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * <p>
 * Validates various metadata provided by the user.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMetadataValidator
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Collection type attribute name.
	private final static String COLLECTION_TYPE_ATTRIBUTE = "Collection type"; 
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Metadata validation rules collection.
	private HpcMetadataValidationRules metadataValidationRules = new HpcMetadataValidationRules();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcMetadataValidator() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param metadataValidationRulesPath The path to the validation rules JSON.
     * 
     * @throws HpcException
     */
    private HpcMetadataValidator(String metadataValidationRulesPath) throws HpcException
    {
    	// TODO: move this to external JSON config.
/*
		JSONParser parser = new JSONParser();
		JSONObject json = new JSONObject();
		try {
	         FileReader reader = new FileReader(metadataValidationRulesPath);
	         json = (JSONObject) parser.parse(reader);
		} catch(FileNotFoundException e) {
		    logger.error("FileNotFoundException failed:", e);
		}catch(IOException e) {
		    logger.error("IOException failed:", e);
		}
		catch(ParseException e) {
		    logger.error("ParseException failed:", e);
		}*/
		
		
		
    	// Collection metadata validation rules.
    	HpcMetadataValidationRule collectionType = new HpcMetadataValidationRule();
    	collectionType.setAttribute("Collection type");
    	collectionType.setMandatory(true);
    	collectionType.setRuleEnabled(true);
    	collectionType.setDOC("DOC-NAME");
    	collectionType.getValidValues().addAll(Arrays.asList("Project", "Dataset"));
    	
    	HpcMetadataValidationRule projectName = new HpcMetadataValidationRule();
    	projectName.setAttribute("Project name");
    	projectName.setMandatory(true);
    	projectName.setRuleEnabled(true);
    	projectName.setDOC("DOC-NAME");
    	projectName.setCollectionType("Project");
    	
    	HpcMetadataValidationRule projectType = new HpcMetadataValidationRule();
    	projectType.setAttribute("Project type");
    	projectType.setDefaultValue("Unknown");
    	projectType.setMandatory(true);
    	projectType.setRuleEnabled(true);
    	projectType.setDOC("DOC-NAME");
    	projectType.setCollectionType("Project");
    	projectType.getValidValues().addAll(Arrays.asList("Unknown", "Sequencing", "Analysis", "Umbrella"));
    	
    	HpcMetadataValidationRule projectDescription = new HpcMetadataValidationRule();
    	projectDescription.setAttribute("Project description");
    	projectDescription.setMandatory(true);
    	projectDescription.setRuleEnabled(true);
    	projectDescription.setDOC("DOC-NAME");
    	projectDescription.setCollectionType("Project");
    	
    	HpcMetadataValidationRule internalProjectId = new HpcMetadataValidationRule();
    	internalProjectId.setAttribute("Internal Project ID");
    	internalProjectId.setMandatory(true);
    	internalProjectId.setRuleEnabled(true);
    	internalProjectId.setDOC("DOC-NAME");
    	internalProjectId.setCollectionType("Project");
    	
    	HpcMetadataValidationRule sourceLabPI = new HpcMetadataValidationRule();
    	sourceLabPI.setAttribute("Source Lab PI");
    	sourceLabPI.setMandatory(true);
    	sourceLabPI.setRuleEnabled(true);
    	sourceLabPI.setDOC("DOC-NAME");
    	sourceLabPI.setCollectionType("Project");
    	
    	HpcMetadataValidationRule labBranch = new HpcMetadataValidationRule();
    	labBranch.setAttribute("Lab / Branch Name");
    	labBranch.setMandatory(true);
    	labBranch.setRuleEnabled(true);
    	labBranch.setDOC("DOC-NAME");
    	labBranch.setCollectionType("Project");
    	
    	HpcMetadataValidationRule piDOC = new HpcMetadataValidationRule();
    	piDOC.setAttribute("DOC of the PI");
    	piDOC.setMandatory(true);
    	piDOC.setRuleEnabled(true);
    	piDOC.setDOC("DOC-NAME");
    	piDOC.setCollectionType("Project");
    	
    	HpcMetadataValidationRule projectDate = new HpcMetadataValidationRule();
    	projectDate.setAttribute("Original Date the Project was created on");
    	projectDate.setMandatory(true);
    	projectDate.setRuleEnabled(true);
    	projectDate.setDOC("DOC-NAME");
    	projectDate.setCollectionType("Project");
    	
    	HpcMetadataValidationRule userName = new HpcMetadataValidationRule();
    	userName.setAttribute("Name of the User registering the Project (Registrar)");
    	userName.setMandatory(true);
    	userName.setRuleEnabled(true);
    	userName.setDOC("DOC-NAME");
    	userName.setCollectionType("Project");
    	
    	HpcMetadataValidationRule registrarDOC = new HpcMetadataValidationRule();
    	registrarDOC.setAttribute("DOC of the Registrar");
    	registrarDOC.setMandatory(true);
    	registrarDOC.setRuleEnabled(true);
    	registrarDOC.setDOC("DOC-NAME");
    	registrarDOC.setCollectionType("Project");
    	
    	metadataValidationRules.getCollectionMetadataValidationRules().addAll(
    			Arrays.asList(collectionType, projectName, projectType, projectDescription,
    					      internalProjectId, sourceLabPI, labBranch, piDOC, projectDate, 
    					      userName, registrarDOC));
    	
    	// Data Object metadata validation rules.
    	HpcMetadataValidationRule datasetName = new HpcMetadataValidationRule();
    	datasetName.setAttribute("Dataset name");
    	datasetName.setMandatory(true);
    	datasetName.setRuleEnabled(true);
    	datasetName.setDOC("DOC-NAME");
    	
    	HpcMetadataValidationRule datasetDescription = new HpcMetadataValidationRule();
    	datasetDescription.setAttribute("Dataset description");
    	datasetDescription.setMandatory(true);
    	datasetDescription.setRuleEnabled(true);
    	datasetDescription.setDOC("DOC-NAME");
    	
    	HpcMetadataValidationRule sourceLabPI_DO = new HpcMetadataValidationRule();
    	sourceLabPI_DO.setAttribute("Source Lab PI");
    	sourceLabPI_DO.setMandatory(true);
    	sourceLabPI_DO.setRuleEnabled(true);
    	sourceLabPI_DO.setDOC("DOC-NAME");
    	
    	HpcMetadataValidationRule labBranch_DO = new HpcMetadataValidationRule();
    	labBranch_DO.setAttribute("Lab/Branch");
    	labBranch_DO.setMandatory(true);
    	labBranch_DO.setRuleEnabled(true);
    	labBranch_DO.setDOC("DOC-NAME");
    	
    	HpcMetadataValidationRule piDOC_DO = new HpcMetadataValidationRule();
    	piDOC_DO.setAttribute("DOC of the PI");
    	piDOC_DO.setMandatory(true);
    	piDOC_DO.setRuleEnabled(true);
    	piDOC_DO.setDOC("DOC-NAME");
    	
    	HpcMetadataValidationRule datasetCreationDate = new HpcMetadataValidationRule();
    	datasetCreationDate.setAttribute("Date the dataset was created Originally");
    	datasetCreationDate.setMandatory(true);
    	datasetCreationDate.setRuleEnabled(true);
    	datasetCreationDate.setDOC("DOC-NAME");
    	
    	HpcMetadataValidationRule creator = new HpcMetadataValidationRule();
    	creator.setAttribute("Data Creator");
    	creator.setDefaultValue("Not Specified");
    	creator.setMandatory(true);
    	creator.setRuleEnabled(true);
    	creator.setDOC("DOC-NAME");
    	
    	HpcMetadataValidationRule registrar = new HpcMetadataValidationRule();
    	registrar.setAttribute("Name of the User registering the dataset");
    	registrar.setMandatory(true);
    	registrar.setRuleEnabled(true);
    	registrar.setDOC("DOC-NAME");
    	
    	HpcMetadataValidationRule registrarDOC_DO = new HpcMetadataValidationRule();
    	registrarDOC_DO.setAttribute("DOC of the user registering the dataset");
    	registrarDOC_DO.setMandatory(true);
    	registrarDOC_DO.setRuleEnabled(true);
    	registrarDOC_DO.setDOC("DOC-NAME");
    	
    	HpcMetadataValidationRule phiContent = new HpcMetadataValidationRule();
    	phiContent.setAttribute("PHI Content");
    	phiContent.setMandatory(true);
    	phiContent.setRuleEnabled(true);
    	phiContent.setDOC("DOC-NAME");
    	phiContent.getValidValues().addAll(Arrays.asList("PHI Present", "PHI Not Present", "Not Specified"));
    	
    	HpcMetadataValidationRule piiContent = new HpcMetadataValidationRule();
    	piiContent.setAttribute("PII Content");
    	piiContent.setMandatory(true);
    	piiContent.setRuleEnabled(true);
    	piiContent.setDOC("DOC-NAME");
    	piiContent.getValidValues().addAll(Arrays.asList("PII Present", "PII Not Present", "Not Specified"));
    	
    	HpcMetadataValidationRule encryption = new HpcMetadataValidationRule();
    	encryption.setAttribute("Data Encryption Status");
    	encryption.setMandatory(true);
    	encryption.setRuleEnabled(true);
    	encryption.setDOC("DOC-NAME");
    	encryption.getValidValues().addAll(Arrays.asList("Encrypted", "Not Encrypted", "Not Specified"));
    	
    	HpcMetadataValidationRule compression = new HpcMetadataValidationRule();
    	compression.setAttribute("Data Compression Statuss");
    	compression.setMandatory(true);
    	compression.setRuleEnabled(true);
    	compression.setDOC("DOC-NAME");
    	compression.getValidValues().addAll(Arrays.asList("Compressed", "Not Compressed", "Not Specified"));
    	
    	HpcMetadataValidationRule fundingOrganization = new HpcMetadataValidationRule();
    	fundingOrganization.setAttribute("Organization Funding the generation of Data");
    	fundingOrganization.setDefaultValue("Not Specified");
    	fundingOrganization.setMandatory(true);
    	fundingOrganization.setRuleEnabled(true);
    	fundingOrganization.setDOC("DOC-NAME");
    	
    	metadataValidationRules.getDataObjectMetadataValidationRules().addAll(
    			Arrays.asList(datasetName, datasetDescription, sourceLabPI_DO,
    					      labBranch_DO, piDOC_DO, datasetCreationDate, creator, registrar,
    					      registrarDOC_DO, phiContent, piiContent, encryption, compression,
    					      fundingOrganization));
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Validate collection metadata. Null unit values are converted to empty strings.
     *
     * @param metadataEntries The metadata entries collection to validate.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    public void validateCollectionMetadata(List<HpcMetadataEntry> metadataEntries) 
    		                              throws HpcException
    {
    	validateMetadata(metadataEntries, 
    			         metadataValidationRules.getCollectionMetadataValidationRules());
    }
    
    /**
     * Validate data object metadata. Null unit values are converted to empty strings.
     *
     * @param metadataEntries The metadata entries collection to validate.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    public void validateDataObjectMetadata(List<HpcMetadataEntry> metadataEntries) 
    		                              throws HpcException
    {
    	validateMetadata(metadataEntries, 
    			         metadataValidationRules.getDataObjectMetadataValidationRules());
    }
    		                               
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Validate metadata. Null unit values are converted to empty strings.
     *
     * @param metadataEntries The metadata entries collection to validate.
     * @param metadataValidationRules Validation rules to apply.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    private void validateMetadata(List<HpcMetadataEntry> metadataEntries,
    		                      List<HpcMetadataValidationRule> metadataValidationRules) 
    		                     throws HpcException
    {
    	// Crate a metadata <attribute, value> map.
    	Map<String, String> metadataEntriesMap = new HashMap<String, String>();
    	for(HpcMetadataEntry metadataEntry : metadataEntries) {
    		metadataEntriesMap.put(metadataEntry.getAttribute(), metadataEntry.getValue());
    		// Default null unit values to empty string (This is an iRODS expectation).
    		if(metadataEntry.getUnit() == null) {
    		   metadataEntry.setUnit("");	
    		}
    	}
    	
	    for(HpcMetadataValidationRule metadataValidationRule: metadataValidationRules) {
	    	// Skip disabled rules.
	    	if(!metadataValidationRule.getRuleEnabled()) {
	    	   continue;
	    	}
	    
	        // Skip rules for other collection types.
	    	String collectionType = metadataEntriesMap.get(COLLECTION_TYPE_ATTRIBUTE);
	    	if(collectionType != null &&
	    	   metadataValidationRule.getCollectionType() != null &&
	    	   !metadataValidationRule.getCollectionType().isEmpty() &&
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

 