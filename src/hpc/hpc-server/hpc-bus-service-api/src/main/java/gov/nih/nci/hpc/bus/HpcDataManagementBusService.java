/**
 * HpcDataManagementBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionsDTO;
import gov.nih.nci.hpc.dto.dataset.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Data Management Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDataManagementBusService 
{         
    /**
     * Register a Collection.
     *
     * @param path The collection's path.
     * @param metadataEntries A list of metadata entries to attach to the collection.
     * 
     * @throws HpcException
     */
    public void registerCollection(String path,
    		                       List<HpcMetadataEntry> metadataEntries) 
    		                      throws HpcException;
    
    /**
     * Get Collections by metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @return A list of HpcCollectionDTO
     * 
     * @throws HpcException
     */
    public HpcCollectionsDTO getCollections(List<HpcMetadataQuery> metadataQueries) 
    		                               throws HpcException;
    
    /**
     * Register a Data object.
     *
     * @param path The data object's path.
     * @param dataObjectRegistrationDTO A DTO contains the metadata and data transfer locations.
     * 
     * @throws HpcException
     */
    public void registerDataObject(String path,
    		                       HpcDataObjectRegistrationDTO dataObjectRegistrationDTO) 
    		                      throws HpcException;
}

 