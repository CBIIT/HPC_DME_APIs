/**
 * HpcManagedDatasetService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.model.HpcManagedDataset;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Managed Dataset Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcManagedDatasetService 
{         
    /**
     * Add managed dataset.
     *
     * @param name The managed dataset name.
     * @param primaryInvestigatorId The primary investigator user ID.
     * @param creatorId The dataset creator user ID.
     * @param registratorId The dataset registrator user ID.
     * @param labBranch The lab / branch which this dataset is associated with.
     * @param description The dataset description.
     * @param comments The dataset comments.
     * @param uploadRequests List of files to upload.
     * @return The registered managed dataset ID.
     * 
     * @throws HpcException
     */
    public String add(String name, String primaryInvestigatorId,
    			      String creatorId, String registratorId,
    			      String labBranch, String description, String comments,
    			      List<HpcFileUploadRequest> uploadRequests) 
    			     throws HpcException;
    
    /**
     * Get managed dataset.
     *
     * @param id The managed dataset ID.
     * @return The managed dataset.
     * 
     * @throws HpcException
     */
    public HpcManagedDataset get(String id) throws HpcException;
}

 