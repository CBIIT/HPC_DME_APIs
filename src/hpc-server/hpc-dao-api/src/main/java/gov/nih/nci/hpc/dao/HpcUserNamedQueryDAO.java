/**
 * HpcUserNamedQueryDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryFrequency;
import gov.nih.nci.hpc.domain.metadata.HpcNamedCompoundMetadataQuery;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC User Named-Query DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcUserNamedQueryDAO 
{    
    /**
     * Upsert a query for a user.
     *
     * @param nciUserId The user ID to save the query for.
     * @param namedCompoundMetadataQuery The named compound metadata query.
     * @throws HpcException on database error.
     */
    public void upsertQuery(String nciUserId,
    		                HpcNamedCompoundMetadataQuery namedCompoundMetadataQuery) 
    		               throws HpcException;
    
    /**
     * Delete a query for a user.
     *
     * @param nciUserId The user ID to delete the query for.
     * @param queryName The query name.
     * @throws HpcException on database error.
     */
    public void deleteQuery(String nciUserId, String queryName) 
    		               throws HpcException;

    /**
     * Get all saved queries for a user.
     *
     * @param nciUserId The registered user ID.
     * @return A list of queries saved by the user.
     * @throws HpcException on database error.
     */
    public List<HpcNamedCompoundMetadataQuery> getQueries(String nciUserId) 
    		                                             throws HpcException;
    
    /**
     * Get a saved query by name for a user.
     *
     * @param nciUserId The registered user ID.
     * @param queryName The query name.
     * @return The requested named query.
     * @throws HpcException on database error.
     */
    public HpcNamedCompoundMetadataQuery getQuery(String nciUserId, String queryName) 
    		                                     throws HpcException;
    /**
     * Get all saved queries with the scheduled frequency.
     *
     * @param HpcCompoundMetadataQueryFrequency The scheduled frequency.
     * @return A list of queries saved by the user with this frequency.
     * @throws HpcException on database error.
     */
    public List<HpcNamedCompoundMetadataQuery> getQueriesByFrequency(HpcCompoundMetadataQueryFrequency frequency)
			throws HpcException;
}

 