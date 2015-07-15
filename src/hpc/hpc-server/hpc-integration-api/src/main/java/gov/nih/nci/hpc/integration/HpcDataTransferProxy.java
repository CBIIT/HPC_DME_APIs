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

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;

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
     * transfer data.
     *
     * @param type The managed data type.
     * @param datasets The datasets to start manage.
     * @return HpcDataTransferReport the data transfer report
     * 
     * @throws HpcTransferException
     */
    public HpcDataTransferReport transferDataset(HpcDataTransferLocations transferLocations, 
    		                       String username, 
    		                       String password,
    		                       String nihUsername) throws Exception;

    /**
     * Retrive task status.
     *
     * @param taskId taskid to retrive status.
     * @param dataTransferAccount 
     * @return HpcDataTransferReport the data transfer report
     * 
     * @throws HpcTransferException
     */
    public HpcDataTransferReport getTaskStatusReport(String taskId, HpcDataTransferAccount dataTransferAccount) throws Exception;
    
}

 