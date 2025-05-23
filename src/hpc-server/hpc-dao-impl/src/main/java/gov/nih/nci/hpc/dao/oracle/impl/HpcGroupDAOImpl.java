/**
 * HpcGroupDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.oracle.impl;

import static gov.nih.nci.hpc.util.HpcUtil.decodeGroupName;
import static gov.nih.nci.hpc.util.HpcUtil.encodeGroupName;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import gov.nih.nci.hpc.dao.HpcGroupDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcGroup;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Group DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcGroupDAOImpl implements HpcGroupDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String UPSERT_GROUP_SQL = "merge into HPC_GROUP using dual on (GROUP_NAME = ?) "
			+ "when matched then update set DOC = ?, ACTIVE = ?, CREATED = ?, LAST_UPDATED = ?, ACTIVE_UPDATED_BY = ? "
			+ "when not matched then insert (GROUP_NAME, DOC, ACTIVE, CREATED, LAST_UPDATED, ACTIVE_UPDATED_BY) "
			+ "values (?, ?, ?, ?, ?, ?) ";

	private static final String UPDATE_GROUP_SQL = "update HPC_GROUP set "
			+ "ACTIVE = ?, LAST_UPDATED = ?, ACTIVE_UPDATED_BY = ? where GROUP_NAME = ?";

	private static final String DELETE_GROUP_SQL = "delete from HPC_GROUP where GROUP_NAME = ?";

	private static final String GET_GROUP_SQL = "select * from HPC_GROUP where GROUP_NAME = ?";

	//private static final String GET_GROUPS_SQL = "select user_name from r_user_main where "
	//		+ "user_type_name = 'rodsgroup' and user_name <> 'rodsadmin'";
	private static final String GET_GROUPS_SQL = "select group_name from HPC_GROUP where active = '1'";

	// Get all groups to which the given user belongs
	private static final String GET_USER_GROUPS_SQL = "select m.user_name from r_user_main m, r_user_group g, r_user_main u "
			+ "where m.user_type_name = 'rodsgroup' and " + "m.user_id = g.group_user_id and "
			+ "g.user_id = u.user_id and " + "u.user_name = ?";

	private static final String GET_GROUPS_DOC_FILTER = "and DOC = ?";

	//private static final String GET_GROUPS_GROUP_NAME_PATTERN_FILTER = " and lower(user_name) like lower(?) ";
	private static final String GET_GROUPS_GROUP_NAME_PATTERN_FILTER = " and lower(group_name) like lower(?) ";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// Row mapper.
	private SingleColumnRowMapper<String> rowMapper = new SingleColumnRowMapper<>();
	private RowMapper<HpcGroup> groupRowMapper = (rs, rowNum) -> {
		HpcGroup group = new HpcGroup();
		group.setName(rs.getString("GROUP_NAME"));
		group.setDoc(rs.getString("DOC"));
		Calendar created = Calendar.getInstance();
		created.setTime(rs.getDate("CREATED"));
		group.setCreated(created);

		Calendar lastUpdated = Calendar.getInstance();
		lastUpdated.setTime(rs.getDate("LAST_UPDATED"));
		group.setLastUpdated(lastUpdated);

		group.setActive(rs.getBoolean("ACTIVE"));
		group.setActiveUpdatedBy(rs.getString("ACTIVE_UPDATED_BY"));

		return group;
	};

	// The logger instance.
	private static final Logger logger = LoggerFactory.getLogger(HpcGroupDAOImpl.class.getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcGroupDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcGroupDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void upsertGroup(HpcGroup group) throws HpcException {
		try {
			jdbcTemplate.update(UPSERT_GROUP_SQL, group.getName(), group.getDoc(), group.getActive(),
					group.getCreated(), group.getLastUpdated(), group.getActiveUpdatedBy(), group.getName(),
					group.getDoc(), group.getActive(), group.getCreated(), group.getLastUpdated(),
					group.getActiveUpdatedBy());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to upsert a user: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void updateGroup(HpcGroup group) throws HpcException {
		try {
			jdbcTemplate.update(UPDATE_GROUP_SQL, group.getActive(), group.getLastUpdated(), group.getActiveUpdatedBy(),
					group.getName());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to update a group: " + group.getName(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void deleteGroup(String name) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_GROUP_SQL, name);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to delete group " + name + ": " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public HpcGroup getGroup(String name) throws HpcException {
		try {
			return jdbcTemplate.queryForObject(GET_GROUP_SQL, groupRowMapper, name);
		} catch (IncorrectResultSizeDataAccessException e) {
			logger.error("Multiple groups with the same name found", e);
			return null;
		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a group: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}


	@Override
	public List<String> getGroups(String doc, String groupPattern) throws HpcException {
		// Build the query based on provided search criteria.
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		sqlQueryBuilder.append(GET_GROUPS_SQL);

		if(doc != null) {
			sqlQueryBuilder.append(GET_GROUPS_DOC_FILTER);
			args.add(doc);
		}

		if (groupPattern != null) {
			sqlQueryBuilder.append(GET_GROUPS_GROUP_NAME_PATTERN_FILTER);
			args.add(encodeGroupName(groupPattern));
		}

		try {
			return decodeGroupNames(jdbcTemplate.query(sqlQueryBuilder.toString(), rowMapper, args.toArray()));

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get groups: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<String> getUserGroups(String userId) throws HpcException {
		// Build the query based on provided search criteria.
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		sqlQueryBuilder.append(GET_USER_GROUPS_SQL);
		args.add(userId);

		try {
			return decodeGroupNames(
					jdbcTemplate.queryForList(sqlQueryBuilder.toString(), String.class, args.toArray()));

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get groups for user: " + userId + ": " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * decode a list of group names.
	 *
	 * @param groupNames A list of group names.
	 * @return A list of decoded group names.
	 */
	private List<String> decodeGroupNames(List<String> groupNames) {
		List<String> decodedGroupNames = new ArrayList<>();
		groupNames.forEach(groupName -> decodedGroupNames.add(decodeGroupName(groupName)));

		return decodedGroupNames;
	}
}
