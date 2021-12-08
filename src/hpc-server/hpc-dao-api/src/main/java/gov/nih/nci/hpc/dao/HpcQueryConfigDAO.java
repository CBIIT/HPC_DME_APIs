/**
 * HpcQueryConfigDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.List;

import gov.nih.nci.hpc.domain.model.HpcQueryConfiguration;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Query Config DAO Interface.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */

public interface HpcQueryConfigDAO 
{    
    /**
     * Store a new query configuration
     *
     * @param basePath The base path to be added.
     * @param encryptionKey The encryption key associate with the base path.
     * @throws HpcException on database error.
     */
	public void upsert(String basePath, String encryptionKey) throws HpcException;
	
	/**
	 * Get all query configurations
	 * 
	 * @return list of HpcQueryConfigurations
	 * @throws HpcException on database error.
	 */
	public List<HpcQueryConfiguration> getQueryConfigurations() throws HpcException;
	
}

 