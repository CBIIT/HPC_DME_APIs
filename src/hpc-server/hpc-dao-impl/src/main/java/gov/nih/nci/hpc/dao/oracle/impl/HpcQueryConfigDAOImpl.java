/**
 * HpcQueryConfigDAOImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import gov.nih.nci.hpc.dao.HpcQueryConfigDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcQueryConfiguration;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Query Config DAO Implementation.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcQueryConfigDAOImpl implements HpcQueryConfigDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String UPSERT_SQL = "merge into HPC_QUERY_CONFIGURATION using dual on (BASE_PATH = ?) "
			+ "when matched then update set ENCRYPTION_KEY = ?, ENCRYPT = 1 "
			+ "when not matched then insert (BASE_PATH, ENCRYPTION_KEY, ENCRYPT ) "
			+ "values (?, ?, 1) ";

	private static final String GET_CONFIGS_SQL = "select * from HPC_QUERY_CONFIGURATION where ENCRYPT = '1'";
	
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// Encryptor.
	@Autowired
	HpcEncryptor encryptor = null;

	// Row mapper.
	private RowMapper<HpcQueryConfiguration> rowMapper = (rs, rowNum) -> {
		HpcQueryConfiguration queryConfiguration = new HpcQueryConfiguration();
		queryConfiguration.setBasePath(rs.getString("BASE_PATH"));
		queryConfiguration.setEncryptionKey(encryptor.decrypt(rs.getBytes("ENCRYPTION_KEY")));
		queryConfiguration.setEncrypt(rs.getBoolean("ENCRYPT"));

		return queryConfiguration;
	};
		
	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcQueryConfigDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcQueryConfigDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void upsert(String basePath, String encryptionKey)
			throws HpcException {
		try {
			jdbcTemplate.update(UPSERT_SQL, basePath, encryptor.encrypt(encryptionKey), basePath, encryptor.encrypt(encryptionKey));

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a system account: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcQueryConfiguration> getQueryConfigurations() throws HpcException {
		// Build the query based on provided search criteria.
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		sqlQueryBuilder.append(GET_CONFIGS_SQL);
		
		try {
			return jdbcTemplate.query(sqlQueryBuilder.toString(), rowMapper, args.toArray());

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get query configurations: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

}
