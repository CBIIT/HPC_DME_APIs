package gov.nih.nci.hpc.integration.s3.impl;

import static gov.nih.nci.hpc.integration.HpcDataTransferProxy.getArchiveDestination;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

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
	HpcFileLocation baseArchiveDestination = null;
    
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
    		                                            HpcDataObjectUploadRequest uploadRequest) 
    		                                           throws HpcException
   {
       	// Calculate the archive destination.
    	HpcFileLocation archiveDestination = 
    	   getArchiveDestination(baseArchiveDestination, uploadRequest.getPath(),
    		                     uploadRequest.getCallerObjectId());
    	
    	// Create a metadata to associate the data management path.
    	ObjectMetadata metadata = new ObjectMetadata();
    	metadata.addUserMetadata("path", uploadRequest.getPath());
    	
    	// Create a S3 upload request.
    	PutObjectRequest request = new PutObjectRequest(archiveDestination.getFileContainerId(), 
    			                                        archiveDestination.getFileId(), 
    			                                        uploadRequest.getSourceInputStream(), 
    			                                        metadata);
    	
    	try {
    	     s3Connection.getTransferManager(authenticatedToken).upload(request).waitForCompletion();
        	
        } catch(AmazonClientException ace) {
        	    throw new HpcException("Failed to upload file via S3", 
        	    		               HpcErrorType.DATA_TRANSFER_ERROR, ace);
        } catch(InterruptedException ie) {
    	    throw new HpcException("S3 upload interrupted", 
		                           HpcErrorType.DATA_TRANSFER_ERROR, ie);   
        }
    	
    	HpcDataObjectUploadResponse uploadResponse = new HpcDataObjectUploadResponse();
    	uploadResponse.setArchiveLocation(archiveDestination);
    	uploadResponse.setDataTransferStatus(HpcDataTransferStatus.ARCHIVED);
    	uploadResponse.setDataTransferType(HpcDataTransferType.S_3);
        
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
    
    @Override
    public HpcDataTransferStatus getDataTransferStatus(Object authenticatedToken,
                                                       String dataTransferRequestId) 
                                                      throws HpcException
    {
    	throw new HpcException("getDataTransferStatus() not supported by S3",
    			               HpcErrorType.UNEXPECTED_ERROR);
    }
    
    @Override
    public long getDataTransferSize(Object authenticatedToken,
                                    String dataTransferRequestId) 
                                   throws HpcException
    {
    	throw new HpcException("getDataTransferSize() not supported by S3",
	                           HpcErrorType.UNEXPECTED_ERROR);
    }
    
    @Override
    public HpcPathAttributes getPathAttributes(Object authenticatedToken, 
                                               HpcFileLocation fileLocation,
                                               boolean getSize) 
                                              throws HpcException
    {
       	throw new HpcException("getPathAttributes() not supported by S3",
                HpcErrorType.UNEXPECTED_ERROR);
    }
    
    @Override
    public void setPermission(Object authenticatedToken,
                              HpcFileLocation fileLocation,
                              HpcUserPermission permissionRequest) 
                             throws HpcException
    {
       	throw new HpcException("setPermission() not supported by S3",
                               HpcErrorType.UNEXPECTED_ERROR); 
    }
}