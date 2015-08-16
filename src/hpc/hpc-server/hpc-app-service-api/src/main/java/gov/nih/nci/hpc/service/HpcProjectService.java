/**
 * HpcProjectService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.model.HpcProject;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Project Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id:  $
 */

public interface HpcProjectService 
{         
    /**
     * Add project.
     *
     * @param metadata The project metadata.
     * @param persist If set to true, the project will be persisted.
     * @return The registered project ID.
     * 
     * @throws HpcException
     */
    public String addProject(HpcProjectMetadata metadata,
    		                 boolean persist) throws HpcException;
    
    /**
     * Associate a dataset with a project.
     *
     * @param projectId The project ID.
     * @param datasetId The dataset ID.
     * @param persist If set to true, the project will be persisted.
     * 
     * @throws HpcException
     */
    public void associateDataset(String projectId, String datasetId,
    		                     boolean persist)
    		                    throws HpcException;
    
    /**
     * Add metadata items to a project.
     *
     * @param project The project.
     * @param metadataItems The metadata items to add.
     * @param persist If set to true, the dataset will be persisted.
     * @return The new list of metadata items
     * 
     * @throws HpcException
     */
    public List<HpcMetadataItem> 
           addMetadataItems(HpcProject project, 
        	                List<HpcMetadataItem> metadataItems,
                            boolean persist) 
                           throws HpcException;
    
    /**
     * Get project.
     *
     * @param id The project ID.
     * @return The project.
     * 
     * @throws HpcException
     */
    public HpcProject getProject(String id) throws HpcException;
    
    /**
     * Get projects associated with a specific user.
     *
     * @param userId the user id.
     * @param association The association between the project and the user.
     * @return HpcProject collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcProject> getProjects(String userId, 
    		                            HpcDatasetUserAssociation association) 
        	                           throws HpcException;
    
    /**
     * Get projects by metadata.
     *
     * @param metadata The meatada to match.
     * @return HpcProject collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcProject> getProjects(HpcProjectMetadata metadata) 
    		                           throws HpcException;
}

 