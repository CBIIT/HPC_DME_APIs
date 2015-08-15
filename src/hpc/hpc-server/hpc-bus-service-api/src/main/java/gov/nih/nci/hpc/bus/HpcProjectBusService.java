/**
 * HpcProjectBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.domain.dataset.HpcDatasetUserAssociation;
import gov.nih.nci.hpc.dto.project.HpcProjectAddMetadataItemsDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectCollectionDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectDTO;
import gov.nih.nci.hpc.dto.project.HpcProjectRegistrationDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Project Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public interface HpcProjectBusService 
{         
    /**
     * Register a Project.
     *
     * @param projectRegistrationDTO The project  DTO.
     * @return The registered project ID.
     * 
     * @throws HpcException
     */
    public String registerProject(HpcProjectRegistrationDTO projectRegistrationDTO) 
    		                     throws HpcException;
    
    /**
     * Add metadata items to a registered project.
     *
     * @param addMetadataItemsDTO The add-metadata-items request DTO.
     * 
     * @throws HpcException
     */
    public void addMetadataItems(HpcProjectAddMetadataItemsDTO addMetadataItemsDTO) 
    		                    throws HpcException;
    
    /**
     * Get a Project by its ID.
     *
     * @param id The project id.
     * @return The project DTO or null if not found.
     * 
     * @throws HpcException
     */
    public HpcProjectDTO getProject(String id) throws HpcException;
    
    /**
     * Get datasets associated with a specific user.
     *
     * @param nihUserId the user id.
     * @param association The association between the project and the user.
     * @return Collection of Project DTO, or null if not found.
     * 
     * @throws HpcException
     */
    public HpcProjectCollectionDTO 
              getProjects(String nihUserId, 
    		              HpcDatasetUserAssociation association) 
    		             throws HpcException;
    
}

 