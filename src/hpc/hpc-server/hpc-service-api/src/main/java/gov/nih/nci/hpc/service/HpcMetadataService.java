/**
 * HpcMetadataService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.dto.metadata.HpcMetadataDTO;

/**
 * <p>
 * HPC Metadata Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcMetadataService 
{         
    /**
     * Add metadata.
     *
     * @param metadata The metadata to add.
     */
    public void addMetadata(HpcMetadataDTO metaData);
    
    /**
     * Get Metadata.
     *
     * @param id The metadata ID.
     * @return The Metada object if found, otherwise returns null.
     */
    public HpcMetadataDTO getMetadata(String id); 
}

 