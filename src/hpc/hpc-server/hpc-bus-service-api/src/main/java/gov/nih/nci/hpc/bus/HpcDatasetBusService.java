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

import java.util.List;

/**
 * <p>
 * HPC Dataset Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDatasetBusService 
{         
    /**
     * Register Dataset.
     *
     * @param datasetRegistrationDTO The dataset registration DTO.
     * @return The registered dataset ID.
     * 
     * @throws HpcException
     */
    public String registerDataset(HpcDatasetRegistrationDTO datasetRegistrationDTO) 
    		                     throws HpcException;
    
    /**
     * Get a dataset by its ID.
     *
     * @param id The dataset id.
     * @return The dataset DTO or null if not found.
     * 
     * @throws HpcException
     */
    public HpcDatasetDTO getDataset(String id) throws HpcException;
    
    /**
     * Get managed datasets associated with a specific user.
     *
     * @param nihUserId the user id.
     * @param association The association between the dataset and the user.
     * @return Collection of Dataset DTO, or null if not found.
     * 
     * @throws HpcException
     */
    public HpcDatasetCollectionDTO 
              getDatasets(String nihUserId, 
    		              HpcDatasetUserAssociation association) 
    		             throws HpcException;
}

 