/**
 * HpcDataSearchService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;


/**
 * <p>
 * HPC Data Search Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDataSearchService 
{  
    /**
     * Get collection paths by compound metadata query.
     *
     * @param compoundMetadataQuery The compound metadata query.
     * @param page The requested results page.
     * @return Collection path list.
     * 
     * @throws HpcException
     */
    public List<String> getCollectionPaths(HpcCompoundMetadataQuery compoundMetadataQuery, int page) 
    		                              throws HpcException;
    
    /**
     * Get the search results page size.
     *
     * @return The search results page size.
     */
    public int getSearchResultsPageSize();
}

 