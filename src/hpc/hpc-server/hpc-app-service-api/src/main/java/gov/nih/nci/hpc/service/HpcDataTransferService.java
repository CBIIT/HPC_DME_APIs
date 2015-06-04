/**
 * HpcManagedDataService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.HpcDataset;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Data Transfer Service Interface.
 * </p>
 *
 * @author <a href="mailto:Mahidhar.Narra@nih.gov">Mahidhar Narra</a>
 * @version $Id: HpcDataTransferService.java 
 */

public interface HpcDataTransferService 
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
    public boolean transferDataset(HpcDataset dataset,String username, String password) throws HpcException;
}

 