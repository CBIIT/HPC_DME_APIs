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
import gov.nih.nci.hpc.dto.dataset.HpcDatasetUpdateFilePrimaryMetadataDTO;
import gov.nih.nci.hpc.dto.dataset.HpcFileDTO;
import gov.nih.nci.hpc.dto.dataset.HpcFilePrimaryMetadataDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDatasetRestService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.DataObjectAO;
import org.irods.jargon.core.pub.IRODSAccessObjectFactory;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.domain.AvuData;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

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
    // Constants
    //---------------------------------------------------------------------//    
    
    // Date format.
	private final static String DATE_FORMAT = "yyyy-MM-dd";
	
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
	
	// IRODS file system.
	private IRODSFileSystem irodsFileSystem = null;
    
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
		
		try {
		     this.irodsFileSystem = IRODSFileSystem.instance();
		     
		} catch(JargonException e) {
			    throw new HpcException("Failed to instantiate iRODs file system",
	                                   HpcErrorType.UNEXPECTED_ERROR, e);
		}
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
		logger.info("Invoking RS: POST /datasets: " + datasetRegistrationDTO);
		
		String datasetId = null;
		try {
			 datasetId = datasetBusService.registerDataset(datasetRegistrationDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /datasets failed:", e);
			    return errorResponse(e);
		}
		
		return createdResponse(datasetId);
	}
    
    @Override
    public Response addFiles(HpcDatasetAddFilesDTO addFilesDTO)
    {	
		logger.info("Invoking RS: POST /datasets/files: " + addFilesDTO);
		
		try {
			 datasetBusService.addFiles(addFilesDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /datasets/files failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(null, false);
	}
    
    @Override
    public Response associateProjects(
	                HpcDatasetAssociateFileProjectsDTO associateFileProjectsDTO)
    {	
		logger.info("Invoking RS: POST /datasets/projects: " + associateFileProjectsDTO);
		
		try {
			 datasetBusService.associateProjects(associateFileProjectsDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /datasets/projects failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(null, false);
	}
    
    @Override
    public Response addPrimaryMetadataItems(HpcDatasetAddMetadataItemsDTO addMetadataItemsDTO)
    {
		logger.info("Invoking RS: POST /datasets/metadata/primary/items: " + addMetadataItemsDTO);
		
		HpcFilePrimaryMetadataDTO primaryMetadataDTO = null;
		try {
			primaryMetadataDTO = datasetBusService.addPrimaryMetadataItems(addMetadataItemsDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /datasets/metadata/primary/items failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(primaryMetadataDTO, false);    	
    }
    
    @Override
    public Response updatePrimaryMetadata(HpcDatasetUpdateFilePrimaryMetadataDTO updateMetadataDTO)
    {
		logger.info("Invoking RS: POST /datasets/metadata/primary: " + updateMetadataDTO);
		
		HpcFilePrimaryMetadataDTO primaryMetadataDTO = null;
		try {
			 primaryMetadataDTO = datasetBusService.updatePrimaryMetadata(updateMetadataDTO);
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /datasets/metadata/primary failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(primaryMetadataDTO, false);      	
    }
    
    @Override
    public Response getDataset(String id, 
    		                   Boolean skipDataTransferStatusUpdate)
    {
		logger.info("Invoking RS: GET /datasets/{id}: " + id);
		
		HpcDatasetDTO datasetDTO = null;
		try {
			 datasetDTO = 
			 datasetBusService.getDataset(
					              id, 
					              skipDataTransferStatusUpdate != null ?
					              skipDataTransferStatusUpdate : false);
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /datasets/{id}: failed:", e);
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
    		                    String nciUserId, String firstName, 
    		                    String lastName, String projectId, 
    		                    String name, Boolean regex,
    		                    HpcDataTransferStatus dataTransferStatus,
    		    		        Boolean uploadRequests, Boolean downloadRequests,
    		    		        String from, String to)
    {
    	logger.info("Invoking RS: GET /datasets");
    	
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
			             
			        case REGISTRAR_NAME:
			        	 datasetCollectionDTO = 
			        	 datasetBusService.getDatasets(
			        	          		   firstName, lastName, 
				                           HpcDatasetUserAssociation.REGISTRAR); 
			             break;
			             
			        case PRINCIPAL_INVESTIGATOR_ID:
			             datasetCollectionDTO = 
			             getDatasets(nciUserId, 
			                         HpcDatasetUserAssociation.PRINCIPAL_INVESTIGATOR);
			             break;
			             
			        case PRINCIPAL_INVESTIGATOR_NAME:
			        	 datasetCollectionDTO = 
			        	 datasetBusService.getDatasets(
			        	                   firstName, lastName, 
				                           HpcDatasetUserAssociation.PRINCIPAL_INVESTIGATOR); 
			        	 break;
			        	 
			        case PROJECT_ID:
			        	 datasetCollectionDTO = 
			        	 datasetBusService.getDatasetsByProjectId(projectId); 
			        	 break;
			        	 
			        case NAME:
			        	 datasetCollectionDTO = 
			        	 datasetBusService.getDatasets(name, 
					                                   regex != null ? regex : true);
			        	 break;
			        	 
			        case DATA_TRANSFER_STATUS:
			        	 datasetCollectionDTO = 
			             datasetBusService.getDatasets(dataTransferStatus, uploadRequests, 
					                                   downloadRequests); 
			        	 break;
			        	 
			        case REGISTRATION_DATE_RANGE:
			        	 datasetCollectionDTO = 
			             datasetBusService.getDatasets(toCalendar(from), toCalendar(to)); 
			        	 break;
			 }
			 
		} catch(HpcException e) {
			    logger.error("RS: GET /datasets: failed:", e);
			    return errorResponse(e);
		}
		
		return okResponse(datasetCollectionDTO, true);
    }
    
    @Override
    public Response getDatasetsByPrimaryMetadata(
    		                     HpcFilePrimaryMetadataDTO primaryMetadataDTO)
	{
    	logger.info("Invoking RS: POST /datasets/query/primaryMetadata: " + 
    			    primaryMetadataDTO);
    	
		HpcDatasetCollectionDTO datasetCollectionDTO = null;
		try {
			 datasetCollectionDTO = datasetBusService.getDatasets(primaryMetadataDTO); 
			 
		} catch(HpcException e) {
			    logger.error("RS: POST /datasets/query/primaryMetadata: failed:", e);
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
    
    /**
     * Parse a calendar string.
     *
     * @param calStr The calendar string
     * @return Calendar instance
     * 
     * @throws HpcException if parsing exception
     */
    private Calendar toCalendar(String calStr) throws HpcException
    {
    	if(calStr == null) {
    	   return null;
    	}
    	
    	DateFormat format = new SimpleDateFormat(DATE_FORMAT);
    	Calendar calendar = Calendar.getInstance();
    	
    	try {
		 	 calendar.setTime(format.parse(calStr));
			
    	} catch(java.text.ParseException e) {
				throw new HpcException("Invalid date format. Date not in [" + DATE_FORMAT + "]",
						               HpcErrorType.INVALID_REQUEST_INPUT, e);
		}
    	
    	return calendar;
    }
    
    @Override
    public Response irodsUpload(String inputPath) 
    {
    	try {
    		 // Account credentials.
    		 IRODSAccount irodsAccount = 
    		              IRODSAccount.instance("52.7.244.225", 1247, "rods", 
    			         		                "irods", "/tempZone/home/rods", 
    			    	    	                "tempZone", "dsnetresource");
    		 
    		 // iRODs factories.
    		 IRODSFileFactory irodsFileFactory = 
    				          irodsFileSystem.getIRODSFileFactory(irodsAccount);
    		 IRODSAccessObjectFactory irodsAccessObjectFactory = 
    				                  irodsFileSystem.getIRODSAccessObjectFactory();
    		 
    		 // Upload a file.
    		 
    		 // Instantiate input/output files.
    		 File inputFile = new File(inputPath);
    		 String irodsFilePath = "/tempZone/home/rods/" + inputFile.getName();
    		 IRODSFile irodsOutputFile = irodsFileFactory.instanceIRODSFile(irodsFilePath);
    		 
    		 // Instantiate input/output streams.
    		 InputStream inputStream = new FileInputStream(inputFile);
    		 OutputStream outputStream = 
    				      irodsFileFactory.instanceIRODSFileOutputStream(irodsOutputFile);
    		 
    		 // Copy data in 1K chunks
    		 byte[] buffer = new byte[1024];
    		 int bytesRead = 0;
    		 try {
    		      while((bytesRead = inputStream.read(buffer)) > 0) {
    		             outputStream.write(buffer, 0, bytesRead);
    		      }
    		 } finally {
    		            inputStream.close();
    		            outputStream.close();
    		 }
    		 
    		 // Attach metadata.
    		 DataObjectAO irodsDataObjectAO = irodsAccessObjectFactory.getDataObjectAO(irodsAccount);
    		 AvuData avu = AvuData.instance("jargon-test", "Success - yeah!", "N/A");
    		 irodsDataObjectAO.addAVUMetadata(irodsFilePath, avu);
    		 
    		 
    	} catch(FileNotFoundException fnfe) {
    		    return errorResponse(new HpcException("File Not Found: " + fnfe.getMessage(),
                                                      HpcErrorType.UNEXPECTED_ERROR, fnfe));
    		
    	} catch(IOException ioe) {
    		    return errorResponse(new HpcException("I/O exception: " + ioe.getMessage(),
                                                      HpcErrorType.UNEXPECTED_ERROR, ioe));
    		    
    	} catch(JargonException e) {
    	        return errorResponse(new HpcException("iRODs error: " + e.getMessage(),
	                                                  HpcErrorType.UNEXPECTED_ERROR, e));
    	}
    	
    	return okResponse(null, false);
    }
    
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
    	BasicAWSCredentials cleversafeCredentials = new BasicAWSCredentials("vDZGQHw6OgBBpeI4D1CA", 
    			                                                            "OVDNthOhfl5npqdSfAD8T9FsIcJlsCJsmuRdfanr");
    	AmazonS3 s3client = new AmazonS3Client(cleversafeCredentials);
    	s3client.setEndpoint("https://8.40.18.82");
    	
        try {
        	 // Put an object.
             System.out.println("Uploading a new object to S3 from a file\n");
             File file = new File(path);
             PutObjectResult result = s3client.putObject(new PutObjectRequest(
              		                                    "CJ090115", "HPC-generated-key", file));
            
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
    	BasicAWSCredentials cleversafeCredentials = new BasicAWSCredentials("vDZGQHw6OgBBpeI4D1CA", 
    			                                                            "OVDNthOhfl5npqdSfAD8T9FsIcJlsCJsmuRdfanr");
    	TransferManager tm = new TransferManager(cleversafeCredentials);
    	tm.getAmazonS3Client().setEndpoint("https://8.40.18.82");
    	
    	// Create an upload request
    	System.out.println("Multipart Uploading a new object to S3 from a file\n");
        File file = new File(path);
        PutObjectRequest request = new PutObjectRequest("CJ090115", "HPC-generated-key", file);
        
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

 