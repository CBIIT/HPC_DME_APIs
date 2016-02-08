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

import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferReport;
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
     * Transfer a data file.
     *
     * @param dataTransferAccount The data transfer account.
     * @param dataTransferLocations The file source/destination.
     * 
     * @return A data transfer request ID.
     * 
     * @throws HpcException
     */
    public String transferData(HpcIntegratedSystemAccount dataTransferAccount,
    		                   HpcFileLocation source, HpcFileLocation destination) 
    		                  throws HpcException;

    /**
     * Get a data transfer request status.
     *
     * @param dataTransferAccount The data transfer account.
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @return HpcDataTransferStatus the data transfer request status.
     * 
     * @throws HpcException
     */
    public HpcDataTransferStatus getDataTransferStatus(HpcIntegratedSystemAccount dataTransferAccount,
    		                                           String dataTransferRequestId) 
    		                                          throws Exception;
    
    /**
     * Get a data transfer report.
     *
    * @param dataTransferAccount The data transfer account.
     * @param dataTransferRequestId The data transfer request ID.
     * 
     * @return HpcDataTransferReport the data transfer report for the request.
     * 
     * @throws HpcException
     */
    public HpcDataTransferReport getDataTransferReport(HpcIntegratedSystemAccount dataTransferAccount,
    		                                           String dataTransferRequestId) 
    		                                          throws Exception;
}

 