/**
 * HpcDatasetRegistrationRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.transfer.HpcDataTransfer;
import gov.nih.nci.hpc.transfer.impl.GlobusOnlineDataTranfer;
import gov.nih.nci.hpc.ws.rs.HpcDatasetRegistrationRestService;
import gov.nih.nci.hpc.dto.datasetregistration.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.datasetregistration.HpcFileDTO;
import gov.nih.nci.hpc.domain.metadata.HpcDatasetRegistrationMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcDatasetMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
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
 * HPC Dataset Registration REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDatasetRegistrationRestServiceImpl extends HpcRestServiceImpl
             implements HpcDatasetRegistrationRestService
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
    private HpcDatasetRegistrationRestServiceImpl() throws HpcException
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
    private HpcDatasetRegistrationRestServiceImpl(
    		                      String registrationBusService)
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
    // HpcDataRegistrationRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response getDataset(String id)
    {
		logger.info("Invoking RS: GET /user/{id}");
		
		HpcDatasetDTO output = new HpcDatasetDTO();
		output.setName("dataset-name");
		output.setPrimaryInvestigatorId("primary-investigator-id");
		output.setCreatorId("creator-id");
		output.setLabBranch("lab-branch");
		HpcFileDTO fileDTO = new HpcFileDTO();
		HpcFile file = new HpcFile();
		file.setType(HpcFileType.FASTQ);
		HpcFileLocation location = new HpcFileLocation();
		location.setEndpoint("endpoint");
		location.setPath("path");
		file.setLocation(location);
		fileDTO.setFile(file);
		HpcDatasetMetadata dsmd = new HpcDatasetMetadata();
		HpcDatasetRegistrationMetadata metadata = new HpcDatasetRegistrationMetadata();
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
		dsmd.setRegistrationMetadata(metadata);
			
		fileDTO.setMetadata(dsmd);
		output.getFiles().add(fileDTO);
		output.getFiles().add(fileDTO);
		
		/*try {
			 registrationOutput = registrationBusService.getRegisteredData(id);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /registration failed:", e);
			    return toResponse(e);
		}*/
		
		return toOkResponse(output);
	}
    
    @Override
    public Response registerDataset(HpcDatasetDTO datasetDTO)
    {	
		logger.info("Invoking RS: POST /registration");
		
		String registeredDataId = "Mock-User-ID";
		/*try {
			 registeredDataId = 
		     registrationBusService.registerData(registrationInput);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /registration failed:", e);
			    return toResponse(e);
		}*/
		
		return toCreatedResponse(registeredDataId);
	}
    
    @Override
    public Response checkDataTransferStatus(String submissionId)
    {	
    	HpcDataTransfer hdt = new GlobusOnlineDataTranfer();		
		return toCreatedResponse(hdt.getTransferStatus(submissionId));
	}    
}

 