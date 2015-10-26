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
import gov.nih.nci.hpc.domain.metadata.HpcCollectionTypeMandatoryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMandatoryMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcMandatoryMetadata;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcCollectionService;

import java.util.ArrayList;
import java.util.List;

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
	private final static String COLLECTION_TYPE_ATTRIBUTE = "type"; 
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
	// A collection of mandatory metadata entries.
	private HpcMandatoryMetadata mandatoryMetadata = new HpcMandatoryMetadata();
	
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

    	// Common mandatory metadata
    	HpcMandatoryMetadataEntry type = new HpcMandatoryMetadataEntry();
    	type.setAttribute("type");
    	mandatoryMetadata.getMandatoryMetadataEntries().add(type);
    	
    	HpcMandatoryMetadataEntry name = new HpcMandatoryMetadataEntry();
    	name.setAttribute("name");
    	mandatoryMetadata.getMandatoryMetadataEntries().add(name);
    	
    	// Project mandatory metadata.
    	HpcCollectionTypeMandatoryMetadata projectMandatoryMetadata = new HpcCollectionTypeMandatoryMetadata();
    	projectMandatoryMetadata.setType("project");
    	
    	HpcMandatoryMetadataEntry internalProjectId = new HpcMandatoryMetadataEntry();
    	internalProjectId.setAttribute("internalProjectId");
    	projectMandatoryMetadata.getMandatoryMetadataEntries().add(internalProjectId);
    	
    	HpcMandatoryMetadataEntry principalInvestigatorNciUserId = new HpcMandatoryMetadataEntry();
    	principalInvestigatorNciUserId.setAttribute("principalInvestigatorNciUserId");
    	projectMandatoryMetadata.getMandatoryMetadataEntries().add(principalInvestigatorNciUserId);
    	
    	mandatoryMetadata.getCollectionsMandatoryMetadata().add(projectMandatoryMetadata);
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
       	
       	// Validate Metadata entries are provided.
       	validateMandatoryMetadata(metadataEntries);
       	
       	dataManagementProxy.addMetadataToCollection(path, metadataEntries);
    }

    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  

    /**
     * Validate mandatory metadata. Null unit values are converted to empty strings.
     *
     * @param metadataEntries The metadata entries collection to validate.
     * 
     * @throws HpcException If the metadata is invalid.
     */
    private void validateMandatoryMetadata(List<HpcMetadataEntry> metadataEntries) 
    		                              throws HpcException
    {
    	String collectionType = null;
    	
    	// Create a collection of all metadata attributes.
    	List<String> metadataEntryAttributes = new ArrayList<String>();
    	for(HpcMetadataEntry metadataEntry : metadataEntries) {
    		metadataEntryAttributes.add(metadataEntry.getAttribute());
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
    	
    	// Validate common mandatory metadata entries are provided.
    	validateMandatoryMetadataEntries(metadataEntries,
    		                             metadataEntryAttributes,
    		                             mandatoryMetadata.getMandatoryMetadataEntries());
    	
    	// Validate type-specific mandatory metadata entries are provided.
    	for(HpcCollectionTypeMandatoryMetadata collectionMandatoryMetadata: 
    		mandatoryMetadata.getCollectionsMandatoryMetadata()) {
    		if(collectionMandatoryMetadata.getType().equals(collectionType)) {
    		   validateMandatoryMetadataEntries(
    				   metadataEntries,
                       metadataEntryAttributes,
                       collectionMandatoryMetadata.getMandatoryMetadataEntries());
    		}
    	}
    }
    
    /**
     * Validate mandatory metadata entries provided. 
     *
     * @param metadataEntries The metadata entries collection to validate.
     * @param metadataEntryAttributes A List of the metadata attributes.
     * @param mandatoryMetadataEntries The mandatory metadata entries to check for.
     * 
     * @throws HpcException If found a mandatory metadata not provided.
     */
    private void validateMandatoryMetadataEntries(
    		                      List<HpcMetadataEntry> metadataEntries,
    		                      List<String> metadataEntryAttributes,
    		                      List<HpcMandatoryMetadataEntry> mandatoryMetadataEntries) 
    		                      throws HpcException
    {
	    for(HpcMandatoryMetadataEntry mandatoryMetadataEntry: mandatoryMetadataEntries) {
			if(!metadataEntryAttributes.contains(mandatoryMetadataEntry.getAttribute())) {
			   if(mandatoryMetadataEntry.getDefaultValue() != null && 
				  !mandatoryMetadataEntry.getDefaultValue().isEmpty()) {
				  // Metadata entry is missing, but a default is defined.
				  HpcMetadataEntry defaultMetadataEntry = new HpcMetadataEntry();
				  defaultMetadataEntry.setAttribute(mandatoryMetadataEntry.getAttribute());
				  defaultMetadataEntry.setValue(mandatoryMetadataEntry.getDefaultValue());
				  defaultMetadataEntry.setUnit(mandatoryMetadataEntry.getDefaultUnit());
				  metadataEntries.add(defaultMetadataEntry);
			   } else {
				       // Metadata entry is missing, but no default is defined.
			           throw new HpcException("Missing mandataory metadata: " + 
			                                  mandatoryMetadataEntry.getAttribute(), 
			                                  HpcErrorType.INVALID_REQUEST_INPUT);
			   }
			}
		}
    }
}

 