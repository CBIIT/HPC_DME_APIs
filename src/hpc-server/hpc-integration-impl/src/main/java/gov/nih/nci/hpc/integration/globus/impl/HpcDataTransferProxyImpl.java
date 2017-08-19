package gov.nih.nci.hpc.integration.globus.impl;

import static gov.nih.nci.hpc.integration.HpcDataTransferProxy.getArchiveDestinationLocation;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.globusonline.transfer.APIError;
import org.globusonline.transfer.BaseTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient;
import org.globusonline.transfer.JSONTransferAPIClient.Result;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDataTransferReport;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

/**
 * <p>
 * HPC Data Transfer Proxy Globus Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataTransferProxyImpl implements HpcDataTransferProxy 
{
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Globus transfer status strings.
	private static final String FAILED_STATUS = "FAILED"; 
	private static final String INACTIVE_STATUS = "INACTIVE";
	private static final String ACTIVE_STATUS = "ACTIVE";
	private static final String SUCCEEDED_STATUS = "SUCCEEDED";
	private static final String PERMISSION_DENIED_STATUS = "PERMISSION_DENIED";
	
	private static final String NOT_DIRECTORY_GLOBUS_CODE = 
			                    "ExternalError.DirListingFailed.NotDirectory";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Globus connection instance.
	@Autowired
    private HpcGlobusConnection globusConnection = null;
	
	// The Globus archive destination. Used to upload data objects.
	@Autowired
	@Qualifier("hpcGlobusArchiveDestination")
	private HpcArchive baseArchiveDestination = null;
	
	// The Globus download source. Used to download data objects.
	@Autowired
	@Qualifier("hpcGlobusDownloadSource")
	private HpcArchive baseDownloadSource = null;
	
	// Retry template. Used to automatically retry Globus service calls.
	@Autowired
	private RetryTemplate retryTemplate = null;
	
	// The Globus active tasks queue size. 
	private int globusQueueSize = 0; 
    
	// The Logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param globusQueueSize The Globus active tasks queue size.
     * 
     */
	private HpcDataTransferProxyImpl(int globusQueueSize)
    {
		this.globusQueueSize = globusQueueSize;
    }
	
    /**
     * Default Constructor is disabled.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcDataTransferProxyImpl() throws HpcException
    {
    	throw new HpcException("Default Constructor Disabled", 
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataTransferProxy Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount,
    		                   String urlNotUsed) 
		                      throws HpcException
    {
    	// Note: At this time, there is no DOC specific configuration for Globus connection.
    	//       The Globus connection URL is configured via Spring. In the future, this may be 
    	//       a new requirement, so the 'url' parameter passed in will be used instead.
    	return globusConnection.authenticate(dataTransferAccount);
    }
    
    @Override
    public boolean acceptsTransferRequests(Object authenticatedToken) throws HpcException
    {
    	return false;
/*		JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);

		return retryTemplate.execute(arg0 -> 
		{
			try {
				 JSONObject jsonTasksLists = client.getResult("/task_list?filter=status:ACTIVE,INACTIVE").document;
				 logger.error("ERAN size of queue: " + jsonTasksLists.getInt("total"));
				 return jsonTasksLists.getInt("total") < globusQueueSize;
			
			} catch(Exception e) {
			        throw new HpcException("[GLOBUS] Failed to determine active tasks count", 
			                               HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
			}
		});*/
    }
    
    @Override
    public HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
    		                                            HpcDataObjectUploadRequest uploadRequest,
    		                                            List<HpcMetadataEntry> metadataEntries,
    		                                            HpcArchive baseArchiveDestinationNotUsed,
    		                                            HpcDataTransferProgressListener progressListener) 
    		                                           throws HpcException
    {
    	// Note: At this time, there is no DOC specific configuration for Globus base archive destination.
    	//       The Globus base archive destination is configured via Spring. In the future, this may be 
    	//       a new requirement, so the parameter passed in will be used instead of the spring injected one.
    	
    	// Progress listener not supported.
    	if(progressListener != null) {
    	   throw new HpcException("Globus data transfer doesn't support progress listener", 
    			                  HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
    	JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);

    	// Calculate the archive destination.
    	HpcFileLocation archiveDestinationLocation = 
    	   getArchiveDestinationLocation(baseArchiveDestination.getFileLocation(), 
    			                         uploadRequest.getPath(),
    		                             uploadRequest.getCallerObjectId(),
    		                             baseArchiveDestination.getType());
    	

    	// Submit a request to Globus to transfer the data.
    	String requestId = transferData(client,
    			                        uploadRequest.getSourceLocation(),
    			                        archiveDestinationLocation);
    	
    	// Package and return the response.
    	HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
    	uploadResponse.setArchiveLocation(archiveDestinationLocation);
    	uploadResponse.setDataTransferRequestId(requestId);
    	uploadResponse.setDataTransferType(HpcDataTransferType.GLOBUS);
    	uploadResponse.setDataTransferStarted(Calendar.getInstance());
    	uploadResponse.setDataTransferCompleted(null);
    	if(baseArchiveDestination.getType().equals(HpcArchiveType.TEMPORARY_ARCHIVE)) {
    	   uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE);
    	} else {
    		    uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE);
    	}
    	return uploadResponse;
    }
    
    @Override
    public HpcDataObjectDownloadResponse 
              downloadDataObject(Object authenticatedToken,
    		                     HpcDataObjectDownloadRequest downloadRequest,
    		                     HpcDataTransferProgressListener progressListener) 
    		                    throws HpcException
    {
    	// Progress listener not supported.
    	if(progressListener != null) {
    	   throw new HpcException("Globus data transfer doesn't support progress listener", 
    			                  HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
    	HpcDataObjectDownloadResponse response = new HpcDataObjectDownloadResponse();
    	
    	// Submit a request to Globus to transfer the data.
    	response.setDataTransferRequestId(
    			    transferData(globusConnection.getTransferClient(authenticatedToken),
    			                 downloadRequest.getArchiveLocation(),
    			                 downloadRequest.getDestinationLocation()));
    	response.setDestinationLocation(downloadRequest.getDestinationLocation());
    	
    	return response;
    }
    
    @Override
    public HpcDataTransferUploadStatus getDataTransferUploadStatus(Object authenticatedToken,
                                                                   String dataTransferRequestId) 
                                                                  throws HpcException
    {
		 HpcGlobusDataTransferReport report = getDataTransferReport(authenticatedToken, 
				                                                    dataTransferRequestId);
		 
		 if(report.getStatus().equals(SUCCEEDED_STATUS)) {
			// Upload completed successfully. Return status based on the archive type.
	    	if(baseArchiveDestination.getType().equals(HpcArchiveType.TEMPORARY_ARCHIVE)) {
	      	   return HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE;
	      	 } else {
	      		     return HpcDataTransferUploadStatus.ARCHIVED;
	      	 }	
		 }

		 if(report.getStatus().equals(FAILED_STATUS) || report.getStatus().equals(INACTIVE_STATUS) ||
		    (report.getStatus().equals(ACTIVE_STATUS) && report.getNiceStatus().equals(PERMISSION_DENIED_STATUS))) {
			// Upload failed.
 			return HpcDataTransferUploadStatus.FAILED;
 		 }
		 
		 // Upload is in progress. Return status based on the archive type.
    	 if(baseArchiveDestination.getType().equals(HpcArchiveType.TEMPORARY_ARCHIVE)) {
     	    return HpcDataTransferUploadStatus.IN_PROGRESS_TO_TEMPORARY_ARCHIVE;
     	 } else {
     	         return HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE;
     	 }		 
    }
    
    @Override
    public HpcDataTransferDownloadStatus getDataTransferDownloadStatus(Object authenticatedToken,
                                                                       String dataTransferRequestId) 
                                                                      throws HpcException
    {
		 HpcGlobusDataTransferReport report = getDataTransferReport(authenticatedToken, 
				                                                    dataTransferRequestId);
		 if(report.getStatus().equals(SUCCEEDED_STATUS)) {
			// Download completed successfully.
	    	return HpcDataTransferDownloadStatus.COMPLETED;
		 }
		 
		 if(report.getStatus().equals(FAILED_STATUS) || report.getStatus().equals(INACTIVE_STATUS) ||
		    (report.getStatus().equals(ACTIVE_STATUS) && report.getNiceStatus().equals(PERMISSION_DENIED_STATUS))) {
			// Download failed.
		 	return HpcDataTransferDownloadStatus.FAILED;
		 }
		 
		 // Download still in progress.
		 return HpcDataTransferDownloadStatus.IN_PROGRESS;
    }
    
    @Override
    public long getDataTransferSize(Object authenticatedToken,
                                    String dataTransferRequestId) 
                                   throws HpcException
    {
    	return getDataTransferReport(authenticatedToken, dataTransferRequestId).getBytesTransferred();
    }
    
    @Override
    public HpcPathAttributes getPathAttributes(Object authenticatedToken, 
                                               HpcFileLocation fileLocation,
                                               boolean getSize) 
                                              throws HpcException
    {
    	JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);
    	autoActivate(fileLocation.getFileContainerId(), client);
    	return getPathAttributes(fileLocation, client, getSize);
    }
    
    @Override
    public String getFilePath(String fileId, boolean archive) throws HpcException
    {
	  	return archive ? fileId.replaceFirst(baseArchiveDestination.getFileLocation().getFileId(), 
                                             baseArchiveDestination.getDirectory()) : 
                         fileId.replaceFirst(baseDownloadSource.getFileLocation().getFileId(), 
                                             baseDownloadSource.getDirectory());
    }
    
    @Override
    public HpcFileLocation getDownloadSourceLocation(String path) throws HpcException
    {
    	// Create a source location. (This is a local GLOBUS endpoint).
    	HpcFileLocation sourceLocation = new HpcFileLocation();
    	sourceLocation.setFileContainerId(baseDownloadSource.getFileLocation().getFileContainerId());
    	sourceLocation.setFileId(baseDownloadSource.getFileLocation().getFileId() + "/" + 
    	                         UUID.randomUUID().toString());
    	
    	return sourceLocation;
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Submit a data transfer request.
     *
     * @param client Client API instance.
     * @param source The source endpoint.
     * @param destination The destination endpoint.
     * @return The data transfer request ID.
     * @throws HpcException on data transfer system failure.
     */
    private String transferData(JSONTransferAPIClient client,
                                HpcFileLocation source, HpcFileLocation destination)
                               throws HpcException
	{
    	// Activate endpoints.
		autoActivate(source.getFileContainerId(), client);
		autoActivate(destination.getFileContainerId(), client); 
		
		// Submit transfer request.
		return retryTemplate.execute(arg0 -> 
		{
			try {
				 JSONTransferAPIClient.Result r;
				 r = client.getResult("/transfer/submission_id");
				 String submissionId = r.document.getString("value");
				 JSONObject transfer = new JSONObject();
				 transfer.put("DATA_TYPE", "transfer");
				 transfer.put("submission_id", submissionId);
				 transfer.put("verify_checksum", true);
				 transfer.put("delete_destination_extra", false);
				 transfer.put("preserve_timestamp", false);
				 transfer.put("encrypt_data", false);
				
				 JSONObject item = setJSONItem(source, destination, client);
				 transfer.append("DATA", item);
				
				 r = client.postResult("/transfer", transfer, null);
				 String taskId = r.document.getString("task_id");
				 logger.debug("Transfer task id :"+ taskId );
				
				 return taskId;
			
			} catch(APIError error) {
			        throw new HpcException("[GLOBUS] Failed to transfer: " + error.message +
			    	     	               ". Source: " + source + 
		                                   ". Destination: " + destination, 
	                                       HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS,
	                                       error);
			        
			} catch(Exception e) {
			        throw new HpcException("[GLOBUS] Failed to transfer. Source: " + source +
			        		               ". Destination: " + destination, 
			                               HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
			}
		});
	}
    
	private JSONObject setJSONItem(HpcFileLocation source, HpcFileLocation destination, 
			                       JSONTransferAPIClient client)  
			                      throws HpcException 
	{
    	JSONObject item = new JSONObject();
    	try {
	        item.put("DATA_TYPE", "transfer_item");
	        item.put("source_endpoint", source.getFileContainerId());
	        item.put("source_path", source.getFileId());
	        item.put("destination_endpoint", destination.getFileContainerId());
	        item.put("destination_path", destination.getFileId());
	        item.put("recursive", getPathAttributes(source, client, false).getIsDirectory());
	        return item;
	        
		} catch(JSONException e) {
	        throw new HpcException(
	        		     "[GLOBUS] Failed to create JSON: " + source + ", " + destination, 
	        		     HpcErrorType.DATA_TRANSFER_ERROR, e);
		}    
    }

	private void autoActivate(String endpointName, JSONTransferAPIClient client)
                             throws HpcException 
	{
		retryTemplate.execute(arg0 -> 
		{
			try {
	             String resource = BaseTransferAPIClient.endpointPath(endpointName)
	                               + "/autoactivate?if_expires_in=100";
	             client.postResult(resource, null, null);
	             return null;
	             
			} catch(APIError error) {
				    HpcIntegratedSystem integratedSystem = error.statusCode >= 500 ? 
				    		                               HpcIntegratedSystem.GLOBUS : null;
				    String message = "";
				    switch(error.statusCode) {
				           case 404:
				                message = "[GLOBUS] Endpoint doesn't exist. Make sure the endpoint name " + 
				                          "is correct and active: " + endpointName;
				                break;
				                
				           case 403:
				        	    message = "[GLOBUS] Endpoint permission denied: " + endpointName;
				        	    break;
				        	    
				           case 503:
				        	    message = "[GLOBUS] Service is down for maintenance";
				        	    break;
				        	    
				           default:
				        	    message = "[GLOBUS] Failed to activate endpoint: " + endpointName;
				        	  	break;
				    }
				    
		            throw new HpcException(message, HpcErrorType.DATA_TRANSFER_ERROR, integratedSystem, error);
		            
			} catch(Exception e) {
			        throw new HpcException("[GLOBUS] Failed to activate endpoint: " + endpointName, 
			        	                   HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
			}
		});
	}
	
    /**
     * Get a data transfer report.
     *
     * @param authenticatedToken An authenticated token.
     * @param dataTransferRequestId The data transfer request ID.
     * @return The data transfer report for the request.
     * @throws HpcException on data transfer system failure.
     */
    private HpcGlobusDataTransferReport getDataTransferReport(Object authenticatedToken,
                                                              String dataTransferRequestId) 
                                                             throws HpcException
    {
		JSONTransferAPIClient client = globusConnection.getTransferClient(authenticatedToken);

		return retryTemplate.execute(arg0 -> 
		{
			try {
				 JSONObject jsonReport = client.getResult("/task/" +  dataTransferRequestId).document;
				
				 HpcGlobusDataTransferReport report = new HpcGlobusDataTransferReport();
				 report.setStatus(jsonReport.getString("status"));
			     report.setNiceStatus(jsonReport.getString("nice_status"));
				 report.setBytesTransferred(jsonReport.getLong("bytes_transferred"));
				 
				 return report;
			
			} catch(Exception e) {
			        throw new HpcException("[GLOBUS] Failed to get task report for task: " + dataTransferRequestId, 
			                               HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
			}
		});
    }
    
    /**
     * Get attributes of a file/directory.
     *
     * @param fileLocation The endpoint/path to check.
     * @param client Globus client API instance.
     * @param getSize If set to true, the file/directory size will be returned. 
     * @return The path attributes.
     * @throws HpcException on data transfer system failure.
     */
    private HpcPathAttributes getPathAttributes(HpcFileLocation fileLocation,
                                                JSONTransferAPIClient client,
                                                boolean getSize) 
                                               throws HpcException
    {
    	HpcPathAttributes pathAttributes = new HpcPathAttributes();
    	pathAttributes.setExists(false);
    	pathAttributes.setIsDirectory(false);
    	pathAttributes.setIsFile(false);
    	pathAttributes.setSize(0);
    	pathAttributes.setIsAccessible(true);
    	
    	// Invoke the Globus directory listing service.
        try {
        	 Result dirContent = listDirectoryContent(fileLocation, client);
        	 pathAttributes.setExists(true);
        	 pathAttributes.setIsDirectory(true);
        	 pathAttributes.setSize(getSize ? getDirectorySize(dirContent, client) : -1);
        	
        } catch(APIError error) {
        	    if(error.statusCode == 502) {
        	       if(error.code.equals(NOT_DIRECTORY_GLOBUS_CODE)) {
        	          // Path exists as a single file
        	          pathAttributes.setExists(true);
        	          pathAttributes.setIsFile(true);
        	          pathAttributes.setSize(getSize ? getFileSize(fileLocation, client) : -1);
        	       } else {
        	    	       throw new HpcException("Invalid file location:" + fileLocation,
        	    	    		                  HpcErrorType.INVALID_REQUEST_INPUT, error);
        	       }
        	    } else if(error.statusCode == 403) {
         	              // Permission denied.
         	              pathAttributes.setExists(true);
         	              pathAttributes.setIsAccessible(false);
         	    } 
        	    // else path was not found. 
        	    
        } catch(Exception e) {
	            throw new HpcException("[GLOBUS] Failed to get path attributes: " + fileLocation, 
       		                           HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.GLOBUS, e);
        }
        
    	return pathAttributes;
    }
    
    /**
     * Get file size.
     *
     * @param fileLocation The file endpoint/path.
     * @param client Globus client API instance.
     * @return The file size in bytes.
     */
    private long getFileSize(HpcFileLocation fileLocation, JSONTransferAPIClient client)
    {
    	// Get the directory location of the file.
    	HpcFileLocation dirLocation = new HpcFileLocation();
    	dirLocation.setFileContainerId(fileLocation.getFileContainerId());
    	int fileNameIndex = fileLocation.getFileId().lastIndexOf('/');
    	if(fileNameIndex != -1)
    		dirLocation.setFileId(fileLocation.getFileId().substring(0, fileNameIndex));
    	else
    		dirLocation.setFileId(fileLocation.getFileId());
    	
    	// Extract the file name from the path.
    	String fileName = fileLocation.getFileId().substring(fileNameIndex + 1);
    	
    	// List the directory content.
    	try {
             Result dirContent = listDirectoryContent(dirLocation, client);  
             JSONArray jsonFiles = dirContent.document.getJSONArray("DATA");
             if(jsonFiles != null) {
                // Iterate through the directory files, and locate the file we look for.
            	int filesNum = jsonFiles.length();
                for(int i = 0; i < filesNum; i++) {
                	JSONObject jsonFile = jsonFiles.getJSONObject(i);
                	String jsonFileName = jsonFile.getString("name");
                	if(jsonFileName != null && jsonFileName.equals(fileName)) {
                	   // The file was found. Return its size
                	   return jsonFile.getLong("size");
                	}
                }
             }
             
    	} catch(Exception e) {
    		    // Unexpected error. Eat this.
    		    logger.error("Failed to calculate file size", e); 
    	}
    	
    	// File not found, or exception was caught.
    	return 0;
    }	
    
    /**
     * Get directory size. Sums up the size of all the files in this directory recursively.
     *
     * @param dirContent The directory content.
     * @param client Globus client API instance.
     * @return The directory size in bytes.
     */
    private long getDirectorySize(Result dirContent, JSONTransferAPIClient client)
    {
    	try {
             JSONArray jsonFiles = dirContent.document.getJSONArray("DATA");
             if(jsonFiles != null) {
                // Iterate through the directory files, and sum up the files size.
            	int filesNum = jsonFiles.length();
            	long size = 0;
                for(int i = 0; i < filesNum; i++) {
                	JSONObject jsonFile = jsonFiles.getJSONObject(i);
                	String jsonFileType = jsonFile.getString("type");
                	if(jsonFileType != null) {
                	   if(jsonFileType.equals("file")) {
                		  // This is a file. Add its size to the total;
                	      size += jsonFile.getLong("size");
                	      continue;
                	   } else if(jsonFileType.equals("dir")) {
                		         // It's a sub directory. Make a recursive call, to add its size.
                		         HpcFileLocation subDirLocation = new HpcFileLocation();
                		         subDirLocation.setFileContainerId(dirContent.document.getString("endpoint"));
                		         subDirLocation.setFileId(dirContent.document.getString("path") +
                		        		                '/' + jsonFile.getString("name"));
                		         
                		         size += getDirectorySize(listDirectoryContent(subDirLocation, 
                		        		                                       client), 
                		        		                  client);
                	   }
                	}
                }
                return size;
             }
             
    	} catch(Exception e) {
    		    // Unexpected error. Eat this.
    		    logger.error("Failed to calculate directory size", e);
    	}
    	
    	// Directory not found, or exception was caught. 
    	return 0;
    }	
    
    /**
     * Call the Globus list directory content service.
     * See: https://docs.globus.org/api/transfer/file_operations/#list_directory_contents
     *
     * @param dirLocation The directory endpoint/path.
     * @param client Globus client API instance.
     * @return The file size in bytes.
     * @throws Exception 
     * @throws HpcException on service failure.
     */
    private Result listDirectoryContent(HpcFileLocation dirLocation, 
    		                            JSONTransferAPIClient client)
    		                           throws Exception
    {
		Map<String, String> params = new HashMap<String, String>();
		params.put("path", dirLocation.getFileId());
		
		return retryTemplate.execute(arg0 -> 
		{
			String resource = BaseTransferAPIClient.endpointPath(dirLocation.getFileContainerId()) + "/ls";
		 	return client.getResult(resource, params);
		});
    }
}
