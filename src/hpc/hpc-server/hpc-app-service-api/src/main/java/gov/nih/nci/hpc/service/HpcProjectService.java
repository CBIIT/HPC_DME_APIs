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

import gov.nih.nci.hpc.domain.model.HpcProject;
import gov.nih.nci.hpc.domain.metadata.HpcProjectMetadata;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
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
     * @param name The project name.
     * @param description The project description.
     * @param comments The project comments.
     * @param uploadRequests List of files to upload.
     * @return The registered project ID.
     * 
     * @throws HpcException
     */
    public String add(HpcProjectMetadata metadata,
    			      List<String> datasetIds) 
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
    
}

 