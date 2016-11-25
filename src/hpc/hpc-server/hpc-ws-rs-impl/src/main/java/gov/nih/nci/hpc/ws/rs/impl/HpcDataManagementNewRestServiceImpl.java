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

import gov.nih.nci.hpc.bus.HpcDataManagementNewBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataManagementNewRestService;
import gov.nih.nci.hpc.ws.rs.provider.HpcMultipartProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Management REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataManagementNewRestServiceImpl extends HpcRestServiceImpl
             implements HpcDataManagementNewRestService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//   
	
    // The Data Management Business Service instance.
	@Autowired
    private HpcDataManagementNewBusService dataManagementBusService = null;
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// The multipart provider.
	@Autowired
	private HpcMultipartProvider multipartProvider = null;
	
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
    private HpcDataManagementNewRestServiceImpl() 
    {
    }  
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataManagementRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response registerCollection(String path, 
    		                           List<HpcMetadataEntry> metadataEntries)
    {	
    	path = toAbsolutePath(path);
		logger.info("Invoking RS: PUT /collection" + path);
		long start = System.currentTimeMillis();
		boolean created = true;
		try {
			 created = dataManagementBusService.registerCollection(path, metadataEntries);
			 
		} catch(HpcException e) {
			    logger.error("RS: PUT /collection" + path + " failed:", e);
			    return errorResponse(e);
		}
		long stop = System.currentTimeMillis();
		logger.info((stop-start) + " registerCollection: Total time - " + path);
		
		if(created) {
		   return createdResponse(null);
		} else {
			    return okResponse(null, false);
		}
	}
    
    @Override
    public Response getCollection(String path)
    {	
    	long start = System.currentTimeMillis();
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
		long stop = System.currentTimeMillis();
		logger.info((stop-start) + " getCollection: Total time - " + path);
		
		return okResponse(!collections.getCollections().isEmpty() ? collections : null , true);
    }
    
    @Override
    public Response registerDataObject(String path, 
    		                           HpcDataObjectRegistrationDTO dataObjectRegistration,
    		                           InputStream dataObjectInputStream)
    {	
    	long start = System.currentTimeMillis();
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
		
		long stop = System.currentTimeMillis();
		logger.info((stop-start) + " registerDataObject: Total time - " + path);
		
		if(created) {
		   return createdResponse(null);
		} else {
				return okResponse(null, false);
		}
	}
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Copy input stream to File and close the input stream.
     * 
     * @param dataObjectInputStream The input stream.
     * @return File
     * 
     * @throws HpcException if copy of input stream failed.
     */
    private File toFile(InputStream dataObjectInputStream) throws HpcException
    {
    	if(dataObjectInputStream == null) {
    	   return null;
    	}

    	File dataObjectFile = FileUtils.getFile(multipartProvider.getTempDirectory(), 
    			                                UUID.randomUUID().toString());
    	try {
	         FileUtils.copyInputStreamToFile(dataObjectInputStream, dataObjectFile);
	         
    	} catch(IOException e) {
    		    throw new HpcException("Failed to copy input stream", HpcErrorType.UNEXPECTED_ERROR, e);
    	}
    	
    	return dataObjectFile;
    }
}

 
