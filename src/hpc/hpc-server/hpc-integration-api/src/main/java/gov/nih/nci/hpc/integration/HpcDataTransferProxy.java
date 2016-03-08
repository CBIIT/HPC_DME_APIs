/**
 * HpcDataTransferProxy.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Transfer Proxy Interface.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$ 
 */

public interface HpcDataTransferProxy 
{    
    /**
     * Authenticate the invoker w/ the data transfer system.
     *
     * @param dataTransferAccount The Data Transfer account to authenticate.
     * @return An authenticated token, to be used in subsequent calls to data transfer.
     *         It returns null if the account is not authenticated.
     * 
     * @throws HpcException
     */
    public Object authenticate(HpcIntegratedSystemAccount dataTransferAccount) 
    		                  throws HpcException;
    
    /**
     * Transfer a data file.
     *
     * @param authenticatedToken An authenticated token.
     * @param source The transfer source.
     * @param destination The transfer destination.
     * 
     * @return A data transfer request ID.
     * 
     * @throws HpcException
     */
    public String transferData(Object authenticatedToken,
    		                   HpcFileLocation source, HpcFileLocation destination) 
    		                  throws HpcException;

    /**
     * Get a data transfer request status.
     *
     * @param authenticatedToken An authenticated token.
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @return HpcDataTransferStatus the data transfer request status.
     * 
     * @throws HpcException
     */
    public HpcDataTransferStatus getDataTransferStatus(Object authenticatedToken,
    		                                           String dataTransferRequestId) 
    		                                          throws HpcException;
    
    /**
     * Get the size of the data transferred of a specific request.
     *
     * @param authenticatedToken An authenticated token.
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @return The size of the data transferred in bytes.
     * 
     * @throws HpcException
     */
    public long getDataTransferSize(Object authenticatedToken,
    		                        String dataTransferRequestId) 
    		                       throws HpcException;
    
    /**
     * Get attributes of a file/directory.
     *
     * @param authenticatedToken An authenticated token.
     * @param fileLocation The endpoint/path to check.
     * @param getSize If set to true, the file/directory size will be returned. 
     * @return HpcDataTransferPathAttributes The path attributes.
     * 
     * @throws HpcException
     */
    public HpcPathAttributes getPathAttributes(Object authenticatedToken, 
    		                                   HpcFileLocation fileLocation,
    		                                   boolean getSize) 
    		                                  throws HpcException;
}

 