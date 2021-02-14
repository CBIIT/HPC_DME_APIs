/**
 * HpcDataTieringDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import gov.nih.nci.hpc.dao.HpcDataTieringDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcTieringRequestType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Tiering DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public class HpcDataTieringDAOImpl implements HpcDataTieringDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	public static final String INSERT_SQL = "insert into HPC_S3_LIFECYCLE_RULE ( "
			+ "USER_ID, REQUEST_TYPE, S3_ARCHIVE_CONFIGURATION_ID,"
			+ "FILTER_PREFIX, "
			+ "COMPLETED) values (?, ?, ?, ?, ?)";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcDataTieringDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTieringDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void insert(String userId, HpcTieringRequestType requestType, 
			String s3ArchiveConfigurationId, Calendar completed, String filterPrefix) throws HpcException {
		try {
			jdbcTemplate.update(INSERT_SQL, userId, requestType.value(),
					s3ArchiveConfigurationId, filterPrefix, completed);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to insert a lifecycle rule record: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

}
