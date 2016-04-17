/**
 * HpcS3Connection.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration.s3.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.transfer.TransferManager;

/**
 * <p>
 * HPC S3 Connection. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcS3Connection 
{ 
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// S3 connection attributes.
	private String s3URL = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param s3URL The S3 Endpoint URL.
     * 
     * @throws HpcException.
     */
    private HpcS3Connection(String s3URL)
    {
        this.s3URL = s3URL;
    }
    
	/**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
	private HpcS3Connection() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Authenticate an account.
     *
     * @param dataTransferAccount A data transfer account to authenticate.
     * @return An authenticated TransferManager object, or null if authentication failed.
     */
    public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount)
    {
    	BasicAWSCredentials cleversafeCredentials = 
    		 new BasicAWSCredentials(dataTransferAccount.getUsername(), 
    				                 dataTransferAccount.getPassword());

    	TransferManager transferManager = new TransferManager(cleversafeCredentials);
    	transferManager.getAmazonS3Client().setEndpoint(s3URL);
    	
    	return transferManager;
    }
	
    /**
     * Get S3 Transfer Manager an authenticated token.
     * 
     * @param authenticatedToken An authenticated token.
     * @return A transfer manager object.
     * 
     * @throws HpcException
     */
    public TransferManager getTransferManager(Object authenticatedToken) throws HpcException
    {
    	if(authenticatedToken == null ||
    	   !(authenticatedToken instanceof TransferManager)) {
    	   throw new HpcException("Invalid S3 authentication token",
    	    			          HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	    	
    	return (TransferManager) authenticatedToken;
	}  
}

 