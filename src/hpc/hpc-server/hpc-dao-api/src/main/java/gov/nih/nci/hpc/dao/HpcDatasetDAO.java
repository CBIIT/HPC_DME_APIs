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

import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
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
     * Update a dataset in the repository.
     *
     * @param dataset The dataset to be updated to the DB.
     * 
     * @throws HpcException
     */
    public void update(HpcDataset dataset) throws HpcException;
    
    /**
     * Get dataset from the repository by ID.
     *
     * @param id The dataset ID.
     * @return The dataset if found, or null otherwise.
     * 
     * @throws HpcException
     */
    public HpcDataset getDataset(String id) throws HpcException;
    
    /**
     * Get file from the repository by ID.
     *
     * @param id The file ID.
     * @return The file if found, or null otherwise.
     * 
     * @throws HpcException
     */
    public HpcFile getFile(String id) throws HpcException;
    
    /**
     * Get datasets associated with a specific user(s).
     *
     * @param nihUserIds The user id collection to match.
     * @param association The association between the dataset and the user.
     * @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcDataset> getDatasets(List<String> nihUserIds, 
    		                            HpcDatasetUserAssociation association) 
        	                           throws HpcException;
    
    /**
     * Get datasets which has 'name' contained within its name.
     *
     * @param name Get datasets which 'name' is contained in their name.
     * @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcDataset> getDatasets(String name) throws HpcException;
    
    /**
     * GET Datasets by a list of .
     *
     * @param primaryMetadata The meatada to match.
     *  @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcDataset> getDatasets(HpcFilePrimaryMetadata primaryMetadata) 
    		                           throws HpcException;
    
    /**
     * Return true if a dataset with a specific name user association exists.
     *
     * @param nihUserId The user id to match.
     * @param association The association between the dataset and the user.
     * @return true if the dataset exists.
     * 
     * @throws HpcException
     */
    public boolean exists(String name, String nihUserId, 
    		              HpcDatasetUserAssociation association) 
        	             throws HpcException;

    /**
     * Get datasets by status.
     *
     * @param transferStatus status as string.
     * 
     * @throws HpcException
     */
	public List<HpcDataset> getDatasetsByStatus(String transferStatus) throws HpcException;
}

 