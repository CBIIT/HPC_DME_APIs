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
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcS3Connection()
    {
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    /**
     * Authenticate an account.
     *
     * @param dataTransferAccount A data transfer account to authenticate.
     * @param s3URL The S3 URL to connect to.
     * @return An authenticated TransferManager object, or null if authentication failed.
     */
    public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount, String s3URL)
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
     * @throws HpcException on invalid authentication token.
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

 