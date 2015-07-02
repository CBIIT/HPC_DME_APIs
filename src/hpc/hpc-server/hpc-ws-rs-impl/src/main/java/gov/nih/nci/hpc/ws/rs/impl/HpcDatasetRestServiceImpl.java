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
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

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
    	super(false);
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }  
    
   /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param registrationBusService The registration business service.
     * @param stackTraceEnabled If set to true, stack trace will be attached to
     *                          exception DTO.
     * 
     * @throws HpcException If the bus service is not provided by Spring.
     */
    private HpcDatasetRestServiceImpl(HpcDatasetBusService datasetBusService,
    		                          boolean stackTraceEnabled,
    		                          String dynamicConfigFile)
                                     throws HpcException
    {
    	super(stackTraceEnabled);
    	
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
		
		HpcDatasetDTO datasetDTO = null;
		try {
			 datasetDTO = datasetBusService.getDataset(id);
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetDTO, true);
	}
    
    @Override
    public Response getDatasets(String creatorId)
    {
    	logger.info("Invoking RS: GET /dataset/creator/{id}: " + creatorId);
    	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 datasetCollectionDTO = datasetBusService.getDatasets(
					                       creatorId, 
						                   HpcDatasetUserAssociation.CREATOR); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/creator/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetCollectionDTO, true);
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
			    return errorResponse(e);
		}
		
		return createdResponse(datasetId);
	}
	
    @Override
    public Response checkDataTransferStatus(String submissionId)
    {	
    	//HpcDataTransfer hdt = new GlobusOnlineDataTranfer();		
		//return createdResponse(hdt.getTransferStatus(submissionId));
    	return null;
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

 