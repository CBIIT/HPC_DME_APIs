/**
 * HpcDataManagementAuditDAOImpl.java
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

import gov.nih.nci.hpc.dao.HpcApiCallsAuditDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Object Deletion DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcApiCallsAuditDAOImpl implements HpcApiCallsAuditDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	public static final String INSERT_SQL = "insert into HPC_API_CALLS_AUDIT ( "
			+ "USER_ID, HTTP_REQUEST_METHOD, ENDPOINT, HTTP_RESPONSE_CODE, SERVER_ID, CREATED, COMPLETED) "
			+ "values (?, ?, ?, ?, ?, ?, ?)";

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
	private HpcApiCallsAuditDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataObjectDeletionDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void insert(String userId, String httpRequestMethod, String endpoint, String httpResponseCode,
			String serverId, Calendar created, Calendar completed) throws HpcException {
		try {
			jdbcTemplate.update(INSERT_SQL, userId, httpRequestMethod, endpoint, httpResponseCode, serverId, created,
					completed);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to insert an API calls audit record: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}
}
