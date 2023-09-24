/**
 * HpcBulkUpdateAuditDAOImpl.java
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

import com.google.gson.Gson;

import gov.nih.nci.hpc.dao.HpcBulkUpdateAuditDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Bulk Update Audit DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public class HpcBulkUpdateAuditDAOImpl implements HpcBulkUpdateAuditDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	public static final String INSERT_SQL = "insert into HPC_BULK_UPDATE_AUDIT ( "
			+ "USER_ID, QUERY, QUERY_TYPE, METADATA_NAME, METADATA_VALUE, CREATED) "
			+ "values (?, ?, ?, ?, ?, ?)";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	private Gson gson = new Gson();

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcBulkUpdateAuditDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcBulkUpdateAuditDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void insert(String userId, HpcCompoundMetadataQuery query, HpcCompoundMetadataQueryType queryType,
			String metadataName, String metadataValue, Calendar created) throws HpcException {
		try {
			jdbcTemplate.update(INSERT_SQL, userId, gson.toJson(query), queryType.toString(), metadataName, metadataValue, created);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to insert an bulk update audit record: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}
}
