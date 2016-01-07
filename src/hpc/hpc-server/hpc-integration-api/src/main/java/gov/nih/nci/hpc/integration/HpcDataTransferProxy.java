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
     * @return A transfer report.
     * 
     * @throws HpcException
     */
    public HpcDataTransferReport transferData(HpcIntegratedSystemAccount dataTransferAccount,
    		                                  HpcFileLocation source, HpcFileLocation destination) 
    		                                 throws HpcException;

    /**
     * Get a data transfer task report.
     *
     * @param dataTransferAccount The data transfer account.
     * @param taskId The data transfer task ID.
     * 
     * @return HpcDataTransferReport the data transfer report
     * 
     * @throws HpcTransferException
     */
    public HpcDataTransferReport getTaskStatusReport(HpcIntegratedSystemAccount dataTransferAccount,
    		                                         String taskId) 
    		                                        throws Exception;
}

 