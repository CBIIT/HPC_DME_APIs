/**
 * HpcDataTransferService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Transfer Service Interface.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$
 */

public interface HpcDataTransferService 
{         
    /**
     * Request Data (file) Transfer.
     *
     * @param dataTransferLocations The file source/destination.
     * @return A data transfer request ID.
     * 
     * @throws HpcException
     */
    public String transferData(HpcFileLocation source, HpcFileLocation destination) 
    		                  throws HpcException;

    /**
     * Get a data transfer request status.
     *
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @return HpcDataTransferStatus the data transfer request status.
     * 
     * @throws HpcException
     */
    public HpcDataTransferStatus getDataTransferStatus(String dataTransferRequestId) 
    		                                          throws HpcException;
    
    /**
     * Get the size of the data transferred of a specific request.
     *
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @return The size of the data transferred in bytes.
     * 
     * @throws HpcException
     */
    public long getDataTransferSize(String dataTransferRequestId) 
    		                       throws HpcException;
    
    /**
     * Validate a data transfer account.
     *
     * @param dataTransferAccount The account to validate.
     * @return True if the account is valid, or false otherwise.
     */
    public boolean validateDataTransferAccount(
    		               HpcIntegratedSystemAccount dataTransferAccount)
    		               throws HpcException;
    
    /**
     * Get endpoint/path attributes .
     *
     * @param fileLocation The endpoint/path to get attributes for.
     * @param getSize If set to true, the file/directory size will be returned. 
     * @return HpcPathAttributes 
     * 
     * @throws HpcException
     */
    public HpcPathAttributes getPathAttributes(HpcFileLocation fileLocation,
    		                                   boolean getSize) 
    		                                  throws HpcException;
}

 