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

import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferReport;
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
     * @return A transfer report.
     * 
     * @throws HpcException
     */
    public HpcDataTransferReport 
              transferData(HpcFileLocation source, HpcFileLocation destination) 
    		              throws HpcException;

    /**
     * Get data transfer request updated status.
     *
     * @param dataTransferId The data transfer request ID.
     * @return A transfer report.
     * 
     * @throws HpcException
     */
    public HpcDataTransferReport 
              getTransferRequestStatus(String dataTransferId) 
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
}

 