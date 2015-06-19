/**
 * HpcDatasetRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.ws.rs.HpcDatasetRestService;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;

import gov.nih.nci.hpc.domain.metadata.HpcDatasetPrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcDatasetMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;

import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Context;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Dataset REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDatasetRestServiceImpl extends HpcRestServiceImpl
             implements HpcDatasetRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Registration Business Service instance.
    //private HpcDataRegistrationService registrationBusService = null;
    
    // The URI Info context instance.
    private @Context UriInfo uriInfo;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcDatasetRestServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param registrationBusService The registration business service.
     * 
     * @throws HpcException If the bus service is not provided by Spring.
     */
    private HpcDatasetRestServiceImpl(String registrationBusService)
                                     throws HpcException
    {
    	if(registrationBusService == null) {
    	   throw new HpcException("Null HpcDataRegistrationService instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	//this.registrationBusService = registrationBusService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response getDataset(String id)
    {
		logger.info("Invoking RS: GET /user/{id}");
		
		HpcDatasetRegistrationDTO output = new HpcDatasetRegistrationDTO();
		output.setName("dataset-name");
		output.setPrimaryInvestigatorId("primary-investigator-id");
		output.setCreatorId("creator-id");
		output.setRegistratorId("registrator-id");
		output.setLabBranch("lab-branch");
		
		
		HpcFileUploadRequest file = new HpcFileUploadRequest();
		file.setType(HpcFileType.FASTQ);
		HpcDataTransferLocations locations = new HpcDataTransferLocations();
		HpcFileLocation source = new HpcFileLocation();
		source.setEndpoint("source-endpoint");
		source.setPath("source-path");
		HpcFileLocation destination = new HpcFileLocation();
		destination.setEndpoint("destination-endpoint");
		destination.setPath("destination-path");
		locations.setSource(source);
		locations.setDestination(destination);
		file.setLocations(locations);
		HpcDatasetPrimaryMetadata metadata = new HpcDatasetPrimaryMetadata();
		metadata.setDataContainsPII(false);
		metadata.setDataContainsPHI(true);
		metadata.setDataEncrypted(false);
		metadata.setDataCompressed(true);
		metadata.setDescription("dataset-description");
		metadata.setFundingOrganization("funding-organization");
		HpcMetadataItem mdi = new HpcMetadataItem();
		mdi.setKey("custom-metadata-key");
		mdi.setValue("custom-metadata-value");
		metadata.getMetadataItems().add(mdi);
		file.setMetadata(metadata);
			
		output.getUploadRequests().add(file);
		output.getUploadRequests().add(file);
		
		/*try {
			 registrationOutput = registrationBusService.getRegisteredData(id);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /registration failed:", e);
			    return toResponse(e);
		}*/
		
		return toOkResponse(output);
	}
    
    @Override
    public Response registerDataset(HpcDatasetRegistrationDTO datasetRegistrationDTO)
    {	
		logger.info("Invoking RS: POST /registration");
		
		String registeredDatasetId = "Mock-User-ID";
		/*try {
			 registeredDataId = 
		     registrationBusService.registerData(registrationInput);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /registration failed:", e);
			    return toResponse(e);
		}*/
		
		return toCreatedResponse(registeredDatasetId);
	}
}

 