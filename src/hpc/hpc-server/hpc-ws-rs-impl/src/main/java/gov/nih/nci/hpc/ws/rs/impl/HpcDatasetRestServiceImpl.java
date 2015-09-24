/**
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
import gov.nih.nci.hpc.dto.dataset.HpcDatasetQueryType;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDateRangeDTO;
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
     * @param dynamicConfigFile The dynamic config file.
     * 
     * @throws HpcException
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
    public Response getDatasets(HpcDatasetQueryType queryType, 
    		                    String nciUserId)
    {
    	logger.info("Invoking RS: GET /dataset");
    	
    	// If no query type provided, default to ALL.
    	if(queryType == null) {
		   queryType = HpcDatasetQueryType.ALL;
    	}
    	
    	// Determine the requested query and invoke the appropriate bus service.
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 switch(queryType) {
			        case ALL:
			        	 datasetCollectionDTO = datasetBusService.getDatasets();
                         break;
                         
			        case REGISTRAR_ID:
			             datasetCollectionDTO = 
			                    getDatasets(nciUserId, 
			             		            HpcDatasetUserAssociation.REGISTRAR);
			             break;
			             
			        case PRINCIPAL_INVESTIGATOR_ID:
			             datasetCollectionDTO = 
			                    getDatasets(nciUserId, 
			             		            HpcDatasetUserAssociation.PRINCIPAL_INVESTIGATOR);
			             break;
			 }
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetCollectionDTO, true);
    }
    
    @Override
    public Response getDatasetsByRegistrarId(String registrarNciUserId)
    {
    	logger.info("Invoking RS: GET /dataset/query/registrar/{id}: " + 
                    registrarNciUserId);
    	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 List<String> userIds = new ArrayList<String>();
			 userIds.add(registrarNciUserId);
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
			                     String principalInvestigatorNciUserId) 
	{
    	logger.info("Invoking RS: GET /dataset/query/principalInvestigator/{id}: " + 
    			    principalInvestigatorNciUserId);
	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		List<String> userIds = new ArrayList<String>();
		try {
			 userIds.add(principalInvestigatorNciUserId);
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
    public Response getDatasetsByName(String name, Boolean regex)
    {
    	logger.info("Invoking RS: GET /dataset/query/name/{name}: " + name);
    	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 datasetCollectionDTO = 
			 datasetBusService.getDatasets(name, 
					                       regex != null ? regex : true); 
			 
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
    
    public Response getDatasetsByRegistrationDateRange(
    		           HpcDatasetRegistrationDateRangeDTO registrationDateRangeDTO)
    {
    	logger.info("Invoking RS: GET /dataset/query/registrationDateRange: " + 
    			    registrationDateRangeDTO);

		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 datasetCollectionDTO = 
					datasetBusService.getDatasets(registrationDateRangeDTO); 
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /dataset/query/registrationDateRangeDTO: failed:", e);
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

    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Query datasets associated with a user.
     *
     * @param nciUserId The user ID.
     * @param association The user's association to the dataset.
     * @return Dataset collection.
     * 
     * @throws HpcException 
     */
    private HpcDatasetCollectionDTO getDatasets(String nciUserId, 
    		                        HpcDatasetUserAssociation association)
    		                        throws HpcException
    {
    	List<String> userIds = new ArrayList<String>();
    	if(nciUserId != null) {
    	   userIds.add(nciUserId);
    	}
    	
    	return datasetBusService.getDatasets(userIds, association); 
    }
    
    /*
    @Override
    public Response s3Upload(String path)
    {
    	//s3SimpleUpload(path);
    	s3MultipartUpload(path);
    	return okResponse(null, false);
    }
    
    private void s3SimpleUpload(String path)
    {
    	BasicAWSCredentials cleversafeCredentials = new BasicAWSCredentials("vDZGQHw6OgBBpeI4D1CA", 
    			                                                            "OVDNthOhfl5npqdSfAD8T9FsIcJlsCJsmuRdfanr");
    	
    	AmazonS3 s3client = new AmazonS3Client(cleversafeCredentials);
    	s3client.setEndpoint("https://8.40.18.82");
    	
        try {
            System.out.println("Uploading a new object to S3 from a file\n");
            File file = new File(path);
            s3client.deleteObject("CJ090115", "HPC-generated-key");
            PutObjectResult result = s3client.putObject(new PutObjectRequest(
            		                                    "CJ090115", "HPC-generated-key", file));
            
            System.out.println("Upload result md5: " + result.getContentMd5());
            
            System.out.println("Get object...");
            S3Object object = s3client.getObject("CJ090115", "HPC-generated-key");
            InputStream objectData = object.getObjectContent();
            System.out.println("received object: bytes=" + objectData.available());
            // Process the objectData stream.
            objectData.close();
            
         } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
            		"means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (IOException ioex) {
        	System.out.println("IO exception: " + ioex);
        }
    }
    
    private void s3MultipartUpload(String path)
    {
    	BasicAWSCredentials cleversafeCredentials = new BasicAWSCredentials("vDZGQHw6OgBBpeI4D1CA", 
    			                                                            "OVDNthOhfl5npqdSfAD8T9FsIcJlsCJsmuRdfanr");
    	TransferManager tm = new TransferManager(cleversafeCredentials);
    	tm.getAmazonS3Client().setEndpoint("http://8.40.18.82");
    	
    	
    	tm.getAmazonS3Client().deleteObject("CJ090115", "HPC-generated-key");
    	System.out.println("Multipart Uploading a new object to S3 from a file\n");
        File file = new File(path);
        
        PutObjectRequest request = new PutObjectRequest("CJ090115", "HPC-generated-key", file);
        
        // You can ask the upload for its progress, or you can 
        // add a ProgressListener to your request to receive notifications 
        // when bytes are transferred.
        request.setGeneralProgressListener(new ProgressListener() {
			@Override
			public void progressChanged(ProgressEvent progressEvent) {
				System.out.println("Progress Event: " + progressEvent.getEventType() +
				                   "    ***   Transferred bytes: " + 
						           progressEvent.getBytesTransferred());
			}
		});
        
        Upload upload = tm.upload(request);

        try {
        	// Block and wait for the upload to finish
        	upload.waitForCompletion();
        	
            System.out.println("\n\nDownloading object...");
            S3Object object = tm.getAmazonS3Client().getObject("CJ090115", "HPC-generated-key");
            InputStream objectData = object.getObjectContent();
            System.out.println("received object: bytes=" + objectData.available());
            
        } catch (AmazonClientException amazonClientException) {
        	System.out.println("Unable to upload file, upload was aborted.");
        	amazonClientException.printStackTrace();
        } catch (Exception ioex) {
        	System.out.println("Interupted exception: " + ioex);
        }
    }
    */
}

 