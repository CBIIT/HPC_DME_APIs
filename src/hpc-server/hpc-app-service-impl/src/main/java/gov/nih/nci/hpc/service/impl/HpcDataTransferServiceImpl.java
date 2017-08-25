/**
 * HpcDataTransferServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidFileLocation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.model.HpcDataTransferAuthenticatedToken;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;

/**
 * <p>
 * HPC Data Transfer Service Implementation.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 */

public class HpcDataTransferServiceImpl implements HpcDataTransferService
{    
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
    // Data transfer system generated metadata attributes (attach to files in archive)
	private static final String PATH_ATTRIBUTE = "path";
	private static final String USER_ID_ATTRIBUTE = "user_id";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// Map data transfer type to its proxy impl.
	private Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies = 
			new EnumMap<>(HpcDataTransferType.class);
	
	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;
	
	// Data object download DAO.
	@Autowired
	private HpcDataDownloadDAO dataDownloadDAO = null;
	
	// Event service
	@Autowired
	private HpcEventService eventService = null;
	
	// DOC configuration locator.
	@Autowired
	private HpcDocConfigurationLocator docConfigurationLocator = null;
	
	// The download directory
	private String downloadDirectory = null;
	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param dataTransferProxies The data transfer proxies.
     * @param downloadDirectory The download directory.
     * @throws HpcException on spring configuration error. 
     */
    public HpcDataTransferServiceImpl(
    		  Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies,
    		  String downloadDirectory) 
    		  throws HpcException
    {
    	if(dataTransferProxies == null || dataTransferProxies.isEmpty() || 
    	   downloadDirectory == null || downloadDirectory.isEmpty()) {
    	   throw new HpcException("Null or empty map of data transfer proxies, or download directory",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.dataTransferProxies.putAll(dataTransferProxies);
    	this.downloadDirectory = downloadDirectory;
    }   
    
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    @SuppressWarnings("unused")
	private HpcDataTransferServiceImpl() throws HpcException
    {
    	throw new HpcException("Default Constructor disabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataTransferService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
	public HpcDataObjectUploadResponse uploadDataObject(HpcFileLocation sourceLocation, 
			                                            File sourceFile, 
			                                            String path, String userId,
			                                            String callerObjectId, String doc)
	                                                   throws HpcException
	{
    	// Validate one and only one data source is provided.
    	if(sourceLocation == null && sourceFile == null) {
    	   throw new HpcException("No data transfer source or data attachment provided",
	                              HpcErrorType.INVALID_REQUEST_INPUT);	
    	}
    	if(sourceLocation != null && sourceFile != null) {
     	   throw new HpcException("Both data transfer source and data attachment provided",
 	                              HpcErrorType.INVALID_REQUEST_INPUT);	
     	}
    	
    	// Create an upload request.
    	HpcDataObjectUploadRequest uploadRequest = new HpcDataObjectUploadRequest();
    	uploadRequest.setPath(path);
    	uploadRequest.setUserId(userId);
    	uploadRequest.setCallerObjectId(callerObjectId);
    	uploadRequest.setSourceLocation(sourceLocation);
    	uploadRequest.setSourceFile(sourceFile);
    	uploadRequest.setDoc(doc);
    	
		// Upload the data object file.
	    return uploadDataObject(uploadRequest);	
	}
    
    @Override
	public HpcDataObjectDownloadResponse downloadDataObject(
			                                     String path,
                                                 HpcFileLocation archiveLocation, 
                                                 HpcFileLocation destinationLocation,
                                                 HpcDataTransferType dataTransferType,
                                                 String doc, String userId, 
                                                 boolean completionEvent) 
                                                 throws HpcException
    {
    	HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
    	downloadRequest.setDataTransferType(dataTransferType);
    	downloadRequest.setArchiveLocation(archiveLocation);
    	downloadRequest.setDestinationLocation(destinationLocation);
    	downloadRequest.setPath(path);
    	downloadRequest.setDoc(doc);
    	downloadRequest.setUserId(userId);
    	downloadRequest.setCompletionEvent(completionEvent);
    	
    	// Create a data object file to download the data if a destination was not provided.
    	if(destinationLocation == null) {
           downloadRequest.setDestinationFile(createDownloadFile());
    	}

    	return downloadDataObject(downloadRequest);
    }
    
    @Override
    public void deleteDataObject(HpcFileLocation fileLocation, 
                                 HpcDataTransferType dataTransferType,
                                 String doc) 
                                throws HpcException
    {
    	// Input validation.
    	if(!HpcDomainValidator.isValidFileLocation(fileLocation)) {	
    	   throw new HpcException("Invalid file location", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	dataTransferProxies.get(dataTransferType).deleteDataObject(
    			                                        getAuthenticatedToken(dataTransferType, doc), 
    			                                        fileLocation);
    }
    
	@Override   
	public HpcDataTransferUploadReport getDataTransferUploadStatus(HpcDataTransferType dataTransferType,
			                                                       String dataTransferRequestId, String doc) 
                                                                  throws HpcException
    {	// Input Validation.
		if(dataTransferRequestId == null) {
		   throw new HpcException("Null data transfer request ID", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		return dataTransferProxies.get(dataTransferType).
				   getDataTransferUploadStatus(getAuthenticatedToken(dataTransferType, doc), 
           	                                   dataTransferRequestId);
    }	
	
	@Override   
	public HpcDataTransferDownloadReport getDataTransferDownloadStatus(HpcDataTransferType dataTransferType,
			                                                           String dataTransferRequestId,
			                                                           String doc) 
                                                                      throws HpcException
    {	// Input Validation.
		if(dataTransferRequestId == null) {
		   throw new HpcException("Null data transfer request ID", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		return dataTransferProxies.get(dataTransferType).
				   getDataTransferDownloadStatus(getAuthenticatedToken(dataTransferType, doc), 
           	                                     dataTransferRequestId);
    }	
	
	@Override   
	public long getDataTransferSize(HpcDataTransferType dataTransferType,
			                        String dataTransferRequestId, String doc) 
                                   throws HpcException
    {	// Input Validation.
		if(dataTransferRequestId == null) {
		   throw new HpcException("Null data transfer request ID", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		return dataTransferProxies.get(dataTransferType).
				   getDataTransferSize(getAuthenticatedToken(dataTransferType, doc), 
           	                           dataTransferRequestId);
    }	
	
	@Override
	public HpcPathAttributes getPathAttributes(HpcDataTransferType dataTransferType,
			                                   HpcFileLocation fileLocation,
			                                   boolean getSize, String doc) 
                                              throws HpcException
    {
    	// Input validation.
    	if(!HpcDomainValidator.isValidFileLocation(fileLocation)) {	
    	   throw new HpcException("Invalid file location", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	return dataTransferProxies.get(dataTransferType).
    			   getPathAttributes(getAuthenticatedToken(dataTransferType, doc), 
    			                     fileLocation, getSize);
    }
	
	@Override
	public File getArchiveFile(HpcDataTransferType dataTransferType,
                               String fileId)  
    	                      throws HpcException
    {
    	// Input validation.
    	if(fileId == null) {	
    	   throw new HpcException("Invalid file id", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	File file = new File(dataTransferProxies.get(dataTransferType).getFilePath(fileId, true));
    	if(!file.exists()) {
    	   throw new HpcException("Archive file could not be found: " + file.getAbsolutePath(),
    			                  HpcRequestRejectReason.FILE_NOT_FOUND);
    	}
    	
    	return file;
    }
	
	@Override
	public List<HpcDataObjectDownloadTask> getDataObjectDownloadTasks(
                                              HpcDataTransferType dataTransferType) throws HpcException
	{
		return dataDownloadDAO.getDataObjectDownloadTasks(dataTransferType);
	}
	
	@Override
	public HpcDownloadTaskStatus getDownloadTaskStatus(String taskId, HpcDownloadTaskType taskType) 
			                                          throws HpcException
	{
		if(taskType == null) {
		   throw new HpcException("Null download task type", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		HpcDownloadTaskStatus taskStatus = new HpcDownloadTaskStatus();
		HpcDownloadTaskResult taskResult = dataDownloadDAO.getDownloadTaskResult(taskId, taskType);
		if(taskResult != null) {
		   // Task completed or failed. Return the result.
		   taskStatus.setInProgress(false);
		   taskStatus.setResult(taskResult);
		   return taskStatus;
		}
	    
		// Task still in-progress. Return either the data-object or the collection active download task.
		taskStatus.setInProgress(true);
		
		if(taskType.equals(HpcDownloadTaskType.DATA_OBJECT)) {
		   HpcDataObjectDownloadTask task = dataDownloadDAO.getDataObjectDownloadTask(taskId);
		   if(task != null) {
		      taskStatus.setDataObjectDownloadTask(task);
		      return taskStatus;	
		   }
		}
		
		if(taskType.equals(HpcDownloadTaskType.COLLECTION) || 
		   taskType.equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
		   HpcCollectionDownloadTask task = dataDownloadDAO.getCollectionDownloadTask(taskId);
		   if(task != null) {
			  taskStatus.setCollectionDownloadTask(task);
			  return taskStatus;	
		   }
		}
		
		// Task not found.
		return null;
	}
	
	@Override
	public void completeDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask,
			                                   boolean result, String message, Calendar completed)
	                                         throws HpcException
	{
		// Input validation
		if(downloadTask == null) {
		   throw new HpcException("Invalid data object download task", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		// Delete the staged download file.
		if(!FileUtils.deleteQuietly(new File(downloadTask.getDownloadFilePath()))) {
		   logger.error("Failed to delete file: " + downloadTask.getDownloadFilePath());
		}
		
		// Cleanup the DB record.
		dataDownloadDAO.deleteDataObjectDownloadTask(downloadTask.getId());
		
		// Create a task result object.
		HpcDownloadTaskResult taskResult = new HpcDownloadTaskResult();
		taskResult.setId(downloadTask.getId());
	    taskResult.setUserId(downloadTask.getUserId());
	    taskResult.setPath(downloadTask.getPath());
	    taskResult.setDoc(downloadTask.getDoc());
	    taskResult.setDataTransferRequestId(downloadTask.getDataTransferRequestId());
	    taskResult.setDataTransferType(downloadTask.getDataTransferType());
	    taskResult.setDestinationLocation(downloadTask.getDestinationLocation());
	    taskResult.setResult(result);
	    taskResult.setType(HpcDownloadTaskType.DATA_OBJECT);
	    taskResult.setMessage(message);
	    taskResult.setCreated(downloadTask.getCreated());
	    taskResult.setCompleted(completed);	
		dataDownloadDAO.upsertDownloadTaskResult(taskResult);
	}
	
	@Override
    public void continueDataObjectDownloadTask(HpcDataObjectDownloadTask downloadTask) 
                                              throws HpcException
    {
    	// Check if Globus accepts transfer requests at this time.
	    if(dataTransferProxies.get(downloadTask.getDataTransferType()).acceptsTransferRequests(
	       getAuthenticatedToken(downloadTask.getDataTransferType(), downloadTask.getDoc()))) {
		      // Globus accepts requests - submit the 2nd hop async download (to Globus).
	    	  HpcDataObjectDownloadRequest secondHopDownloadRequest = new HpcDataObjectDownloadRequest();
	    	  secondHopDownloadRequest.setArchiveLocation(downloadTask.getArchiveLocation());
	    	  secondHopDownloadRequest.setCompletionEvent(downloadTask.getCompletionEvent());
	    	  secondHopDownloadRequest.setDataTransferType(downloadTask.getDataTransferType());
	    	  secondHopDownloadRequest.setDestinationLocation(downloadTask.getDestinationLocation());
	    	  secondHopDownloadRequest.setDoc(downloadTask.getDoc());
	    	  secondHopDownloadRequest.setPath(downloadTask.getPath());
	    	  secondHopDownloadRequest.setUserId(downloadTask.getUserId());
	    	  
		      downloadTask.setDataTransferRequestId(
		    		          downloadDataObject(secondHopDownloadRequest).getDataTransferRequestId());
	    	  
	    	  // Persist the download task.
		      downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
			  dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
	    }
    }
	
	@Override
	public HpcCollectionDownloadTask downloadCollection(String path,
                                                        HpcFileLocation destinationLocation,
                                                        String userId, String doc)
                                                       throws HpcException
    {
		// Validate the requested destination location.
		validateDownloadDestinationFileLocation(HpcDataTransferType.GLOBUS, destinationLocation, 
				                                false, doc);
		
		// Create a new collection download task.
		HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setDestinationLocation(destinationLocation);
		downloadTask.setPath(path);
		downloadTask.setUserId(userId);
		downloadTask.setType(HpcDownloadTaskType.COLLECTION);
		downloadTask.setStatus(HpcCollectionDownloadTaskStatus.RECEIVED);
		
		// Persist the request.
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);
		
		return downloadTask;
    }
	
	@Override
	public HpcCollectionDownloadTask downloadDataObjects(List<String> dataObjectPath,
                                                         HpcFileLocation destinationLocation,
                                                         String userId, String doc)
                                                        throws HpcException
    {
		// Validate the requested destination location.
		validateDownloadDestinationFileLocation(HpcDataTransferType.GLOBUS, destinationLocation, 
				                                false, doc);
		
		// Create a new collection download task.
		HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
		downloadTask.setCreated(Calendar.getInstance());
		downloadTask.setDestinationLocation(destinationLocation);
		downloadTask.getDataObjectPaths().addAll(dataObjectPath);
		downloadTask.setUserId(userId);
		downloadTask.setType(HpcDownloadTaskType.DATA_OBJECT_LIST);
		downloadTask.setStatus(HpcCollectionDownloadTaskStatus.RECEIVED);
		
		// Persist the request.
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);
		
		return downloadTask;
    }
	
	@Override
	public void updateCollectionDownloadTask(HpcCollectionDownloadTask downloadTask)
                                            throws HpcException
    {
		dataDownloadDAO.upsertCollectionDownloadTask(downloadTask);
    }
	
	@Override
	public List<HpcCollectionDownloadTask> getCollectionDownloadTasks(
                                              HpcCollectionDownloadTaskStatus status) 
                                              throws HpcException
	{
		return dataDownloadDAO.getCollectionDownloadTasks(status);
	}
	
	@Override
	public void completeCollectionDownloadTask(HpcCollectionDownloadTask downloadTask,
			                                   boolean result, String message, Calendar completed)
	                                         throws HpcException
	{
		// Input validation
		if(downloadTask == null) {
		   throw new HpcException("Invalid collection download task", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		// Cleanup the DB record.
		dataDownloadDAO.deleteCollectionDownloadTask(downloadTask.getId());
		
		// Create a task result object.
		HpcDownloadTaskResult taskResult = new HpcDownloadTaskResult();
		taskResult.setId(downloadTask.getId());
	    taskResult.setUserId(downloadTask.getUserId());
	    taskResult.setPath(downloadTask.getPath());
	    taskResult.setDestinationLocation(downloadTask.getDestinationLocation());
	    taskResult.setResult(result);
	    taskResult.setType(downloadTask.getType());
	    taskResult.setMessage(message);
	    taskResult.setCreated(downloadTask.getCreated());
	    taskResult.setCompleted(completed);	
	    taskResult.getItems().addAll(downloadTask.getItems());
		dataDownloadDAO.upsertDownloadTaskResult(taskResult);
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get the data transfer authenticated token from the request context.
     * If it's not in the context, get a token by authenticating.
     * 
     * @param dataTransferType The data transfer type.
     * @param doc The doc archive to authenticate to.
     * @return A data transfer authenticated token.
     * @throws HpcException If it failed to obtain an authentication token.
     */
    private Object getAuthenticatedToken(HpcDataTransferType dataTransferType,
    		                             String doc) throws HpcException
    {
    	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    	if(invoker == null) {
	       throw new HpcException("Unknown user", HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
    	// Search for an existing token.
    	for(HpcDataTransferAuthenticatedToken authenticatedToken : 
    		invoker.getDataTransferAuthenticatedTokens()) {
    		if(authenticatedToken.getDataTransferType().equals(dataTransferType) &&
    	       authenticatedToken.getDoc().equals(doc)) {
    		   logger.error("ERAN: Reusing " + dataTransferType + " token");
    	       return authenticatedToken.getDataTransferAuthenticatedToken();
    		} 
    	}

    	// No authenticated token found for this request. Create one.
    	HpcIntegratedSystemAccount dataTransferSystemAccount = systemAccountLocator.getSystemAccount(dataTransferType);
    	if(dataTransferSystemAccount == null) {
    	   throw new HpcException("System account not registered for " + dataTransferType.value(), 
    			                  HpcErrorType.UNEXPECTED_ERROR);
    	}
        Object token = dataTransferProxies.get(dataTransferType).
        		           authenticate(dataTransferSystemAccount, 
        		                        docConfigurationLocator.getArchiveURL(doc, dataTransferType));
    	if(token == null) {
    	   throw new HpcException("Invalid data transfer account credentials",
    			                  HpcErrorType.DATA_TRANSFER_ERROR, 
    			                  dataTransferSystemAccount.getIntegratedSystem());
    	}
    	
    	// Store token on the request context.
    	HpcDataTransferAuthenticatedToken authenticatedToken = new HpcDataTransferAuthenticatedToken();
    	authenticatedToken.setDataTransferAuthenticatedToken(token);
    	authenticatedToken.setDataTransferType(dataTransferType);
    	authenticatedToken.setDoc(doc);
    	invoker.getDataTransferAuthenticatedTokens().add(authenticatedToken);
    	HpcRequestContext.setRequestInvoker(invoker);
    	
    	return token;
    }
    
    /**
     * Calculate download destination location.
     * 
     * @param destinationLocation The destination location requested by the caller..
     * @param dataTransferType The data transfer type to create the request.
     * @param sourcePath The source path.
     * @param doc The doc (needed to determine the archive connection config).
     * @return The calculated destination file location. The source file name is added if the caller provided 
     *         a directory destination.
     * @throws HpcException on service failure.
     */    
    private HpcFileLocation calculateDownloadDestinationFileLocation(HpcFileLocation destinationLocation,
    		                                                         HpcDataTransferType dataTransferType,
    		                                                         String sourcePath, String doc) 
    		                                                        throws HpcException
    {
    	// Validate the download destination location.
	   	HpcPathAttributes pathAttributes = 	   			
	   	   validateDownloadDestinationFileLocation(dataTransferType, destinationLocation, false, doc);

	   	// Calculate the destination.
	    if(pathAttributes.getIsDirectory()) {
           // Caller requested to download to a directory. Append the source file name.
           HpcFileLocation calcDestination = new HpcFileLocation();
           calcDestination.setFileContainerId(destinationLocation.getFileContainerId());
           calcDestination.setFileId(destinationLocation.getFileId() + 
                                     sourcePath.substring(sourcePath.lastIndexOf('/')));
           
           // Validate the calculated download destination.
           validateDownloadDestinationFileLocation(dataTransferType, calcDestination, true, doc);
           
           return calcDestination;
           
	    } else {
	    	    return destinationLocation;
	    }
    }
    
    /**
     * Generate metadata to attach to the data object in the archive:
     * 1. Path - the data object path in the DM system (iRODS)
     * 2. User ID - the user id that registers the data object. 
     * 
     * @param path The data object logical path.
     * @param userId The user-id uploaded the data.
     * @return a List of the 2 metadata.
     */
    private List<HpcMetadataEntry> generateMetadata(String path, String userId) 
    {
       	List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
       	
       	// Create the user-id metadata.
       	HpcMetadataEntry entry = new HpcMetadataEntry();
    	entry.setAttribute(USER_ID_ATTRIBUTE);
    	entry.setValue(userId);
       	metadataEntries.add(entry);
       	
       	// Create the path metadata.
       	entry = new HpcMetadataEntry();
    	entry.setAttribute(PATH_ATTRIBUTE);
    	entry.setValue(path);
       	metadataEntries.add(entry);
       	
       	return metadataEntries;
    }
    
    /**
     * Create an empty file.
     * 
     * @param filePath The file's path
     * @return The created file.
     * @throws HpcException on service failure.
     */
    private File createFile(String filePath) throws HpcException
    {
    	File file = new File(filePath);
    	try {
    		 FileUtils.touch(file);
  	         
    	} catch(IOException e) {
  		        throw new HpcException("Failed to create a file: " + filePath, 
                                       HpcErrorType.DATA_TRANSFER_ERROR, e);
    	}
    	
    	return file;
  	}
    
    /**
     * Create an empty file placed in the download folder.
     * 
     * @return The created file.
     * @throws HpcException on service failure.
     */
    private File createDownloadFile() throws HpcException
    {
    	return createFile(downloadDirectory + "/" + UUID.randomUUID().toString());
  	}
   
    /**
     * Upload a data object file.
     *
     * @param uploadRequest The data upload request.
     * @return A data object upload response.
     * @throws HpcException on service failure.
     */
    private HpcDataObjectUploadResponse uploadDataObject(HpcDataObjectUploadRequest uploadRequest) 
                                                        throws HpcException
    {
    	// Input validation.
    	if(uploadRequest.getPath() == null || uploadRequest.getUserId() == null) {
    	   throw new HpcException("Null data object path or user-id", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
        	
    	// Determine the data transfer type.
    	HpcDataTransferType dataTransferType;
    	if(uploadRequest.getSourceLocation() != null) {
    	   dataTransferType = HpcDataTransferType.GLOBUS;
    	   
    	} else if(uploadRequest.getSourceFile() != null) {
    		      dataTransferType = HpcDataTransferType.S_3;
    		      
    	} else {
    		    // Could not determine data transfer type.
    		    throw new HpcException("Could not determine data transfer type",
    		    		               HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
    	// Validate source location exists and accessible.
    	String doc = uploadRequest.getDoc();
    	validateUploadSourceFileLocation(dataTransferType, uploadRequest.getSourceLocation(), doc);
    	
    	// Check that the data transfer system can accept transfer requests.
    	Object authenticatedToken = getAuthenticatedToken(dataTransferType, doc);
    	if(!dataTransferProxies.get(dataTransferType).acceptsTransferRequests(authenticatedToken)) {
    	   // The data transfer system is busy. Queue the request (upload status set to 'RECEIVED'),
    	   // and the upload will be performed later by a scheduled task.
    	   HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
       	   uploadResponse.setDataTransferType(dataTransferType);
       	   uploadResponse.setDataTransferStarted(Calendar.getInstance());
       	   uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.RECEIVED);
       	   return uploadResponse;
    	}
    	
    	// Upload the data object using the appropriate data transfer system proxy.
  	    return dataTransferProxies.get(dataTransferType).
  	    		   uploadDataObject(authenticatedToken, 
  	    		                    uploadRequest, 
  	    		                    generateMetadata(uploadRequest.getPath(),
  	    		                    		         uploadRequest.getUserId()),
  	    		                    docConfigurationLocator.getBaseArchiveDestination(doc, dataTransferType), 
  	    		                    null);
    }
    
    /**
     * Download a data object file.
     *
     * @param downloadRequest The data object download request.
     * @return A data object download response.
     * @throws HpcException on service failure.
     */
    private HpcDataObjectDownloadResponse 
               downloadDataObject(HpcDataObjectDownloadRequest downloadRequest) 
                                 throws HpcException
    {
    	// Input Validation.
    	HpcDataTransferType dataTransferType = downloadRequest.getDataTransferType();
    	HpcFileLocation destinationLocation = downloadRequest.getDestinationLocation();
    	if(dataTransferType == null || 
    	   !isValidFileLocation(downloadRequest.getArchiveLocation())) {
  	       throw new HpcException("Invalid data transfer request", 
  	    	                      HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// if data is in an archive with S3 data-transfer, and the requested
    	// destination is a GLOBUS endpoint, then we need to perform a 2 hop download. i.e. store 
    	// the data to a local GLOBUS endpoint, and submit a transfer request to the caller's 
    	// GLOBUS destination. Both first and second hop downloads are performed asynchronously.
    	HpcSecondHopDownload secondHopDownload = null;
    	if(dataTransferType.equals(HpcDataTransferType.S_3) && destinationLocation != null) {
    	   secondHopDownload = new HpcSecondHopDownload(downloadRequest);

    	   // Set the first hop file destination to be the source file of the second hop.
    	   downloadRequest.setDestinationFile(secondHopDownload.getSourceFile());
    	}
    	
    	// Download the data object using the appropriate data transfer proxy.
    	try {
    	     HpcDataObjectDownloadResponse downloadResponse =  
    	        dataTransferProxies.get(dataTransferType).
  	    		            downloadDataObject(getAuthenticatedToken(dataTransferType, downloadRequest.getDoc()), 
  	                                           downloadRequest, secondHopDownload);	

    	        return secondHopDownload == null ? downloadResponse : secondHopDownload.getDownloadResponse();
    	        
    	} catch(HpcException e) {
    		    // Cleanup the download task (if needed) and rethrow.
    		    if(secondHopDownload != null) {
    		       completeDataObjectDownloadTask(secondHopDownload.getDownloadTask(), false, e.getMessage(),
    		    		                          Calendar.getInstance());
    		    }
    		    
    		    throw(e);
    	}
    }
	
    /**
     * Validate upload source file location.
     * 
     * @param dataTransferType The data transfer type.
     * @param sourceFileLocation The file location to validate. If null, no validation is performed.
     * @param doc The doc (needed to determine the archive connection config).
     * @throws HpcException if the upload source location doesn't exist, or not accessible, or it's a directory.
     */
    private void validateUploadSourceFileLocation(HpcDataTransferType dataTransferType,
                                                  HpcFileLocation sourceFileLocation,
                                                  String doc) 
                                                 throws HpcException
    		                                   
    {
    	if(sourceFileLocation == null) {
    	   return;
    	}
    
	   	HpcPathAttributes pathAttributes = getPathAttributes(dataTransferType, 
	   			                                             sourceFileLocation, false, doc);
		
	   	// Validate source file accessible
		if(!pathAttributes.getIsAccessible()) {
	 	   throw new HpcException("Source file location not accessible: " + 
	 			                  sourceFileLocation.getFileContainerId() + ":" +
	 			                  sourceFileLocation.getFileId(), 
	 	                          HpcErrorType.INVALID_REQUEST_INPUT);	
	 	}
		
	   	// Validate source file exists.
		if(!pathAttributes.getExists()) {
	 	   throw new HpcException("Source file location doesn't exist: " + 
	 			                  sourceFileLocation.getFileContainerId() + ":" +
	 			                  sourceFileLocation.getFileId(), 
	 	                          HpcErrorType.INVALID_REQUEST_INPUT);	
	 	}
		
	   	// Validate source file is not a directory.
		if(pathAttributes.getIsDirectory()) {
	 	   throw new HpcException("Source file location is a directory: " + 
	 			                  sourceFileLocation.getFileContainerId() + ":" +
	 			                  sourceFileLocation.getFileId(), 
	 	                          HpcErrorType.INVALID_REQUEST_INPUT);	
	 	}
    }
    
    /**
     * Validate download destination file location.
     * 
     * @param dataTransferType The data transfer type.
     * @param destinationLocation The file location to validate.
     * @param validateExistsAsDirectory If true, an exception will thrown if the path is an 
     *                                  existing directory.
     * @param doc The doc (needed to determine the archive connection config).                                 
     * @return The path attributes.
     * @throws HpcException if the destination location not accessible or exist as a file.
     */
    private HpcPathAttributes 
            validateDownloadDestinationFileLocation(HpcDataTransferType dataTransferType,
                                                    HpcFileLocation destinationLocation,
                                                    boolean validateExistsAsDirectory, String doc) 
                                                   throws HpcException
    {
	   	HpcPathAttributes pathAttributes = getPathAttributes(dataTransferType, 
                                                             destinationLocation, false, doc);

		// Validate destination file accessible.
		if(!pathAttributes.getIsAccessible()) {
		   throw new HpcException("Destination file location not accessible: " + 
		                          destinationLocation.getFileContainerId() + ":" +
		                          destinationLocation.getFileId(), 
		                          HpcErrorType.INVALID_REQUEST_INPUT);	
		}
		
		// Validate destination file doesn't exist as a file.
		if(pathAttributes.getIsFile()) {
		   throw new HpcException("A file already exists with the same destination path: " + 
		                          destinationLocation.getFileContainerId() + ":" +
		                          destinationLocation.getFileId(), 
		                          HpcErrorType.INVALID_REQUEST_INPUT);	
		}
		
		// Validate destination file doesn't exist as a directory.
		if(validateExistsAsDirectory && pathAttributes.getIsDirectory()) {
      	   throw new HpcException("A directory already exists with the same destination path: " + 
      			                  destinationLocation.getFileContainerId() + ":" +
      			                  destinationLocation.getFileId(), 
                                  HpcErrorType.INVALID_REQUEST_INPUT);	
		}
		
		return pathAttributes;
    }
    
	// Second hop download.
	private class HpcSecondHopDownload implements HpcDataTransferProgressListener
	{
	    //---------------------------------------------------------------------//
	    // Instance members
	    //---------------------------------------------------------------------//
		
		// The second hop download request.
    	HpcDataObjectDownloadRequest secondHopDownloadRequest = null;
    	
    	// A download data object task (keeps track of the async 2-hop download end-to-end.
    	HpcDataObjectDownloadTask downloadTask = new HpcDataObjectDownloadTask();
    	
    	// The second hop download's source file.
    	File sourceFile = null;
    	
    	// The data object path.
    	String path = null;
    	
		//---------------------------------------------------------------------//
	    // Constructors
	    //---------------------------------------------------------------------//
    	
		public HpcSecondHopDownload(HpcDataObjectDownloadRequest firstHopDownloadRequest) throws HpcException
		{
	       	// Get the service invoker.
	       	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
	       	if(invoker == null) {
	       	   throw new HpcException("Unknown service invoker", 
			                          HpcErrorType.UNEXPECTED_ERROR);
	       	}
	       	path = firstHopDownloadRequest.getPath();
	       	
			// Create the 2nd hop download request.
			secondHopDownloadRequest =
 		          toSecondHopDownloadRequest(
 			        calculateDownloadDestinationFileLocation(firstHopDownloadRequest.getDestinationLocation(), 
 				  	                                         HpcDataTransferType.GLOBUS,
 				  	                                         firstHopDownloadRequest.getArchiveLocation().getFileId(),
 				  	                                         firstHopDownloadRequest.getDoc()),
 			        HpcDataTransferType.GLOBUS,
 			        firstHopDownloadRequest.getPath(),
 			        firstHopDownloadRequest.getDoc(),
 			        firstHopDownloadRequest.getUserId(),
 			        firstHopDownloadRequest.getCompletionEvent());
			
			// Create the source file for the second hop download
			sourceFile = createFile(
			             dataTransferProxies.get(HpcDataTransferType.GLOBUS).
	                         getFilePath(secondHopDownloadRequest.getArchiveLocation().getFileId(), 
	                        		     false));
			
			// Create an persist a download task. This object tracks the download request through the 2-hop async 
			// download requests.
			createDownloadTask();
		}
		
		//---------------------------------------------------------------------//
	    // Methods
	    //---------------------------------------------------------------------//
		
	    /**
	     * Get the second hop download source file.
	     * 
	     * @return The second hop source file.
	     */
		public File getSourceFile()
		{
			return sourceFile;
		}
		
	    /**
	     * Get the second hop download response.
	     * 
	     * @return The second hop download response. Note: 
	     */
		public HpcDataObjectDownloadResponse getDownloadResponse()
		{
			HpcDataObjectDownloadResponse downloadResponse = new HpcDataObjectDownloadResponse();
			downloadResponse.setDestinationLocation(secondHopDownloadRequest.getDestinationLocation());
			downloadResponse.setDownloadTaskId(downloadTask.getId());
			return downloadResponse;
		}
		
	    /**
	     * Return the download task associated with this 2nd hop download.
	     */
	    public HpcDataObjectDownloadTask getDownloadTask() 
	    {
	    	return downloadTask;
	    }
		
		//---------------------------------------------------------------------//
	    // HpcDataTransferProgressListener Interface Implementation
	    //---------------------------------------------------------------------//  
		
		@Override public void transferCompleted()
		{
			// This callback method is called when the first hop (S3) download completed.
			try {
				   // Update the download task to reflect 1st hop transfer completed.
				   downloadTask.setDataTransferType(secondHopDownloadRequest.getDataTransferType());
				   
				   // Check if Globus accepts transfer requests at this time.
			       if(dataTransferProxies.get(downloadTask.getDataTransferType()).acceptsTransferRequests(
			    	      getAuthenticatedToken(downloadTask.getDataTransferType(), downloadTask.getDoc()))) {
				      // Globus accepts requests - submit the 2nd hop async download (to Globus).
				      downloadTask.setDataTransferRequestId(
				    		          downloadDataObject(secondHopDownloadRequest).getDataTransferRequestId());
				      
			       } else {
			    	       // Globus doesn't accept transfer requests at this time. Queue the 2nd hop transfer.
			    	       downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.RECEIVED);
			       }
			       
			       // Persist the download task.
				   dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
				      
			} catch(HpcException e) {
				    logger.error("Failed to submit 2nd hop download request, or update download task", e);
				    transferFailed(e.getMessage());
			}
		}
		
		// This callback method is called when the first hop download failed.
	    @Override
	    public void transferFailed()
	    {
		    transferFailed("Failed to get data from archive via S3");
	    }
	    
	    //---------------------------------------------------------------------//
	    // Helper Methods
	    //---------------------------------------------------------------------//  
	    
	    /**
	     * Create a download request for a 2nd hop download from local file (as Globus endpoint)
	     * to caller's Globus endpoint destination.
	     * 
	     * @param destinationLocation The caller's destination.
	     * @param dataTransferType The data transfer type to create the request
	     * @param path The data object logical path.
	     * @param doc The DOC.
	     * @param userId The user ID submitting the request.
	     * @param completionEvent If true, an event will be added when async download is complete.
	     * @return Data object download request.
	     * @throws HpcException If it failed to obtain an authentication token.
	     */
	    private HpcDataObjectDownloadRequest toSecondHopDownloadRequest(HpcFileLocation destinationLocation,
	    		                                                        HpcDataTransferType dataTransferType,
	    		                                                        String path, String doc, String userId,
	    		                                                        boolean completionEvent)
	    		                                                       throws HpcException
	    {
	    	// Create a source location.
	    	HpcFileLocation sourceLocation = 
	    	   dataTransferProxies.get(dataTransferType).getDownloadSourceLocation(path);
	    	                        
	    	// Create and return a download request, from the local GLOBUS endpoint, to the caller's
	    	// destination.
	    	HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
	    	downloadRequest.setArchiveLocation(sourceLocation);
	    	downloadRequest.setDestinationLocation(destinationLocation);
	    	downloadRequest.setDataTransferType(dataTransferType);
	    	downloadRequest.setPath(path);
	    	downloadRequest.setDoc(doc);
	    	downloadRequest.setUserId(userId);
	    	downloadRequest.setCompletionEvent(completionEvent);
	    	
	    	return downloadRequest;
	    }
	    
	    /**
	     * Create and store an entry in the DB to cleanup download file after 2nd hop async
	     * transfer is complete. 
	     * 
	     * @param dataTransferType The data transfer type.
	     * @param downloadFilePath The download file path to delete after download is complete.
	     * @param path The data object path.
	     * @param doc The DOC.
	     * @param destinationLocation The download destination path.
	     * @throws HpcException If it failed to persist the task.
	     */
	    private void createDownloadTask() throws HpcException
	    {
	    	downloadTask.setDataTransferType(HpcDataTransferType.S_3);
	    	downloadTask.setDataTransferStatus(HpcDataTransferDownloadStatus.IN_PROGRESS);
	    	downloadTask.setDownloadFilePath(sourceFile.getAbsolutePath());
	    	downloadTask.setUserId(secondHopDownloadRequest.getUserId());
	    	downloadTask.setPath(secondHopDownloadRequest.getPath());
	    	downloadTask.setDoc(secondHopDownloadRequest.getDoc());
	    	downloadTask.setCompletionEvent(secondHopDownloadRequest.getCompletionEvent());
	    	downloadTask.setArchiveLocation(secondHopDownloadRequest.getArchiveLocation());
	    	downloadTask.setDestinationLocation(secondHopDownloadRequest.getDestinationLocation());
	    	downloadTask.setCreated(Calendar.getInstance());
	    	
	    	dataDownloadDAO.upsertDataObjectDownloadTask(downloadTask);
	    }
	    
	    /**
	     * Handle the case when transfer failed. Send a download failed event and cleanup the download task.
	     * 
	     * @param message The message to include in the download failed event.
	     */ 
   		private void transferFailed(String message) 
   		{
   			Calendar transferFailedTimestamp = Calendar.getInstance();
   			try {
	    		 // Record a download failed event if requested to.
	    		 if(secondHopDownloadRequest.getCompletionEvent()) {
	    		    eventService.addDataTransferDownloadFailedEvent(
	    		    		        secondHopDownloadRequest.getUserId(), path, 
	    		    		        HpcDownloadTaskType.DATA_OBJECT, downloadTask.getId(),
	    				            secondHopDownloadRequest.getDestinationLocation(),
	    				            transferFailedTimestamp, message);
	    		 }
	    		 
	    	} catch(HpcException e) {
	    		    logger.error("Failed to add data transfer download failed event", e);
	    	}
	    	
	    	// Cleanup the download task.
	    	try {
	    	     completeDataObjectDownloadTask(downloadTask, false, message, transferFailedTimestamp);
	    	     
	    	} catch(HpcException ex) {
	    		    logger.error("Failed to cleanup download task", ex);
	    	}
   		}
	}
}
 