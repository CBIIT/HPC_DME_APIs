/**
 * HpcDatasetService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.model.HpcDataset;
import gov.nih.nci.hpc.domain.dataset.HpcFile;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Dataset Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDatasetService 
{         
    /**
     * Add dataset.
     *
     * @param name The dataset name.
     * @param description The dataset description.
     * @param comments The dataset comments.
     * @return The registered dataset.
     * 
     * @throws HpcException
     */
    public HpcDataset addDataset(String name, String description, String comments) 
    			                throws HpcException;
    
    /**
     * Add files to a dataset.
     *
     * @param dataset The dataset.
     * @param uploadRequests List of files to upload.
     * 
     * @throws HpcException
     */
    public void addFiles(HpcDataset dataset,
    			         List<HpcFileUploadRequest> uploadRequests) 
    			        throws HpcException;
    
    /**
     * Get dataset.
     *
     * @param id The dataset ID.
     * @return The dataset.
     * 
     * @throws HpcException
     */
    public HpcDataset getDataset(String id) throws HpcException;
    
    /**
     * Get file.
     *
     * @param id The file ID.
     * @return The file.
     * 
     * @throws HpcException
     */
    public HpcFile getFile(String id) throws HpcException;
    
    /**
     * Get datasets associated with a specific user(s).
     *
     * @param userIds The list of user ids to match.
     * @param association The association between the dataset and the user.
     * @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcDataset> getDatasets(List<String> userIds, 
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
     * Get datasets by Project Id
     *
     * @param projectId Get datasets by Project Id
     * @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcDataset> getDatasetsByProjectId(String projectId) throws HpcException;
    
    /**
     * GET Datasets by primary metadata.
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
     * GET Datasets by transfer status.
     *
     * @param transferStatus status as string.
     *  @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
	public List<HpcDataset> getDatasetsByStatus(String transferStatus) throws HpcException;
}

 