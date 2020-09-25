/**
 * HpcCatalogDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.catalog.HpcCatalogCriteria;
import gov.nih.nci.hpc.domain.catalog.HpcCatalogMetadataEntry;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Catalog DAO Interface.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */

public interface HpcCatalogDAO 
{    
    /**
     * Get catalog entries for the specified criteria. 
     *
     * @param criteria The user specified criteria.
     * @param offset Skip that many records in the returned results.
     * @param limit No more than 'limit' records will be returned.
     * @return List of meta data catalog entries.
     * @throws HpcException on database error.
     */
    public List<HpcCatalogMetadataEntry> getCatalog(HpcCatalogCriteria criteria, int offset, int limit) throws HpcException;
    
    /**
     * Get count of catalog entries for the specified criteria. 
     *
     * @param criteria The user specified criteria.
     * @return Count of meta data catalog entries.
     * @throws HpcException on database error.
     */
    public int getCatalogCount(HpcCatalogCriteria criteria) throws HpcException;
    
}

 