/**
 * HpcUserDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC User DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcUserDAOImpl implements HpcUserDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String UPSERT_USER_SQL = "merge into HPC_USER using dual on (USER_ID = ?) "
			+ "when matched then update set FIRST_NAME = ?, LAST_NAME = ?, DOC = ?, "
			+ "DEFAULT_CONFIGURATION_ID = ?, ACTIVE = ?, CREATED = ?, LAST_UPDATED = ?, ACTIVE_UPDATED_BY = ? "
			+ "when not matched then insert (USER_ID, FIRST_NAME, LAST_NAME, DOC, DEFAULT_CONFIGURATION_ID, ACTIVE, "
			+ "CREATED, LAST_UPDATED, ACTIVE_UPDATED_BY) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String DELETE_USER_SQL = "delete from HPC_USER where USER_ID = ?";

	private static final String GET_USER_SQL = "select * from HPC_USER where USER_ID = ?";

	private static final String GET_USERS_SQL = "select * from HPC_USER where 1 = ?";

	private static final String GET_USERS_SQL_BY_ROLE = "SELECT USER_ID, FIRST_NAME, LAST_NAME, DOC, DEFAULT_CONFIGURATION_ID, "
			+ "ACTIVE, CREATED, LAST_UPDATED, ACTIVE_UPDATED_BY " + "FROM HPC_USER u, r_user_main r where "
			+ "u.USER_ID = r.user_name and r.user_type_name = ? ";

	private static final String GET_USERS_USER_ID_FILTER = " and lower(USER_ID) = lower(?) ";

	private static final String GET_USERS_USER_ID_PATTERN_FILTER = " lower(USER_ID) like lower(?) ";

	private static final String GET_USERS_FIRST_NAME_PATTERN_FILTER = " and lower(FIRST_NAME) like lower(?) ";

	private static final String GET_USERS_FIRST_NAME_PATTERN_FILTER_OR = " lower(FIRST_NAME) like lower(?) ";

	private static final String GET_USERS_LAST_NAME_PATTERN_FILTER = " and lower(LAST_NAME) like lower(?) ";

	private static final String GET_USERS_LAST_NAME_PATTERN_FILTER_OR = " lower(LAST_NAME) like lower(?) ";

	private static final String GET_USERS_DOC_FILTER = " and lower(DOC) = lower(?) ";

	private static final String GET_USERS_DEFAULT_CONFIGURATION_ID_FILTER = " and lower(DEFAULT_CONFIGURATION_ID) = lower(?) ";

	private static final String GET_USERS_ACTIVE_FILTER = " and ACTIVE = 'Y' ";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	// TODO: Remove after Oracle migration
	@Qualifier("hpcOracleJdbcTemplate")
	// TODO: END
	private JdbcTemplate jdbcTemplate = null;

	// Encryptor.
	@Autowired
	HpcEncryptor encryptor = null;

	// Row mapper.
	private RowMapper<HpcUser> rowMapper = (rs, rowNum) -> {
		HpcNciAccount nciAccount = new HpcNciAccount();
		nciAccount.setUserId(rs.getString("USER_ID"));
		nciAccount.setFirstName(rs.getString("FIRST_NAME"));
		nciAccount.setLastName(rs.getString("LAST_NAME"));
		nciAccount.setDoc(rs.getString("DOC"));
		nciAccount.setDefaultConfigurationId(rs.getString("DEFAULT_CONFIGURATION_ID"));

		HpcUser user = new HpcUser();
		Calendar created = Calendar.getInstance();
		created.setTime(rs.getDate("CREATED"));
		user.setCreated(created);

		Calendar lastUpdated = Calendar.getInstance();
		lastUpdated.setTime(rs.getDate("LAST_UPDATED"));
		user.setLastUpdated(lastUpdated);

		user.setActive(rs.getBoolean("ACTIVE"));
		user.setActiveUpdatedBy(rs.getString("ACTIVE_UPDATED_BY"));

		user.setNciAccount(nciAccount);

		return user;
	};

	// The logger instance.
	private static final Logger logger = LoggerFactory.getLogger(HpcUserDAOImpl.class.getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcUserDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcUserDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void upsertUser(HpcUser user) throws HpcException {
		try {
			jdbcTemplate.update(UPSERT_USER_SQL, user.getNciAccount().getUserId(), user.getNciAccount().getFirstName(),
					user.getNciAccount().getLastName(), user.getNciAccount().getDoc(),
					user.getNciAccount().getDefaultConfigurationId(), user.getActive(), user.getCreated(),
					user.getLastUpdated(), user.getActiveUpdatedBy(), user.getNciAccount().getUserId(),
					user.getNciAccount().getFirstName(), user.getNciAccount().getLastName(),
					user.getNciAccount().getDoc(), user.getNciAccount().getDefaultConfigurationId(), user.getActive(),
					user.getCreated(), user.getLastUpdated(), user.getActiveUpdatedBy());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a user: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void deleteUser(String userId) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_USER_SQL, userId);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to delete user " + userId + ": " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public HpcUser getUser(String nciUserId) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_USER_SQL, rowMapper, nciUserId);

		} catch (IncorrectResultSizeDataAccessException irse) {
			logger.error("Multiple users with the same ID found", irse);
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a user: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUser> getUsers(String nciUserId, String firstNamePattern, String lastNamePattern, String doc,
			String defaultConfigurationId, boolean active) throws HpcException {
		// Build the query based on provided search criteria.
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		sqlQueryBuilder.append(GET_USERS_SQL);
		args.add("1");

		if (nciUserId != null) {
			sqlQueryBuilder.append(GET_USERS_USER_ID_FILTER);
			args.add(nciUserId);
		}
		if (firstNamePattern != null) {
			sqlQueryBuilder.append(GET_USERS_FIRST_NAME_PATTERN_FILTER);
			args.add(firstNamePattern);
		}
		if (lastNamePattern != null) {
			sqlQueryBuilder.append(GET_USERS_LAST_NAME_PATTERN_FILTER);
			args.add(lastNamePattern);
		}
		if (doc != null) {
			sqlQueryBuilder.append(GET_USERS_DOC_FILTER);
			args.add(doc);
		}
		if (defaultConfigurationId != null) {
			sqlQueryBuilder.append(GET_USERS_DEFAULT_CONFIGURATION_ID_FILTER);
			args.add(defaultConfigurationId);
		}
		if (active) {
			sqlQueryBuilder.append(GET_USERS_ACTIVE_FILTER);
		}

		try {
			return jdbcTemplate.query(sqlQueryBuilder.toString(), rowMapper, args.toArray());

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get users: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUser> getUsersByRole(String role, String doc, String defaultConfigurationId, boolean active)
			throws HpcException {
		// Build the query based on provided search criteria.
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		sqlQueryBuilder.append(GET_USERS_SQL_BY_ROLE);
		args.add(role);

		if (doc != null) {
			sqlQueryBuilder.append(GET_USERS_DOC_FILTER);
			args.add(doc);
		}
		if (defaultConfigurationId != null) {
			sqlQueryBuilder.append(GET_USERS_DEFAULT_CONFIGURATION_ID_FILTER);
			args.add(defaultConfigurationId);
		}
		if (active) {
			sqlQueryBuilder.append(GET_USERS_ACTIVE_FILTER);
		}

		try {
			return jdbcTemplate.query(sqlQueryBuilder.toString(), rowMapper, args.toArray());

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get users for role = " + role + " in doc = " + doc + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcUser> queryUsers(String nciUserIdPattern, String firstNamePattern, String lastNamePattern,
			String doc, String defaultConfigurationId, boolean active) throws HpcException {
		// Build the query based on provided search criteria.
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		sqlQueryBuilder.append(GET_USERS_SQL);
		args.add("1");

		if (nciUserIdPattern != null) {
			sqlQueryBuilder.append(" and (");
			sqlQueryBuilder.append(GET_USERS_USER_ID_PATTERN_FILTER);
			args.add(nciUserIdPattern);
		}
		if (firstNamePattern != null) {
			if (nciUserIdPattern == null)
				sqlQueryBuilder.append(" and (");
			else
				sqlQueryBuilder.append(" or ");
			sqlQueryBuilder.append(GET_USERS_FIRST_NAME_PATTERN_FILTER_OR);
			args.add(firstNamePattern);
		}
		if (lastNamePattern != null) {
			if (nciUserIdPattern == null && firstNamePattern == null)
				sqlQueryBuilder.append(" and (");
			else
				sqlQueryBuilder.append(" or ");
			sqlQueryBuilder.append(GET_USERS_LAST_NAME_PATTERN_FILTER_OR);
			args.add(lastNamePattern);
		}
		if (nciUserIdPattern != null || firstNamePattern != null || lastNamePattern != null)
			sqlQueryBuilder.append(" ) ");
		if (doc != null) {
			sqlQueryBuilder.append(GET_USERS_DOC_FILTER);
			args.add(doc);
		}
		if (defaultConfigurationId != null) {
			sqlQueryBuilder.append(GET_USERS_DEFAULT_CONFIGURATION_ID_FILTER);
			args.add(defaultConfigurationId);
		}
		if (active) {
			sqlQueryBuilder.append(GET_USERS_ACTIVE_FILTER);
		}

		try {
			return jdbcTemplate.query(sqlQueryBuilder.toString(), rowMapper, args.toArray());

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get users: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}
}
