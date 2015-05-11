/**
 * HpcManagedDatasetDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.dto.HpcDatasetRegistrationInputDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Managed Dataset DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcManagedDatasetDAO 
{         
    /**
     * Add managed dataset.
     *
     * @param registrationInputDTO The dataset registration input DTO.
     * 
     * @throws HpcException
     */
    public void add(HpcDatasetRegistrationInputDTO registrationInputDTO)
    		       throws HpcException;
}

 