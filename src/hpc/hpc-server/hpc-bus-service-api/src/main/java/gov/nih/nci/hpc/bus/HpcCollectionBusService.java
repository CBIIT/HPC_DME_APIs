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

import gov.nih.nci.hpc.dto.collection.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Collection Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcCollectionBusService 
{         
    /**
     * Register a Collection.
     *
     * @param path The collection's path.
     * @param collectionRegistrationDTO The collection registration DTO.
     * @return The registered collection ID.
     * 
     * @throws HpcException
     */
    public String registerCollection(String path,
    		                         HpcCollectionRegistrationDTO collectionRegistrationDTO) 
    		                        throws HpcException;
}

 