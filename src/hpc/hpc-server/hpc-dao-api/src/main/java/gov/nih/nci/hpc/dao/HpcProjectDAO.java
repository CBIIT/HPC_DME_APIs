/**
 * HpcProjectDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.model.HpcProject;
import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Project DAO Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public interface HpcProjectDAO 
{         
    /**
     * Store a new project to the repository or update it if it exists.
     *
     * @param project The dataset to add to the DB.
     * 
     * @throws HpcException
     */
    public void upsert(HpcProject dataset) throws HpcException;
    
    /**
     * Get project from the repository by ID.
     *
     * @param id The project ID.
     * @return The project if found, or null otherwise.
     * 
     * @throws HpcException
     */
    public HpcProject getProject(String id) throws HpcException;
    
    /**
     * Get projects associated with a specific user.
     *
     * @param nihUserId the user id.
     * @param association The association between the project and the user.
     * @return HpcProject collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcProject> getProjects(String nihUserId, 
    		                            HpcDatasetUserAssociation association) 
        	                           throws HpcException;
 
}

 