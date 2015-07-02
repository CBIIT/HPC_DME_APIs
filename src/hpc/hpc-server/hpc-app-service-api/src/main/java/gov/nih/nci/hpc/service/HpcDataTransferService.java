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

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
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
     * @param dataTransferLocations The file source/destination
     * @param dataTransferAccount The account to use for the transfer.
     * @return A transfer report.
     * 
     * @throws HpcException
     */
    public HpcDataTransferReport 
              transferDataset(HpcDataTransferLocations dataTransferLocations,
    		                  HpcDataTransferAccount dataTransferAccount) 
    		                 throws HpcException;
    
    /**
     * Validate a data transfer account.
     *
     * @param transferAccount The account to use for the transfer.
     * @return True if the account is valid, or false otherwise.
     */
    public boolean isValidDataTransferAccount(
    		              HpcDataTransferAccount transferAccount)
    		              throws HpcException;
}

 