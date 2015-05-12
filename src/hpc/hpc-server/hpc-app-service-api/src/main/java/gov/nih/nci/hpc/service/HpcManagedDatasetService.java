/**
 * HpcManagedDatasetService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.dto.api.HpcDatasetsRegistrationInputDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Managed Dataset Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcManagedDatasetService 
{         
    /**
     * Add managed dataset.
     *
     * @param registrationInputDTO The dataset registration input DTO.
     * 
     * @throws HpcException
     */
    public void add(HpcDatasetsRegistrationInputDTO registrationInputDTO)
    		       throws HpcException;
}

 