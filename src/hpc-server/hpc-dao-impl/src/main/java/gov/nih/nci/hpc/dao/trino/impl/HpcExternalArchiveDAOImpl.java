/**
 * HpcExternalArchiveDAOImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.trino.impl;

import gov.nih.nci.hpc.dao.HpcExternalArchiveDAO;
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
 * HPC External Archive DAO Implementation.
 *
 * <p>This implementation queries external archives (VAST managed archives mounted via NFS on the
 * DME server) to identify files that have not been accessed within a specified time period, for
 * auto-tiering migration to S3 Glacier Deep Archive.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcExternalArchiveDAOImpl implements HpcExternalArchiveDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String GET_FILES_NOT_ACCESSED_SQL =
			"select replace(search_path, ?, '') || name as path " +
			"from \"vast-big-catalog-bucket|vast_big_catalog_schema\".\"vast_big_catalog_table\" " +
			"where element_type = 'FILE' " +
			"and atime < current_timestamp - INTERVAL '{months}' MONTH " +
			"and search_path like ? " +
			"and name NOT LIKE '%/'";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	@Qualifier("hpcTrinoJdbcTemplate")
	private JdbcTemplate jdbcTemplate = null;

	// The logger instance.
	private static final Logger logger = LoggerFactory.getLogger(HpcExternalArchiveDAOImpl.class.getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcExternalArchiveDAOImpl() {}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcExternalArchiveDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public List<String> getFilesNotAccessed(String searchPath, Integer monthsNotAccessed) throws HpcException {
		try {
			return jdbcTemplate.queryForList(
					GET_FILES_NOT_ACCESSED_SQL.replace("{months}", monthsNotAccessed.toString()),
					String.class, searchPath, searchPath);

		} catch (DataAccessException e) {
			throw new HpcException(
					"Failed to query files not accessed in external archive [searchPath=" + searchPath +
					", monthsNotAccessed=" + monthsNotAccessed + "]: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.VAST, e);
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Verify connection to Trino DB. Called by Spring as init-method.
	 *
	 * @throws HpcException If it failed to connect to the database.
	 */
	@SuppressWarnings("unused")
	private void dbConnect() throws HpcException {
		try {
			jdbcTemplate.getDataSource().getConnection();

		} catch (Exception e) {
			throw new HpcException("Failed to connect to Trino DB. Check connection & credentials config",
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.VAST, e);
		}
	}
}
