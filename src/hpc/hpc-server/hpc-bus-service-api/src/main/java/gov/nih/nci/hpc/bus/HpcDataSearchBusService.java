/**
 * HpcDataSearchBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcMetadataAttributesListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcNamedCompoundMetadataQueryDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcNamedCompoundMetadataQueryListDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Search Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDataSearchBusService 
{  
    /**
     * Get Collections by compound metadata query.
     *
     * @param compoundMetadataQueryDTO The compound metadata query DTO.
     * @return A list of Collection DTO.
     * @throws HpcException on service failure.
     */
    public HpcCollectionListDTO getCollections(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
    		                                  throws HpcException;
    
    /**
     * Get Collections by named query.
     *
     * @param queryName The query name.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @param totalCount If set to true, return the total count of collections matching the query
     *                   regardless of the limit on returned entities.
     * @return A list of Collection DTO.
     * @throws HpcException on service failure.
     */
    public HpcCollectionListDTO getCollections(String queryName, boolean detailedResponse,
    		                                   int page, boolean totalCount) 
    		                                  throws HpcException;
    
    /**
     * Get data objects by compound metadata query.
     *
     * @param compoundMetadataQueryDTO The compound metadata query DTO.
     * @return A list of Data Object DTO.
     * @throws HpcException on service failure.
     */
    public HpcDataObjectListDTO getDataObjects(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
    		                                  throws HpcException;
    
    /**
     * Get data objects by named query.
     *
     * @param queryName The query name.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @param totalCount If set to true, return the total count of collections matching the query
     *                   regardless of the limit on returned entities.
     * @return A list of Data Object DTO.
     * @throws HpcException on service failure.
     */
    public HpcDataObjectListDTO getDataObjects(String queryName, boolean detailedResponse,
    		                                   int page, boolean totalCount) 
    		                                  throws HpcException;

    /**
     * Add a named query for a user.
     *
     * @param queryName The query name.
     * @param compoundMetadataQueryDTO The compound query DTO.
     * @throws HpcException on service failure.
     */
    public void addQuery(String queryName,
    		              HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
    		             throws HpcException;
    
    /**
     * Update a named query for a user.
     *
     * @param queryName The query name.
     * @param compoundMetadataQueryDTO The compound query DTO.
     * @throws HpcException on service failure.
     */
    public void updateQuery(String queryName,
    		                HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
    		               throws HpcException;
    
    /**
     * Delete a query.
     *
     * @param queryName The query name.
     * @throws HpcException on service failure.
     */
    public void deleteQuery(String queryName) throws HpcException;

    /**
     * Get a named query by name.
     *
     * @param queryName The query name.
     * @return The compound query DTO.
     * @throws HpcException on service failure.
     */
    public HpcNamedCompoundMetadataQueryDTO getQuery(String queryName) throws HpcException;
    
    /**
     * Get all saved queries.
     *
     * @return A list of compound queries DTO.
     * @throws HpcException on service failure.
     */
    public HpcNamedCompoundMetadataQueryListDTO getQueries() throws HpcException;
    
  /**
  * Get a list of metadata attributes currently registered.
  *
  * @param levelLabel Filter the results by level label. (Optional).
  * @return A list of metadata attributes
  * @throws HpcException on service failure.
  */
	public HpcMetadataAttributesListDTO getMetadataAttributes(String levelLabel) 
			                                                 throws HpcException;
}

 