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
import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcEntityPermissionResponseListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcNamedCompoundMetadataQueryListDTO;
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
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @return A list of HpcCollectionDTO
     * 
     * @throws HpcException
     */
    public HpcCollectionListDTO getCollections(List<HpcMetadataQuery> metadataQueries,
    		                                   boolean detailedResponse) 
    		                                  throws HpcException;
    
    /**
     * Get Collections by compound metadata query.
     *
     * @param compoundMetadataQueryDTO The compound metadata query DTO.
     * @return A list of HpcCollectionDTO
     * 
     * @throws HpcException
     */
    public HpcCollectionListDTO getCollections(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
    		                                  throws HpcException;
    
    /**
     * Get Collections by named query.
     *
     * @param queryName The query name.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @return A list of HpcCollectionDTO
     * 
     * @throws HpcException
     */
    public HpcCollectionListDTO getCollections(String queryName, boolean detailedResponse) 
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
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @return A list of HpcDataObjectDTO.
     * 
     * @throws HpcException
     */
    public HpcDataObjectListDTO getDataObjects(List<HpcMetadataQuery> metadataQueries,
    		                                   boolean detailedResponse) 
    		                                  throws HpcException;
    
    /**
     * Get data objects by compound metadata query.
     *
     * @param compoundMetadataQueryDTO The compound metadata query DTO.
     * @return A list of HpcDataObjectDTO.
     * 
     * @throws HpcException
     */
    public HpcDataObjectListDTO getDataObjects(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
    		                                  throws HpcException;
    
    /**
     * Get data objects by named query.
     *
     * @param queryName The query name.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @return A list of HpcDataObjectDTO.
     * 
     * @throws HpcException
     */
    public HpcDataObjectListDTO getDataObjects(String queryName, boolean detailedResponse) 
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
	
    /**
     * Get the Data Management Model (Metadata validation rules and hierarchy definition) for a DOC.
     *
     * @param doc The DOC to get the model for.
     * @return HpcDataManagementModelDTO
     *      
     * @throws HpcException
     */
	public HpcDataManagementModelDTO getDataManagementModel(String doc) throws HpcException;
	
    /**
     * Save a query for a user.
     *
     * @param queryName The query name.
     * @param compoundMetadataQueryDTO The compound query DTO.
     * @throws HpcException
     */
    public void saveQuery(String queryName,
    		              HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
    		             throws HpcException;
    
    /**
     * Delete a query.
     *
     * @param queryName The query name.
     * @throws HpcException
     */
    public void deleteQuery(String queryName) throws HpcException;

    /**
     * Get all saved queries.
     *
     * @return HpcNamedCompoundMetadataQueryListDTO 
     * @throws HpcException
     */
    public HpcNamedCompoundMetadataQueryListDTO getQueries() throws HpcException;
}

 