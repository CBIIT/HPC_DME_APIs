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
     * @return The registered dataset DTO or null if not found.
     * 
     * @throws HpcException
     */
    public HpcDatasetDTO getDataset(String id) throws HpcException;
    
    /**
     * Get registered dataset by ID.
     *
     * @param creatorId The creator user id.
     * @return A collection of datasets created by this user.
     * 
     * @throws HpcException
     */
    public List<HpcDatasetDTO> getDatasets(String userId, HpcUserType) throws HpcException;
}

 