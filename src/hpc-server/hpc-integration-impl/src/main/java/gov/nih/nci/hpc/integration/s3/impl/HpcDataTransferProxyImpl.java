package gov.nih.nci.hpc.integration.s3.impl;

import static gov.nih.nci.hpc.integration.HpcDataTransferProxy.getArchiveDestinationLocation;

import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;

import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

/**
 * <p>
 * HPC Data Transfer Proxy S3 Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataTransferProxyImpl implements HpcDataTransferProxy 
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The S3 connection instance.
	@Autowired
    private HpcS3Connection s3Connection = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for spring injection.
     * 
     */
	private HpcDataTransferProxyImpl() 
    {
    }
	
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataTransferProxy Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount,
    		                   String url) 
		                      throws HpcException
    {
    	return s3Connection.authenticate(dataTransferAccount, url);
    }
    
    @Override
    public HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
    		                                            HpcDataObjectUploadRequest uploadRequest,
    		                                            List<HpcMetadataEntry> metadataEntries,
    		                                            HpcArchive baseArchiveDestination,
    		                                            HpcDataTransferProgressListener progressListener) 
    		                                           throws HpcException
   {
       	// Calculate the archive destination.
    	HpcFileLocation archiveDestinationLocation = 
    	   getArchiveDestinationLocation(baseArchiveDestination.getFileLocation(), 
    			                         uploadRequest.getPath(),
    		                             uploadRequest.getCallerObjectId(),
    		                             baseArchiveDestination.getType());
    	
    	// Create a metadata to associate with the data object.
    	ObjectMetadata objectMetadata = new ObjectMetadata();
    	if(metadataEntries != null) {
    	   for(HpcMetadataEntry metadataEntry : metadataEntries) {
    	       objectMetadata.addUserMetadata(metadataEntry.getAttribute(), 
    	    		                          metadataEntry.getValue());
    	   }
    	}
    	
    	// Create a S3 upload request.
    	PutObjectRequest request = 
    	   new PutObjectRequest(archiveDestinationLocation.getFileContainerId(), 
    			                archiveDestinationLocation.getFileId(), 
    			                uploadRequest.getSourceFile()).withMetadata(objectMetadata); 
    	
    	// Upload the data.
    	Upload s3Upload = null;
    	UploadResult s3UploadResult = null;
    	Calendar dataTransferStarted = Calendar.getInstance();
    	Calendar dataTransferCompleted = null;
    	try {
    	     s3Upload = s3Connection.getTransferManager(authenticatedToken).upload(request);
    		 if(progressListener == null) {
    			// Upload synchronously.
    			s3UploadResult = s3Upload.waitForUploadResult();
    			dataTransferCompleted = Calendar.getInstance();
    		 } else {
    			     // Upload asynchronously
    			     s3Upload.addProgressListener(new HpcS3ProgressListener(progressListener));
    			 
    		 }
    		 
        } catch(AmazonClientException ace) {
        	    throw new HpcException("[S3] Failed to upload file.", 
        	    		               HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.CLEVERSAFE, ace);
        	    
        } catch(InterruptedException ie) {
        	    Thread.currentThread().interrupt();
        }
    	
    	// Upload completed. Create and populate the response object.
    	HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
    	uploadResponse.setArchiveLocation(archiveDestinationLocation);
    	uploadResponse.setDataTransferType(HpcDataTransferType.S_3);
    	uploadResponse.setDataTransferStarted(dataTransferStarted);
    	uploadResponse.setDataTransferCompleted(dataTransferCompleted);
    	uploadResponse.setDataTransferRequestId(String.valueOf(s3Upload.hashCode()));
    	uploadResponse.setChecksum(s3UploadResult != null ? s3UploadResult.getETag() : "Unknown");
    	if(baseArchiveDestination.getType().equals(HpcArchiveType.ARCHIVE)) {
    	   uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.ARCHIVED);
    	} else {
    		    uploadResponse.setDataTransferStatus(HpcDataTransferUploadStatus.IN_TEMPORARY_ARCHIVE);
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
    	// Create a S3 download request.
    	GetObjectRequest request = 
    	   new GetObjectRequest(downloadRequest.getArchiveLocation().getFileContainerId(), 
    			                downloadRequest.getArchiveLocation().getFileId());
    	
    	// Download the file via S3. 
    	Download s3Download = null;
    	try {
    		 s3Download = s3Connection.getTransferManager(authenticatedToken).
    				        download(request, downloadRequest.getDestinationFile());
    		 if(progressListener == null) {
    			// Download synchronously.
    		    s3Download.waitForCompletion(); 
    		 } else {
    			     // Download asynchronously.
    			     s3Download.addProgressListener(new HpcS3ProgressListener(progressListener));
    		 }
    		    
        } catch(AmazonClientException ace) {
    	        throw new HpcException("[S3] Failed to download file: [" + ace.getMessage() + "]", 
    	    	     	               HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.CLEVERSAFE, ace);
    	    
        } catch(InterruptedException ie) {
    	        Thread.currentThread().interrupt();
        }
    	
    	HpcDataObjectDownloadResponse downloadResponse = new HpcDataObjectDownloadResponse();
    	downloadResponse.setDataTransferRequestId(String.valueOf(s3Download.hashCode()));
    	downloadResponse.setDestinationFile(downloadRequest.getDestinationFile());
    	
    	return downloadResponse;
    }
    
    @Override
    public void deleteDataObject(Object authenticatedToken, HpcFileLocation fileLocation)
    		                    throws HpcException 
    {
    	// Create a S3 delete request.
    	DeleteObjectRequest request = new DeleteObjectRequest(fileLocation.getFileContainerId(), 
    			                                              fileLocation.getFileId());
    	try {
   		     s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client().deleteObject(request);
   		    
    	} catch(AmazonServiceException ase) {
    		    throw new HpcException("[S3] Failed to delete file: " + request, 
  	                                   HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.CLEVERSAFE, ase);
    		    
        } catch(AmazonClientException ace) {
        	    throw new HpcException("[S3] Failed to delete file: " + request, 
                                       HpcErrorType.DATA_TRANSFER_ERROR, HpcIntegratedSystem.CLEVERSAFE, ace);
        }	
    }
}