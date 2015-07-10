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
     * @return Submit the data transfer request
     * 
     * @throws HpcTransferException
     */
    public HpcDataTransferReport transferDataset(HpcDataTransferLocations transferLocations, 
    		                       String username, 
    		                       String password) throws Exception;
    public HpcDataTransferReport getTransferStatus(HpcDataTransferReport hpcDataTransferReport);
    
}

 