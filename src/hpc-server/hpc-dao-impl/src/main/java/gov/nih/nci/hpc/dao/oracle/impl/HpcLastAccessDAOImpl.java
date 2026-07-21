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
import org.springframework.beans.factory.annotation.Qualifier;
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
	// Instance members
	// -------------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
    @Qualifier("hpcOracleJdbcTemplate")
	private JdbcTemplate jdbcTemplate = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// Configurable bucket intervals (in days) and labels – injected via Spring.
	private int bucketInterval1 = 90;
	private int bucketInterval2 = 180;
	private int bucketInterval3 = 365;
	private String bucketLabel1 = "Green: accessed within 90 days";
	private String bucketLabel2 = "Yellow: 90-180 days";
	private String bucketLabel3 = "Red: 180-365 days";
	private String bucketLabel4 = "Dark red: over 365 days";

	// SQL built at init time.
	private String pieChartSql;
	private String barChartSql;

	// Row mapper for pie chart entries.
	private RowMapper<HpcLastAccessPieChartEntry> pieChartRowMapper = (rs, rowNum) -> {
		HpcLastAccessPieChartEntry entry = new HpcLastAccessPieChartEntry();
		entry.setBucketLabel(rs.getString("bucket_label"));
		entry.setBucketOrder(rs.getInt("bucket_order"));
		entry.setFileCount(rs.getLong("file_count"));
		entry.setDataSize(rs.getLong("data_size"));
		entry.setDataSizePercentage(rs.getDouble("data_size_percentage"));
		return entry;
	};

	// Row mapper for bar chart entries.
	private RowMapper<HpcLastAccessBarChartEntry> barChartRowMapper = (rs, rowNum) -> {
		HpcLastAccessBarChartEntry entry = new HpcLastAccessBarChartEntry();
		entry.setSubfolder(rs.getString("subfolder"));
		entry.setBucketLabel(rs.getString("bucket_label"));
		entry.setBucketOrder(rs.getInt("bucket_order"));
		entry.setFileCount(rs.getLong("file_count"));
		entry.setDataSize(rs.getLong("data_size"));
		entry.setDataSizePercentage(rs.getDouble("data_size_percentage"));
		return entry;
	};

	// -------------------------------------------------------------------------//
	// Constructors
	// -------------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcLastAccessDAOImpl() {
	}

	// -------------------------------------------------------------------------//
	// Spring setters for configurable bucket properties
	// -------------------------------------------------------------------------//

	public void setBucketInterval1(int bucketInterval1) { this.bucketInterval1 = bucketInterval1; }
	public void setBucketInterval2(int bucketInterval2) { this.bucketInterval2 = bucketInterval2; }
	public void setBucketInterval3(int bucketInterval3) { this.bucketInterval3 = bucketInterval3; }
	public void setBucketLabel1(String bucketLabel1)    { this.bucketLabel1 = bucketLabel1; }
	public void setBucketLabel2(String bucketLabel2)    { this.bucketLabel2 = bucketLabel2; }
	public void setBucketLabel3(String bucketLabel3)    { this.bucketLabel3 = bucketLabel3; }
	public void setBucketLabel4(String bucketLabel4)    { this.bucketLabel4 = bucketLabel4; }

	// -------------------------------------------------------------------------//
	// init-method – builds SQL from configured intervals/labels
	// -------------------------------------------------------------------------//

	public void init() {
		String bucketCaseLabel =
			"case " +
			"    when {col}effective_accessed_date >= systimestamp - numtodsinterval(" + bucketInterval1 + ", 'DAY') " +
			"         then '" + bucketLabel1 + "' " +
			"    when {col}effective_accessed_date >= systimestamp - numtodsinterval(" + bucketInterval2 + ", 'DAY') " +
			"         then '" + bucketLabel2 + "' " +
			"    when {col}effective_accessed_date >= systimestamp - numtodsinterval(" + bucketInterval3 + ", 'DAY') " +
			"         then '" + bucketLabel3 + "' " +
			"    else '" + bucketLabel4 + "' " +
			"end";

		String bucketCaseOrder =
			"case " +
			"    when {col}effective_accessed_date >= systimestamp - numtodsinterval(" + bucketInterval1 + ", 'DAY') " +
			"         then 1 " +
			"    when {col}effective_accessed_date >= systimestamp - numtodsinterval(" + bucketInterval2 + ", 'DAY') " +
			"         then 2 " +
			"    when {col}effective_accessed_date >= systimestamp - numtodsinterval(" + bucketInterval3 + ", 'DAY') " +
			"         then 3 " +
			"    else 4 " +
			"end";

		// PIE CHART SQL (no column prefix needed for the subquery)
		String pieLabelCase  = bucketCaseLabel.replace("{col}", "");
		String pieOrderCase  = bucketCaseOrder.replace("{col}", "");

		pieChartSql =
			"with bucketed_files as ( " +
			"    select " +
			"           doc, base_path, bucket, data_size, effective_accessed_date, " +
			"           " + pieLabelCase + " as bucket_label, " +
			"           " + pieOrderCase + " as bucket_order " +
			"    from irods.hpc_data_object_last_access_mv " +
			"    where effective_accessed_date is not null " +
			"      and (? is null or base_path = ?) " +
			"      and (? is null or path like ? || '/%') " +
			"      and bucket not like ? " +
			") " +
			"select " +
			"       bucket_label, bucket_order, " +
			"       count(*) as file_count, " +
			"       sum(data_size) as data_size, " +
			"       nvl(round(sum(data_size) * 100 / nullif(sum(sum(data_size)) over (), 0), 2), 0) as data_size_percentage " +
			"from bucketed_files " +
			"group by bucket_label, bucket_order " +
			"order by bucket_order";

		// BAR CHART SQL (column alias h.)
		String barLabelCase  = bucketCaseLabel.replace("{col}", "h.");
		String barOrderCase  = bucketCaseOrder.replace("{col}", "h.");

		barChartSql =
			"with params as ( " +
			"    select ? as base_path_filter, " +
			"           ? as path_prefix, " +
			"           ? as bucket_filter " +
			"    from dual " +
			"), " +
			"bucketed_files as ( " +
			"    select " +
			"           h.path, h.base_path, h.bucket, h.doc, h.data_size, h.effective_accessed_date, " +
			"           p.path_prefix, " +
			"           " + barLabelCase + " as bucket_label, " +
			"           " + barOrderCase + " as bucket_order " +
			"    from irods.hpc_data_object_last_access_mv h " +
			"    cross join params p " +
			"    where h.effective_accessed_date is not null " +
			"      and (p.base_path_filter is null or h.base_path = p.base_path_filter) " +
			"      and (p.path_prefix is null or h.path like p.path_prefix || '/%') " +
			"      and h.bucket not like p.bucket_filter " +
			"), " +
			"subfolder_counts as ( " +
			"    select " +
			"           case " +
			"               when path_prefix is null then regexp_substr(path, '[^/]+', 1, 1) " +
			"               else regexp_substr(substr(path, length(path_prefix) + 2), '[^/]+', 1, 1) " +
			"           end as subfolder, " +
			"           bucket_label, bucket_order, " +
			"           count(*) as file_count, " +
			"           sum(data_size) as data_size " +
			"    from bucketed_files " +
			"    group by " +
			"           case " +
			"               when path_prefix is null then regexp_substr(path, '[^/]+', 1, 1) " +
			"               else regexp_substr(substr(path, length(path_prefix) + 2), '[^/]+', 1, 1) " +
			"           end, " +
			"           bucket_label, bucket_order " +
			") " +
			"select " +
			"       subfolder, bucket_label, bucket_order, file_count, data_size, " +
			"       nvl(round(data_size * 100 / nullif(sum(data_size) over (), 0), 2), 0) as data_size_percentage " +
			"from subfolder_counts " +
			"where subfolder is not null " +
			"order by subfolder";

		logger.info("HpcLastAccessDAOImpl initialized with intervals [{},{},{}] and labels ['{}','{}','{}','{}']",
				bucketInterval1, bucketInterval2, bucketInterval3,
				bucketLabel1, bucketLabel2, bucketLabel3, bucketLabel4);
	}

	// -------------------------------------------------------------------------//
	// HpcLastAccessDAO Interface Implementation
	// -------------------------------------------------------------------------//

	@Override
	public List<HpcLastAccessPieChartEntry> getLastAccessPieChartData(String basePath, String currentPath, boolean includeAWSBucket)
			throws HpcException {
		try {
			String basePathFilter = (basePath != null && !basePath.trim().isEmpty()) ? basePath : null;
			String currentPathFilter = (currentPath != null && !currentPath.trim().isEmpty()) ? currentPath : null;
			return jdbcTemplate.query(pieChartSql, pieChartRowMapper,
					basePathFilter, basePathFilter, currentPathFilter, currentPathFilter, includeAWSBucket ? "/" : "%aws%");
		} catch (DataAccessException e) {
			throw new HpcException("Failed to query last access pie chart data: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<HpcLastAccessBarChartEntry> getLastAccessBarChartData(String basePath, String currentPath, boolean includeAWSBucket)
			throws HpcException {
		try {
			String basePathFilter = (basePath != null && !basePath.trim().isEmpty()) ? basePath : null;
			String currentPathFilter = (currentPath != null && !currentPath.trim().isEmpty()) ? currentPath : null;
			return jdbcTemplate.query(barChartSql, barChartRowMapper,
					basePathFilter, currentPathFilter, includeAWSBucket ? "/" : "%aws%");
		} catch (DataAccessException e) {
			throw new HpcException("Failed to query last access bar chart data: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}
}
