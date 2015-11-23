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

import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferRequest;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Transfer Service Interface.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 * @version $Id: HpcDataTransferService.java 275 2015-07-02 22:42:45Z narram $
 */

public interface HpcTransferStatusService 
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
    public HpcDataTransferRequest 
              addUpdateStatus(HpcDataTransferRequest hpcDataTransferRequest) 
    		                 throws HpcException;
    
}

 