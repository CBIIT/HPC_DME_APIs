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
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadReceiptDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadResponseListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseListDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;
import gov.nih.nci.hpc.ws.rs.provider.HpcMultipartProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Management REST Service Implementation.
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
	
	// The attribue name to save download file path on the message context.
	public static String DATA_OBJECT_DOWNLOAD_FILE_MC_ATTRIBUTE = 
			             "gov.nih.nci.hpc.ws.rs.impl.HpcDataManagementRestService.dataObjectDownloadFile";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Business Service instance.
	@Autowired
    private HpcDataManagementBusService dataManagementBusService = null;
	
	// The multipart provider.
	@Autowired
	private HpcMultipartProvider multipartProvider = null;
	
	
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
    // HpcDataManagementRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public Response registerCollection(String path, 
    		                           HpcCollectionRegistrationDTO collectionRegistration)
    {	
		boolean created = true;
		try {
			 created = dataManagementBusService.registerCollection(toAbsolutePath(path), collectionRegistration);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		if(created) {
		   return createdResponse(null);
		} else {
			    return okResponse(null, false);
		}
	}
    
    @Override
    public Response getCollection(String path, Boolean list)
    {	
    	HpcCollectionListDTO collections = new HpcCollectionListDTO();
		try {
			 HpcCollectionDTO collection = dataManagementBusService.getCollection(toAbsolutePath(path), list);
			 if(collection != null) {
				collections.getCollections().add(collection);
			 }
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(!collections.getCollections().isEmpty() ? collections : null , true);
    }
    
    @Override
	public Response downloadCollection(String path,
                                       HpcDownloadRequestDTO downloadRequest,
                                       MessageContext messageContext)
    {
    	HpcDownloadResponseListDTO downloadResponses = null;
		try {
			 downloadResponses = dataManagementBusService.downloadCollection(toAbsolutePath(path), downloadRequest);

		} catch(HpcException e) {
			    return errorResponse(e);
		}

		return okResponse(downloadResponses != null && !downloadResponses.getDownloadReceipts().isEmpty() ?
		                  downloadResponses : null, true);
    }
    
    @Override
    public Response registerDataObject(String path, 
    		                           HpcDataObjectRegistrationDTO dataObjectRegistration,
    		                           InputStream dataObjectInputStream)
    {	
		File dataObjectFile = null;
		boolean created = true;
		try {
			 dataObjectFile = toFile(dataObjectInputStream);
			 created = dataManagementBusService.registerDataObject(toAbsolutePath(path), 
					                                               dataObjectRegistration,
					                                               dataObjectFile);
			 
		} catch(HpcException e) {
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
    	HpcDataObjectListDTO dataObjects = new HpcDataObjectListDTO();
		try {
			 HpcDataObjectDTO dataObject = dataManagementBusService.getDataObject(toAbsolutePath(path));
			 if(dataObject != null) {
				dataObjects.getDataObjects().add(dataObject);
			 }
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}

		return okResponse(!dataObjects.getDataObjects().isEmpty() ? dataObjects : null, true);
    }
    
    @Override
	public Response downloadDataObject(String path,
                                       HpcDownloadRequestDTO downloadRequest,
                                       MessageContext messageContext)
    {
    	HpcDownloadResponseDTO downloadResponse = null;
		try {
			 downloadResponse = dataManagementBusService.downloadDataObject(toAbsolutePath(path), downloadRequest);

		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return downloadResponse(downloadResponse, messageContext);
    }
    
    @Override
    public Response setPermissions(List<HpcEntityPermissionRequestDTO> entityPermissionRequests)
    {
    	HpcEntityPermissionResponseListDTO permissionResponseList = null;
		try {
			 permissionResponseList = dataManagementBusService.setPermissions(
					                                              entityPermissionRequests);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(permissionResponseList, false);
    }
    
    @Override
    public Response getDataManagementModel(String doc)
    {
    	HpcDataManagementModelDTO dataModel = null;
		try {
			 dataModel = dataManagementBusService.getDataManagementModel(doc);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(dataModel, true);
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
    
    /**
     * Create a Response object out of the DTO. Also set the download file path on the message context,
     * so that the cleanup interceptor can remove it after rge file reached the caller. 
     * 
     * @param downloadResponse The download response.
     * @param messageContext The message context.
     * @return an OK response.
     */
    private Response downloadResponse(HpcDownloadResponseDTO downloadResponse, 
    		                          MessageContext messageContext)
    {
    	if(downloadResponse == null) {
    	   return okResponse(null, true);
    	}
    	
    	HpcDownloadReceiptDTO downloadReceipt = downloadResponse.getDownloadReceipt();
		if(downloadReceipt != null && downloadReceipt.getDestinationFile() != null) {
		   // Put the download file on the message context, so the cleanup interceptor can
		   // delete it after the file was received by the caller.
		   messageContext.put(DATA_OBJECT_DOWNLOAD_FILE_MC_ATTRIBUTE, 
				              downloadReceipt.getDestinationFile());
		   return okResponse(downloadReceipt.getDestinationFile(), 
				             MediaType.APPLICATION_OCTET_STREAM_TYPE);
		} else {
			    return okResponse(downloadResponse, true);
		}
    }
}

 
