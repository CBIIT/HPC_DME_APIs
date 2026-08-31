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
package gov.nih.nci.hpc.dao.trino.impl;

import gov.nih.nci.hpc.dao.HpcAutoTieringDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcAutoTieringDataObject;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

/**
 * HPC Auto Tiering DAO Implementation.
 *
 * <p>This implementation queries external archives (VAST managed archives mounted via NFS on the
 * DME server) to identify files that have not been accessed within a specified time period, for
 * auto-tiering migration to S3 Glacier Deep Archive.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcAutoTieringDAOImpl implements HpcAutoTieringDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String GET_FILES_FOR_AUTO_TIERING_SQL =
			"select parent_path || name as path, size " +
			"from \"vast-big-catalog-bucket/vast_big_catalog_schema\".\"vast_big_catalog_table\" " +
			"where element_type = 'FILE' " +
			"and parent_path LIKE ? " +
			"and (user_tags['dme_access_time'] is null " +
			"or TRY_CAST(user_tags['dme_access_time'] AS TIMESTAMP) < current_timestamp - INTERVAL '{inactivityMonths}' MONTH) " +
			"and ctime < current_timestamp - INTERVAL '{archivedMonths}' MONTH";
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	@Qualifier("hpcTrinoJdbcTemplate")
	private JdbcTemplate jdbcTemplate = null;

	// Flag indicating whether Trino is available.
	@Value("${hpc.dao.trino.available:true}")
	private Boolean trinoAvailable = null;

	// The logger instance.
	private static final Logger logger = LoggerFactory.getLogger(HpcAutoTieringDAOImpl.class.getName());

	// HpcAutoTieringDataObject table to object mapper.
	private final RowMapper<HpcAutoTieringDataObject> rowMapper = (rs, rowNum) -> {
		HpcAutoTieringDataObject dataObject = new HpcAutoTieringDataObject();
		dataObject.setPath(rs.getString("path"));
		dataObject.setSize(rs.getLong("size"));
		// externalArchiveFileLocation is left unset.
		return dataObject;
	};

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcAutoTieringDAOImpl() {}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcExternalArchiveDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public List<HpcAutoTieringDataObject> getAutoTieringDataObjects(String searchPath, Integer inactivityMonths, Integer archivedMonths, String s3ArchiveConfigurationId) throws HpcException {
		try {
			return jdbcTemplate.query(
					GET_FILES_FOR_AUTO_TIERING_SQL.replace("{inactivityMonths}", inactivityMonths.toString())
							.replace("{archivedMonths}", archivedMonths.toString()),
					rowMapper, searchPath + "%");

		} catch (DataAccessException e) {
			throw new HpcException(
					"Failed to query files not accessed in external archive [searchPath=" + searchPath +
					", inactivityMonths=" + inactivityMonths + ", archivedMonths=" + archivedMonths + "]: " + e.getMessage(),
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
		if (Boolean.FALSE.equals(trinoAvailable)) {
			logger.info("Trino is not available. Skipping DB connection test");
			return;
		}

		try {
			jdbcTemplate.getDataSource().getConnection();

		} catch (Exception e) {
			throw new HpcException("Failed to connect to Trino DB. Check connection & credentials config",
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.VAST, e);
		}
	}
}
