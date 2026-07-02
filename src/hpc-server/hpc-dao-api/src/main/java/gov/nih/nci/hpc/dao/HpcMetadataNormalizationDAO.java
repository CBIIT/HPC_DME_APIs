/**
 * HpcMetadataNormalizationDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.Map;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Metadata Normalization DAO Interface.
 * </p>
 */
public interface HpcMetadataNormalizationDAO {

	/**
	 * Get metadata normalization mapping.
	 *
	 * @return map of SYNONYM_NAME to CANONICAL_NAME.
	 * @throws HpcException on database error.
	 */
	public Map<String, String> getNormalizationMapping() throws HpcException;
}

