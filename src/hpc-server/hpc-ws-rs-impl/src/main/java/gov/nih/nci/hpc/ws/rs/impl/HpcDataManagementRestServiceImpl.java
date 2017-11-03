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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcBulkDataObjectRegistrationStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDeleteResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadStatusDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadSummaryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionsResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;
import gov.nih.nci.hpc.ws.rs.provider.HpcMultipartProvider;

/**
 * <p>
 * HPC Data Management REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
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
		boolean collectionCreated = true;
		try {
			 collectionCreated = dataManagementBusService.registerCollection(toAbsolutePath(path), 
					                                                         collectionRegistration);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return collectionCreated ? createdResponse(null) : okResponse(null, false);
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
    public Response getCollectionChildren(String path)
    {	
    	HpcCollectionListDTO collections = new HpcCollectionListDTO();
		try {
			 HpcCollectionDTO collection = dataManagementBusService.getCollectionChildren(toAbsolutePath(path));
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
                                       HpcDownloadRequestDTO downloadRequest)
    {
    	HpcCollectionDownloadResponseDTO downloadResponse = null;
		try {
			 downloadResponse = dataManagementBusService.downloadCollection(toAbsolutePath(path), downloadRequest);

		} catch(HpcException e) {
			    return errorResponse(e);
		}

		return okResponse(downloadResponse, false);
    }
    
    @Override
    public Response getCollectionDownloadStatus(String taskId)
    {
    	HpcCollectionDownloadStatusDTO downloadStatus = null;
		try {
			 downloadStatus = dataManagementBusService.getCollectionDownloadStatus(taskId);

		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(downloadStatus, true);
    }
    
    @Override
    public Response setCollectionPermissions(String path, HpcEntityPermissionsDTO collectionPermissionsRequest)
    {
    	HpcEntityPermissionsResponseDTO permissionsResponse = null;
		try {
			 permissionsResponse = 
					    dataManagementBusService.setCollectionPermissions(toAbsolutePath(path), 
					                                                      collectionPermissionsRequest);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(permissionsResponse, false);
    }
    
    @Override
    public Response getCollectionPermissions(String path)
    {
    	HpcEntityPermissionsDTO entityPermissions = null;
		try {
			 entityPermissions = dataManagementBusService.getCollectionPermissions(toAbsolutePath(path));
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(entityPermissions, true);
    }
    
    @Override
    public Response getCollectionPermissionForUser(String path, String userId)
    {
    	HpcUserPermissionDTO hpcUserPermissionDTO = null;
		try {
			hpcUserPermissionDTO = dataManagementBusService.getCollectionPermissionForUser(toAbsolutePath(path), userId);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(hpcUserPermissionDTO, true);
    }

    @Override
    public Response registerDataObject(String path, 
    		                           HpcDataObjectRegistrationDTO dataObjectRegistration,
    		                           InputStream dataObjectInputStream)
    {	
		File dataObjectFile = null;
		boolean dataObjectCreated = true;
		try {
			 dataObjectFile = toFile(dataObjectInputStream);
			 dataObjectCreated = dataManagementBusService.registerDataObject(toAbsolutePath(path), 
					                                                         dataObjectRegistration,
					                                                         dataObjectFile);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
			    
		} finally {
			       // Delete the temporary file (if provided).
	    	       FileUtils.deleteQuietly(dataObjectFile);
		}
		
		return dataObjectCreated ? createdResponse(null) : okResponse(null, false);
	}
    
    @Override
    public Response registerDataObjects(
	                        HpcBulkDataObjectRegistrationRequestDTO bulkDataObjectRegistrationRequest)
    {
    	HpcBulkDataObjectRegistrationResponseDTO registrationResponse = null;
		try {
			 registrationResponse = 
					     dataManagementBusService.registerDataObjects(bulkDataObjectRegistrationRequest);

		} catch(HpcException e) {
			    return errorResponse(e);
		}

		return !StringUtils.isEmpty(registrationResponse.getTaskId()) ?
			   createdResponse(registrationResponse.getTaskId()) :
			   okResponse(registrationResponse, false);    	
    }
    
    @Override
    public Response getDataObjectsRegistrationStatus(String taskId)
    {
    	HpcBulkDataObjectRegistrationStatusDTO registrationStatus = null;
		try {
			 registrationStatus = dataManagementBusService.getDataObjectsRegistrationStatus(taskId);

		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(registrationStatus, true);    	
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
    	HpcDataObjectDownloadResponseDTO downloadResponse = null;
		try {
			 downloadResponse = dataManagementBusService.downloadDataObject(toAbsolutePath(path), downloadRequest);

		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return downloadResponse(downloadResponse, messageContext);
    }
    
    @Override
    public Response getDataObjectDownloadStatus(String taskId)
    {
    	HpcDataObjectDownloadStatusDTO downloadStatus = null;
		try {
			 downloadStatus = dataManagementBusService.getDataObjectDownloadStatus(taskId);

		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(downloadStatus, true);
    }
    
    @Override
	public Response deleteDataObject(String path)
    {
    	HpcDataObjectDeleteResponseDTO dataObjectDeleteResponse = null;
		try {
			 dataObjectDeleteResponse = dataManagementBusService.deleteDataObject(toAbsolutePath(path));
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(dataObjectDeleteResponse, false);
    }
    
    @Override
    public Response setDataObjectPermissions(String path, HpcEntityPermissionsDTO dataObjectPermissionsRequest)
    {
    	HpcEntityPermissionsResponseDTO permissionsResponse = null;
		try {
			 permissionsResponse = 
					    dataManagementBusService.setDataObjectPermissions(toAbsolutePath(path), 
					                                                      dataObjectPermissionsRequest);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(permissionsResponse, false);
    }
    
    @Override
    public Response getDataObjectPermissions(String path)
    {
    	HpcEntityPermissionsDTO entityPermissions = null;
		try {
			 entityPermissions = dataManagementBusService.getDataObjectPermissions(toAbsolutePath(path));
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(entityPermissions, true);
    }
    
    @Override
    public Response getDataObjectPermissionForUser(String path, String userId)
    {
    	HpcUserPermissionDTO hpcUserPermissionDTO = null;
		try {
			hpcUserPermissionDTO = dataManagementBusService.getDataObjectPermissionForUser(toAbsolutePath(path), userId);
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(hpcUserPermissionDTO, true);
    }
    
    @Override
	public Response downloadDataObjects(HpcBulkDataObjectDownloadRequestDTO downloadRequest)
    {
    	HpcBulkDataObjectDownloadResponseDTO downloadResponse = null;
		try {
			 downloadResponse = dataManagementBusService.downloadDataObjects(downloadRequest);

		} catch(HpcException e) {
			    return errorResponse(e);
		}

		return okResponse(downloadResponse, false);
    }
    
    @Override
    public Response getDataObjectsDownloadStatus(String taskId)
    {
    	HpcCollectionDownloadStatusDTO downloadStatus = null;
		try {
			 downloadStatus = dataManagementBusService.getDataObjectsDownloadStatus(taskId);

		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(downloadStatus, true);
    }
    
    @Override
    public Response getDownloadSummary(Integer page, Boolean totalCount)
    {
    	HpcDownloadSummaryDTO downloadSummary = null;
		try {
			downloadSummary = dataManagementBusService.getDownloadSummary(
					                                      page != null ? page : 1,
                                                          totalCount != null ? totalCount : false);

		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
    	return okResponse(downloadSummary.getActiveTasks().isEmpty() && 
    			          downloadSummary.getCompletedTasks().isEmpty() ? null : downloadSummary , true);
    }
    
    @Override
    public Response getDataManagementModel()
    {
    	HpcDataManagementModelDTO docModel = null;
		try {
			 docModel = dataManagementBusService.getDataManagementModel();
			 
		} catch(HpcException e) {
			    return errorResponse(e);
		}
		
		return okResponse(docModel, true);
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
    private Response downloadResponse(HpcDataObjectDownloadResponseDTO downloadResponse, 
    		                          MessageContext messageContext)
    {
    	if(downloadResponse == null) {
    	   return okResponse(null, false);
    	}
    	
		if(downloadResponse.getDestinationFile() != null) {
		   // Put the download file on the message context, so the cleanup interceptor can
		   // delete it after the file was received by the caller.
		   messageContext.put(DATA_OBJECT_DOWNLOAD_FILE_MC_ATTRIBUTE, 
				              downloadResponse.getDestinationFile());
		   return okResponse(downloadResponse.getDestinationFile(), 
				             MediaType.APPLICATION_OCTET_STREAM_TYPE);
		} else {
			    return okResponse(downloadResponse, false);
		}
    }
}

 
