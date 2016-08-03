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
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseListDTO;
import gov.nih.nci.hpc.exception.HpcException;

import java.io.File;
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
     * @return true if a new collection was registered, false if the collection already exists
     *         and its metadata got updated.
     * 
     * @throws HpcException
     */
    public boolean registerCollection(String path,
    		                          List<HpcMetadataEntry> metadataEntries) 
    		                         throws HpcException;
    
    /**
     * Get Collection.
     *
     * @param path The collection's path.
     * @return A HpcCollectionDTO
     * 
     * @throws HpcException
     */
    public HpcCollectionDTO getCollection(String path) throws HpcException;
    
    /**
     * Get Collections by metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @return A list of HpcCollectionDTO
     * 
     * @throws HpcException
     */
    public HpcCollectionListDTO getCollections(List<HpcMetadataQuery> metadataQueries) 
    		                                  throws HpcException;
    
    /**
     * Register a Data object.
     *
     * @param path The data object's path.
     * @param dataObjectRegistrationDTO A DTO contains the metadata and data transfer locations.
     * @param dataObjectFile (Optional) The data object file. 2 options are available to upload the data -
     *                         Specify a source in 'dataObjectRegistrationDTO' or provide this file. The caller
     *                         is expected to provide one and only one option.
     * @return true if a new data object was registered, false if the collection already exists
     *         and its metadata got updated.
     *         
     * @throws HpcException
     */
    public boolean registerDataObject(String path,
    		                          HpcDataObjectRegistrationDTO dataObjectRegistrationDTO,
    		                          File dataObjectFile) 
    		                         throws HpcException;
    
    /**
     * Get Data Object.
     *
     * @param path The data object's path.
     * @return A HpcDataObjectDTO
     * 
     * @throws HpcException
     */
    public HpcDataObjectDTO getDataObject(String path) throws HpcException;
    
    /**
     * Get data objects by metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @param collectionMetadataQueries (Optional) The metadata queries for the parent collections(s).
     * @return A list of HpcDataObjectDTO.
     * 
     * @throws HpcException
     */
    public HpcDataObjectListDTO getDataObjects(List<HpcMetadataQuery> metadataQueries,
    		                                   List<HpcMetadataQuery> collectionMetadataQueries) 
    		                                  throws HpcException;
    
    /**
     * Download Data Object.
     *
     * @param path The data object path.
     * @param downloadRequest The download request DTO.
     * @return HpcDataObjectDownloadResponseDTO 
     * 
     * @throws HpcException
     */
	public HpcDataObjectDownloadResponseDTO 
	          downloadDataObject(String path,
			                     HpcDataObjectDownloadRequestDTO downloadRequest)
			                    throws HpcException;
    
    /**
     * Close connection to Data Management system for the current service call.
     */
    public void closeConnection();
    
    /**
     * POST Set permissions.
     *
     * @param entityPermissionRequests Requests to set entities (Collections or Data Objects) permissions.
     * @return Responses with each request's result.
     * 
     * @throws HpcException
     */
	public HpcEntityPermissionResponseListDTO setPermissions(
			                  List<HpcEntityPermissionRequestDTO> entityPermissionRequests)
			                  throws HpcException;
}

 