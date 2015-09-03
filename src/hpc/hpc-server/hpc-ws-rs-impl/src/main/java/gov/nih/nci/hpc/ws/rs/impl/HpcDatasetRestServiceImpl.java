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

import gov.nih.nci.hpc.bus.HpcDatasetBusService;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetAddFilesDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetAddMetadataItemsDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetAssociateFileProjectsDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetUpdateFilePrimaryMetadataDTO;
import gov.nih.nci.hpc.dto.dataset.HpcFileDTO;
import gov.nih.nci.hpc.dto.dataset.HpcFilePrimaryMetadataDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDatasetRestService;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
	@Autowired
    private HpcDatasetBusService datasetBusService = null;
    
    // The URI Info context instance.
    private @Context UriInfo uriInfo;
    
    private String dynamicConfigFile = null;
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------//
    // Constructors
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
     * @param stackTraceEnabled If set to true, stack trace will be attached to
     *                          exception DTO.
     * 
     * @throws HpcException If the bus service is not provided by Spring.
     */
    private HpcDatasetRestServiceImpl(String dynamicConfigFile)
                                     throws HpcException
    {
    	if(dynamicConfigFile == null) {
    	   throw new HpcException("Null confing file",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
		this.dynamicConfigFile = dynamicConfigFile;
    }	
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
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
    public Response addFiles(HpcDatasetAddFilesDTO addFilesDTO)
    {	
		logger.info("Invoking RS: POST /dataset/files: " + addFilesDTO);
		
		try {
			 datasetBusService.addFiles(addFilesDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /dataset/files failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(null, false);
	}
    
    @Override
    public Response associateProjects(
	                HpcDatasetAssociateFileProjectsDTO associateFileProjectsDTO)
    {	
		logger.info("Invoking RS: POST /dataset/projects: " + associateFileProjectsDTO);
		
		try {
			 datasetBusService.associateProjects(associateFileProjectsDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /dataset/projects failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(null, false);
	}
    
    @Override
    public Response addPrimaryMetadataItems(HpcDatasetAddMetadataItemsDTO addMetadataItemsDTO)
    {
		logger.info("Invoking RS: POST /dataset/metadata/primary/items: " + addMetadataItemsDTO);
		
		HpcFilePrimaryMetadataDTO primaryMetadataDTO = null;
		try {
			primaryMetadataDTO = datasetBusService.addPrimaryMetadataItems(addMetadataItemsDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /dataset/metadata/primary/items failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(primaryMetadataDTO, false);    	
    }
    
    @Override
    public Response updatePrimaryMetadata(HpcDatasetUpdateFilePrimaryMetadataDTO updateMetadataDTO)
    {
		logger.info("Invoking RS: POST /dataset/metadata/primary: " + updateMetadataDTO);
		
		HpcFilePrimaryMetadataDTO primaryMetadataDTO = null;
		try {
			 primaryMetadataDTO = datasetBusService.updatePrimaryMetadata(updateMetadataDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /dataset/metadata/primary failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(primaryMetadataDTO, false);      	
    }
    
    @Override
    public Response getDataset(String id, 
    		                   Boolean skipDataTransferStatusUpdate)
    {
		logger.info("Invoking RS: GET /dataset/{id}: " + id);
		
		HpcDatasetDTO datasetDTO = null;
		try {
			 datasetDTO = 
			 datasetBusService.getDataset(
					              id, 
					              skipDataTransferStatusUpdate != null ?
					              skipDataTransferStatusUpdate : false);
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetDTO, true);
	}
    
    @Override
    public Response getFile(String id)
    {
		logger.info("Invoking RS: GET /file/{id}: " + id);
		
		HpcFileDTO fileDTO = null;
		try {
			 fileDTO = datasetBusService.getFile(id);
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /file/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(fileDTO, true);
	}
    
    @Override
    public Response getDatasetsByRegistrarId(String registrarNihUserId)
    {
    	logger.info("Invoking RS: GET /dataset/query/registrar/{id}: " + 
                    registrarNihUserId);
    	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 List<String> userIds = new ArrayList<String>();
			 userIds.add(registrarNihUserId);
			 datasetCollectionDTO = datasetBusService.getDatasets(
					                       userIds, 
						                   HpcDatasetUserAssociation.REGISTRAR); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/query/registrar/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetCollectionDTO, true);
    }
    
	@Override
	public Response getDatasetsByPrincipalInvestigatorId(
			                     String principalInvestigatorNihUserId) 
	{
    	logger.info("Invoking RS: GET /dataset/query/principalInvestigator/{id}: " + 
    			    principalInvestigatorNihUserId);
	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		List<String> userIds = new ArrayList<String>();
		try {
			 userIds.add(principalInvestigatorNihUserId);
			 datasetCollectionDTO = datasetBusService.getDatasets(
					                userIds, 
						            HpcDatasetUserAssociation.PRINCIPAL_INVESTIGATOR); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/query/principalInvestigator/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetCollectionDTO, true);
	}
	
    @Override
    public Response getDatasetsByPrincipalInvestigatorName(String firstName,
    		                                               String lastName)
    {
    	logger.info("Invoking RS: GET /dataset/query/principalInvestigator: " + 
    			    firstName + " " + lastName);
    	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 datasetCollectionDTO = datasetBusService.getDatasets(
					                       firstName, lastName, 
						                   HpcDatasetUserAssociation.PRINCIPAL_INVESTIGATOR); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/query/principalInvestigator: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetCollectionDTO, true);
    }
    
    @Override
    public Response getDatasetsByRegistrarName(String firstName,
    		                                   String lastName)
    {
    	logger.info("Invoking RS: GET /dataset/query/registrar: " + 
    			    firstName + " " + lastName);
    	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 datasetCollectionDTO = datasetBusService.getDatasets(
					                       firstName, lastName, 
						                   HpcDatasetUserAssociation.REGISTRAR); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/query/registrar: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetCollectionDTO, true);
    }
    
	@Override
	public Response getDatasetsByProjectId(String projectId) 
	{
    	logger.info("Invoking RS: GET /dataset/query/project/{id}: " + 
    			    projectId);
	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 datasetCollectionDTO = datasetBusService.getDatasetsByProjectId(projectId); 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/query/project/{id}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetCollectionDTO, true);	
	} 	
    
    @Override
    public Response getDatasetsByName(String name)
    {
    	logger.info("Invoking RS: GET /dataset/query/name/{name}: " + name);
    	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 datasetCollectionDTO = datasetBusService.getDatasets(name); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/query/name/{name}: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetCollectionDTO, true);
    }
    
    @Override
    public Response getDatasetsByPrimaryMetadata(
    		                     HpcFilePrimaryMetadataDTO primaryMetadataDTO)
	{
    	logger.info("Invoking RS: POST /dataset/query/primaryMetadata: " + 
    			    primaryMetadataDTO);
    	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 datasetCollectionDTO = datasetBusService.getDatasets(primaryMetadataDTO); 
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /dataset/query/primaryMetadata: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetCollectionDTO, true);
	}
    
    @Override
    public Response getDatasetsByDataTransferStatus(HpcDataTransferStatus dataTransferStatus,
    		                                        Boolean uploadRequests, 
	                                                Boolean downloadRequests)
    {	
    	logger.info("Invoking RS: GET /dataset/query/dataTransferStatus/upload/{status}: " + 
                    dataTransferStatus);
	
    	HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 datasetCollectionDTO = 
					datasetBusService.getDatasets(dataTransferStatus, uploadRequests, 
							                      downloadRequests); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/query/dataTransferStatus/upload/{status}: failed:", e);
			    return errorResponse(e);
		}
	
		return okResponse(datasetCollectionDTO, true);
	}
	
    @Override
    public String getPrimaryConfigurableDataFields(String type, String callBackFn)
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

 