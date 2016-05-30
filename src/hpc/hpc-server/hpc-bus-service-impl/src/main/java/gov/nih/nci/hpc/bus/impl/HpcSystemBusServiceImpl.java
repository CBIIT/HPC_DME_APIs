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

import gov.nih.nci.hpc.bus.HpcSystemBusService;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadCleanup;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.model.HpcDataObjectSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcSecurityService;

import java.io.File;
import java.util.Calendar;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC System Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcSystemBusServiceImpl implements HpcSystemBusService
{  
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // Data transfer status check timeout, in days. If these many days pass 
	// after the data registration date, and we still can't get a data transfer 
	// status, then the state will move to UNKNOWN.
	private static final int DATA_TRANSFER_STATUS_CHECK_TIMEOUT_PERIOD = 1;
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // Application service instances.
	
	@Autowired
    private HpcSecurityService securityService = null;
	
	@Autowired
    private HpcDataTransferService dataTransferService = null;
	
	@Autowired
    private HpcDataManagementService dataManagementService = null;
	
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
    public void updateDataTransferUploadStatus() throws HpcException
    {
    	// Use system account to perform this service.
        // TODO: Make this AOP. 
    	securityService.setSystemRequestInvoker();
    	
    	// Iterate through the data objects that their data transfer is in-progress
    	for(HpcDataObject dataObject : dataManagementService.getDataObjectsInProgress()) {
    		String path = dataObject.getAbsolutePath();
    		try {
    		     // Get current data transfer Request Info.
    			 HpcDataObjectSystemGeneratedMetadata systemGeneratedMetadata = 
    			    dataManagementService.getDataObjectSystemGeneratedMetadata(path);
    			 
    			 // Get the data transfer upload request status.
    			 HpcDataTransferUploadStatus dataTransferStatus =
    		        dataTransferService.getDataTransferUploadStatus(
    		        		               systemGeneratedMetadata.getDataTransferType(),
    		        		               systemGeneratedMetadata.getDataTransferRequestId());
    			 
    		     if(dataTransferStatus.equals(HpcDataTransferUploadStatus.ARCHIVED) ||
    		        dataTransferStatus.equals(HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE)) {
    		    	// Data transfer completed successfully. Update the system metadata.
     			    setDataTransferUploadStatus(path, dataTransferStatus);
    		    	logger.info("Data transfer completed [" + dataTransferStatus + "]: " + path);
    		    	
    		     } else if(dataTransferStatus.equals(HpcDataTransferUploadStatus.FAILED)) {
     		    	       // Data transfer failed. Remove the data object
     		    	       dataManagementService.delete(path);
     		    	       logger.info("Data transfer failed: " + path);
    		     }
    		     
    		} catch(HpcException e) {
    			    logger.error("Failed to process data transfer upload update:" + path, e);
    			    
    			    // If timeout occurred, move the status to unknown.
    			    setTransferUploadStatusToUnknown(dataObject, true);
    		}
    	}
    }
    
    @Override
    public void processTemporaryArchive() throws HpcException
    {
    	// Use system account to perform this service.
    	securityService.setSystemRequestInvoker();
    	
    	// Iterate through the data objects that their data is in temporary archive
    	for(HpcDataObject dataObject : dataManagementService.getDataObjectsInTemporaryArchive()) {
    		String path = dataObject.getAbsolutePath();
    		HpcDataObjectSystemGeneratedMetadata systemGeneratedMetadata = null;
    		try {
    		     // Get the data object system generated metadata.
    			 systemGeneratedMetadata = 
    			       dataManagementService.getDataObjectSystemGeneratedMetadata(path);
    			 
    			 // Get an input stream to the data object in the temporary archive.
    			 File file = dataTransferService.getArchiveFile(
    					         systemGeneratedMetadata.getDataTransferType(),
    					         systemGeneratedMetadata.getArchiveLocation().getFileId());
    			 
 				 // Transfer the data file.
 		         HpcDataObjectUploadResponse uploadResponse = 
 		        	dataTransferService.uploadDataObject(null, file, path, 
 		        			                             systemGeneratedMetadata.getRegistrarId(),
 		        			                             systemGeneratedMetadata.getCallerObjectId());
 		     
 		         // Delete the file.
 		         if(!FileUtils.deleteQuietly(file)) {
 		        	logger.error("Failed to delete file: " + 
 		                          systemGeneratedMetadata.getArchiveLocation().getFileId());
 		         }
 		         
 			     // Update system metadata of the data object.
 			     dataManagementService.updateDataObjectSystemGeneratedMetadata(
 			           		                 path, uploadResponse.getArchiveLocation(),
 			    			                 uploadResponse.getRequestId(), 
 			    			                 uploadResponse.getDataTransferStatus(),
 			    			                 uploadResponse.getDataTransferType()); 
 			     
    		} catch(HpcException e) {
    			    logger.error("Failed to transfer data from temporary archive:" + path, e);
    			    
    			    // If timeout occurred, move the status to unknown.
    			    setTransferUploadStatusToUnknown(dataObject, true);
    		}
    	}
    }
    
    @Override
    public void cleanupDataTransferDownloadFiles() throws HpcException
    {
    	// Use system account to perform this service.
    	securityService.setSystemRequestInvoker();
    	
    	for(HpcDataObjectDownloadCleanup dataObjectDownloadCleanup :
    		dataTransferService.getDataObjectDownloadCleanups()) {
    		// Get the data transfer download status.
    		HpcDataTransferDownloadStatus dataTransferDownloadStatus = 
    		   dataTransferService.getDataTransferDownloadStatus(
    			                      dataObjectDownloadCleanup.getDataTransferType(), 
    			                      dataObjectDownloadCleanup.getDataTransferRequestId());
    		
    		// Cleanup the file if the transfer is no longer in-progress.
    		if(!dataTransferDownloadStatus.equals(HpcDataTransferDownloadStatus.IN_PROGRESS)) {
    		   dataTransferService.cleanupDataObjectDownloadFile(dataObjectDownloadCleanup);
    		}
    	}
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /** 
     * Determine if data transfer status check timed out.
     * 
     * @param dataObject The data object to check the timeout for.
     * 
     * @return true if status check timeout occurred. 
     */
	private boolean isDataTransferStatusCheckTimedOut(HpcDataObject dataObject) 
	{
		if(dataObject.getCreatedAt() == null) {
		   // Creation time unknown.
		   return true;	
		}
		
		// Calculate the timeout.
		Calendar timeout = Calendar.getInstance();
	    timeout.setTime(dataObject.getCreatedAt().getTime());
	    timeout.add(Calendar.DATE, DATA_TRANSFER_STATUS_CHECK_TIMEOUT_PERIOD);
	    
	    // Compare to now.
	    return Calendar.getInstance().after(timeout);
	}
	
    /** 
     * Set the data transfer upload status of a data object to unknown.
     * 
     * @param dataObject The data object
     * @param checkTimeout If 'true', this method checks for transfer status timeout occurred 
     *                     before setting the status. 
     * @return HpcDataObjectUploadResponse
     * 
     * @throws HpcException
     */
	private void setTransferUploadStatusToUnknown(HpcDataObject dataObject, 
			                                      boolean checkTimeout)
	{
		// If timeout occurred, move the status to unknown.
		if(!checkTimeout || isDataTransferStatusCheckTimedOut(dataObject)) {
			try {
				 setDataTransferUploadStatus(dataObject.getAbsolutePath(), 
    	                                     HpcDataTransferUploadStatus.UNKNOWN);
			} catch(Exception ex) {
				    logger.error("failed to set data transfer status to unknown: " + 
				    		     dataObject.getAbsolutePath(), ex);
			}
			
			logger.error("Unknown data transfer status: " + dataObject.getAbsolutePath());
		}
	}
	
    /** 
     * Set data transfer upload status of a data object.
     * 
     * @param path The data object path.
     * @param dataTransferStatus The data transfer status.
     * 
     * @throws HpcException
     */
	private void setDataTransferUploadStatus(String path, HpcDataTransferUploadStatus dataTransferStatus)
	                                        throws HpcException
	{
		dataManagementService.updateDataObjectSystemGeneratedMetadata(
                                    path, null, null, dataTransferStatus, null);
	}
}

 