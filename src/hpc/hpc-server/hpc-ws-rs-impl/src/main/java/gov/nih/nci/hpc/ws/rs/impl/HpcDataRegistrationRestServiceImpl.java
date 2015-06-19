/**
 * HpcDatasetsRegistrationRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.ws.rs.HpcDataRegistrationRestService;
import gov.nih.nci.hpc.dto.HpcDataRegistrationInput;
import gov.nih.nci.hpc.dto.HpcDataRegistrationOutput;
import gov.nih.nci.hpc.bus.HpcDataRegistrationService;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.exception.HpcErrorType;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Context;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/**
 * <p>
 * HPC Data Registration REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataRegistrationRestServiceImpl extends HpcRestServiceImpl
             implements HpcDataRegistrationRestService
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Registration Business Service instance.
    private HpcDataRegistrationService registrationBusService = null;
    
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
    private HpcDataRegistrationRestServiceImpl() throws HpcException
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
    private HpcDataRegistrationRestServiceImpl(
    		       HpcDataRegistrationService registrationBusService)
                   throws HpcException
    {
    	if(registrationBusService == null) {
    	   throw new HpcException("Null HpcDataRegistrationService instance",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.registrationBusService = registrationBusService;
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataRegistrationRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response getRegisterdData(String id)
    {
		logger.info("Invoking RS: GET /registration{id}");
		
		HpcDataRegistrationOutput registrationOutput = null;
		try {
			 registrationOutput = registrationBusService.getRegisteredData(id);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /registration failed:", e);
			    return toResponse(e);
		}
		
		return toOkResponse(registrationOutput);
	}
    
    @Override
    public Response registerData(
    		        HpcDataRegistrationInput registrationInput)
    {	
		logger.info("Invoking RS: POST /registration");
		
		String registeredDataId = null;
		try {
			 registeredDataId = 
		     registrationBusService.registerData(registrationInput);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /registration failed:", e);
			    return toResponse(e);
		}
		
		return toCreatedResponse(registeredDataId);
	}
    
    @Override
    public String getPrimaryConfigurableDataFields(String type,String callBackFn)
    {
		logger.info("Invoking RS: GET /registration/getPrimaryConfigurableDataFields for type {type}");
		logger.info("callBackFn::" + callBackFn);
		logger.info("type::" + type);
		JSONParser parser = new JSONParser();
		try {
        //InputStream inputStream = getClass().getClassLoader().getResourceAsStream("dynamicfields.json");
	        FileReader reader = new FileReader("dynamicfields.json");
	        JSONObject json = (JSONObject) parser.parse(reader);
		} catch(FileNotFoundException e) {
		    logger.error("FileNotFoundException failed:", e);
		}catch(IOException e) {
		    logger.error("IOException failed:", e);
		}
		catch(ParseException e) {
		    logger.error("ParseException failed:", e);
		}
		
		return callBackFn +"({\"name\": \"Mahidhar Narra\"});";
	}    
}

 