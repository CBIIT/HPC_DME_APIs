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
import gov.nih.nci.hpc.domain.metadata.HpcMandatoryMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
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
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
	// A collection of mandatory metadata entries.
	private List<HpcMandatoryMetadataEntry> mandatoryMetadataEntries = 
			new ArrayList<HpcMandatoryMetadataEntry>();
	
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
    	HpcMandatoryMetadataEntry name = new HpcMandatoryMetadataEntry();
    	name.setAttribute("name");
    	mandatoryMetadataEntries.add(name);
    	
    	HpcMandatoryMetadataEntry type = new HpcMandatoryMetadataEntry();
    	type.setAttribute("type");
    	type.setDefaultValue("default-type");
    	type.setDefaultUnit("N/A");
    	mandatoryMetadataEntries.add(type);
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
       	   throw new HpcException("Invalid add metadata entries input", 
       			                  HpcErrorType.INVALID_REQUEST_INPUT);
       	}	
       	
       	// Validate Metadata entries are provided.
       	validateMandatoryMetadataEntries(metadataEntries);
       	
       	dataManagementProxy.addMetadataToCollection(path, metadataEntries);
    }

    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Validate mandatory metadata entries provided.
     *
     * @param metadataEntries The metadata entries collection to validate.
     * @param update The updated metadata.
     * 
     * @throws HpcException If the metadata to update was not found.
     */
    private void validateMandatoryMetadataEntries(
    		                      List<HpcMetadataEntry> metadataEntries) 
    		                      throws HpcException
    {
    	// Create a collection of all metadata attributes.
    	List<String> metadataEntryAttributes = new ArrayList<String>();
    	for(HpcMetadataEntry metadataEntry : metadataEntries) {
    		metadataEntryAttributes.add(metadataEntry.getAttribute());
    	}
    	
    	// Validate all the mandatory attributes are included.
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

 