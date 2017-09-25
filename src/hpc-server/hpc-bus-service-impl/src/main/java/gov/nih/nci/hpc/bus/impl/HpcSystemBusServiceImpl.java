/**
 * HpcSystemBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.bus.HpcSystemBusService;
import gov.nih.nci.hpc.bus.aspect.SystemBusServiceImpl;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectListRegistrationTaskStatus;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectRegistrationTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcDataObjectListRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcReportService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * <p>
 * HPC System Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcSystemBusServiceImpl implements HpcSystemBusService
{  
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

	// Security Application Service Instance.
	@Autowired
    private HpcSecurityService securityService = null;
	
	// Data Transer Application Service Instance.
	@Autowired
    private HpcDataTransferService dataTransferService = null;
	
	// Data Management Application Service Instance.
	@Autowired
    private HpcDataManagementService dataManagementService = null;
	
	// Data Management Bus Service Instance.
	@Autowired
    private HpcDataManagementBusService dataManagementBusService = null;
	
	// Notification Application Service Instance.
	@Autowired
    private HpcNotificationService notificationService = null;
	
	// Event Application Service Instance.
	@Autowired
    private HpcEventService eventService = null;
	
	// Metadata Application Service Instance
	@Autowired
    private HpcMetadataService metadataService = null;

	// Reports Application Service Instance
	@Autowired
    private HpcReportService reportsService = null;

	// The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcSystemBusServiceImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcSystemBusService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void processDataTranferUploadReceived() throws HpcException
    {
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	// Iterate through the data objects that their data transfer is in-progress.
    	List<HpcDataObject> dataObjectsReceived = dataManagementService.getDataObjectsUploadReceived();
    	logger.info(dataObjectsReceived.size() + " Data Objects Upload Received: " + dataObjectsReceived);
    	for(HpcDataObject dataObject : dataObjectsReceived) {
    		String path = dataObject.getAbsolutePath();
    		logger.info("Processing data object upload queued: " + path);
    		try {
    		     // Get the system metadata.
    			 HpcSystemGeneratedMetadata systemGeneratedMetadata = 
    			    metadataService.getDataObjectSystemGeneratedMetadata(path);
    			 
 				// Transfer the data file.
 		        HpcDataObjectUploadResponse uploadResponse = 
 		           dataTransferService.uploadDataObject(systemGeneratedMetadata.getSourceLocation(), 
 		        		                                null, path, 
 		        		                                systemGeneratedMetadata.getRegistrarId(),
 		        		                                systemGeneratedMetadata.getCallerObjectId(), 
 		        		                                systemGeneratedMetadata.getRegistrarDOC());
 		        
 			    // Generate system metadata and attach to the data object.
 			    metadataService.updateDataObjectSystemGeneratedMetadata(
 			        		                    path, uploadResponse.getArchiveLocation(),
 			    			                    uploadResponse.getDataTransferRequestId(), 
 			    			                    uploadResponse.getChecksum(), 
 			    			                    uploadResponse.getDataTransferStatus(),
 			    			                    uploadResponse.getDataTransferType(),
 			    			                    uploadResponse.getDataTransferCompleted()); 
    		     
    		} catch(HpcException e) {
    			    logger.error("Failed to process queued data transfer upload :" + path, e);
    			    
    			    // Delete the data object.
    			    deleteDataObject(path);
    		}
    	}
    	
    }
    
    @Override
    public void processDataTranferUploadInProgress() throws HpcException
    {
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	// Iterate through the data objects that their data transfer is in-progress.
    	List<HpcDataObject> dataObjectsInProgress = dataManagementService.getDataObjectsUploadInProgress();
    	logger.info(dataObjectsInProgress.size() + " Data Objects Upload In Progress: " + dataObjectsInProgress);
    	for(HpcDataObject dataObject : dataObjectsInProgress) {
    		String path = dataObject.getAbsolutePath();
    		logger.info("Processing data object upload in-progress: " + path);
    		try {
    		     // Get the system metadata.
    			 HpcSystemGeneratedMetadata systemGeneratedMetadata = 
    			    metadataService.getDataObjectSystemGeneratedMetadata(path);
    			 
    			 // Get the data transfer upload request status.
    			 HpcDataTransferUploadReport dataTransferUploadReport =
    		        dataTransferService.getDataTransferUploadStatus(
    		        		               systemGeneratedMetadata.getDataTransferType(),
    		        		               systemGeneratedMetadata.getDataTransferRequestId(),
    		        		               systemGeneratedMetadata.getRegistrarDOC());
    			 
    			 HpcDataTransferUploadStatus dataTransferStatus = dataTransferUploadReport.getStatus();
    			 Calendar dataTransferCompleted = null;
    			 switch(dataTransferStatus) {
    		            case ARCHIVED:
    		            	 // Data object is archived. Update data transfer status and completion time.
    		            	 dataTransferCompleted = Calendar.getInstance();
	     			         metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null, 
	     			    		                                                     dataTransferStatus, null, 
	     			    		                                                     dataTransferCompleted);
	    		             break;

    		            case IN_TEMPORARY_ARCHIVE:
    			             // Data object is in temp archive. Update data transfer status.
    	     			     metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null, 
    	     			    		                                                 dataTransferStatus, null, null);
    	    		         break;
	   	    		         
    			        case FAILED:
    			             // Data transfer failed. 
    			        	 throw new HpcException("Data transfer failed: " + dataTransferUploadReport.getMessage(),
    			        			                HpcErrorType.DATA_TRANSFER_ERROR); 
  		    	             
  		    	        default:
  		    	        	 // Transfer is still in progress.
  		    	        	 continue;
    		     }
    			 
    			 // Data transfer upload completed (successfully or failed). Add an event.
    			 addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path, 
		                                    dataTransferStatus, null, systemGeneratedMetadata.getSourceLocation(), 
		                                    dataTransferCompleted, systemGeneratedMetadata.getDataTransferType(),
		                                    systemGeneratedMetadata.getRegistrarDOC());
    		     
    		} catch(HpcException e) {
    			    logger.error("Failed to process data transfer upload in progress:" + path, e);
    			    
    			    // Delete the data object.
    			    deleteDataObject(path);
    		}
    	}
    }
    
    @Override
    public void processTemporaryArchive() throws HpcException
    {
    	// Use system account to perform this service.
    	securityService.setSystemRequestInvoker();
    	
    	// Iterate through the data objects that their data is in temporary archive.
    	List<HpcDataObject> dataObjectsInTemporaryArchive = 
    			            dataManagementService.getDataObjectsUploadInTemporaryArchive();
    	logger.info(dataObjectsInTemporaryArchive.size() + " Data Objects Upload In Temporary Archive: " + 
    	            dataObjectsInTemporaryArchive);
    	for(HpcDataObject dataObject : dataObjectsInTemporaryArchive) {
    		String path = dataObject.getAbsolutePath();
    		logger.info("Process Temporary Archive for: " + path);
    		HpcSystemGeneratedMetadata systemGeneratedMetadata = null;
    		try {
    		     // Get the data object system generated metadata.
    			 systemGeneratedMetadata = 
    			       metadataService.getDataObjectSystemGeneratedMetadata(path);
    			 
    			 // Get the file associated with the data object in the temporary archive.
    			 File file = dataTransferService.getArchiveFile(
    					         systemGeneratedMetadata.getDataTransferType(),
    					         systemGeneratedMetadata.getArchiveLocation().getFileId());
    			 
 				 // Transfer the data file.
 		         HpcDataObjectUploadResponse uploadResponse = 
 		        	dataTransferService.uploadDataObject(null, file, path, 
 		        			                             systemGeneratedMetadata.getRegistrarId(),
 		        			                             systemGeneratedMetadata.getCallerObjectId(),
 		        			                             systemGeneratedMetadata.getRegistrarDOC());
 		     
 		         // Delete the file.
 		         if(!FileUtils.deleteQuietly(file)) {
 		        	logger.error("Failed to delete file: " + 
 		                          systemGeneratedMetadata.getArchiveLocation().getFileId());
 		         }
 		         
 			     // Update system metadata of the data object.
 			     metadataService.updateDataObjectSystemGeneratedMetadata(
 			           		               path, uploadResponse.getArchiveLocation(),
 			    			               uploadResponse.getDataTransferRequestId(), 
 			    			               uploadResponse.getChecksum(), 
 			    			               uploadResponse.getDataTransferStatus(),
 			    			               uploadResponse.getDataTransferType(),
 			    			               uploadResponse.getDataTransferCompleted()); 
 			     
 			     // Data transfer upload completed (successfully or failed). Add an event.
    			 addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path, 
    					                    uploadResponse.getDataTransferStatus(),
    					                    uploadResponse.getChecksum(), 
    					                    systemGeneratedMetadata.getSourceLocation(),
    					                    uploadResponse.getDataTransferCompleted(),
    					                    uploadResponse.getDataTransferType(),
    					                    systemGeneratedMetadata.getRegistrarDOC());
 			     
    		} catch(HpcException e) {
    			    logger.error("Failed to transfer data from temporary archive:" + path, e);
    			    
    			    // Delete the data object.
    			    deleteDataObject(path);
    		}
    	}
    }
    
    @Override
    public void completeDataObjectDownloadTasks() throws HpcException
    {
    	// Use system account to perform this service.
    	securityService.setSystemRequestInvoker();
    	
    	// Iterate through all the data object download tasks that are in their 2nd hop (i.e. GLOBUS download
    	// to user's endpoint is in progress).
    	for(HpcDataObjectDownloadTask downloadTask :
    		dataTransferService.getDataObjectDownloadTasks(HpcDataTransferType.GLOBUS)) {
    		try {
    		     switch(downloadTask.getDataTransferStatus()) {
    		            case RECEIVED:
    		    	         dataTransferService.continueDataObjectDownloadTask(downloadTask);
    		    	         break;
    		    	   
    		            case IN_PROGRESS:
    			             completeDataObjectDownloadTaskInProgress(downloadTask);
    			             break;
    			        
    			        default:
    				            throw new HpcException("Unexpected data transfer download status [" + 
    			                                       downloadTask.getDataTransferStatus() + "] for task: " +
    			                                       downloadTask.getId(), HpcErrorType.UNEXPECTED_ERROR);
    		     }
    		     
    		} catch(HpcException e) {
    			    logger.error("Failed to complete download task: " + downloadTask.getId(), e);
    		}
    	}
    }
    
    @Override
    public void processCollectionDownloadTasks() throws HpcException
    {
    	// Use system account to perform this service.
    	securityService.setSystemRequestInvoker();
    	
    	// Iterate through all the collection download requests that were submitted (not processed yet).
    	for(HpcCollectionDownloadTask downloadTask :
    		dataTransferService.getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus.RECEIVED)) {
    		try {
    			 List<HpcCollectionDownloadTaskItem> downloadItems = null;
    			 
    			 if(downloadTask.getType().equals(HpcDownloadTaskType.COLLECTION)) {
    			    // Get the collection to be downloaded.
        		    HpcCollection collection = dataManagementService.getCollection(downloadTask.getPath(), true);
        		    if(collection == null) {
        			   throw new HpcException("Collection not found", HpcErrorType.INVALID_REQUEST_INPUT);
        		    }
        		 
        		    // Download all files under this collection.
        		    downloadItems = downloadCollection(collection, downloadTask.getDestinationLocation(), 
			                                           downloadTask.getUserId());
        		    
    			 } else if(downloadTask.getType().equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
    				       downloadItems = downloadDataObjects(downloadTask.getDataObjectPaths(), 
    				    		                               downloadTask.getDestinationLocation(), 
                                                               downloadTask.getUserId());
    			 }
    			 
        		 // Verify data objects found under this collection.
    			 if(downloadItems == null || downloadItems.isEmpty()) {
    				// No data objects found under this collection.
    				throw new HpcException("No data objects found under collection",
				                           HpcErrorType.INVALID_REQUEST_INPUT);
    			 }
    			 
    			 // 'Activate' the collection download request. 
    			 downloadTask.setStatus(HpcCollectionDownloadTaskStatus.ACTIVE);
    			 downloadTask.getItems().addAll(downloadItems);
    			 
    			// Persist the collection download task.
			    dataTransferService.updateCollectionDownloadTask(downloadTask);
    		     
    		} catch(HpcException e) {
    			    logger.error("Failed to process a collection download: " + downloadTask.getId(), e);
    			    completeCollectionDownloadTask(downloadTask, false, e.getMessage());
    			    
    		} 
    	}
    }
    
    @Override
    public void completeCollectionDownloadTasks() throws HpcException
    {
    	// Use system account to perform this service.
    	securityService.setSystemRequestInvoker();
    	
    	// Iterate through all the active collection download requests.
    	for(HpcCollectionDownloadTask downloadTask :
    		dataTransferService.getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus.ACTIVE)) {
    		boolean downloadCompleted = true;
    		
    		// Update status of individual download items in this collection download task.
			for(HpcCollectionDownloadTaskItem downloadItem : downloadTask.getItems()) {
				try {
				     if(downloadItem.getResult() == null) {
				     	// This download item in progress - check its status.
				    	HpcDownloadTaskStatus downloadItemStatus = 
				    	   downloadItem.getDataObjectDownloadTaskId() != null ?
				    	           dataTransferService.getDownloadTaskStatus(
				    			                          downloadItem.getDataObjectDownloadTaskId(),
				    			                          HpcDownloadTaskType.DATA_OBJECT) :
				    			   null;
				    			                          
				        if(downloadItemStatus == null) {
				    	   throw new HpcException("Data object download task status is unknown", 
				    			                  HpcErrorType.UNEXPECTED_ERROR);
				    	}
				        
				        if(!downloadItemStatus.getInProgress()) {
				           // This download item is now complete. Update the result.
				           downloadItem.setResult(downloadItemStatus.getResult().getResult());
				           downloadItem.setMessage(downloadItemStatus.getResult().getMessage());
				        } else {
				        	    // There is at least one download item still in progress.
				        	    downloadCompleted = false;
				        }
				     }
    		
				 } catch(HpcException e) {
					     logger.error("Failed to check collection download item status", e);
					     downloadItem.setResult(false);
					     downloadItem.setMessage(e.getMessage());
				 }
			}
			
			
			// Update the collection download task.
			if(downloadCompleted) {
			   // The collection download task finished. Determine if the collection download was successful. 
			   // It is successful if and only if all items (data objects under the collection) were 
			   // completed successfully. 
			   int completedItemsCount = 0;
			   for(HpcCollectionDownloadTaskItem downloadItem : downloadTask.getItems()) {
				   if(downloadItem.getResult()) {
					  completedItemsCount++;
				   }
			   }
			   
			   int itemsCount = downloadTask.getItems().size();
			   boolean result = completedItemsCount == itemsCount;
			   completeCollectionDownloadTask(downloadTask, result, 
					                          result ? null : completedItemsCount + " items downloaded successfully out of " + itemsCount);
			   
			} else {
				    dataTransferService.updateCollectionDownloadTask(downloadTask);
			}
    	}
    }
    
    @Override
    public void processDataObjectListRegistrationTasks() throws HpcException
    {
    	// Use system account to perform this service.
    	securityService.setSystemRequestInvoker();
    	
    	// Iterate through all the data object list registration requests that were submitted (not processed yet).
        dataManagementService.getDataObjectListRegistrationTasks(HpcDataObjectListRegistrationTaskStatus.RECEIVED).forEach(
    		listRegistrationTask -> 
    		{
	    		try {
	    			 // 'Activate' the registration task. 
	    			listRegistrationTask.setStatus(HpcDataObjectListRegistrationTaskStatus.ACTIVE);
	    			 
	    			 // Register all items in this list registration task.
	    			listRegistrationTask.getItems().forEach(
	    					        item -> registerDataObject(item, 
	    					        		                   listRegistrationTask.getUserId(), 
	    					        		                   listRegistrationTask.getDoc()));
	    			 
	    			 // Persist the data object list registration task.
	    			 dataManagementService.updateDataObjectListRegistrationTask(listRegistrationTask);
	    		     
	    		} catch(HpcException e) {
	    			    logger.error("Failed to process a data object list registration: " + 
	    		                     listRegistrationTask.getId(), e);
	    			    completeDataObjectListRegistrationTask(listRegistrationTask, false, e.getMessage());
	    		} 
    		});
    }
    
    @Override
    public void completeDataObjectListRegistrationTasks() throws HpcException
    {
    	// Use system account to perform this service.
    	securityService.setSystemRequestInvoker();
    	
    	// Iterate through all the data object list registration requests that are active.
        dataManagementService.getDataObjectListRegistrationTasks(HpcDataObjectListRegistrationTaskStatus.ACTIVE).forEach(
    		listRegistrationTask -> 
    		{
    			// Update status of items in this list registration task.
    			listRegistrationTask.getItems().forEach(this::updateRegistrationItemStatus);
	
    			// Check if registration task completed.
    			int completedItemsCount = 0;
    			for(HpcDataObjectListRegistrationItem registrationItem : listRegistrationTask.getItems()) {
    				if(registrationItem.getTask().getResult() == null) {
    				   // Task still in progress. Update progress.
    				   try {
		                    dataManagementService.updateDataObjectListRegistrationTask(listRegistrationTask);
   				    
    				   } catch(HpcException e) {
   				    	       logger.error("Failed to update data object list task: " +  
    					       listRegistrationTask.getId());
    				   }
    				   return;
    				}
    			   
    				if(registrationItem.getTask().getResult()) {
		               completedItemsCount++;
					}
    			}
    			
    			// List registration task completed.
    			int itemsCount = listRegistrationTask.getItems().size();
    			boolean result = completedItemsCount == itemsCount;
    			completeDataObjectListRegistrationTask(
    			 	    listRegistrationTask, result, 
			            result ? null : completedItemsCount + " items registered successfully out of " + itemsCount);
    		}
		);
    }
    
    @Override
    public void processEvents() throws HpcException
    {
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	// Get and process the pending notification events.
    	for(HpcEvent event : eventService.getEvents()) {
    		// Notify all users associated with this event.
    		try {
    		     for(String userId : event.getUserIds()) {
    		    	 try {
		    		      // Get the subscription.
		    			  HpcEventType eventType = event.getType();
		    		      HpcNotificationSubscription subscription = 
		    		         notificationService.getNotificationSubscription(userId, eventType);
		    		      if(subscription != null) {
		    		         // Iterate through all the delivery methods the user is subscribed to.
		    		         for(HpcNotificationDeliveryMethod deliveryMethod : 
		    			         subscription.getNotificationDeliveryMethods()) {
		    			         // Send notification via this delivery method.
		    		        	 boolean notificationSent = 
		    		        	 notificationService.sendNotification(userId, eventType, 
		    		        	 		                              event.getPayloadEntries(), 
		    		        			                              deliveryMethod);
		
		    		        	 // Create a delivery receipt for this delivery method.
		    		      		 notificationService.createNotificationDeliveryReceipt(userId,
		    		      				                                               event.getId(), 
		    		      				                                               deliveryMethod, 
		    		      				                                               notificationSent);
		    		        }
		    		     }
		    		      
    		    	 } catch(Exception e) {
    		    		     logger.error("Failed to deliver notifications to: " + userId);
    		    	 }
	    		}
    		     
    		} finally {
    			       eventService.archiveEvent(event);
    		}
    	}
    }
    
    @Override
	public void generateSummaryReportEvent() throws HpcException
	{
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	List<String> summaryReportUsers = notificationService.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_REPORT);
    	if(summaryReportUsers != null && summaryReportUsers.size() > 0)
    	{
    		HpcReportCriteria criteria = new HpcReportCriteria();
    		criteria.setType(HpcReportType.USAGE_SUMMARY);
    		eventService.generateReportsEvents(summaryReportUsers, criteria);
    	}
	}

    @Override
	public void generateWeeklySummaryReportEvent() throws HpcException
	{
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	List<String> summaryReportByDateUsers = notificationService.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_WEEKLY_REPORT);
    	if(summaryReportByDateUsers != null && summaryReportByDateUsers.size() > 0)
    	{
    		HpcReportCriteria criteria = new HpcReportCriteria();
    		criteria.setType(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE);
    		Calendar today = Calendar.getInstance();
    		Calendar oneWeekbefore = Calendar.getInstance();
    		oneWeekbefore.add(Calendar.DATE, -7);
    		criteria.setFromDate(oneWeekbefore);
    		criteria.setToDate(today);
    		eventService.generateReportsEvents(summaryReportByDateUsers, criteria);
    	}
	}
    
    @Override
	public void generateDocReportEvent() throws HpcException
	{
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	List<String> summaryReportUsers = notificationService.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_DOC_REPORT);
    	if(summaryReportUsers != null && summaryReportUsers.size() > 0)
    	{
    		HpcReportCriteria criteria = new HpcReportCriteria();
    		criteria.setType(HpcReportType.USAGE_SUMMARY_BY_DOC);
    		eventService.generateReportsEvents(summaryReportUsers, criteria);
    	}
	}
    
    @Override
	public void generateWeeklyDocReportEvent() throws HpcException
	{
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	List<String> summaryReportByDateUsers = notificationService.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_DOC_BY_WEEKLY_REPORT);
    	if(summaryReportByDateUsers != null && summaryReportByDateUsers.size() > 0)
    	{
    		HpcReportCriteria criteria = new HpcReportCriteria();
    		criteria.setType(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE);
    		Calendar today = Calendar.getInstance();
    		Calendar oneWeekbefore = Calendar.getInstance();
    		oneWeekbefore.add(Calendar.DATE, -7);
    		criteria.setFromDate(oneWeekbefore);
    		criteria.setToDate(today);
    		eventService.generateReportsEvents(summaryReportByDateUsers, criteria);
    	}
	}
    
    @Override
	public void generateUserReportEvent() throws HpcException
	{
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	List<String> summaryReportUsers = notificationService.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_USER_REPORT);
    	if(summaryReportUsers != null && summaryReportUsers.size() > 0)
    	{
    		HpcReportCriteria criteria = new HpcReportCriteria();
    		criteria.setType(HpcReportType.USAGE_SUMMARY_BY_USER);
    		eventService.generateReportsEvents(summaryReportUsers, criteria);
    	}
	}
    
    @Override
	public void generateWeeklyUserReportEvent() throws HpcException
	{
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	List<String> summaryReportByDateUsers = notificationService.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_USER_BY_WEEKLY_REPORT);
    	if(summaryReportByDateUsers != null && summaryReportByDateUsers.size() > 0) {
    		HpcReportCriteria criteria = new HpcReportCriteria();
    		criteria.setType(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE);
    		Calendar today = Calendar.getInstance();
    		Calendar oneWeekbefore = Calendar.getInstance();
    		oneWeekbefore.add(Calendar.DATE, -7);
    		criteria.setFromDate(oneWeekbefore);
    		criteria.setToDate(today);
    		eventService.generateReportsEvents(summaryReportByDateUsers, criteria);
    	}
	}
    
    @Override
    @SystemBusServiceImpl // Weave setSystemRequestInvoker() advice.
    public void refreshMetadataViews() throws HpcException
    {
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	metadataService.refreshViews();
    }
    
    @Override
    @SystemBusServiceImpl // Weave setSystemRequestInvoker() advice.
    public void refreshReportViews() throws HpcException
    {
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	reportsService.refreshViews();
    }

    @Override
    public void closeConnection()
    {
    	dataManagementService.closeConnection();
    }

    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
	
    /** 
     * add data transfer upload event.
     * 
     * @param userId The user ID.
     * @param path The data object path.
     * @param dataTransferStatus The data transfer upload status.
     * @param checksum (Optional) The data checksum.
     * @param sourceLocation (Optional) The data transfer source location.
     * @param dataTransferCompleted (Optional) The time the data upload completed.
     * @param dataTransferType The type of data transfer used to upload (Globus, S3, etc).
     * @param doc The DOC.
     */
	private void addDataTransferUploadEvent(String userId, String path,
			                                HpcDataTransferUploadStatus dataTransferStatus,
			                                String checksum, HpcFileLocation sourceLocation, 
			                                Calendar dataTransferCompleted, 
			                                HpcDataTransferType dataTransferType, String doc) 
	{
		setFileContainerName(HpcDataTransferType.GLOBUS, doc, sourceLocation);
		try {
			 switch(dataTransferStatus) {
			        case ARCHIVED: 
		                 eventService.addDataTransferUploadArchivedEvent(userId, path, checksum, 
		                		                                         sourceLocation, dataTransferCompleted);
		                 break;
		                 
			        case IN_TEMPORARY_ARCHIVE: 
		                 eventService.addDataTransferUploadInTemporaryArchiveEvent(userId, path);
		                 break;
		                 
			        case FAILED: 
		                 eventService.addDataTransferUploadFailedEvent(userId, path, sourceLocation, 
		                		                                       dataTransferCompleted,
		                		                                       dataTransferType.value() + " failure");
		                 break;
		                 
		            default: 
		                 logger.error("Unexpected data transfer status: " + dataTransferStatus); 
			 }

		} catch(HpcException e) {
			    logger.error("Failed to add a data transfer upload event", e);
		}
	}
	
    /** 
     * add data transfer download event.
     * 
     * @param userId The user ID.
     * @param path The collection or data objection path.
     * @param downloadTaskType The download task type.
     * @param downloadTaskId The download task ID.
     * @param dataTransferType The data transfer type,
     * @param doc The doc.
     * @param result The download result.
     * @param message A failure message.
     * @param destinationLocation The download destination location.
     * @param dataTransferCompleted The download completion time.
     */
	private void addDataTransferDownloadEvent(String userId, String path, 
			                                  HpcDownloadTaskType downloadTaskType,
                                              String downloadTaskId,
                                              HpcDataTransferType dataTransferType, String doc,
			                                  boolean result, String message,
			                                  HpcFileLocation destinationLocation, 
			                                  Calendar dataTransferCompleted) 
	{
		setFileContainerName(dataTransferType, doc, destinationLocation);
		try {
			 if(result) {
		        eventService.addDataTransferDownloadCompletedEvent(userId, path, downloadTaskType, 
		                		                                   downloadTaskId, 
		                		                                   destinationLocation, dataTransferCompleted);
			 } else {
		             eventService.addDataTransferDownloadFailedEvent(userId, path, downloadTaskType, 
                                                                     downloadTaskId, destinationLocation, 
                                                                     dataTransferCompleted, message);
			 }

		} catch(HpcException e) {
			    logger.error("Failed to add a data transfer download event", e);
		}
	}
	
    /** 
     * Download a collection. Traverse the collection tree and submit download request to all files in the tree. 
     * 
     * @param collection The collection to download.
     * @param destinationLocation The download destination location.
     * @param userId The user ID who requested the collection download.
     * @return The download task items (each item represent a data-object download under the collection).
     * @throws HpcException on service failure.
     */
	private List<HpcCollectionDownloadTaskItem> 
	        downloadCollection(HpcCollection collection, HpcFileLocation destinationLocation, String userId) 
			                  throws HpcException
	{
		List<HpcCollectionDownloadTaskItem> downloadItems = new ArrayList<>();
		
		// Iterate through the data objects in the collection and download them.
		for(HpcCollectionListingEntry dataObjectEntry : collection.getDataObjects()) {
			downloadItems.add(downloadDataObject(dataObjectEntry.getPath(), destinationLocation, userId));
		}
		
		// Iterate through the sub-collections and download them.
		for(HpcCollectionListingEntry subCollectionEntry : collection.getSubCollections()) {
			String subCollectionPath = subCollectionEntry.getPath();
			HpcCollection subCollection = dataManagementService.getCollection(subCollectionPath, true);
	    	if(subCollection != null) {
	    	   // Download this sub-collection. 
	    	   downloadItems.addAll(
				       downloadCollection(subCollection,
					           calculateDownloadDestinationFileLocation(destinationLocation, 
						    	                                        subCollectionPath),
						       userId));
	    	}
		}
		
		return downloadItems;
	}
	
    /** 
     * Download a list of data objects.
     * 
     * @param dataObjectPaths The list of data object path to download.
     * @param destinationLocation The download destination location.
     * @param userId The user ID who requested the collection download.
     * @return The download task items (each item represent a data-object download from the requested list).
     * @throws HpcException on service failure.
     */
	private List<HpcCollectionDownloadTaskItem> 
	        downloadDataObjects(List<String> dataObjectPaths, 
	        		            HpcFileLocation destinationLocation, String userId) 
			                   throws HpcException
	{
		List<HpcCollectionDownloadTaskItem> downloadItems = new ArrayList<>();
		
		// Iterate through the data objects in the collection and download them.
		for(String dataObjectPath : dataObjectPaths) {
			downloadItems.add(downloadDataObject(dataObjectPath, destinationLocation, userId));
		}
		
		return downloadItems;
	}
	
    /** 
     * Download a data object.
     * 
     * @param path The data object path.
     * @param destinationLocation The download destination location.
     * @param userId The user ID who requested the collection download.
     * @return The download task item.
     * @throws HpcException on service failure.
     */
	private HpcCollectionDownloadTaskItem downloadDataObject(String path, 
			                                                 HpcFileLocation destinationLocation, 
			                                                 String userId) 
	{
		HpcDownloadRequestDTO dataObjectDownloadRequest = new HpcDownloadRequestDTO();
		dataObjectDownloadRequest.setDestination(
			calculateDownloadDestinationFileLocation(destinationLocation, path));
		
		// Instantiate a download item for this data object.
		HpcCollectionDownloadTaskItem downloadItem = new HpcCollectionDownloadTaskItem();
		downloadItem.setPath(path);
		
		// Download this data object.
		try {
			 HpcDataObjectDownloadResponseDTO dataObjectDownloadResponse = 
			    dataManagementBusService.downloadDataObject(path, dataObjectDownloadRequest,
				    	                                    userId, false);
			 
			 downloadItem.setDataObjectDownloadTaskId(dataObjectDownloadResponse.getTaskId());
			 downloadItem.setDestinationLocation(dataObjectDownloadResponse.getDestinationLocation());
		     
		} catch(HpcException e) {
			    // Data object download failed. 
			    logger.error("Failed to download data object in a collection" , e); 
			    
			    downloadItem.setResult(false);
			    downloadItem.setDestinationLocation(dataObjectDownloadRequest.getDestination());
			    downloadItem.setMessage(e.getMessage());
			    
		} 
		
		return downloadItem;
	}
	
    /** 
     * Calculate a download destination path for a collection entry under a collection.
     * 
     * @param collectionDestination The collection destination location.
     * @param collectionListingEntryPath The entry path under the collection to calculate the destination location for.
     * @return A calculated destination location.
     */
	
	private HpcFileLocation calculateDownloadDestinationFileLocation(HpcFileLocation collectionDestination,
			                                                         String collectionListingEntryPath)
	{
		HpcFileLocation calcDestination = new HpcFileLocation();
	    calcDestination.setFileContainerId(collectionDestination.getFileContainerId());
	    calcDestination.setFileId(collectionDestination.getFileId() + 
	    		                  collectionListingEntryPath.substring(collectionListingEntryPath.lastIndexOf('/')));
	    return calcDestination;
	}
	
    /**
     * Complete a collection download task.
     * 1. Update task info in DB with results info.
     * 2. Send an event.
     *
     * @param downloadTask The download task to complete.
     * @param result The result of the task (true is successful, false is failed).
     * @param message (Optional) If the task failed, a message describing the failure.
     * @throws HpcException on service failure.
     */
    private void completeCollectionDownloadTask(HpcCollectionDownloadTask downloadTask,
    		                                   boolean result, String message) 
    		                                  throws HpcException
    {
    	Calendar completed = Calendar.getInstance();
    	dataTransferService.completeCollectionDownloadTask(downloadTask, result, message, completed);
    	
    	// Set the payload with either the collection path or the list of data object paths.
        String path = "";
        if(downloadTask.getType().equals(HpcDownloadTaskType.COLLECTION)) {
           path = downloadTask.getPath();
        } else if(downloadTask.getType().equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
        	      path = StringUtils.join(downloadTask.getDataObjectPaths(), ", ");
        }
        
        // Send download completed/failed event.
    	addDataTransferDownloadEvent(downloadTask.getUserId(), path,
    			                     downloadTask.getType(), downloadTask.getId(),
    			                     // TODO: data-transfer-type and DOC needs to be carried in the collection download
    			                     //       task instead of hard-coded here. This will be critical when we have DOC
    			                     //       specific Globus config. Until then - this works fine as is.
    			                     HpcDataTransferType.GLOBUS, "", 
                                     result, message, downloadTask.getDestinationLocation(),
                                     completed);
    }
    
    /**
     * Complete a data object download task that is in-progress (Globus transfer is in-progress).
     * 1. Check the status of Globus transfer.
     * 2. If completed (succeeded ot failed), record the result.
     *
     * @param downloadTask The download task to complete.
     * @throws HpcException on service failure.
     */
    private void completeDataObjectDownloadTaskInProgress(HpcDataObjectDownloadTask downloadTask) 
                                                         throws HpcException
    {
    
	    // Get the data transfer download status.
	    HpcDataTransferDownloadReport dataTransferDownloadReport = 
	    dataTransferService.getDataTransferDownloadStatus(
		  	                    downloadTask.getDataTransferType(), 
			                    downloadTask.getDataTransferRequestId(),
			                    downloadTask.getDoc());
	
	    // Check the status of the data transfer. 
	    HpcDataTransferDownloadStatus dataTransferDownloadStatus = dataTransferDownloadReport.getStatus();
	    if(!dataTransferDownloadStatus.equals(HpcDataTransferDownloadStatus.IN_PROGRESS)) {
	   	   // This download task is no longer in-progress - complete it.
	       boolean result = dataTransferDownloadStatus.equals(HpcDataTransferDownloadStatus.COMPLETED);
	       String message = result ? null : 
	       	                downloadTask.getDataTransferType() + " transfer failed [" +
	       	                dataTransferDownloadReport.getMessage() + "].";
	       Calendar completed = Calendar.getInstance();
	   	   dataTransferService.completeDataObjectDownloadTask(downloadTask, result, message, completed);
	   	
	   	   // Send a download completion event (if requested to).
	   	   if(downloadTask.getCompletionEvent()) {
	          addDataTransferDownloadEvent(downloadTask.getUserId(), downloadTask.getPath(),
	       		                           HpcDownloadTaskType.DATA_OBJECT, downloadTask.getId(),
	       		                           downloadTask.getDataTransferType(), downloadTask.getDoc(),
	       		                           result, message, downloadTask.getDestinationLocation(),
			                               completed);
	   	   }
	    }
    }
    
    /**
     * Set the file container name.
     *
     * @param dataTransferType The data transfer type.
     * @param doc The DOC.
     * @param fileLocation The file location.
     * @throws HpcException on service failure.
     */
    private void setFileContainerName(HpcDataTransferType dataTransferType,
    		                          String doc, HpcFileLocation fileLocation) 
    {
    	if(fileLocation == null) {
    	   return;
    	}
    	
		try {
			 // Get the file container ID name.
			 fileLocation.setFileContainerName(
					         dataTransferService.getFileContainerName(dataTransferType, doc, 
					    	     	                                  fileLocation.getFileContainerId()));
			 
		} catch(HpcException e) {
			    logger.error("Failed to get file container name: " + fileLocation.getFileContainerId());
		}
    }
    
    /**
     * Delete a data object (from the data management system)
     *
     * @param path The data object path.
     */
    private void deleteDataObject(String path)
    {
    	// Update the data transfer status. This is needed in case the actual deletion failed.
    	HpcSystemGeneratedMetadata systemGeneratedMetadata = null;
    	try {
    		 metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null, 
                                                                     HpcDataTransferUploadStatus.FAILED, null, null);
    		 
    		 systemGeneratedMetadata =  metadataService.getDataObjectSystemGeneratedMetadata(path);
    		 
    	} catch(HpcException e) {
    		    logger.error("Failed to update system metadata: " + path, HpcErrorType.UNEXPECTED_ERROR, e);
    	}
    	
    	// Delete the data object.
    	try {
    	     dataManagementService.delete(path, true);
    	     
    	} catch(HpcException e) {
    		    logger.error("Failed to delete data object: " + path, HpcErrorType.UNEXPECTED_ERROR, e);
    	}
    	
	    // Send an an event.
    	if(systemGeneratedMetadata != null) {
		   addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path, 
		    		                  systemGeneratedMetadata.getDataTransferStatus(), null, 
		    		                  systemGeneratedMetadata.getSourceLocation(), 
		    		                  systemGeneratedMetadata.getDataTransferCompleted(), 
		    		                  systemGeneratedMetadata.getDataTransferType(),
                                      systemGeneratedMetadata.getRegistrarDOC());
    	}
    }
    
    /**
     * Register a data object.
     *
     * @param registrationItem The data object registration item (one in a list).
     * @param userId The registrar user-id.
     * @param doc The registrar DOC.
     */
    private void registerDataObject(HpcDataObjectListRegistrationItem registrationItem,
    		                        String userId, String doc)
    {
    	HpcDataObjectRegistrationRequest registrationRequest = registrationItem.getRequest();
    	HpcDataObjectRegistrationTaskItem registrationTask = registrationItem.getTask();
    	
    	// Get the user name.
    	HpcUser user = null;
    	try {
    	     user = securityService.getUser(userId);
    	} catch(HpcException e) {
    		    logger.error("Failed to get user: " + userId);
    	}
    	String userName = user != null ? user.getNciAccount().getFirstName() + " " +
    			                         user.getNciAccount().getLastName() : "UNKNOWN";
          	
    	// Map request to a DTO.
    	HpcDataObjectRegistrationDTO registrationDTO = new HpcDataObjectRegistrationDTO();
    	registrationDTO.setCallerObjectId(registrationRequest.getCallerObjectId());
    	registrationDTO.setCreateParentCollections(registrationRequest.getCreateParentCollections());
    	registrationDTO.setSource(registrationRequest.getSource());
    	registrationDTO.getMetadataEntries().addAll(registrationRequest.getMetadataEntries());
    	registrationDTO.getParentCollectionMetadataEntries().addAll(registrationRequest.getParentCollectionMetadataEntries());
    	
    	try {
    	     dataManagementBusService.registerDataObject(registrationTask.getPath(), 
    		        	                                 registrationDTO, null, userId, userName, doc);
    	     
    	} catch(HpcException e) {
    		    // Data object registration failed. Update the task accordingly.
    		    registrationTask.setResult(false);
    		    registrationTask.setMessage(e.getMessage());
    		    registrationTask.setCompleted(Calendar.getInstance());
    	}
    }
    
    /**
     * Complete a data object list registration task.
     * 1. Update task info in DB with results info.
     * 2. Send an event.
     *
     * @param registrationTask The registration task to complete.
     * @param result The result of the task (true is successful, false is failed).
     * @param message (Optional) If the task failed, a message describing the failure.
     */
    private void completeDataObjectListRegistrationTask(HpcDataObjectListRegistrationTask registrationTask,
    		                                            boolean result, String message)
    {
    	Calendar completed = Calendar.getInstance();
    	
    	try {
    	     dataManagementService.completeDataObjectListRegistrationTask(registrationTask, result, message, completed);
    	     
    	} catch(HpcException e) {
    		    logger.error("Failed to complete data object list registration request", e);
    	}
    	
        // TODO: Send data object registration list completed/failed event.
    	//addDataRegistrationEvent();
    }
    
    /**
     * Check and update status of a data object registration item
     *
     * @param registrationItem The registration item to check.
     */
    private void updateRegistrationItemStatus(HpcDataObjectListRegistrationItem registrationItem)
    {
		HpcDataObjectRegistrationTaskItem registrationTask = registrationItem.getTask();
		try {
             if(registrationTask.getResult() == null) {
	                // This registration item in progress - check its status.
            	 
             	// If the data object doesn't exist, it means the upload failed and it was removed.
             	if(dataManagementService.getDataObject(registrationTask.getPath()) == null) {
             	   registrationTask.setResult(false);
             	   registrationTask.setMessage("Data object upload failed");
             	   registrationTask.setCompleted(Calendar.getInstance());
             	}

             	// Get the System generated metadata.
             	HpcSystemGeneratedMetadata metadata = 
             			 metadataService.getDataObjectSystemGeneratedMetadata(registrationTask.getPath());

             	// Check the upload status.
             	if(metadata.getDataTransferStatus().equals(HpcDataTransferUploadStatus.ARCHIVED)) {
             	   registrationTask.setResult(true);
             	   registrationTask.setCompleted(metadata.getDataTransferCompleted());
             	}
             }

		} catch(HpcException e) {
                logger.error("Failed to check data object registration item status", e);
                registrationTask.setResult(false);
                registrationTask.setMessage(e.getMessage());
		}
	}
}


 