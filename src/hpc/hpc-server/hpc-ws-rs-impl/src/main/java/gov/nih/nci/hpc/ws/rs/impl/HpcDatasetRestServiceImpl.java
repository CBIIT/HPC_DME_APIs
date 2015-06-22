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
import gov.nih.nci.hpc.bus.HpcDatasetBusService;
import gov.nih.nci.hpc.transfer.HpcDataTransfer;
import gov.nih.nci.hpc.transfer.impl.GlobusOnlineDataTranfer;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;

//import gov.nih.nci.hpc.domain.metadata.HpcDatasetPrimaryMetadata;
//import gov.nih.nci.hpc.domain.metadata.HpcDatasetMetadata;
//import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
//import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
//import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
//import gov.nih.nci.hpc.domain.dataset.HpcFileType;
//import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;

import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Context;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

    // The Dataset Registration Business Service instance.
    private HpcDatasetBusService datasetBusService = null;
    
    // The URI Info context instance.
    private @Context UriInfo uriInfo;
    
    private String dynamicConfigFile = null;
    
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
    private HpcDatasetRestServiceImpl(HpcDatasetBusService datasetBusService,
    		                          String dynamicConfigFile)
                                     throws HpcException
    {
    	if(datasetBusService == null || dynamicConfigFile == null) {
    	   throw new HpcException("Null HpcDatasetBusService/confing file",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.datasetBusService = datasetBusService;
		this.dynamicConfigFile = dynamicConfigFile;
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
		logger.info("Invoking RS: GET /dataset/{id}: " + id);
		
		/*
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
		metadata.setFundingOrganization("funding-organization");
		HpcMetadataItem mdi = new HpcMetadataItem();
		mdi.setKey("custom-metadata-key");
		mdi.setValue("custom-metadata-value");
		metadata.getMetadataItems().add(mdi);
		file.setMetadata(metadata);
			
		output.getUploadRequests().add(file);
		output.getUploadRequests().add(file);
		*/
		HpcDatasetDTO datasetDTO = null;
		try {
			 datasetDTO = datasetBusService.getDataset(id);
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/{id}: failed:", e);
			    return toResponse(e);
		}
		
		return toOkResponse(datasetDTO);
	}
    
    @Override
    public Response getDatasets(String creatorId)
    {
    	logger.info("Invoking RS: GET /dataset/creator/{id}: " + creatorId);
    	
		HpcDatasetDTO datasetDTO = null;
		try {
			 datasetDTO = datasetBusService.getDatasets(
					                        creatorId, 
						                    HpcDatasetUserAssociation.CREATOR); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/creator/{id}: failed:", e);
			    return toResponse(e);
		}
		
		return toOkResponse(datasetDTO);
    }
    
    @Override
    public Response registerDataset(HpcDatasetRegistrationDTO datasetRegistrationDTO)
    {	
		logger.info("Invoking RS: POST /dataset: " + datasetRegistrationDTO);
		
		String datasetId = null;
		try {
			 datasetId = datasetBusService.registerDataset(datasetRegistrationDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /dataset failed:", e);
			    return toResponse(e);
		}
		
		return toCreatedResponse(datasetId);
	}
	
    @Override
    public Response checkDataTransferStatus(String submissionId)
    {	
    	HpcDataTransfer hdt = new GlobusOnlineDataTranfer();		
		return toCreatedResponse(hdt.getTransferStatus(submissionId));
	}
	
    @Override
    public String getPrimaryConfigurableDataFields(String type,String callBackFn)
    {
		logger.info("Invoking RS: GET /registration/getPrimaryConfigurableDataFields for type {type}");
		logger.info("callBackFn::" + callBackFn);
		logger.info("type::" + type);
		JSONParser parser = new JSONParser();
		JSONObject json = new JSONObject();
		try {
        //InputStream inputStream = getClass().getClassLoader().getResourceAsStream("dynamicfields.json");
	        FileReader reader = new FileReader(dynamicConfigFile);
	        json = (JSONObject) parser.parse(reader);
		} catch(FileNotFoundException e) {
		    logger.error("FileNotFoundException failed:", e);
		}catch(IOException e) {
		    logger.error("IOException failed:", e);
		}
		catch(ParseException e) {
		    logger.error("ParseException failed:", e);
		}
		
		return callBackFn +"("+json.toString()+");";
	} 	
}

 