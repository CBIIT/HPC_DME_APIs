/**
 * HpcMetadataNormalizationDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import gov.nih.nci.hpc.dao.HpcMetadataNormalizationDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * Metadata normalization DAO implementation.
 */
public class HpcMetadataNormalizationDAOImpl implements HpcMetadataNormalizationDAO {

	private static final String GET_NORMALIZATION_MAPPING_SQL = "select SYNONYM_NAME, CANONICAL_NAME "
			+ "from HPC_METADATA_NORMALIZATION";

	@Autowired
	@Qualifier("hpcOracleJdbcTemplate")
	private JdbcTemplate jdbcTemplate = null;

	/** Default constructor for Spring dependency injection. */
	private HpcMetadataNormalizationDAOImpl() {
	}

	@Override
	public Map<String, String> getNormalizationMapping() throws HpcException {
		try {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(GET_NORMALIZATION_MAPPING_SQL);
			Map<String, String> normalizedMapping = new TreeMap<>();
			for (Map<String, Object> row : rows) {
				String synonymName = (String) row.get("SYNONYM_NAME");
				String canonicalName = (String) row.get("CANONICAL_NAME");
				if (!StringUtils.isBlank(synonymName) && !StringUtils.isBlank(canonicalName)) {
					normalizedMapping.put(synonymName, canonicalName);
				}
			}
			return new LinkedHashMap<>(normalizedMapping);
		} catch (DataAccessException e) {
			throw new HpcException("Failed to get metadata normalization mapping: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}
}

