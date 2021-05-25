/**
 * HpcCatalogDAOImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.dao.HpcReviewDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationReview;
import gov.nih.nci.hpc.domain.review.HpcReviewEntry;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Review DAO Implementation.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcReviewDAOImpl implements HpcReviewDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String GET_REVIEW_CURATOR_EQUAL_SQL = " data_curator = ?";

	private static final String GET_REVIEW_STATUS_EQUAL_SQL = " project_status = ?";

	private static final String GET_REVIEW_END_SQL = " order by project_status, last_reviewed desc";

	private static final String GET_REVIEW_SQL = "select id, path, project_title, project_description, "
			+ "project_start_date, data_owner, data_curator, data_curator_name, project_status, publications, deposition, sunset_date, last_reviewed "
			+ ",review_sent ,reminder_sent " + "from r_review_meta_main ";

	private static final String GET_REVIEW_COUNT_SQL = "select count(id) from r_review_meta_main ";

	private static final String INSERT_REVIEW_NOTIFICATION_SQL = "insert into HPC_NOTIFICATION_REVIEW ( "
			+ "USER_ID, EVENT_TYPE, DELIVERED) values (?, ?, ?)";
	
	private static final String GET_CURATORS_FOR_REVIEW_SQL = "select distinct review.data_curator from R_REVIEW_META_MAIN review " 
			+ "left outer join HPC_NOTIFICATION_REVIEW notification "
			+ "on review.data_curator=notification.USER_ID "
			+ "where (review.project_status='Active' or review.project_status is null) "
			+ "and (notification.EVENT_TYPE is null or notification.EVENT_TYPE='REVIEW_SENT') "
			+ "group by review.data_curator "
			+ "having trunc(max(notification.DELIVERED)) < add_months(sysdate,-12) or max(notification.DELIVERED) is null";
	
	private static final String GET_CURATORS_FOR_REMINDER_SQL = "select distinct review.data_curator "
			+ "from R_REVIEW_META_MAIN review left outer join HPC_NOTIFICATION_REVIEW notification "
			+ "on review.data_curator=notification.USER_ID "
			+ "where (review.project_status='Active' or review.project_status is null) "
			+ "and notification.EVENT_TYPE='REVIEW_SENT' "
			+ "group by review.data_curator, last_reviewed "
			+ "having last_reviewed is null or max(notification.DELIVERED) > to_date(last_reviewed, 'YYYY-MM-DD')";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// Row mappers.
	private SingleColumnRowMapper<String> curatorRowMapper = new SingleColumnRowMapper<>();
		
	private RowMapper<HpcReviewEntry> reviewEntryRowMapper = (rs, rowNum) -> {
		HpcReviewEntry reviewEntry = new HpcReviewEntry();
		reviewEntry.setId(rs.getInt(1));
		reviewEntry.setPath(rs.getString(2));
		reviewEntry.setProjectTitle(rs.getString(3));
		reviewEntry.setProjectDescription(rs.getString(4));
		reviewEntry.setProjectStartDate(rs.getString(5));
		reviewEntry.setDataOwner(rs.getString(6));
		reviewEntry.setDataCurator(rs.getString(7));
		reviewEntry.setDataCuratorName(rs.getString(8));
		reviewEntry.setProjectStatus(rs.getString(9));
		reviewEntry.setPublications(rs.getString(10));
		reviewEntry.setDeposition(rs.getString(11));
		reviewEntry.setSunsetDate(rs.getString(12));
		reviewEntry.setLastReviewed(rs.getString(13));
		reviewEntry.setReviewSent(rs.getString(14));
		reviewEntry.setReminderSent(rs.getString(15));

		return reviewEntry;
	};

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcReviewDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcCatalogDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public List<HpcReviewEntry> getReview(String projectStatus, String dataCurator) throws HpcException {
		// Build the query based on provided search criteria.
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		sqlQueryBuilder.append(GET_REVIEW_SQL);

		if (!StringUtils.isEmpty(projectStatus)) {
			sqlQueryBuilder.append(" where ");
			sqlQueryBuilder.append(GET_REVIEW_STATUS_EQUAL_SQL);
			args.add(projectStatus);
		}
		if (!StringUtils.isEmpty(dataCurator)) {
			if (StringUtils.isEmpty(projectStatus))
				sqlQueryBuilder.append(" where ");
			else
				sqlQueryBuilder.append(" and ");
			sqlQueryBuilder.append(GET_REVIEW_CURATOR_EQUAL_SQL);
			args.add(dataCurator);
		}

		sqlQueryBuilder.append(GET_REVIEW_END_SQL);

		try {
			return jdbcTemplate.query(sqlQueryBuilder.toString(), reviewEntryRowMapper, args.toArray());

		} catch (IncorrectResultSizeDataAccessException irse) {
			return Collections.emptyList();

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get projects for review: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public int getReviewCount(String projectStatus, String dataCurator) throws HpcException {
		// Build the query based on provided search criteria.
		StringBuilder sqlQueryBuilder = new StringBuilder();
		List<Object> args = new ArrayList<>();

		sqlQueryBuilder.append(GET_REVIEW_COUNT_SQL);

		sqlQueryBuilder.append(GET_REVIEW_SQL);

		if (!StringUtils.isEmpty(projectStatus)) {
			sqlQueryBuilder.append(" where ");
			sqlQueryBuilder.append(GET_REVIEW_STATUS_EQUAL_SQL);
			args.add(projectStatus);
		}
		if (!StringUtils.isEmpty(dataCurator)) {
			if (StringUtils.isEmpty(projectStatus))
				sqlQueryBuilder.append(" where ");
			else
				sqlQueryBuilder.append(" and ");
			sqlQueryBuilder.append(GET_REVIEW_CURATOR_EQUAL_SQL);
			args.add(dataCurator);
		}

		try {
			return jdbcTemplate.queryForObject(sqlQueryBuilder.toString(), Integer.class, args.toArray());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get count of projects for review: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public void addReviewNotification(HpcNotificationReview reviewNotification) throws HpcException {
		try {
			jdbcTemplate.update(INSERT_REVIEW_NOTIFICATION_SQL, reviewNotification.getUserId(),
					reviewNotification.getEventType().value(), reviewNotification.getDelivered());

		} catch (DataAccessException e) {
			throw new HpcException("Failed to add a review notification record: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<String> getCuratorsForAnnualReview() throws HpcException {
		try {
			return jdbcTemplate.query(GET_CURATORS_FOR_REVIEW_SQL, curatorRowMapper);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a list of curators to send annual review email: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<String> getCuratorsForAnnualReviewReminder() throws HpcException {
		try {
			return jdbcTemplate.query(GET_CURATORS_FOR_REMINDER_SQL, curatorRowMapper);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get a list of curators to send annual review reminder email: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

}
