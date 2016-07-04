/**
 * HpcDataManagementRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseListDTO;
import gov.nih.nci.hpc.dto.metadata.HpcMetadataQueryParam;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Collection REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataManagementRestServiceImpl extends HpcRestServiceImpl
             implements HpcDataManagementRestService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//   
	
	public static String DATA_OBJECT_DOWNLOAD_FILE = "DataObjectDownloadFile";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Business Service instance.
	@Autowired
    private HpcDataManagementBusService dataManagementBusService = null;
	
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
    
    //---------------------------------------------------------------------//
    // constructors
    //---------------------------------------------------------------------//
     
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcDataManagementRestServiceImpl() 
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcCollectionRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response registerCollection(String path, 
    		                           List<HpcMetadataEntry> metadataEntries)
    {	
    	path = toAbsolutePath(path);
		logger.info("Invoking RS: PUT /collection" + path);
		
		boolean created = true;
		try {
			 created = dataManagementBusService.registerCollection(path, metadataEntries);
			 
		} catch(HpcException e) {
			    logger.error("RS: PUT /collection" + path + " failed:", e);
			    return errorResponse(e);
		}
		
		if(created) {
		   return createdResponse(null);
		} else {
			    return okResponse(null, false);
		}
	}
    
    @Override
    public Response getCollection(String path)
    {	
    	path = toAbsolutePath(path);
    	logger.info("Invoking RS: GET /collection/" + path);
    	
    	HpcCollectionListDTO collections = new HpcCollectionListDTO();
		try {
			 HpcCollectionDTO collection = dataManagementBusService.getCollection(path);
			 if(collection != null) {
				collections.getCollections().add(collection);
			 }
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /collection/" + path + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(!collections.getCollections().isEmpty() ? collections : null , true);
    }
    
    @Override
    public Response getCollections(List<HpcMetadataQueryParam> metadataQueries)
    {
    	logger.info("Invoking RS: GET /collection/" + metadataQueries);
    	
    	HpcCollectionListDTO collections = null;
		try {
			 collections = dataManagementBusService.getCollections(
					                     unmarshallQueryParams(metadataQueries));
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /collection/" + metadataQueries + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(!collections.getCollections().isEmpty() ? collections : null , true);
    }
    
    @Override
    public Response registerDataObject(String path, 
    		                           HpcDataObjectRegistrationDTO dataObjectRegistration,
    		                           InputStream dataObjectInputStream)
    {	
    	path = toAbsolutePath(path);
		logger.info("Invoking RS: PUT /dataObject" + path);
		
		File dataObjectFile = null;
		boolean created = true;

		try {
			 dataObjectFile = toFile(dataObjectInputStream);
			 created = dataManagementBusService.registerDataObject(path, 
					                                               dataObjectRegistration,
					                                               dataObjectFile);
			 
		} catch(HpcException e) {
			    logger.error("RS: PUT /dataObject" + path + " failed:", e);
			    return errorResponse(e);
			    
		} finally {
			       // Delete the temporary file (if provided).
	    	       FileUtils.deleteQuietly(dataObjectFile);
		}
		
		if(created) {
		   return createdResponse(null);
		} else {
				return okResponse(null, false);
		}
	}
    
    @Override
    public Response getDataObject(String path)
    {	
    	path = toAbsolutePath(path);
    	logger.info("Invoking RS: GET /dataObject/" + path);
    	
    	HpcDataObjectListDTO dataObjects = new HpcDataObjectListDTO();
		try {
			 HpcDataObjectDTO dataObject = dataManagementBusService.getDataObject(path);
			 if(dataObject != null) {
				dataObjects.getDataObjects().add(dataObject);
			 }
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataObjects/" + path + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(!dataObjects.getDataObjects().isEmpty() ? dataObjects : null, true);
    }
    
    @Override
    public Response getDataObjects(List<HpcMetadataQueryParam> metadataQueries)
    {
    	logger.info("Invoking RS: GET /dataObject/" + metadataQueries);
    	
    	HpcDataObjectListDTO dataObjects = null;
		try {
			 dataObjects = dataManagementBusService.getDataObjects(
					                     unmarshallQueryParams(metadataQueries));
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataObject/" + metadataQueries + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(!dataObjects.getDataObjects().isEmpty() ? dataObjects : null, true);
    }
    
    @Override
	public Response downloadDataObject(String path,
                                       HpcDataObjectDownloadRequestDTO downloadRequest,
                                       MessageContext messageContext)
    {
    	path = toAbsolutePath(path);
    	logger.info("Invoking RS: POST /dataObject/" + path + "/download: " + downloadRequest);
    	
    	HpcDataObjectDownloadResponseDTO downloadResponse = null;
		try {
			 downloadResponse = dataManagementBusService.downloadDataObject(path, downloadRequest);

		} catch(HpcException e) {
			    logger.error("RS: POST /dataObject/" + path + "/download: " + downloadRequest + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		
		if(downloadResponse.getDestinationFile() != null) {
		   // Put the download file on the message context, so the cleanup interceptor can
		   // delete it after it was received by the caller.
		   messageContext.put(DATA_OBJECT_DOWNLOAD_FILE, 
				              downloadResponse.getDestinationFile());
		   return okResponse(downloadResponse.getDestinationFile(), 
				             MediaType.APPLICATION_OCTET_STREAM_TYPE);
		} else {
			    return okResponse(downloadResponse, false);
		}
    }
    
    @Override
    public Response setPermissions(List<HpcEntityPermissionRequestDTO> entityPermissionRequests)
    {
    	logger.info("Invoking RS: POST /acl: " + entityPermissionRequests);
    	
    	HpcEntityPermissionResponseListDTO permissionResponseList = null;
		try {
			 permissionResponseList = dataManagementBusService.setPermissions(
					                                              entityPermissionRequests);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /acl: " + entityPermissionRequests + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(permissionResponseList, false);
    }
    
 
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Unmarshall metadata query passed as JSON in a URL parameter.
     * 
     * @param metadataQueries The query params to unmarshall.
     * @return List of HpcMetadataQuery.
     * 
     * @throws HpcException if the params unmarshalling failed.
     */
    private List<HpcMetadataQuery> unmarshallQueryParams(
    		                                 List<HpcMetadataQueryParam> metadataQueries)
    		                                 throws HpcException
    {
		 // Validate the metadata entries input (JSON) was parsed successfully.
		 List<HpcMetadataQuery> queries = new ArrayList<HpcMetadataQuery>();
		 for(HpcMetadataQueryParam queryParam : metadataQueries) {
		     if(queryParam.getJSONParsingException() != null) {
			    throw queryParam.getJSONParsingException();
		     }
		     queries.add(queryParam);
		 }
		 
		 return queries;
    }
    
    /**
     * Copy input stream tp File and close the input stream
     * 
     * @param dataObjectInputStream The input stream
     * @return File
     * 
     * @throws HpcException if copy of input stream failed.
     */
    private File toFile(InputStream dataObjectInputStream) throws HpcException
    {
    	if(dataObjectInputStream == null) {
    	   return null;
    	}

    	File dataObjectFile = FileUtils.getFile(FileUtils.getTempDirectory(), 
    			                                UUID.randomUUID().toString());
    	try {
	         FileUtils.copyInputStreamToFile(dataObjectInputStream, dataObjectFile);
	         
    	} catch(IOException e) {
    		    throw new HpcException("Failed to copy input stream", HpcErrorType.UNEXPECTED_ERROR, e);
    	}
    	
    	return dataObjectFile;
    }
}

 