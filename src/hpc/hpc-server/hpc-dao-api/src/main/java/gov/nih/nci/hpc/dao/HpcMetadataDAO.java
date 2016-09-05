/**
 * HpcMetadataDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Metadata DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcMetadataDAO 
{    
    /**
     * Get collection IDs by metadata query.
     *
     * @param metadataQueries The metadata entries to query for.
     * @return List of collectiont IDs.
     * 
     * @throws HpcException
     */
    public List<Integer> getCollectionIds(List<HpcMetadataQuery> metadataQueries) 
    		                             throws HpcException;
    
    /**
     * Get data object IDs by metadata query.
     *
     * @param metadataQueries The metadata entries to query for.
     * @return List of data object IDs.
     * 
     * @throws HpcException
     */
    public List<Integer> getDataObjectIds(List<HpcMetadataQuery> metadataQueries) 
    		                             throws HpcException;
}

 