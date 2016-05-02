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
import gov.nih.nci.hpc.dao.HpcSystemAccountDAO;
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
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcDataTransferAuthenticatedToken;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;

import java.util.HashMap;
import java.util.Map;

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
	
	// Map data transfer type to a 'credentials' structure
	private Map<HpcDataTransferType, HpcDataTransferCredential> dataTransferCredentials = null;
	private class HpcDataTransferCredential
	{
		private HpcDataTransferProxy proxy = null;
		private HpcIntegratedSystemAccount account = null;
	};
    
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
    		Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies,
    		HpcSystemAccountDAO systemAccountDAO) 
    		throws HpcException
    {
    	dataTransferCredentials = new HashMap<HpcDataTransferType, HpcDataTransferCredential>();
    	for(HpcDataTransferType dataTransferType : dataTransferProxies.keySet()) {
    		// Get the data transfer system account.
    		HpcIntegratedSystemAccount account = systemAccountDAO.getSystemAccount(dataTransferType);
        	if(account == null) {	
         	   throw new HpcException("System account not configured for: " + dataTransferType.value(), 
         			                  HpcErrorType.UNEXPECTED_ERROR);
         	}	
        	
        	// Create a credential object and authenticate.
        	HpcDataTransferCredential dataTransferCredential = new HpcDataTransferCredential();
        	dataTransferCredential.proxy = dataTransferProxies.get(dataTransferType);
        	dataTransferCredential.account = account;
        	
        	dataTransferCredentials.put(dataTransferType, dataTransferCredential);
    	}
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
    	HpcDataTransferCredential dataTransferCredential = 
    			                  dataTransferCredentials.get(dataTransferType);
  	    return dataTransferCredential.proxy.uploadDataObject(getAuthenticatedToken(dataTransferType), 
  	    		                		                     uploadRequest);
    }
    
    @Override
    public HpcDataObjectDownloadResponse 
              downloadDataObject(HpcDataObjectDownloadRequest downloadRequest) 
                                throws HpcException
    {
    	// Input Validation.
    	HpcDataTransferType dataTransferType = downloadRequest.getTransferType();
    	if(dataTransferType == null ||
    	   (dataTransferType.equals(HpcDataTransferType.GLOBUS) && 
    	    !isValidFileLocation(downloadRequest.getDestinationLocation()))) {
  	       throw new HpcException("Invalid data transfer type or upload file location", 
  	    	                      HpcErrorType.INVALID_REQUEST_INPUT);
  	   }
    	
    	// Download the data object using the appropriate data transfer proxy.
    	HpcDataTransferCredential dataTransferCredential = 
                                  dataTransferCredentials.get(dataTransferType);
  	    return dataTransferCredential.proxy.downloadDataObject(getAuthenticatedToken(dataTransferType), 
  	                                                           downloadRequest);	
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
		
    	HpcDataTransferCredential dataTransferCredential = 
                                  dataTransferCredentials.get(dataTransferType);
		return dataTransferCredential.proxy.getDataTransferStatus(getAuthenticatedToken(dataTransferType), 
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
		
    	HpcDataTransferCredential dataTransferCredential = 
                                  dataTransferCredentials.get(dataTransferType);
		return dataTransferCredential.proxy.getDataTransferSize(getAuthenticatedToken(dataTransferType), 
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
    	
    	HpcDataTransferCredential dataTransferCredential = 
                                  dataTransferCredentials.get(dataTransferType);
    	return dataTransferCredential.proxy.getPathAttributes(getAuthenticatedToken(dataTransferType), 
    			                                              fileLocation, getSize);
    }
	
	@Override
	public void setPermission(HpcDataTransferType dataTransferType,
			                  HpcFileLocation fileLocation,
                              HpcUserPermission permissionRequest,
                              HpcIntegratedSystemAccount dataTransferAccount) 
                             throws HpcException
    {
    	// Input validation.
    	if(!HpcDomainValidator.isValidFileLocation(fileLocation)) {	
    	   throw new HpcException("Invalid file location", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Determine the user ID to use on the permission request.
    	HpcUserPermission request = permissionRequest;
    	if(dataTransferAccount != null) {
    	   request = new HpcUserPermission();
    	   request.setPermission(permissionRequest.getPermission());
    	   request.setUserId(dataTransferAccount.getUsername());
    	}
    	
    	HpcDataTransferCredential dataTransferCredential = 
                                  dataTransferCredentials.get(dataTransferType);
    	dataTransferCredential.proxy.setPermission(getAuthenticatedToken(dataTransferType), 
                                                   fileLocation, request);
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
        HpcDataTransferCredential dataTransferCredential = 
                                  dataTransferCredentials.get(dataTransferType);
        Object token = dataTransferCredential.proxy.authenticate(dataTransferCredential.account);
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
}
 