/**
/**
 * HpcLastAccessDAOImpl.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import gov.nih.nci.hpc.dao.HpcLastAccessDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.lastaccess.HpcLastAccessPieChartEntry;
import gov.nih.nci.hpc.domain.lastaccess.HpcLastAccessBarChartEntry;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Last Access DAO Implementation.
 *
 * @author <a href="mailto:NCIDataVault@mail.nih.gov">NCI Data Vault</a>
 */
public class HpcLastAccessDAOImpl implements HpcLastAccessDAO {

	// -------------------------------------------------------------------------//
	// Constants
	// -------------------------------------------------------------------------//

	// Pie chart SQL: groups all files under basePath/currentPath by last-access bucket.
	// base_path = ? scopes to the authoritative base path.
	// path LIKE ? || '/%' scopes to the current drill-down path and all descendants.
	private static final String PIE_CHART_SQL =
		"with bucketed_files as ( " +
		"    select " +
		"           doc, " +
		"           base_path, " +
		"           bucket, " +
		"           effective_accessed_date, " +
		"           case " +
		"               when effective_accessed_date >= systimestamp - numtodsinterval(90, 'DAY') " +
		"                    then 'Green: accessed within 90 days' " +
		"               when effective_accessed_date >= systimestamp - numtodsinterval(180, 'DAY') " +
		"                    then 'Yellow: 90-180 days' " +
		"               when effective_accessed_date >= systimestamp - numtodsinterval(365, 'DAY') " +
		"                    then 'Red: 180-365 days' " +
		"               else 'Dark red: over 365 days' " +
		"           end as bucket_label, " +
		"           case " +
		"               when effective_accessed_date >= systimestamp - numtodsinterval(90, 'DAY') " +
		"                    then 1 " +
		"               when effective_accessed_date >= systimestamp - numtodsinterval(180, 'DAY') " +
		"                    then 2 " +
		"               when effective_accessed_date >= systimestamp - numtodsinterval(365, 'DAY') " +
		"                    then 3 " +
		"               else 4 " +
		"           end as bucket_order " +
		"    from irods.hpc_data_object_last_access_mv " +
		"    where effective_accessed_date is not null " +
		"      and base_path = ? " +
		"      and path like ? || '/%' " +
		"      and bucket not like ? " +
		") " +
		"select " +
		"       bucket_label, " +
		"       bucket_order, " +
		"       count(*) as file_count, " +
		"       round(count(*) * 100 / sum(count(*)) over (), 2) as percentage " +
		"from bucketed_files " +
		"group by bucket_label, bucket_order " +
		"order by bucket_order";

	// Bar chart SQL: extracts the first immediate subfolder under currentPath
	// for each file, then groups by subfolder and last-access bucket.
	private static final String BAR_CHART_SQL =
		"with params as ( " +
		"    select ? as base_path_filter, " +
		"           ? as path_prefix, " +
		"           ? as bucket_filter" +
		"    from dual " +
		")," +
		"bucketed_files as ( " +
		"    select " +
		"           h.path, " +
		"           h.base_path, " +
		"           h.bucket, " +
		"           h.doc, " +
		"           h.effective_accessed_date, " +
		"           p.path_prefix, " +
		"           case " +
		"               when h.effective_accessed_date >= systimestamp - numtodsinterval(90, 'DAY') " +
		"                    then 'Green: accessed within 90 days' " +
		"               when h.effective_accessed_date >= systimestamp - numtodsinterval(180, 'DAY') " +
		"                    then 'Yellow: 90-180 days' " +
		"               when h.effective_accessed_date >= systimestamp - numtodsinterval(365, 'DAY') " +
		"                    then 'Red: 180-365 days' " +
		"               else 'Dark red: over 365 days' " +
		"           end as bucket_label, " +
		"           case " +
		"               when h.effective_accessed_date >= systimestamp - numtodsinterval(90, 'DAY') " +
		"                    then 1 " +
		"               when h.effective_accessed_date >= systimestamp - numtodsinterval(180, 'DAY') " +
		"                    then 2 " +
		"               when h.effective_accessed_date >= systimestamp - numtodsinterval(365, 'DAY') " +
		"                    then 3 " +
		"               else 4 " +
		"           end as bucket_order " +
		"    from irods.hpc_data_object_last_access_mv h" +
		"    cross join params p " +
		"    where h.effective_accessed_date is not null " +
		"      and h.base_path = p.base_path_filter " +
		"      and h.path like p.path_prefix || '/%' " +
		"      and h.bucket not like p.bucket_filter " +
		"), " +
		"subfolder_counts as ( " +
		"    select " +
		"           regexp_substr( " +
		"               substr(path, length(path_prefix) + 2), " +
		"               '[^/]+', " +
		"               1, " +
		"               1 " +
		"           ) as subfolder, " +
		"           bucket_label, " +
		"           bucket_order, " +
		"           count(*) as file_count " +
		"    from bucketed_files " +
		"    group by " +
		"           regexp_substr( " +
		"               substr(path, length(path_prefix) + 2), " +
		"               '[^/]+', " +
		"               1, " +
		"               1 " +
		"           ), " +
		"           bucket_label, " +
		"           bucket_order " +
		") " +
		"select " +
		"       subfolder, " +
		"       bucket_label, " +
		"       bucket_order, " +
		"       file_count, " +
		"       round(file_count * 100 / sum(file_count) over (), 2) as percentage " +
		"from subfolder_counts " +
		"where subfolder is not null " +
		"order by subfolder";

	// -------------------------------------------------------------------------//
	// Instance members
	// -------------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// Row mapper for pie chart entries.
	private RowMapper<HpcLastAccessPieChartEntry> pieChartRowMapper = (rs, rowNum) -> {
		HpcLastAccessPieChartEntry entry = new HpcLastAccessPieChartEntry();
		entry.setBucketLabel(rs.getString("bucket_label"));
		entry.setBucketOrder(rs.getInt("bucket_order"));
		entry.setFileCount(rs.getLong("file_count"));
		entry.setPercentage(rs.getDouble("percentage"));
		return entry;
	};

	// Row mapper for bar chart entries.
	private RowMapper<HpcLastAccessBarChartEntry> barChartRowMapper = (rs, rowNum) -> {
		HpcLastAccessBarChartEntry entry = new HpcLastAccessBarChartEntry();
		entry.setSubfolder(rs.getString("subfolder"));
		entry.setBucketLabel(rs.getString("bucket_label"));
		entry.setBucketOrder(rs.getInt("bucket_order"));
		entry.setFileCount(rs.getLong("file_count"));
		entry.setPercentage(rs.getDouble("percentage"));
		return entry;
	};

	// -------------------------------------------------------------------------//
	// Constructors
	// -------------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcLastAccessDAOImpl() {
	}

	// -------------------------------------------------------------------------//
	// Methods
	// -------------------------------------------------------------------------//

	// -------------------------------------------------------------------------//
	// HpcLastAccessDAO Interface Implementation
	// -------------------------------------------------------------------------//

	@Override
	public List<HpcLastAccessPieChartEntry> getLastAccessPieChartData(String basePath, String currentPath, boolean includeAWSBucket)
			throws HpcException {
		try {
			return jdbcTemplate.query(PIE_CHART_SQL, pieChartRowMapper, basePath, currentPath,includeAWSBucket ? "/" : "%aws%");
		} catch (DataAccessException e) {
			throw new HpcException("Failed to query last access pie chart data: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcLastAccessBarChartEntry> getLastAccessBarChartData(String basePath, String currentPath, boolean includeAWSBucket)
			throws HpcException {
		try {
			// currentPath is bound 4 times: once in path LIKE, twice in substr(path, length(?)+2)
			// in SELECT and GROUP BY clauses.
			return jdbcTemplate.query(BAR_CHART_SQL, barChartRowMapper,
					basePath, currentPath, includeAWSBucket ? "/" : "%aws%");
		} catch (DataAccessException e) {
			throw new HpcException("Failed to query last access bar chart data: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}
}
