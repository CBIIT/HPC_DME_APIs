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
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;

import java.util.Map;

import javax.annotation.Resource;

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
	
    // The Data Transfer Proxies. Mapped by type.
	// @Autowired did not work for a map with an enum key. This explicitly wired instead.
    private Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies = null;
    
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcDataTransferServiceImpl() throws HpcException
    {
    }   
    
    //---------------------------------------------------------------------//
    // HpcDataTransferService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public HpcDataObjectUploadResponse uploadDataObject(HpcDataObjectUploadRequest uploadRequest) 
                                                       throws HpcException
    {
    	// Input validation.
    	Object source = uploadRequest.getSource();
    	if(source == null || uploadRequest.getPath() == null ||
    	   uploadRequest.getTransferType() == null ||
    	   ((source instanceof HpcFileLocation) && !isValidFileLocation((HpcFileLocation)source))) {
    	   throw new HpcException("Invalid data object upload request", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
        	
    	// Upload the data object using the appropriate data transfer proxy.
    	HpcDataTransferType dataTransferType = uploadRequest.getTransferType();
  	    return dataTransferProxies.get(dataTransferType).
  	    		                   uploadDataObject(getAuthenticatedToken(dataTransferType), 
  	    		                		            uploadRequest);
    }
    
    @Override
    public void downloadDataObject(HpcDataObjectDownloadRequest downloadRequest) 
                                  throws HpcException
    {
    	// Input validation.
    	if(!isValidFileLocation(downloadRequest.getArchiveLocation()) || 
    	   downloadRequest.getDestination() == null || 
    	   downloadRequest.getTransferType() == null) {
    	   throw new HpcException("Invalid data object download request", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
        	
    	// Download the data object using the appropriate data transfer proxy.
    	HpcDataTransferType dataTransferType = downloadRequest.getTransferType();
  	    dataTransferProxies.get(dataTransferType).
  	                        downloadDataObject(getAuthenticatedToken(dataTransferType), 
  	                        		           downloadRequest);	
    }
    
	@Override   
	public HpcDataTransferStatus getDataTransferStatus(String dataTransferRequestId) 
                                                      throws HpcException
    {	// Input Validation.
		if(dataTransferRequestId == null) {
		   throw new HpcException("Null data transfer request ID", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		//TODO: Fix API instead of defaulting to Globus
		HpcDataTransferType dataTransferType = HpcDataTransferType.GLOBUS;
		return dataTransferProxies.get(dataTransferType).
				                   getDataTransferStatus(getAuthenticatedToken(dataTransferType), 
           	                                             dataTransferRequestId);
    }	
	
	@Override   
	public long getDataTransferSize(String dataTransferRequestId) 
                                   throws HpcException
    {	// Input Validation.
		if(dataTransferRequestId == null) {
		   throw new HpcException("Null data transfer request ID", 
	                              HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		//TODO: Fix API instead of defaulting to Globus
		HpcDataTransferType dataTransferType = HpcDataTransferType.GLOBUS;
		return dataTransferProxies.get(dataTransferType).
				                   getDataTransferSize(getAuthenticatedToken(dataTransferType), 
           	                                           dataTransferRequestId);
    }	
	
	@Override
	public boolean validateDataTransferAccount(HpcIntegratedSystemAccount dataTransferAccount)
                                              throws HpcException
    {
    	// Input validation.
    	if(!HpcDomainValidator.isValidIntegratedSystemAccount(dataTransferAccount)) {	
    	   throw new HpcException("Invalid data transfer account", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
		//TODO: Fix API instead of defaulting to Globus
		HpcDataTransferType dataTransferType = HpcDataTransferType.GLOBUS;
		return (dataTransferProxies.get(dataTransferType).authenticate(dataTransferAccount) != null);
	}  
	
	@Override
	public HpcPathAttributes getPathAttributes(HpcFileLocation fileLocation,
			                                   boolean getSize) 
                                              throws HpcException
    {
    	// Input validation.
    	if(!HpcDomainValidator.isValidFileLocation(fileLocation)) {	
    	   throw new HpcException("Invalid file location", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}	
    	
		//TODO: Fix API instead of defaulting to Globus
		HpcDataTransferType dataTransferType = HpcDataTransferType.GLOBUS;
    	return dataTransferProxies.get(dataTransferType).
    			                   getPathAttributes(getAuthenticatedToken(dataTransferType), 
    			                                     fileLocation, getSize);
    }
	
	public void setPermission(HpcFileLocation fileLocation,
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
    	
		//TODO: Fix API instead of defaulting to Globus
		HpcDataTransferType dataTransferType = HpcDataTransferType.GLOBUS;
		dataTransferProxies.get(dataTransferType).
		                    setPermission(getAuthenticatedToken(dataTransferType), 
                                          fileLocation, request);
    }
	
	public void setDataTransferProxies(Map<HpcDataTransferType, HpcDataTransferProxy>  dataTransferProxies)
    {
    	this.dataTransferProxies = dataTransferProxies;
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
    	
    	// TODO - Rework this when refactoring to a system account.
    	if(dataTransferType.equals(HpcDataTransferType.S_3)) {
    	   return dataTransferProxies.get(dataTransferType).authenticate(invoker.getDataTransferAccount());
    	}
    	
    	if(invoker.getDataTransferAuthenticatedToken() != null) {
    	   return invoker.getDataTransferAuthenticatedToken();
    	}
    	
    	// No authenticated token found in the request token. Authenticate the invoker.
    	if(invoker.getDataTransferAccount() == null) {
    		throw new HpcException("Unknown data transfer account",
                                   HpcRequestRejectReason.INVALID_DATA_TRANSFER_ACCOUNT);
    	}
    	
    	// Authenticate w/ data management
    	Object token = dataTransferProxies.get(dataTransferType).authenticate(invoker.getDataTransferAccount());
    	if(token == null) {
    	   throw new HpcException("Invalid data transfer account credentials",
                                  HpcRequestRejectReason.INVALID_DATA_TRANSFER_ACCOUNT);
    	}
    	
    	// Store token on the request context.
    	invoker.setDataTransferAuthenticatedToken(token);
    	return token;
    }
}
 