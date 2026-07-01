/**
 * HpcAutoTieringDAOImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.oracle.impl;

import gov.nih.nci.hpc.dao.HpcAutoTieringDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * HPC Auto Tiering DAO Oracle Implementation.
 *
 * <p>This implementation queries the Oracle materialized view HPC_DATA_OBJECT_LAST_ACCESS_MV
 * to identify files that have not been accessed within a specified time period, for
 * auto-tiering migration to S3 Glacier Deep Archive.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcAutoTieringDAOImpl implements HpcAutoTieringDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String GET_FILES_NOT_ACCESSED_SQL =
			"SELECT path FROM HPC_DATA_OBJECT_LAST_ACCESS_MV " +
			"WHERE path LIKE ? " +
			"AND (S3_ARCHIVE_CONFIGURATION_ID IS NULL OR S3_ARCHIVE_CONFIGURATION_ID != ?) " +
			"AND (effective_accessed_date IS NULL " +
			"OR effective_accessed_date < current_timestamp - INTERVAL '{months}' MONTH)";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	@Qualifier("hpcOracleJdbcTemplate")
	private JdbcTemplate jdbcTemplate = null;

	// The logger instance.
	private static final Logger logger = LoggerFactory.getLogger(HpcAutoTieringDAOImpl.class.getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcAutoTieringDAOImpl() {}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcAutoTieringDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public List<String> getFilesNotAccessed(String searchPath, Integer monthsNotAccessed, String s3ArchiveConfigurationId) throws HpcException {
		try {
			return jdbcTemplate.queryForList(
					GET_FILES_NOT_ACCESSED_SQL.replace("{months}", monthsNotAccessed.toString()),
					String.class, searchPath + "%", s3ArchiveConfigurationId);

		} catch (DataAccessException e) {
			throw new HpcException(
					"Failed to query files not accessed in Oracle [searchPath=" + searchPath +
					", monthsNotAccessed=" + monthsNotAccessed + "]: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}
}


