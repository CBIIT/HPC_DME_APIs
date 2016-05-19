package gov.nih.nci.hpc.integration.s3.impl;

import static gov.nih.nci.hpc.integration.HpcDataTransferProxy.getArchiveDestinationLocation;

import java.util.List;

import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.Upload;

/**
 * <p>
 * HPC Data Transfer Proxy S3 Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataTransferProxyImpl implements HpcDataTransferProxy 
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The S3 connection instance.
	@Autowired
    private HpcS3Connection s3Connection = null;
	
	@Autowired
	@Qualifier("hpcS3ArchiveDestination")
	HpcArchive baseArchiveDestination = null;
    
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
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
    public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount) 
		                      throws HpcException
    {
    	return s3Connection.authenticate(dataTransferAccount);
    }
    
    @Override
    public HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
    		                                            HpcDataObjectUploadRequest uploadRequest,
    		                                            List<HpcMetadataEntry> metadataEntries) 
    		                                           throws HpcException
   {
       	// Calculate the archive destination.
    	HpcFileLocation archiveDestinationLocation = 
    	   getArchiveDestinationLocation(baseArchiveDestination.getFileLocation(), 
    			                         uploadRequest.getPath(),
    		                             uploadRequest.getCallerObjectId());
    	
    	// Create a metadata to associate with the data object.
    	ObjectMetadata objectMetadata = new ObjectMetadata();
    	if(metadataEntries != null) {
    	   for(HpcMetadataEntry metadataEntry : metadataEntries) {
    	       objectMetadata.addUserMetadata(metadataEntry.getAttribute(), 
    	    		                          metadataEntry.getValue());
    	   }
    	}
    	
    	// Create a S3 upload request.
    	PutObjectRequest request = new PutObjectRequest(archiveDestinationLocation.getFileContainerId(), 
    			                                        archiveDestinationLocation.getFileId(), 
    			                                        uploadRequest.getSourceInputStream(), 
    			                                        objectMetadata);
    	
    	// Upload the data.
    	Upload s3Upload = null;
    	try {
    	     s3Upload = s3Connection.getTransferManager(authenticatedToken).upload(request);
    	     s3Upload.waitForCompletion();
        	
        } catch(AmazonClientException ace) {
        	    throw new HpcException("Failed to upload file via S3", 
        	    		               HpcErrorType.DATA_TRANSFER_ERROR, ace);
        } catch(InterruptedException ie) {
        	    Thread.currentThread().interrupt();
        }
    	
    	// Upload completed. Create and populate the response object.
    	HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
    	uploadResponse.setArchiveLocation(archiveDestinationLocation);
    	uploadResponse.setDataTransferType(HpcDataTransferType.S_3);
    	uploadResponse.setRequestId(String.valueOf(s3Upload.hashCode()));
    	if(baseArchiveDestination.getType().equals(HpcArchiveType.ARCHIVE)) {
    	   uploadResponse.setDataTransferStatus(HpcDataTransferStatus.ARCHIVED);
    	} else {
    		    uploadResponse.setDataTransferStatus(HpcDataTransferStatus.IN_TEMPORARY_ARCHIVE);
    	}
        
        return uploadResponse;
   }
    
    @Override
    public HpcDataObjectDownloadResponse 
              downloadDataObject(Object authenticatedToken,
    		                     HpcDataObjectDownloadRequest downloadRequest) 
    		                    throws HpcException
    {
    	HpcDataObjectDownloadResponse response = new HpcDataObjectDownloadResponse();
    	
    	// Download the file via S3. 
    	// Note: TransferManager currently not supporting download w/ an InputStream. 
    	//       We use the Client API until this is added.
    	try {
    		 response.setInputStream(
    		 s3Connection.getTransferManager(authenticatedToken).getAmazonS3Client().
    		   getObject(downloadRequest.getArchiveLocation().getFileContainerId(),
    			         downloadRequest.getArchiveLocation().getFileId()).getObjectContent());
    			             
    	     return response;
    	     
        } catch(AmazonClientException ace) {
    	        throw new HpcException("Failed to upload file via S3", 
    	    	     	               HpcErrorType.DATA_TRANSFER_ERROR, ace);
        }
    }
}