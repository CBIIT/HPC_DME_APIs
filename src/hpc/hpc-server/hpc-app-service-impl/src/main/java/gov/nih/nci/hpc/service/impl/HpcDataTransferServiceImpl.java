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
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcDataTransferAuthenticatedToken;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Transfer Service Implementation.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$
 */

public class HpcDataTransferServiceImpl implements HpcDataTransferService
{     
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// Map data transfer type to its proxy impl.
	private Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies = 
			new HashMap<HpcDataTransferType, HpcDataTransferProxy>();
	
	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param dataTransferProxies The data transfer proxies.
     * @throws HpcException
     */
    private HpcDataTransferServiceImpl(
    		Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies) 
    		throws HpcException
    {
    	if(dataTransferProxies == null || dataTransferProxies.isEmpty()) {
    	   throw new HpcException("Null or empty map of data transfer proxies",
    			                  HpcErrorType.SPRING_CONFIGURATION_ERROR);
    	}
    	
    	this.dataTransferProxies.putAll(dataTransferProxies);
    }   
    
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcDataTransferServiceImpl() throws HpcException
    {
    	throw new HpcException("Default Constructor disabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // HpcDataTransferService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public HpcDataObjectUploadResponse uploadDataObject(HpcDataObjectUploadRequest uploadRequest) 
                                                       throws HpcException
    {
    	// Input validation.
    	if(uploadRequest.getPath() == null) {
    	   throw new HpcException("Null data object path", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
        	
    	// Determine the data transfer type.
    	HpcDataTransferType dataTransferType = null;
    	if(uploadRequest.getSourceLocation() != null) {
    	   if(!isValidFileLocation(uploadRequest.getSourceLocation())) {
    	      throw new HpcException("Invalid upload file location", 
    	    	                     HpcErrorType.INVALID_REQUEST_INPUT);
    	   }
    	   dataTransferType = HpcDataTransferType.GLOBUS;
    	   
    	} else if(uploadRequest.getSourceInputStream() != null) {
    		      dataTransferType = HpcDataTransferType.S_3;
    		      
    	} else {
    		    // Could not determine data transfer type.
    		    throw new HpcException("Could not determine data transfer type",
    		    		               HpcErrorType.UNEXPECTED_ERROR);
    	}

    	// Upload the data object using the appropriate data transfer proxy.
  	    return dataTransferProxies.get(dataTransferType).
  	    		   uploadDataObject(getAuthenticatedToken(dataTransferType), 
  	    		                    uploadRequest);
    }
    
    @Override
    public HpcDataObjectDownloadResponse 
              downloadDataObject(HpcDataObjectDownloadRequest downloadRequest) 
                                throws HpcException
    {
    	// Input Validation.
    	HpcDataTransferType dataTransferType = downloadRequest.getTransferType();
    	HpcFileLocation destinationLocation = downloadRequest.getDestinationLocation();
    	if(dataTransferType == null || 
    	   !isValidFileLocation(downloadRequest.getArchiveLocation()) ||
    	   (destinationLocation != null && !isValidFileLocation(destinationLocation))) {
  	       throw new HpcException("Invalid data transfer request", 
  	    	                      HpcErrorType.INVALID_REQUEST_INPUT);
  	   }
    	
    	// Download the data object using the appropriate data transfer proxy.
    	HpcDataObjectDownloadResponse downloadResponse =  
    	   dataTransferProxies.get(dataTransferType).
  	    		   downloadDataObject(getAuthenticatedToken(dataTransferType), 
  	                                  downloadRequest);	
    	
    	// Data was downloaded. 
    	// if data was downloaded from archive with S3, and the requested
    	// destination is a GLOBUS endpoint, then we need to perform a 2nd hop. i.e. store 
    	// the data to a local GLOBUS endpoint, and submit a transfer request to the caller's 
    	// destination.
    	if(dataTransferType.equals(HpcDataTransferType.S_3) && destinationLocation != null) {
    	   // 2nd Hop needed.
    	   return downloadDataObject(
    			          toDownloadRequest(downloadResponse.getInputStream(),
    			        		            destinationLocation,
    			        		            HpcDataTransferType.GLOBUS,
    			                            downloadRequest.getArchiveLocation().getFileId()));
    	   
    	} else {
    		    return downloadResponse;
    	}
    }
    
	@Override   
	public HpcDataTransferStatus getDataTransferStatus(HpcDataTransferType dataTransferType,
			                                           String dataTransferRequestId) 
                                                      throws HpcException
    {	// Input Validation.
		if(dataTransferRequestId == null) {
		   throw new HpcException("Null data transfer request ID", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		return dataTransferProxies.get(dataTransferType).
				   getDataTransferStatus(getAuthenticatedToken(dataTransferType), 
           	                             dataTransferRequestId);
    }	
	
	@Override   
	public long getDataTransferSize(HpcDataTransferType dataTransferType,
			                        String dataTransferRequestId) 
                                   throws HpcException
    {	// Input Validation.
		if(dataTransferRequestId == null) {
		   throw new HpcException("Null data transfer request ID", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		return dataTransferProxies.get(dataTransferType).
				   getDataTransferSize(getAuthenticatedToken(dataTransferType), 
           	                           dataTransferRequestId);
    }	
	
	@Override
	public HpcPathAttributes getPathAttributes(HpcDataTransferType dataTransferType,
			                                   HpcFileLocation fileLocation,
			                                   boolean getSize) 
                                              throws HpcException
    {
    	// Input validation.
    	if(!HpcDomainValidator.isValidFileLocation(fileLocation)) {	
    	   throw new HpcException("Invalid file location", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	return dataTransferProxies.get(dataTransferType).
    			   getPathAttributes(getAuthenticatedToken(dataTransferType), 
    			                     fileLocation, getSize);
    }
	
	@Override
	public File getUploadFile(HpcDataTransferType dataTransferType,
                              String fileId)  
    	                     throws HpcException
    {
    	// Input validation.
    	if(fileId == null) {	
    	   throw new HpcException("Invalid file id", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	return dataTransferProxies.get(dataTransferType).getUploadFile(fileId);
    }
	
	@Override
	public File getDownloadFile(HpcDataTransferType dataTransferType,
                                String fileId)  
    	                       throws HpcException
    {
    	// Input validation.
    	if(fileId == null) {	
    	   throw new HpcException("Invalid file id", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
    	return dataTransferProxies.get(dataTransferType).getDownloadFile(fileId);
    }

    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
    /**
     * Get the data transfer authenticated token from the request context.
     * If it's not in the context, get a token by authenticating.
     * 
     * @param dataTransferType The data transfer type.
     *
     * @throws HpcException If it failed to obtain an authentication token.
     */
    private Object getAuthenticatedToken(HpcDataTransferType dataTransferType) throws HpcException
    {
    	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    	if(invoker == null) {
	       throw new HpcException("Unknown user",
			                      HpcRequestRejectReason.INVALID_DATA_TRANSFER_ACCOUNT);
    	}
    	
    	// Search for an existing token.
    	for(HpcDataTransferAuthenticatedToken authenticatedToken : 
    		invoker.getDataTransferAuthenticatedTokens()) {
    		if(authenticatedToken.getDataTransferType().equals(dataTransferType)) {
    	       return authenticatedToken.getDataTransferAuthenticatedToken();
    		}
    	}
    	
    	// No authenticated token found for this request. Create one.
        Object token = dataTransferProxies.get(dataTransferType).
        		           authenticate(systemAccountLocator.getSystemAccount(dataTransferType));
    	if(token == null) {
    	   throw new HpcException("Invalid data transfer account credentials",
                                  HpcRequestRejectReason.INVALID_DATA_TRANSFER_ACCOUNT);
    	}
    	
    	// Store token on the request context.
    	HpcDataTransferAuthenticatedToken authenticatedToken = new HpcDataTransferAuthenticatedToken();
    	authenticatedToken.setDataTransferAuthenticatedToken(token);
    	authenticatedToken.setDataTransferType(dataTransferType);
    	invoker.getDataTransferAuthenticatedTokens().add(authenticatedToken);
    	
    	return token;
    }
    
    /**
     * Create a download request for a 2nd hop download from local Globus endpoint
     * to caller's destination.
     * 
     * @param dataObjectInputStream The data object input stream (downloaded from S3).
     * @param destinationLocation The caller's destination.
     * @param dataTransferType The data transfer type to create the request
     * @param The data object logical path.
     *
     * @throws HpcException If it failed to obtain an authentication token.
     */
    HpcDataObjectDownloadRequest toDownloadRequest(InputStream dataObjectInputStream, 
    		                                       HpcFileLocation destinationLocation,
    		                                       HpcDataTransferType dataTransferType,
    		                                       String path)
    		                                      throws HpcException
    {
    	// Create a source location.
    	HpcFileLocation sourceLocation = 
    	   dataTransferProxies.get(dataTransferType).getDownloadSourceLocation(path);
    	                        
    	// Store the input stream to the local GLOBUS endpoint.
    	try {
	         FileOutputStream dataObjectOutputStream = 
	             new FileOutputStream(
	            		 dataTransferProxies.get(dataTransferType).
	            		                     getDownloadFile(sourceLocation.getFileId()));
	         IOUtils.copyLarge(dataObjectInputStream, dataObjectOutputStream);
	         IOUtils.closeQuietly(dataObjectInputStream);
	         IOUtils.closeQuietly(dataObjectOutputStream);
	         
    	} catch(Exception e) {
    		    throw new HpcException("Failed to store data object to local GLOBUS endpoint", 
    		    		               HpcErrorType.DATA_TRANSFER_ERROR, e);
    	}
    	
    	// Create and return a download request, from the local GLOBUS endpoint, to the caller's
    	// destination.
    	HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
    	downloadRequest.setArchiveLocation(sourceLocation);
    	downloadRequest.setDestinationLocation(destinationLocation);
    	downloadRequest.setTransferType(dataTransferType);

    	return downloadRequest;
    }
}
 