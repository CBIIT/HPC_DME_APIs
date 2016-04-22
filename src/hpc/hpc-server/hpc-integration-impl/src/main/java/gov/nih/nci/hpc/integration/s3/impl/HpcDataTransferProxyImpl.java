package gov.nih.nci.hpc.integration.s3.impl;

import static gov.nih.nci.hpc.integration.HpcDataTransferProxy.getArchiveDestination;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;

import java.io.File;
import java.io.InputStream;

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
    
    /**
     * Upload a data object file.
     *
      *@param authenticatedToken An authenticated token.
     * @param dataUploadRequest The data upload request
     * @return HpcDataObjectUploadResponse A data object upload response.
     * 
     * @throws HpcException
     */
    public HpcDataObjectUploadResponse uploadDataObject(Object authenticatedToken,
    		                                            HpcDataObjectUploadRequest uploadRequest) 
    		                                           throws HpcException
   {
    	// Input validation.
    	if(!(uploadRequest.getSource() instanceof InputStream)) {
    	   throw new HpcException("Invalid source type: " + 
    	                          uploadRequest.getSource().getClass().getName(),
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Calculate the archive destination.
    	InputStream inputStream = (InputStream) uploadRequest.getSource();
       	// Calculate the archive destination.
    	HpcFileLocation archiveDestination = 
    	   getArchiveDestination(baseArchiveDestination, uploadRequest.getPath(),
    		                     uploadRequest.getCallerObjectId());
    	
    	// Create a metadata to associate the data management path.
    	ObjectMetadata metadata = new ObjectMetadata();
    	metadata.addUserMetadata("path", uploadRequest.getPath());
    	
    	// Create a S3 upload request.
    	PutObjectRequest request = new PutObjectRequest(archiveDestination.getFileContainerId(), 
    			                                        archiveDestination.getFileId(), inputStream, 
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
    
    /**
     * Download a data object file.
     *
     * @param authenticatedToken An authenticated token.
     * @param dataDownloadRequest The data object download request.
     * 
     * @throws HpcException
     */
    public void downloadDataObject(Object authenticatedToken,
    		                       HpcDataObjectDownloadRequest downloadRequest) 
    		                      throws HpcException
    {
    	
    	// Input validation.
    	if(!(downloadRequest.getDestination() instanceof File)) {
    	   throw new HpcException("Invalid destination type: " + 
    			                  downloadRequest.getDestination().getClass().getName(),
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Download the file via S3.
    	try {
    	     s3Connection.getTransferManager(authenticatedToken).download(
    		         	     downloadRequest.getArchiveLocation().getFileContainerId(),
    			             downloadRequest.getArchiveLocation().getFileId(), 
    			             (File) downloadRequest.getDestination());
    	     
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