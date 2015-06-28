/**
 * HpcDatasetDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Dataset DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDatasetDAO 
{         
    /**
     * Add dataset to the repository.
     *
     * @param dataset The dataset to add to the DB.
     * 
     * @throws HpcException
     */
    public void add(HpcDataset dataset) throws HpcException;
    
    /**
     * Get dataset from the repository by ID.
     *
     * @param id The dataset ID.
     * @return The dataset if found, or null otherwise.
     * 
     * @throws HpcException
     */
    public HpcDataset get(String id) throws HpcException;
    
    /**
     * Get datasets associated with a specific user.
     *
     * @param nihUserId the user id.
     * @param association The association between the dataset and the user.
     * @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcDataset> get(String nihUserId, 
    		                    HpcDatasetUserAssociation association) 
        	                   throws HpcException;
}

 