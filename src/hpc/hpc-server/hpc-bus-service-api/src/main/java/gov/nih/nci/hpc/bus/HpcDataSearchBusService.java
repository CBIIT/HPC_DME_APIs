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

import java.util.List;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCompoundMetadataQueryDTO;
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
     * Get Collections by simple metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @param detailedResponse If set to true, return entity details (attributes + metadata).
     * @param page The requested results page.
     * @return A list of HpcCollectionDTO
     * 
     * @throws HpcException
     */
    public HpcCollectionListDTO getCollections(List<HpcMetadataQuery> metadataQueries,
    		                                   boolean detailedResponse, int page) 
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
//    
//    /**
//     * Get Collections by named query.
//     *
//     * @param queryName The query name.
//     * @param detailedResponse If set to true, return entity details (attributes + metadata).
//     * @param page The requested results page.
//     * @return A list of HpcCollectionDTO
//     * 
//     * @throws HpcException
//     */
//    public HpcCollectionListDTO getCollections(String queryName, boolean detailedResponse,
//    		                                   int page) 
//    		                                  throws HpcException;
//    
//    /**
//     * Get data objects by metadata query.
//     *
//     * @param metadataQueries The metadata queries.
//     * @param detailedResponse If set to true, return entity details (attributes + metadata).
//     * @param page The requested results page.
//     * @return A list of HpcDataObjectDTO.
//     * 
//     * @throws HpcException
//     */
//    public HpcDataObjectListDTO getDataObjects(List<HpcMetadataQuery> metadataQueries,
//    		                                   boolean detailedResponse, int page) 
//    		                                  throws HpcException;
//    /**
//     * Get data objects by compound metadata query.
//     *
//     * @param compoundMetadataQueryDTO The compound metadata query DTO.
//     * @return A list of HpcDataObjectDTO.
//     * 
//     * @throws HpcException
//     */
//    public HpcDataObjectListDTO getDataObjects(HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
//    		                                  throws HpcException;
//    
//    /**
//     * Get data objects by named query.
//     *
//     * @param queryName The query name.
//     * @param detailedResponse If set to true, return entity details (attributes + metadata).
//     * @param page The requested results page.
//     * @return A list of HpcDataObjectDTO.
//     * 
//     * @throws HpcException
//     */
//    public HpcDataObjectListDTO getDataObjects(String queryName, boolean detailedResponse,
//    		                                   int page) 
//    		                                  throws HpcException;
//    
//    /**
//     * Get a list of metadata attributes currently registered.
//     *
//     * @param level Filter the results by level. (Optional).
//     * @param levelOperator The operator to use in the level filter. (Optional).
//     * @return A list of metadata attributes
//     */
//	public HpcMetadataAttributesListDTO 
//	          getMetadataAttributes(Integer level, HpcMetadataQueryOperator levelOperator) 
//			                       throws HpcException;
//	
//    /**
//     * Save a query for a user.
//     *
//     * @param queryName The query name.
//     * @param compoundMetadataQueryDTO The compound query DTO.
//     * @throws HpcException
//     */
//    public void saveQuery(String queryName,
//    		              HpcCompoundMetadataQueryDTO compoundMetadataQueryDTO) 
//    		             throws HpcException;
//    
//    /**
//     * Delete a query.
//     *
//     * @param queryName The query name.
//     * @throws HpcException
//     */
//    public void deleteQuery(String queryName) throws HpcException;
//
//    /**
//     * Get all saved queries.
//     *
//     * @return HpcNamedCompoundMetadataQueryListDTO 
//     * @throws HpcException
//     */
//    public HpcNamedCompoundMetadataQueryListDTO getQueries() throws HpcException;

}

 