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
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupResponseDTO;
import gov.nih.nci.hpc.dto.metadata.HpcMetadataQueryParam;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

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
    		                           HpcDataObjectRegistrationDTO dataObjectRegistration)
    {	
    	path = toAbsolutePath(path);
		logger.info("Invoking RS: PUT /dataObject" + path);
		
		boolean created = true;
		try {
			 created = dataManagementBusService.registerDataObject(path, 
					                                               dataObjectRegistration);
			 
		} catch(HpcException e) {
			    logger.error("RS: PUT /dataObject" + path + " failed:", e);
			    return errorResponse(e);
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
                                       HpcDataObjectDownloadDTO downloadRequest)
    {
    	path = toAbsolutePath(path);
    	logger.info("Invoking RS: POST /dataObject/" + path + "/download: " + downloadRequest);
    	
		try {
			 dataManagementBusService.downloadDataObject(path, downloadRequest);

		} catch(HpcException e) {
			    logger.error("RS: POST /dataObject/" + path + "/download: " + downloadRequest + 
			    		     " failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(null, false);
    	
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
    
    @Override
    public Response setGroup(HpcGroupRequestDTO groupRequest)
    {
    	logger.info("Invoking RS: POST /group: " + groupRequest);
    	
    	HpcGroupResponseDTO groupResponse = null;
		try {
			 groupResponse = dataManagementBusService.setGroup(groupRequest);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /group: " + groupResponse + " failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(groupResponse, false);
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
    
    
    //---------------------------------------------------------------------//
    // S3 POC
    //---------------------------------------------------------------------//
    
    @Override
    public Response s3Upload(String path)
    {
    	//s3SimpleUpload(path);
    	s3MultipartUpload(path);
    	return okResponse(null, false);
    }
    
    private void s3SimpleUpload(String path)
    {
    	// Instantiate S3 Client and set the Cleversafe endpoint URL.
    	//BasicAWSCredentials cleversafeCredentials = new BasicAWSCredentials("vDZGQHw6OgBBpeI4D1CA", 
    		//	                                                            "OVDNthOhfl5npqdSfAD8T9FsIcJlsCJsmuRdfanr");
    	BasicAWSCredentials cleversafeCredentials = new BasicAWSCredentials("rhwXa402NFW1OwxqY6Xb", 
                                                                            "Y3U4GPAZPKZL7z5Sb71R5fFU0I88gFYswS0U8uxA");
    	AmazonS3 s3client = new AmazonS3Client(cleversafeCredentials);
    	//s3client.setEndpoint("https://8.40.18.82");
    	s3client.setEndpoint("https://fr-s-clvrsf-01.ncifcrf.gov");
    	
        try {
        	 // Put an object.
             System.out.println("Uploading a new object to S3 from a file\n");
             File file = new File(path);
             //PutObjectResult result = s3client.putObject(new PutObjectRequest(
              	//	                                    "CJ090115", "HPC-generated-key", file));
             PutObjectResult result = s3client.putObject(new PutObjectRequest(
                                                         "DSE-TestVault1", "HPC-generated-key", file));
            
             System.out.println("Upload result md5: " + result.getContentMd5());
            
         } catch(AmazonServiceException ase) {
                 System.out.println("Caught an AmazonServiceException, which " +
            	                    "means your request made it " +
                                    "to Amazon S3, but was rejected with an error response" +
                                    " for some reason.");
                 System.out.println("Error Message:    " + ase.getMessage());
                 System.out.println("HTTP Status Code: " + ase.getStatusCode());
                 System.out.println("AWS Error Code:   " + ase.getErrorCode());
                 System.out.println("Error Type:       " + ase.getErrorType());
                 System.out.println("Request ID:       " + ase.getRequestId());
                 
         } catch(AmazonClientException ace) {
                 System.out.println("Caught an AmazonClientException, which " +
            	 "means the client encountered " +
                 "an internal error while trying to " +
                 "communicate with S3, " +
                 "such as not being able to access the network.");
                 System.out.println("Error Message: " + ace.getMessage());
        } 
    }
    
    private void s3MultipartUpload(String path)
    {
    	// Instantiate a Transfer Manager using Cleversafe AWS credentials
    	//BasicAWSCredentials cleversafeCredentials = new BasicAWSCredentials("vDZGQHw6OgBBpeI4D1CA", 
    		//	                                                            "OVDNthOhfl5npqdSfAD8T9FsIcJlsCJsmuRdfanr");
    	BasicAWSCredentials cleversafeCredentials = new BasicAWSCredentials("rQ5sO4vedFMpCJrbEBqA", 
                                                                            "J7aNcIKXmJUDm5NUN70wVQq4zyv0WaMdykpBASEh");
    	
    	TransferManager tm = new TransferManager(cleversafeCredentials);
    	tm.getAmazonS3Client().setEndpoint("https://8.40.18.82");
    	
    	// Create an upload request
    	System.out.println("Multipart Uploading a new object to S3 from a file\n");
        File file = new File(path);
        PutObjectRequest request = new PutObjectRequest("CJ011916", "HPC-generated-key3", file);
        
        // Attach a progress listener.
        request.setGeneralProgressListener(new ProgressListener() {
			@Override
			public void progressChanged(ProgressEvent progressEvent) {
				System.out.println("Progress Event: " + progressEvent.getEventType() +
				                   "    ***   Transferred bytes: " + 
						           progressEvent.getBytesTransferred());
			}
		});
        
        // Invoke the asynchrnous upload.
        Upload upload = tm.upload(request);

        // For a demo we'll block and wait for the upload to complete.
        try {
        	 upload.waitForCompletion();
        	 
        	 System.out.println("Async upload completed");
        	
        } catch(AmazonClientException amazonClientException) {
        	    System.out.println("Unable to upload file, upload was aborted.");
        	    amazonClientException.printStackTrace();
        } catch(Exception ioex) {
        	    System.out.println("Interupted exception: " + ioex);
        }
    }
}

 