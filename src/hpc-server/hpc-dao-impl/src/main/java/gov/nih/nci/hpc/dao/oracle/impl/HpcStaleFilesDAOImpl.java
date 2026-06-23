/**
 * HpcStaleFilesDAOImpl.java
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

import gov.nih.nci.hpc.dao.HpcStaleFilesDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.stalefiles.HpcStalePieChartEntry;
import gov.nih.nci.hpc.domain.stalefiles.HpcStaleBarChartEntry;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Stale Files DAO Implementation.
 *
 * @author <a href="mailto:NCIDataVault@mail.nih.gov">NCI Data Vault</a>
 */
public class HpcStaleFilesDAOImpl implements HpcStaleFilesDAO {

	// -------------------------------------------------------------------------//
	// Constants
	// -------------------------------------------------------------------------//

	// Pie chart SQL: groups all files under basePath/currentPath by stale-access bucket.
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
		"           end as stale_bucket_label, " +
		"           case " +
		"               when effective_accessed_date >= systimestamp - numtodsinterval(90, 'DAY') " +
		"                    then 1 " +
		"               when effective_accessed_date >= systimestamp - numtodsinterval(180, 'DAY') " +
		"                    then 2 " +
		"               when effective_accessed_date >= systimestamp - numtodsinterval(365, 'DAY') " +
		"                    then 3 " +
		"               else 4 " +
		"           end as stale_bucket_order " +
		"    from irods.hpc_data_object_last_access_mv " +
		"    where effective_accessed_date is not null " +
		"      and base_path = ? " +
		"      and path like ? || '/%' " +
		") " +
		"select " +
		"       stale_bucket_label, " +
		"       stale_bucket_order, " +
		"       count(*) as file_count, " +
		"       round(count(*) * 100 / sum(count(*)) over (), 2) as percentage " +
		"from bucketed_files " +
		"group by stale_bucket_label, stale_bucket_order " +
		"order by stale_bucket_order";

	// Bar chart SQL: extracts the first immediate subfolder under currentPath
	// for each file, then groups by subfolder and stale-access bucket.
	private static final String BAR_CHART_SQL =
		"with params as ( " +
		"    select ? as base_path_filter, " +
		"           ? as path_prefix " +
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
		"           end as stale_bucket_label, " +
		"           case " +
		"               when h.effective_accessed_date >= systimestamp - numtodsinterval(90, 'DAY') " +
		"                    then 1 " +
		"               when h.effective_accessed_date >= systimestamp - numtodsinterval(180, 'DAY') " +
		"                    then 2 " +
		"               when h.effective_accessed_date >= systimestamp - numtodsinterval(365, 'DAY') " +
		"                    then 3 " +
		"               else 4 " +
		"           end as stale_bucket_order " +
		"    from irods.hpc_data_object_last_access_mv h" +
		"    cross join params p " +
		"    where h.effective_accessed_date is not null " +
		"      and h.base_path = p.base_path_filter " +
		"      and h.path like p.path_prefix || '/%' " +
		"), " +
		"subfolder_counts as ( " +
		"    select " +
		"           regexp_substr( " +
		"               substr(path, length(path_prefix) + 2), " +
		"               '[^/]+', " +
		"               1, " +
		"               1 " +
		"           ) as subfolder, " +
		"           stale_bucket_label, " +
		"           stale_bucket_order, " +
		"           count(*) as file_count " +
		"    from bucketed_files " +
		"    group by " +
		"           regexp_substr( " +
		"               substr(path, length(path_prefix) + 2), " +
		"               '[^/]+', " +
		"               1, " +
		"               1 " +
		"           ), " +
		"           stale_bucket_label, " +
		"           stale_bucket_order " +
		") " +
		"select " +
		"       subfolder, " +
		"       stale_bucket_label, " +
		"       stale_bucket_order, " +
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
	private RowMapper<HpcStalePieChartEntry> pieChartRowMapper = (rs, rowNum) -> {
		HpcStalePieChartEntry entry = new HpcStalePieChartEntry();
		entry.setStaleBucketLabel(rs.getString("stale_bucket_label"));
		entry.setStaleBucketOrder(rs.getInt("stale_bucket_order"));
		entry.setFileCount(rs.getLong("file_count"));
		entry.setPercentage(rs.getDouble("percentage"));
		return entry;
	};

	// Row mapper for bar chart entries.
	private RowMapper<HpcStaleBarChartEntry> barChartRowMapper = (rs, rowNum) -> {
		HpcStaleBarChartEntry entry = new HpcStaleBarChartEntry();
		entry.setSubfolder(rs.getString("subfolder"));
		entry.setStaleBucketLabel(rs.getString("stale_bucket_label"));
		entry.setStaleBucketOrder(rs.getInt("stale_bucket_order"));
		entry.setFileCount(rs.getLong("file_count"));
		entry.setPercentage(rs.getDouble("percentage"));
		return entry;
	};

	// -------------------------------------------------------------------------//
	// Constructors
	// -------------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcStaleFilesDAOImpl() {
	}

	// -------------------------------------------------------------------------//
	// Methods
	// -------------------------------------------------------------------------//

	// -------------------------------------------------------------------------//
	// HpcStaleFilesDAO Interface Implementation
	// -------------------------------------------------------------------------//

	@Override
	public List<HpcStalePieChartEntry> getPieChartData(String basePath, String currentPath)
			throws HpcException {
		try {
			return jdbcTemplate.query(PIE_CHART_SQL, pieChartRowMapper, basePath, currentPath);
		} catch (DataAccessException e) {
			throw new HpcException("Failed to query stale files pie chart data: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcStaleBarChartEntry> getBarChartData(String basePath, String currentPath)
			throws HpcException {
		try {
			// currentPath is bound 4 times: once in path LIKE, twice in substr(path, length(?)+2)
			// in SELECT and GROUP BY clauses.
			return jdbcTemplate.query(BAR_CHART_SQL, barChartRowMapper,
					basePath, currentPath);
		} catch (DataAccessException e) {
			throw new HpcException("Failed to query stale files bar chart data: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}
}
