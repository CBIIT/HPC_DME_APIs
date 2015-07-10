/**
 * HpcDatasetBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetCollectionDTO;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Transfer Status Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Mahidhar Narra</a>
 * @version $Id: HpcTransferStatusBusService.java 291 2015-07-05 14:05:20Z narram $
 */

public interface HpcTransferStatusBusService 
{         
    /**
     * Retrive/Save TransferStatus.
     *
     * @param datasetRegistrationDTO The dataset registration DTO.
     * @return The registered dataset ID.
     * 
     * @throws HpcException
     */
    public String retriveSaveTransferStatus(String id) 
    		                     throws HpcException;
    

}

 