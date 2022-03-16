/**
 * HpcEventDAOImpl.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.oracle.impl;

import static gov.nih.nci.hpc.util.HpcUtil.humanReadableByteCount;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import gov.nih.nci.hpc.dao.HpcReportsDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.domain.report.HpcReportEntry;
import gov.nih.nci.hpc.domain.report.HpcReportEntryAttribute;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Reports DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 */

public class HpcReportsDAOImpl implements HpcReportsDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//
	// File size range
	private static final String FILE_RANGE_SELECT = "select (case when to_number(a.meta_attr_value, '9999999999999999999') <=  1000000 then 'range1' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 1000000 and to_number(a.meta_attr_value, '9999999999999999999')     <= 10000000 then 'range2' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 10000000 and to_number(a.meta_attr_value, '9999999999999999999')    <= 50000000 then 'range3' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 50000000 and to_number(a.meta_attr_value, '9999999999999999999')    <= 100000000 then 'range4' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 100000000 and to_number(a.meta_attr_value, '9999999999999999999')   <= 500000000 then 'range5' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 500000000 and to_number(a.meta_attr_value, '9999999999999999999')   <= 1000000000 then 'range6' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 1000000000 and to_number(a.meta_attr_value, '9999999999999999999')  <= 10000000000 then 'range7' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') >  10000000000  then 'range8' "
			+ "end) as range, count(*) as cnt ";

	private static String FILE_RANGE_FROM = "";
	private static String FILE_RANGE_WHERE = "";

	private static final String FILE_RANGE_GROUP = " group by (case when to_number(a.meta_attr_value, '9999999999999999999') <= 1000000 then 'range1' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 1000000 and to_number(a.meta_attr_value, '9999999999999999999')     <= 10000000 then 'range2' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 10000000 and to_number(a.meta_attr_value, '9999999999999999999')    <= 50000000 then 'range3' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 50000000 and to_number(a.meta_attr_value, '9999999999999999999')    <= 100000000 then 'range4' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 100000000 and to_number(a.meta_attr_value, '9999999999999999999')   <= 500000000 then 'range5' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 500000000 and to_number(a.meta_attr_value, '9999999999999999999')   <= 1000000000 then 'range6' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') > 1000000000 and to_number(a.meta_attr_value, '9999999999999999999')  <= 10000000000 then 'range7' "
			+ "when to_number(a.meta_attr_value, '9999999999999999999') >  10000000000  then 'range8' " + "end)";

	private RowMapper<Map<String, Object>> fileSizeRangeRowMapper = (rs, rowNum) -> {
		Map<String, Object> range = new HashMap<String, Object>();
		String rangeName = rs.getString("range");
		int count = rs.getInt("cnt");
		range.put(rangeName, count);
		return range;
	};

	////////////////////////// USAGE_SUMMARY.
	private static final String SUM_OF_DATA_SQL = "select sum(to_number(meta_attr_value, '9999999999999999999')) totalSize, "
			+ "max(to_number(meta_attr_value, '9999999999999999999')) maxSize, "
			+ "avg(to_number(meta_attr_value, '9999999999999999999')) avgSize " + "from r_report_source_file_size";

	private static final String TOTAL_NUM_OF_USERS_SQL = "SELECT count(*) totalUsers FROM \"HPC_USER\"";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_SQL = "SELECT count(distinct data_id) totalObjs FROM r_report_data_objects";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_SQL = "select meta_attr_value attr, count(coll_id) cnt from r_report_collection_type group by meta_attr_value";

	private static final String AVG_NUM_OF_DATA_OBJECT_META_ATTRS_SQL = "SELECT count(meta_attr_name) / greatest( count(distinct data_id), 1 ) FROM r_report_data_objects";

	/////////////////////////////// USAGE_SUMMARY_DATE_RANGE.
	private static final String SUM_OF_DATA_BY_DATE_SQL = "select sum(to_number(meta_attr_value, '9999999999999999999')) totalSize, "
			+ "max(to_number(meta_attr_value, '9999999999999999999')) maxSize, "
			+ "avg(to_number(meta_attr_value, '9999999999999999999')) avgSize "
			+ "from r_report_source_file_size where CAST(create_ts as double precision) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_USERS_BY_DATE_SQL = "SELECT count(*) totalUsers FROM \"HPC_USER\" where \"CREATED\" BETWEEN ? and ?";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_DATE_SQL = "SELECT count(distinct data_id) totalObjs FROM r_report_data_objects "
			+ "where CAST(create_ts as double precision) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_AND_DATE_SQL = "select meta_attr_value attr, count(coll_id) cnt from r_report_collection_type where CAST(create_ts as double precision) BETWEEN ? AND ? group by meta_attr_value";

	private static final String AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_DATE_SQL = "SELECT count(meta_attr_name) / greatest( count(distinct data_id), 1 ) FROM r_report_data_objects where CAST(create_ts as double precision) BETWEEN ? AND ?";

	/////////////////////////// USAGE_SUMMARY_BY_DOC.
	private static final String SUM_OF_DATA_BY_DOC_SQL = "select sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize, "
			+ "max(to_number(a.meta_attr_value, '9999999999999999999')) maxSize, "
			+ "avg(to_number(a.meta_attr_value, '9999999999999999999')) avgSize "
			+ "from r_report_source_file_size a, r_report_registered_by_doc b "
			+ "where a.object_id = b.object_id and b.\"DOC\"=?";

	private static final String TOTAL_NUM_OF_USERS_BY_DOC_SQL = "SELECT count(*) totalUsers FROM \"HPC_USER\" where \"DOC\"=?";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_DOC_SQL = "SELECT count(distinct a.data_id) totalObjs FROM r_report_data_objects a, \"HPC_DATA_MANAGEMENT_CONFIGURATION\" b "
			+ "where a.meta_attr_name='configuration_id' and a.meta_attr_value=b.\"ID\" and b.\"DOC\"=?";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_DOC_SQL = "select a.meta_attr_value attr, count(a.coll_id) cnt from r_report_collection_type a,  "
			+ "r_report_coll_registered_by_doc b where b.\"DOC\"=? and a.coll_id=b.coll_id group by a.meta_attr_value";

	private static final String AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_DOC_SQL = "SELECT count(a.meta_attr_name) / greatest( count(distinct data_id), 1 ) "
			+ "FROM r_report_data_objects a, r_report_registered_by_doc b "
			+ "where a.data_id = b.object_id and b.\"DOC\"=?";

	/////////////////////////////////// USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE.
	private static final String SUM_OF_DATA_BY_DOC_DATE_SQL = "select sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize, "
			+ "max(to_number(a.meta_attr_value, '9999999999999999999')) maxSize, "
			+ "avg(to_number(a.meta_attr_value, '9999999999999999999')) avgSize "
			+ "from r_report_source_file_size a, r_report_registered_by_doc b "
			+ "where a.object_id = b.object_id and b.\"DOC\"=? and CAST(a.create_ts as double precision) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_USERS_BY_DOC_DATE_SQL = "SELECT count(*) totalUsers FROM \"HPC_USER\" where \"DOC\"=? and \"CREATED\" BETWEEN ?  AND ?";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_DOC_DATE_SQL = "SELECT count(distinct data_id) totalObjs FROM r_report_data_objects a, \"HPC_DATA_MANAGEMENT_CONFIGURATION\" b  "
			+ "where a.meta_attr_name='configuration_id' and a.meta_attr_value=b.\"ID\" and b.\"DOC\"=? and CAST(a.create_ts as double precision) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_DOC_DATE_SQL = "select a.meta_attr_value attr, count(a.coll_id) cnt from r_report_collection_type a,  "
			+ "r_report_coll_registered_by_doc b where b.\"DOC\"=? and a.coll_id=b.coll_id "
			+ "and CAST(b.create_ts as double precision) BETWEEN ? AND ? group by a.meta_attr_value";

	private static final String AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_DOC_DATE_SQL = "SELECT count(a.meta_attr_name) / greatest( count(distinct data_id), 1 ) "
			+ "FROM r_report_data_objects a, r_report_registered_by_doc b "
			+ "where a.data_id = b.object_id and b.\"DOC\"=? and CAST(a.create_ts as double precision) BETWEEN ? AND ?";

	////////////////////////////////////// USAGE_SUMMARY_BY_USER.
	private static final String SUM_OF_DATA_BY_USER_SQL = "select sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize, "
			+ "max(to_number(a.meta_attr_value, '9999999999999999999')) maxSize, "
			+ "avg(to_number(a.meta_attr_value, '9999999999999999999')) avgSize "
			+ "from r_report_source_file_size a, r_report_registered_by b "
			+ "where a.object_id = b.object_id and b.meta_attr_value=?";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_USER_SQL = "SELECT count(distinct data_id) totalObjs FROM r_report_data_objects "
			+ "where meta_attr_name='registered_by' and meta_attr_value=?";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_USER_SQL = "select a.meta_attr_value attr, count(a.coll_id) cnt from r_report_collection_type a,  "
			+ "r_report_coll_registered_by b where b.meta_attr_value=? and a.coll_id=b.coll_id group by a.meta_attr_value";

	private static final String AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_USER_SQL = "SELECT count(a.meta_attr_name) / greatest( count(distinct data_id), 1 ) "
			+ "FROM r_report_data_objects a, r_report_registered_by b "
			+ "where a.data_id = b.object_id and b.meta_attr_value=?";

	//////////////////////////////////// USAGE_SUMMARY_BY_USER_BY_DATE_RANGE.
	private static final String SUM_OF_DATA_BY_USER_DATE_SQL = "select sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize, "
			+ "max(to_number(a.meta_attr_value, '9999999999999999999')) maxSize, "
			+ "avg(to_number(a.meta_attr_value, '9999999999999999999')) avgSize "
			+ "from r_report_source_file_size a, r_report_registered_by b "
			+ "where a.object_id = b.object_id and b.meta_attr_value=? and CAST(a.create_ts as double precision) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_USER_DATE_SQL = "SELECT count(distinct data_id) totalObjs FROM r_report_data_objects "
			+ "where meta_attr_name='registered_by' and meta_attr_value=? and CAST(create_ts as double precision) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_USER_DATE_SQL = "select a.meta_attr_value attr, count(a.coll_id) cnt from r_report_collection_type a,  "
			+ "r_report_coll_registered_by b where b.meta_attr_value=? and a.coll_id=b.coll_id "
			+ "and CAST(b.create_ts as double precision) BETWEEN ? AND ? group by a.meta_attr_value";

	private static final String AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_USER_DATE_SQL = "SELECT count(a.meta_attr_name) / greatest( count(distinct data_id), 1 ) "
			+ "FROM r_report_data_objects a, r_report_registered_by b "
			+ "where a.data_id = b.object_id and b.meta_attr_value=? and CAST(a.create_ts as double precision) BETWEEN ? AND ?";

	/////////////////////////// USAGE_SUMMARY_BY_BASEPATH.
	private static final String SUM_OF_DATA_BY_BASEPATH_SQL = "select sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize, "
			+ "max(to_number(a.meta_attr_value, '9999999999999999999')) maxSize, "
			+ "avg(to_number(a.meta_attr_value, '9999999999999999999')) avgSize "
			+ "from r_report_source_file_size a, r_report_registered_by_basepath b "
			+ "where a.object_id = b.object_id and b.\"BASE_PATH\"=?";

	private static final String TOTAL_NUM_OF_USERS_BY_BASEPATH_SQL = "SELECT count(*) totalUsers FROM \"HPC_USER\" a,  \"HPC_DATA_MANAGEMENT_CONFIGURATION\" b "
			+ "where a.\"DEFAULT_CONFIGURATION_ID\"=b.\"ID\" and b.\"BASE_PATH\"=?";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_BASEPATH_SQL = "SELECT count(distinct a.data_id) totalObjs FROM r_report_data_objects a, \"HPC_DATA_MANAGEMENT_CONFIGURATION\" b "
			+ "where a.meta_attr_name='configuration_id' and a.meta_attr_value=b.\"ID\" and b.\"BASE_PATH\"=?";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_BASEPATH_SQL = "select a.meta_attr_value attr, count(a.coll_id) cnt from r_report_collection_type a,  "
			+ "r_report_coll_registered_by_basepath b where b.\"BASE_PATH\"=? and a.coll_id=b.coll_id group by a.meta_attr_value";

	private static final String AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_BASEPATH_SQL = "SELECT count(a.meta_attr_name) / greatest( count(distinct data_id), 1 ) "
			+ "FROM r_report_data_objects a, r_report_registered_by_basepath b "
			+ "where a.data_id = b.object_id and b.\"BASE_PATH\"=?";

	/////////////////////////////////// USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE.
	private static final String SUM_OF_DATA_BY_BASEPATH_DATE_SQL = "select sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize, "
			+ "max(to_number(a.meta_attr_value, '9999999999999999999')) maxSize, "
			+ "avg(to_number(a.meta_attr_value, '9999999999999999999')) avgSize "
			+ "from r_report_source_file_size a, r_report_registered_by_basepath b "
			+ "where a.object_id = b.object_id and b.\"BASE_PATH\"=? and CAST(a.create_ts as double precision) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_USERS_BY_BASEPATH_DATE_SQL = "SELECT count(*) totalUsers FROM \"HPC_USER\" a,  \"HPC_DATA_MANAGEMENT_CONFIGURATION\" b "
			+ "where a.\"DEFAULT_CONFIGURATION_ID\"=b.\"ID\" and b.\"BASE_PATH\"=? and a.\"CREATED\" BETWEEN ?  AND ?";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_BASEPATH_DATE_SQL = "SELECT count(distinct data_id) totalObjs FROM r_report_data_objects a, \"HPC_DATA_MANAGEMENT_CONFIGURATION\" b  "
			+ "where a.meta_attr_name='configuration_id' and a.meta_attr_value=b.\"ID\" and b.\"BASE_PATH\"=? and CAST(a.create_ts as double precision) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_BASEPATH_DATE_SQL = "select a.meta_attr_value attr, count(a.coll_id) cnt from r_report_collection_type a,  "
			+ "r_report_coll_registered_by_basepath b where b.\"BASE_PATH\"=? and a.coll_id=b.coll_id "
			+ "and CAST(b.create_ts as double precision) BETWEEN ? AND ? group by a.meta_attr_value";

	private static final String AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_BASEPATH_DATE_SQL = "SELECT count(a.meta_attr_name) / greatest( count(distinct data_id), 1 ) "
			+ "FROM r_report_data_objects a, r_report_registered_by_basepath b "
			+ "where a.data_id = b.object_id and b.\"BASE_PATH\"=? and CAST(a.create_ts as double precision) BETWEEN ? AND ?";

	/////////////////////////// USAGE_SUMMARY_BY_PATH.
	private static final String SUM_OF_DATA_BY_PATH_SQL = "select sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize, "
			+ "max(to_number(a.meta_attr_value, '9999999999999999999')) maxSize, "
			+ "avg(to_number(a.meta_attr_value, '9999999999999999999')) avgSize "
			+ "from r_report_source_file_size a, r_report_collection_path b "
			+ "where a.object_id = b.object_id and (b.coll_name like ? or b.coll_name = ?)";

	private static final String TOTAL_NUM_OF_USERS_BY_PATH_SQL = "select COALESCE( count(distinct a.data_owner_name), 0) + COALESCE(count(distinct b.coll_owner_name), 0) from r_data_main a, r_coll_main b where a.coll_id=b.coll_id and (b.coll_name like ? or b.coll_name = ?) ";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_PATH_SQL = "select count(distinct a.object_id) "
			+ "from r_report_source_file_size a, r_report_collection_path b "
			+ "where a.object_id=b.object_id and (b.coll_name like ? or b.coll_name = ?)";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_PATH_SQL = "select a.meta_attr_value attr, count(distinct a.coll_id) cnt from r_report_collection_type a, "
			+ "r_report_coll_registered_by_path b where (b.coll_name like ? or b.coll_name = ?) and a.coll_id=b.coll_id group by a.meta_attr_value";

	private static final String AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_PATH_SQL = "SELECT count(a.meta_attr_name) / greatest( count(distinct a.data_id), 1 ) "
			+ "FROM r_report_data_objects a, r_report_collection_path b "
			+ "where a.data_id = b.object_id and (b.coll_name like ? or b.coll_name = ?)";

	/////////////////////////// USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE.
	private static final String SUM_OF_DATA_BY_PATH_DATE_SQL = "select sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize, "
			+ "max(to_number(a.meta_attr_value, '9999999999999999999')) maxSize, "
			+ "avg(to_number(a.meta_attr_value, '9999999999999999999')) avgSize "
			+ "from r_report_source_file_size a, r_report_collection_path b "
			+ "where a.object_id = b.object_id and (b.coll_name like ? or b.coll_name = ?) and CAST(a.create_ts as double precision) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_USERS_BY_PATH_DATE_SQL = "select COALESCE( count(distinct a.data_owner_name), 0) + COALESCE(count(distinct b.coll_owner_name), 0) from r_data_main a, r_coll_main b where a.coll_id=b.coll_id and (b.coll_name like ? or b.coll_name = ?) and CAST(a.create_ts as double precision) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_PATH_DATE_SQL = "select count(distinct a.object_id) "
			+ "from r_report_source_file_size a, r_report_collection_path b "
			+ "where a.object_id=b.object_id and (b.coll_name like ? or b.coll_name = ?)  and CAST(b.create_ts as double precision) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_PATH_DATE_SQL = "select a.meta_attr_value attr, count(distinct a.coll_id) cnt from r_report_collection_type a, "
			+ "r_report_coll_registered_by_path b where (b.coll_name like ? or b.coll_name = ?) and a.coll_id=b.coll_id and CAST(b.create_ts as double precision) BETWEEN ? AND ? group by a.meta_attr_value";

	private static final String AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_PATH_DATE_SQL = "SELECT count( a.meta_attr_name) / greatest( count(distinct a.data_id), 1 ) "
			+ "FROM r_report_data_objects a, r_report_collection_path b "
			+ "where a.data_id = b.object_id and (b.coll_name like ? or b.coll_name = ?) and CAST(a.create_ts as double precision) BETWEEN ? AND ?";

	private static final String USERS_SQL = "select \"USER_ID\" from \"HPC_USER\"";

	private static final String DOCS_SQL = "select distinct meta_attr_value from r_meta_main where meta_attr_name='registered_by_doc'";

	private static final String REFRESH_VIEWS_SQL = "call REFRESH_REPORT_META_VIEW()";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	private String iRodsBasePath = "";

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 * @param iRodsBasePath The IRODS base path.
	 */
	private HpcReportsDAOImpl(String iRodsBasePath) {
		this.iRodsBasePath = iRodsBasePath;
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcReportsDAO Interface Implementation
	// ---------------------------------------------------------------------//

	private String getUsersSize(HpcReportCriteria criteria, Date[] dates, Object[] docArg, Object[] docDateArgs,
			Object[] basepathArg, Object[] basepathDateArgs, Object[] pathArg, Object[] pathDateArgs) {
		Long usersSize = null;
		try {
			if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
				usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_SQL, Long.class);
			else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
				usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_BY_DATE_SQL, dates, Long.class);
			else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
				usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_BY_DOC_SQL, docArg, Long.class);
			else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
				usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_BY_DOC_DATE_SQL, docDateArgs, Long.class);
			else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH))
				usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_BY_BASEPATH_SQL, basepathArg, Long.class);
			else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE))
				usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_BY_BASEPATH_DATE_SQL, basepathDateArgs,
						Long.class);
			else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH)) {
				usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_BY_PATH_SQL, pathArg, Long.class);
			} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE)) {
				usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_BY_PATH_DATE_SQL, pathDateArgs, Long.class);
			}
		} catch (EmptyResultDataAccessException e) {
		}
		if (usersSize != null)
			return usersSize.toString();
		else
			return "0";
	}

	private String[] getTotalDataSize(HpcReportCriteria criteria, Long[] dates, String[] docArg, Object[] docDateArgs,
			String[] userArg, Object[] userDateArgs, Object[] basepathArg, Object[] basepathDateArgs, Object[] pathArg,
			Object[] pathDateArgs) {
		Map<String, Object> totals = null;
		if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			totals = jdbcTemplate.queryForMap(SUM_OF_DATA_SQL);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			totals = jdbcTemplate.queryForMap(SUM_OF_DATA_BY_DATE_SQL, (Object[]) dates);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
			totals = jdbcTemplate.queryForMap(SUM_OF_DATA_BY_DOC_SQL, (Object[]) docArg);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
			totals = jdbcTemplate.queryForMap(SUM_OF_DATA_BY_DOC_DATE_SQL, docDateArgs);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER))
			totals = jdbcTemplate.queryForMap(SUM_OF_DATA_BY_USER_SQL, (Object[]) userArg);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
			totals = jdbcTemplate.queryForMap(SUM_OF_DATA_BY_USER_DATE_SQL, userDateArgs);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH))
			totals = jdbcTemplate.queryForMap(SUM_OF_DATA_BY_BASEPATH_SQL, basepathArg);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE))
			totals = jdbcTemplate.queryForMap(SUM_OF_DATA_BY_BASEPATH_DATE_SQL, basepathDateArgs);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH))
			totals = jdbcTemplate.queryForMap(SUM_OF_DATA_BY_PATH_SQL, pathArg);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE))
			totals = jdbcTemplate.queryForMap(SUM_OF_DATA_BY_PATH_DATE_SQL, pathDateArgs);

		String[] returnVal = new String[] { "0", "0", "0" };
		if (totals != null) {
			Iterator<Object> values = totals.values().iterator();
			Object value1 = values.next();
			Object value2 = values.next();
			Object value3 = values.next();
			if (value1 != null)
				returnVal[0] = criteria.getIsMachineReadable() ? value1.toString()
						: humanReadableByteCount(new Double(value1.toString()), true);
			if (value2 != null)
				returnVal[1] = criteria.getIsMachineReadable() ? value2.toString()
						: humanReadableByteCount(new Double(value2.toString()), true);
			if (value3 != null)
				returnVal[2] = criteria.getIsMachineReadable() ? value3.toString()
						: humanReadableByteCount(new Double(value3.toString()), true);

		}
		return returnVal;
	}

	private String getTotalDataObjSize(HpcReportCriteria criteria, Long[] dates, String[] docArg, Object[] docDateArgs,
			String[] userArg, Object[] userDateArgs, Object[] basepathArg, Object[] basepathDateArgs, Object[] pathArg,
			Object[] pathDateArgs) {
		Long dataSize = null;
		if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_SQL, Long.class);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_DATE_SQL, dates, Long.class);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_DOC_SQL, docArg, Long.class);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_DOC_DATE_SQL, docDateArgs, Long.class);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_USER_SQL, userArg, Long.class);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_USER_DATE_SQL, userDateArgs,
					Long.class);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_BASEPATH_SQL, basepathArg, Long.class);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_BASEPATH_DATE_SQL, basepathDateArgs,
					Long.class);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_PATH_SQL, pathArg, Long.class);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_PATH_DATE_SQL, pathDateArgs,
					Long.class);

		if (dataSize != null)
			return dataSize.toString();
		else
			return "0";
	}

	private List<Map<String, Object>> getTotalCollectionsSize(HpcReportCriteria criteria, Long[] dates, String[] docArg,
			Object[] docDateArgs, String[] userArg, Object[] userDateArgs, Object[] basepathArg,
			Object[] basepathDateArgs, Object[] pathArg, Object[] pathDateArgs) {
		List<Map<String, Object>> list = null;
		if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_SQL);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_AND_DATE_SQL, (Object[]) dates);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_DOC_SQL, (Object[]) docArg);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_DOC_DATE_SQL, docDateArgs);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_USER_SQL, (Object[]) userArg);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_USER_DATE_SQL, userDateArgs);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_BASEPATH_SQL, basepathArg);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_BASEPATH_DATE_SQL, basepathDateArgs);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_PATH_SQL, pathArg);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_PATH_DATE_SQL, pathDateArgs);
		return list;
	}

	private String getTotalMetaAttrCount(HpcReportCriteria criteria, Long[] dates, String[] docArg,
			Object[] docDateArgs, String[] userArg, Object[] userDateArgs, String[] basepathArg,
			Object[] basepathDateArgs, String[] pathArg, Object[] pathDateArgs) {
		Long metaAttrCount = null;
		if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			metaAttrCount = jdbcTemplate.queryForObject(AVG_NUM_OF_DATA_OBJECT_META_ATTRS_SQL, Long.class);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			metaAttrCount = jdbcTemplate.queryForObject(AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_DATE_SQL, dates,
					Long.class);
		else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC)) {
			String[] newDoc = new String[1];
			newDoc[0] = docArg[0];
			metaAttrCount = jdbcTemplate.queryForObject(AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_DOC_SQL, newDoc,
					Long.class);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE)) {
			Object[] newdocDateArgs = new Object[3];
			newdocDateArgs[0] = docDateArgs[0];
			newdocDateArgs[1] = docDateArgs[1];
			newdocDateArgs[2] = docDateArgs[2];

			metaAttrCount = jdbcTemplate.queryForObject(AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_DOC_DATE_SQL,
					newdocDateArgs, Long.class);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER)) {
			String[] newUser = new String[1];
			newUser[0] = userArg[0];

			metaAttrCount = jdbcTemplate.queryForObject(AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_USER_SQL, newUser,
					Long.class);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE)) {
			Object[] newuserDateArgs = new Object[3];
			newuserDateArgs[0] = userDateArgs[0];
			newuserDateArgs[1] = userDateArgs[1];
			newuserDateArgs[2] = userDateArgs[2];

			metaAttrCount = jdbcTemplate.queryForObject(AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_USER_DATE_SQL,
					newuserDateArgs, Long.class);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH)) {
			String[] newPath = new String[1];
			newPath[0] = basepathArg[0];
			metaAttrCount = jdbcTemplate.queryForObject(AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_BASEPATH_SQL, newPath,
					Long.class);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE)) {
			Object[] newPathDateArgs = new Object[3];
			newPathDateArgs[0] = basepathDateArgs[0];
			newPathDateArgs[1] = basepathDateArgs[1];
			newPathDateArgs[2] = basepathDateArgs[2];

			metaAttrCount = jdbcTemplate.queryForObject(AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_BASEPATH_DATE_SQL,
					newPathDateArgs, Long.class);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH)) {
			String[] newPath = new String[2];
			newPath[0] = pathArg[0];
			newPath[1] = pathArg[1];
			metaAttrCount = jdbcTemplate.queryForObject(AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_PATH_SQL, newPath,
					Long.class);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE)) {
			Object[] newPathDateArgs = new Object[4];
			newPathDateArgs[0] = pathDateArgs[0];
			newPathDateArgs[1] = pathDateArgs[1];
			newPathDateArgs[2] = pathDateArgs[2];
			newPathDateArgs[3] = pathDateArgs[3];

			metaAttrCount = jdbcTemplate.queryForObject(AVG_NUM_OF_DATA_OBJECT_META_ATTRS_BY_PATH_DATE_SQL,
					newPathDateArgs, Long.class);
		}
		if (metaAttrCount != null)
			return metaAttrCount.toString();
		else
			return "0";
	}

	private List<Map<String, Object>> getFileSizeRange(HpcReportCriteria criteria, Object[] filesizedateArgs,
			Object[] filesizedocArgs, Object[] filesizedocDateArgs, Object[] filesizeuserArgs,
			Object[] filesizeuserDateArgs, Object[] filesizebasePathArgs, Object[] filesizebasePathDateArgs,
			Object[] filesizePathArgs, Object[] filesizePathDateArgs) {
		List<Map<String, Object>> results = null;
		if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY)) {
			FILE_RANGE_FROM = " from r_report_source_file_size a ";
			return jdbcTemplate.query(FILE_RANGE_SELECT + FILE_RANGE_FROM + FILE_RANGE_GROUP, fileSizeRangeRowMapper);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE)) {
			FILE_RANGE_FROM = " from r_report_source_file_size a ";
			FILE_RANGE_WHERE = " where CAST(a.create_ts as double precision) BETWEEN ? AND ? ";
			return jdbcTemplate.query(FILE_RANGE_SELECT + FILE_RANGE_FROM + FILE_RANGE_WHERE + FILE_RANGE_GROUP,
					fileSizeRangeRowMapper, filesizedateArgs);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC)) {
			FILE_RANGE_FROM = " from r_report_source_file_size a, r_report_registered_by_doc b ";
			FILE_RANGE_WHERE = " where a.object_id=b.object_id and b.\"DOC\"=?";
			return jdbcTemplate.query(FILE_RANGE_SELECT + FILE_RANGE_FROM + FILE_RANGE_WHERE + FILE_RANGE_GROUP,
					fileSizeRangeRowMapper, filesizedocArgs);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE)) {
			FILE_RANGE_FROM = " from r_report_source_file_size a, r_report_registered_by_doc b ";
			FILE_RANGE_WHERE = " where a.object_id=b.object_id and "
					+ "b.\"DOC\"=? and CAST(a.create_ts as double precision) BETWEEN ? AND ?";
			return jdbcTemplate.query(FILE_RANGE_SELECT + FILE_RANGE_FROM + FILE_RANGE_WHERE + FILE_RANGE_GROUP,
					fileSizeRangeRowMapper, filesizedocDateArgs);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER)) {
			FILE_RANGE_FROM = " from r_report_source_file_size a, r_report_registered_by b ";
			FILE_RANGE_WHERE = " where a.object_id=b.object_id and " + "b.meta_attr_value=?";
			return jdbcTemplate.query(FILE_RANGE_SELECT + FILE_RANGE_FROM + FILE_RANGE_WHERE + FILE_RANGE_GROUP,
					fileSizeRangeRowMapper, filesizeuserArgs);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE)) {
			FILE_RANGE_FROM = " from r_report_source_file_size a, r_report_registered_by b ";
			FILE_RANGE_WHERE = " where a.object_id=b.object_id and "
					+ "b.meta_attr_value=? and CAST(a.create_ts as double precision) BETWEEN ? AND ?";
			return jdbcTemplate.query(FILE_RANGE_SELECT + FILE_RANGE_FROM + FILE_RANGE_WHERE + FILE_RANGE_GROUP,
					fileSizeRangeRowMapper, filesizeuserDateArgs);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH)) {
			FILE_RANGE_FROM = " from r_report_source_file_size a, r_report_registered_by_basepath b ";
			FILE_RANGE_WHERE = " where a.object_id=b.object_id and b.\"BASE_PATH\"=?";
			return jdbcTemplate.query(FILE_RANGE_SELECT + FILE_RANGE_FROM + FILE_RANGE_WHERE + FILE_RANGE_GROUP,
					fileSizeRangeRowMapper, filesizebasePathArgs);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE)) {
			FILE_RANGE_FROM = " from r_report_source_file_size a, r_report_registered_by_basepath b ";
			FILE_RANGE_WHERE = " where a.object_id=b.object_id and "
					+ "b.\"BASE_PATH\"=? and CAST(a.create_ts as double precision) BETWEEN ? AND ?";
			return jdbcTemplate.query(FILE_RANGE_SELECT + FILE_RANGE_FROM + FILE_RANGE_WHERE + FILE_RANGE_GROUP,
					fileSizeRangeRowMapper, filesizebasePathDateArgs);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH)) {
			FILE_RANGE_FROM = " from r_report_source_file_size a, r_report_collection_path b ";
			FILE_RANGE_WHERE = " where a.object_id=b.object_id and (b.coll_name like ? or b.coll_name = ?)";
			return jdbcTemplate.query(FILE_RANGE_SELECT + FILE_RANGE_FROM + FILE_RANGE_WHERE + FILE_RANGE_GROUP,
					fileSizeRangeRowMapper, filesizePathArgs);
		} else if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE)) {
			FILE_RANGE_FROM = " from r_report_source_file_size a, r_report_collection_path b ";
			FILE_RANGE_WHERE = " where a.object_id=b.object_id "
					+ "and (b.coll_name like ? or b.coll_name = ?) and CAST(a.create_ts as double precision) BETWEEN ? AND ?";
			return jdbcTemplate.query(FILE_RANGE_SELECT + FILE_RANGE_FROM + FILE_RANGE_WHERE + FILE_RANGE_GROUP,
					fileSizeRangeRowMapper, filesizePathDateArgs);
		}
		return results;
	}

	public List<HpcReport> generatReport(HpcReportCriteria criteria) {
		List<HpcReport> reports = new ArrayList<HpcReport>();

		if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY)
				|| criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE)
				|| criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH)
				|| criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_BASEPATH_BY_DATE_RANGE)
				|| criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH)
				|| criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE))
			reports.add(getReport(criteria));

		if (criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC)
				|| criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE)) {
			if (criteria.getDocs().isEmpty())
				criteria.getDocs().addAll(getDocs());

			List<String> docs = new ArrayList<String>();
			docs.addAll(criteria.getDocs());

			for (String doc : docs) {
				criteria.getDocs().clear();
				criteria.getDocs().add(doc);
				HpcReport report = getReport(criteria);
				reports.add(report);
			}
		}

		if ((criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER)
				|| criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))) {
			if (criteria.getUsers().isEmpty())
				criteria.getUsers().addAll(getUsers());

			List<String> users = new ArrayList<String>();
			users.addAll(criteria.getUsers());

			for (String user : users) {
				criteria.getUsers().clear();
				criteria.getUsers().add(user);
				HpcReport report = getReport(criteria);
				reports.add(report);
			}
		}

		return reports;
	}

	private List<String> getUsers() {
		return jdbcTemplate.queryForList(USERS_SQL, String.class);
	}

	private List<String> getDocs() {
		return jdbcTemplate.queryForList(DOCS_SQL, String.class);
	}

	public HpcReport getReport(HpcReportCriteria criteria) {
		HpcReport report = new HpcReport();

		// Total Users
		Date fromDate = null;
		Date toDate = null;
		Long fromDateLong = null;
		Long toDateLong = null;
		Long[] dateLongArgs = new Long[2];
		Date[] dateArgs = new Date[2];
		if (criteria.getFromDate() != null && criteria.getToDate() != null) {
			criteria.getFromDate().set(Calendar.HOUR_OF_DAY, 0);
			criteria.getFromDate().set(Calendar.MINUTE, 0);
			criteria.getFromDate().set(Calendar.SECOND, 0);
			criteria.getFromDate().set(Calendar.MILLISECOND, 0);

			criteria.getToDate().set(Calendar.HOUR_OF_DAY, 23);
			criteria.getToDate().set(Calendar.MINUTE, 59);
			criteria.getToDate().set(Calendar.SECOND, 60);
			criteria.getToDate().set(Calendar.MILLISECOND, 0);

			fromDate = criteria.getFromDate().getTime();
			toDate = criteria.getToDate().getTime();
			fromDateLong = criteria.getFromDate().getTime().getTime() / 1000;
			toDateLong = criteria.getToDate().getTime().getTime() / 1000;
			dateArgs[0] = fromDate;
			dateArgs[1] = toDate;
			dateLongArgs[0] = fromDateLong;
			dateLongArgs[1] = toDateLong;
		}

		String[] docArg = new String[1];
		if (criteria.getDocs() != null && criteria.getDocs().size() > 0)
			docArg[0] = criteria.getDocs().get(0);

		Object[] docDateUsersArgs = new Object[3];
		if (criteria.getDocs() != null && criteria.getDocs().size() > 0) {
			docDateUsersArgs[0] = criteria.getDocs().get(0);
			docDateUsersArgs[1] = fromDate;
			docDateUsersArgs[2] = toDate;
		}

		Object[] docDateArgs = new Object[3];
		if (criteria.getDocs() != null && criteria.getDocs().size() > 0) {
			docDateArgs[0] = criteria.getDocs().get(0);
			docDateArgs[1] = fromDateLong;
			docDateArgs[2] = toDateLong;
		}

		String[] userArg = new String[1];
		if (criteria.getUsers() != null && criteria.getUsers().size() > 0)
			userArg[0] = criteria.getUsers().get(0);

		Object[] userDateArgs = new Object[3];
		if (criteria.getUsers() != null && criteria.getUsers().size() > 0) {
			userDateArgs[0] = criteria.getUsers().get(0);
			userDateArgs[1] = fromDateLong;
			userDateArgs[2] = toDateLong;
		}

		String[] basepathArg = new String[1];
		if (criteria.getPath() != null)
			basepathArg[0] = criteria.getPath();

		Object[] basepathDateArgs = new Object[3];
		if (criteria.getPath() != null) {
			basepathDateArgs[0] = criteria.getPath();
			basepathDateArgs[1] = fromDate;
			basepathDateArgs[2] = toDate;
		}

		Object[] basepathDateLongArgs = new Object[3];
		if (criteria.getPath() != null) {
			basepathDateLongArgs[0] = criteria.getPath();
			basepathDateLongArgs[1] = fromDateLong;
			basepathDateLongArgs[2] = toDateLong;
		}

		String[] pathArg = new String[2];
		if (criteria.getPath() != null) {
			String collPath = criteria.getPath();
			if (!collPath.startsWith("/"))
				collPath = "/" + collPath;
			collPath = iRodsBasePath + collPath;
			pathArg[0] = collPath + "/%";
			pathArg[1] = collPath;
		}

		Object[] pathDateArgs = new Object[4];
		if (criteria.getPath() != null) {
			String collPath = criteria.getPath();
			if (!collPath.startsWith("/"))
				collPath = "/" + collPath;
			collPath = iRodsBasePath + collPath;
			pathDateArgs[0] = collPath + "/%";
			pathDateArgs[1] = collPath;
			pathDateArgs[2] = fromDate;
			pathDateArgs[3] = toDate;
		}

		Object[] pathDateLongArgs = new Object[4];
		if (criteria.getPath() != null) {
			String collPath = criteria.getPath();
			if (!collPath.startsWith("/"))
				collPath = "/" + collPath;
			collPath = iRodsBasePath + collPath;
			pathDateLongArgs[0] = collPath + "/%";
			pathDateLongArgs[1] = collPath;
			pathDateLongArgs[2] = fromDateLong;
			pathDateLongArgs[3] = toDateLong;
		}

		if (criteria.getPath() != null)
			report.setPath(criteria.getPath());

		String[] totals = new String[2];
		boolean allAttributes = criteria.getAttributes() == null || criteria.getAttributes().isEmpty();

		// TOTAL_NUM_OF_REGISTERED_USERS
		if (allAttributes || criteria.getAttributes().contains(HpcReportEntryAttribute.TOTAL_NUM_OF_REGISTERED_USERS)) {
			if (!(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER)
					|| criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))) {
				HpcReportEntry userSizeEntry = new HpcReportEntry();
				userSizeEntry.setAttribute(HpcReportEntryAttribute.TOTAL_NUM_OF_REGISTERED_USERS);
				userSizeEntry.setValue(getUsersSize(criteria, dateArgs, docArg, docDateUsersArgs, basepathArg,
						basepathDateArgs, pathArg, pathDateLongArgs));
				report.getReportEntries().add(userSizeEntry);
			}
		}

		// Total Size - TOTAL_DATA_SIZE
		if (allAttributes || criteria.getAttributes().contains(HpcReportEntryAttribute.TOTAL_DATA_SIZE)) {
			HpcReportEntry sizeEntry = new HpcReportEntry();
			sizeEntry.setAttribute(HpcReportEntryAttribute.TOTAL_DATA_SIZE);
			totals = getTotalDataSize(criteria, dateLongArgs, docArg, docDateArgs, userArg, userDateArgs, basepathArg,
					basepathDateLongArgs, pathArg, pathDateLongArgs);
			sizeEntry.setValue(totals[0]);
			report.getReportEntries().add(sizeEntry);
		}

		// Largest file - LARGEST_FILE_SIZE
		if (allAttributes || criteria.getAttributes().contains(HpcReportEntryAttribute.LARGEST_FILE_SIZE)) {
			HpcReportEntry largestFileSizeEntry = new HpcReportEntry();
			largestFileSizeEntry.setAttribute(HpcReportEntryAttribute.LARGEST_FILE_SIZE);
			largestFileSizeEntry.setValue(totals[1]);
			report.getReportEntries().add(largestFileSizeEntry);
		}

		// Average file - AVERAGE_FILE_SIZE
		if (allAttributes || criteria.getAttributes().contains(HpcReportEntryAttribute.AVERAGE_FILE_SIZE)) {
			HpcReportEntry averageFileSizeEntry = new HpcReportEntry();
			averageFileSizeEntry.setAttribute(HpcReportEntryAttribute.AVERAGE_FILE_SIZE);
			averageFileSizeEntry.setValue(totals[2]);
			report.getReportEntries().add(averageFileSizeEntry);
		}

		// Total number of data objects - TOTAL_NUM_OF_DATA_OBJECTS
		if (allAttributes || criteria.getAttributes().contains(HpcReportEntryAttribute.TOTAL_NUM_OF_DATA_OBJECTS)) {
			HpcReportEntry numOfDataObjEntry = new HpcReportEntry();
			numOfDataObjEntry.setAttribute(HpcReportEntryAttribute.TOTAL_NUM_OF_DATA_OBJECTS);
			numOfDataObjEntry.setValue(getTotalDataObjSize(criteria, dateLongArgs, docArg, docDateArgs, userArg,
					userDateArgs, basepathArg, basepathDateLongArgs, pathArg, pathDateLongArgs));
			report.getReportEntries().add(numOfDataObjEntry);
		}

		// Total number of collections - TOTAL_NUM_OF_COLLECTIONS
		if (allAttributes || criteria.getAttributes().contains(HpcReportEntryAttribute.TOTAL_NUM_OF_COLLECTIONS)) {
			List<Map<String, Object>> list = getTotalCollectionsSize(criteria, dateLongArgs, docArg, docDateArgs,
					userArg, userDateArgs, basepathArg, basepathDateLongArgs, pathArg, pathDateLongArgs);
			StringBuffer str = new StringBuffer();
			str.append("[");
			if (list != null) {
				for (Map<String, Object> listEntry : list) {
					String type = null;
					String count = null;
					Iterator<String> iter = listEntry.keySet().iterator();
					while (iter.hasNext()) {
						String name = iter.next();
						if (name.equalsIgnoreCase("cnt")) {
							java.math.BigDecimal value = (java.math.BigDecimal) listEntry.get(name);
							count = value.toString();
						} else
							type = (String) listEntry.get(name);
					}
					str.append("{" + type + ": " + count + "}");
				}
			}
			str.append("]");
			HpcReportEntry numOfCollEntry = new HpcReportEntry();
			numOfCollEntry.setAttribute(HpcReportEntryAttribute.TOTAL_NUM_OF_COLLECTIONS);
			numOfCollEntry.setValue(str.toString());
			report.getReportEntries().add(numOfCollEntry);
		}

		// Total Meta attributes Size - TOTAL_NUMBER_OF_META_ATTRS
		if (allAttributes
				|| criteria.getAttributes().contains(HpcReportEntryAttribute.AVG_NUMBER_OF_DATA_OBJECT_META_ATTRS)) {
			HpcReportEntry metasizeEntry = new HpcReportEntry();
			metasizeEntry.setAttribute(HpcReportEntryAttribute.AVG_NUMBER_OF_DATA_OBJECT_META_ATTRS);
			metasizeEntry.setValue(getTotalMetaAttrCount(criteria, dateLongArgs, docArg, docDateArgs, userArg,
					userDateArgs, basepathArg, basepathDateLongArgs, pathArg, pathDateLongArgs));
			report.getReportEntries().add(metasizeEntry);
		}

		// File size ranges
		Object[] filesizedateArgs = new Object[2];
		filesizedateArgs[0] = fromDateLong;
		filesizedateArgs[1] = toDateLong;

		Object[] filesizedocArgs = new Object[1];
		if (criteria.getDocs() != null && criteria.getDocs().size() > 0)
			filesizedocArgs[0] = criteria.getDocs().get(0);

		Object[] filesizedocDateArgs = new Object[3];
		if (criteria.getDocs() != null && criteria.getDocs().size() > 0)
			filesizedocDateArgs[0] = criteria.getDocs().get(0);
		filesizedocDateArgs[1] = fromDateLong;
		filesizedocDateArgs[2] = toDateLong;

		Object[] filesizeuserArgs = new Object[1];
		if (criteria.getUsers() != null && criteria.getUsers().size() > 0)
			filesizeuserArgs[0] = criteria.getUsers().get(0);

		Object[] filesizeuserDateArgs = new Object[3];
		if (criteria.getUsers() != null && criteria.getUsers().size() > 0)
			filesizeuserDateArgs[0] = criteria.getUsers().get(0);
		filesizeuserDateArgs[1] = fromDateLong;
		filesizeuserDateArgs[2] = toDateLong;

		if (allAttributes || criteria.getAttributes().contains(HpcReportEntryAttribute.FILE_SIZES)) {
			// Get File size ranges
			List<Map<String, Object>> fileSizeRanges = getFileSizeRange(criteria, filesizedateArgs, filesizedocArgs,
					filesizedocDateArgs, filesizeuserArgs, filesizeuserDateArgs, basepathArg, basepathDateLongArgs,
					pathArg, pathDateLongArgs);
			HpcReportEntry oneMBEntry = new HpcReportEntry();
			oneMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_BELOW_1_MB);
			oneMBEntry.setValue(getFilesSize("range1", fileSizeRanges));
			report.getReportEntries().add(oneMBEntry);

			HpcReportEntry tenMBEntry = new HpcReportEntry();
			tenMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_1_MB_10_MB);
			tenMBEntry.setValue(getFilesSize("range2", fileSizeRanges));
			report.getReportEntries().add(tenMBEntry);

			HpcReportEntry fiftyMBEntry = new HpcReportEntry();
			fiftyMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_10_MB_50_MB);
			fiftyMBEntry.setValue(getFilesSize("range3", fileSizeRanges));
			report.getReportEntries().add(fiftyMBEntry);

			HpcReportEntry hundredMBEntry = new HpcReportEntry();
			hundredMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_50_MB_100_MB);
			hundredMBEntry.setValue(getFilesSize("range4", fileSizeRanges));
			report.getReportEntries().add(hundredMBEntry);

			HpcReportEntry fivehundredMBEntry = new HpcReportEntry();
			fivehundredMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_100_MB_500_MB);
			fivehundredMBEntry.setValue(getFilesSize("range5", fileSizeRanges));
			report.getReportEntries().add(fivehundredMBEntry);

			HpcReportEntry onegbEntry = new HpcReportEntry();
			onegbEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_500_MB_1_GB);
			onegbEntry.setValue(getFilesSize("range6", fileSizeRanges));
			report.getReportEntries().add(onegbEntry);

			HpcReportEntry tengbEntry = new HpcReportEntry();
			tengbEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_1_GB_10_GB);
			tengbEntry.setValue(getFilesSize("range7", fileSizeRanges));
			report.getReportEntries().add(tengbEntry);

			HpcReportEntry overtengbEntry = new HpcReportEntry();
			overtengbEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_OVER_10_GB);
			overtengbEntry.setValue(getFilesSize("range8", fileSizeRanges));
			report.getReportEntries().add(overtengbEntry);
		}

		report.setGeneratedOn(Calendar.getInstance());
		if (criteria.getDocs() != null && criteria.getDocs().size() > 0)
			report.setDoc(criteria.getDocs().get(0));
		if (criteria.getUsers() != null && criteria.getUsers().size() > 0)
			report.setUser(criteria.getUsers().get(0));
		report.setType(criteria.getType());
		if (criteria.getFromDate() != null)
			report.setFromDate(criteria.getFromDate());
		if (criteria.getToDate() != null)
			report.setToDate(criteria.getToDate());

		return report;
	}

	private String getFilesSize(String range, List<Map<String, Object>> fileSizeRanges) {
		if (fileSizeRanges != null && fileSizeRanges.size() > 0) {
			for (Map<String, Object> rangeMap : fileSizeRanges) {
				if (rangeMap.get(range) != null)
					return rangeMap.get(range).toString();
			}
		}
		return "0";
	}

	@Override
	public void refreshViews() throws HpcException {
		try {
			jdbcTemplate.execute(REFRESH_VIEWS_SQL);

		} catch (DataAccessException e) {
			throw new HpcException("Failed to refresh report views: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

}
