/**
 * HpcEventDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import gov.nih.nci.hpc.dao.HpcReportsDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.domain.report.HpcReportEntry;
import gov.nih.nci.hpc.domain.report.HpcReportEntryAttribute;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Reports DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id $
 */

public class HpcReportsDAOImpl implements HpcReportsDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
	
	//USAGE_SUMMARY
	private static final String SUM_OF_DATA_SQL = 
			"SELECT sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, r_objt_metamap b, r_data_main c where a.meta_attr_name = 'source_file_size' and b.meta_id=a.meta_id and b.object_id=c.data_id";

	private static final String LARGEST_FILE_SQL = 
			"SELECT max(to_number(meta_attr_value, '9999999999999999999')) maxSize FROM public.r_meta_main where meta_attr_name = 'source_file_size'";

	private static final String AVERAGE_FILE_SQL = 
			"SELECT avg(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, r_objt_metamap b, r_data_main c where a.meta_attr_name = 'source_file_size' and b.meta_id=a.meta_id and b.object_id=c.data_id";

	private static final String TOTAL_NUM_OF_USERS_SQL = 
			"SELECT count(*) totalUsers FROM public.\"HPC_USER\"";
	
	private static final String TOTAL_NUM_OF_DATA_OBJECTS_SQL = 
			"SELECT count(*) totalObjs FROM public.r_data_main";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_SQL = 
			"select a.meta_attr_value attr, count(a.meta_attr_name) cnt from r_meta_main a, r_coll_main b, r_objt_metamap c where b.coll_id=c.object_id and c.meta_id=a.meta_id and a.meta_attr_name='collection_type' group by a.meta_attr_value";

	private static final String TOTAL_NUM_OF_META_ATTRS_SQL = 
			"SELECT count(*) totalAttrs FROM public.r_meta_main";
	
	private static final String FILE_SIZE_RANGE_SQL = 
			"SELECT count(*) FROM public.r_meta_main a, r_objt_metamap b, r_data_main c where a.meta_attr_name = 'source_file_size' and b.meta_id=a.meta_id and b.object_id=c.data_id and to_number(a.meta_attr_value, '9999999999999999999') BETWEEN ? AND ?";
	

	//USAGE_SUMMARY_DATE_RANGE
	private static final String SUM_OF_DATA_BY_DATE_SQL = 
			"SELECT sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, r_objt_metamap b, r_data_main c where a.meta_attr_name = 'source_file_size' and b.meta_id=a.meta_id and b.object_id=c.data_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?";

	private static final String LARGEST_FILE_BY_DATE_SQL = 
			"SELECT max(to_number(meta_attr_value, '9999999999999999999')) maxSize FROM public.r_meta_main where meta_attr_name = 'source_file_size' and to_timestamp(CAST(create_ts as double precision)) BETWEEN ? AND ?";

	private static final String AVERAGE_FILE_BY_DATE_SQL = 
			"SELECT avg(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, r_objt_metamap b, r_data_main c where a.meta_attr_name = 'source_file_size' and b.meta_id=a.meta_id and b.object_id=c.data_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_USERS_BY_DATE_SQL = 
	"SELECT count(*) totalUsers FROM public.\"HPC_USER\" where \"CREATED\" BETWEEN ? and ?";
	
	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_DATE_SQL = 
			"SELECT count(*) totalObjs FROM public.r_data_main  where to_timestamp(CAST(create_ts as double precision)) BETWEEN ? AND ?";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_AND_DATE_SQL = 
			"select a.meta_attr_value attr, count(a.meta_attr_name) cnt from r_meta_main a, r_coll_main b, r_objt_metamap c where b.coll_id=c.object_id and c.meta_id=a.meta_id and a.meta_attr_name='collection_type' and to_timestamp(CAST(b.create_ts as double precision)) BETWEEN ? AND ? group by a.meta_attr_value";

	private static final String TOTAL_NUM_OF_META_ATTRS_BY_DATE_SQL = 
			"SELECT count(*) totalAttrs FROM public.r_meta_main where to_timestamp(CAST(create_ts as double precision)) BETWEEN ? AND ?";

	private static final String FILE_SIZE_RANGE_BY_DATE_SQL = 
			"SELECT count(*) FROM public.r_meta_main a, r_objt_metamap b, r_data_main c  where a.meta_attr_name = 'source_file_size' and b.meta_id=a.meta_id and b.object_id=c.data_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ? and to_number(meta_attr_value, '9999999999999999999') BETWEEN ? AND ?";

	//USAGE_SUMMARY_BY_DOC
	private static final String SUM_OF_DATA_BY_DOC_SQL = 
			"SELECT sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_attr_name = 'source_file_size' and b.object_id = c.data_id and b.object_id in " +
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b, r_data_main c  where a.meta_attr_name='registered_by_doc' and b.object_id=c.data_id and a.meta_attr_value=? and a.meta_id=b.meta_id and b.object_id = c.data_id)";

	private static final String LARGEST_FILE_BY_DOC_SQL = 
			"SELECT max(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name = 'source_file_size' and b.object_id in " +
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name='registered_by_doc' and a.meta_attr_value=? and a.meta_id=b.meta_id)";

	private static final String AVERAGE_FILE_BY_DOC_SQL = 
			"SELECT avg(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_attr_name = 'source_file_size' and b.object_id = c.data_id and b.object_id in " +
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name='registered_by_doc' and b.object_id=c.data_id and a.meta_attr_value=? and a.meta_id=b.meta_id)";

	private static final String TOTAL_NUM_OF_USERS_BY_DOC_SQL = 
			"SELECT count(*) totalUsers FROM public.\"HPC_USER\" where \"DOC\"=?";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_DOC_SQL = 
			"SELECT count(distinct c.data_id) totalObjs FROM public.r_meta_main a, public.r_objt_metamap b, public.r_data_main c where c.data_id = b.object_id and a.meta_attr_name='registered_by_doc' and a.meta_attr_value=? and a.meta_id=b.meta_id"; 

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_DOC_SQL = 
			"select a.meta_attr_value attr, count(a.meta_attr_name) cnt from r_meta_main a, r_coll_main b, r_objt_metamap c where b.coll_id=c.object_id and c.meta_id=a.meta_id and a.meta_attr_name='collection_type' and b.coll_id in"+
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b, r_coll_main c where c.coll_id=b.object_id and a.meta_attr_name='registered_by_doc' and a.meta_attr_value=? and a.meta_id=b.meta_id) "+
			"group by a.meta_attr_value";

	private static final String TOTAL_NUM_OF_META_ATTRS_BY_DOC_SQL = 
			"SELECT count(a.meta_id) totalAttrs FROM public.r_meta_main a, public.r_objt_metamap b  where a.meta_id = b.meta_id and b.object_id in "+
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name='registered_by_doc' and a.meta_attr_value=? and a.meta_id=b.meta_id) ";

	private static final String FILE_SIZE_RANGE_BY_DOC_SQL = 
			"SELECT count(*) FROM public.r_meta_main a, public.r_objt_metamap b, r_data_main c  where a.meta_id = b.meta_id and a.meta_attr_name = 'source_file_size' and to_number(a.meta_attr_value, '9999999999999999999') BETWEEN ? AND ? and b.object_id = c.data_id and b.object_id in " +
					"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_attr_name='registered_by_doc' and a.meta_attr_value=? and a.meta_id=b.meta_id and b.object_id = c.data_id)";
	
	//USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE
	private static final String SUM_OF_DATA_BY_DOC_DATE_SQL = 
			"SELECT sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_attr_name = 'source_file_size' and b.object_id = c.data_id and b.object_id in " +
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_attr_name='registered_by_doc' and b.object_id=c.data_id and a.meta_attr_value=? and a.meta_id=b.meta_id and b.object_id = c.data_id " +
			"and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?)";

	private static final String LARGEST_FILE_BY_DOC_DATE_SQL = 
			"SELECT max(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name = 'source_file_size' and b.object_id in " +
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name='registered_by_doc' and a.meta_attr_value=? and a.meta_id=b.meta_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?)";

	private static final String AVERAGE_FILE_BY_DOC_DATE_SQL = 
			"SELECT avg(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_attr_name = 'source_file_size' and b.object_id = c.data_id and b.object_id in " +
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_attr_name='registered_by_doc' and b.object_id=c.data_id and a.meta_attr_value=? and a.meta_id=b.meta_id and b.object_id = c.data_id " +
			"and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?)";

	private static final String TOTAL_NUM_OF_USERS_BY_DOC_DATE_SQL = 
			"SELECT count(*) totalUsers FROM public.\"HPC_USER\" where \"DOC\"=? and \"CREATED\" BETWEEN ?  AND ?";

	
	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_DOC_DATE_SQL = 
			"SELECT count(distinct c.data_id) totalObjs FROM public.r_meta_main a, public.r_objt_metamap b, public.r_data_main c where c.data_id = b.object_id and a.meta_attr_name='registered_by_doc' and a.meta_attr_value=? and a.meta_id=b.meta_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ? "; 

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_DOC_DATE_SQL = 
			"select a.meta_attr_value attr, count(a.meta_attr_name) cnt from r_meta_main a, r_coll_main b, r_objt_metamap c where b.coll_id=c.object_id and c.meta_id=a.meta_id and a.meta_attr_name='collection_type' and b.coll_id in"+
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b, r_coll_main c where c.coll_id=b.object_id and a.meta_attr_name='registered_by_doc' and a.meta_attr_value=? and a.meta_id=b.meta_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ? ) "+
			"group by a.meta_attr_value";

	private static final String TOTAL_NUM_OF_META_ATTRS_BY_DOC_DATE_SQL = 
			"SELECT count(a.meta_id) totalAttrs FROM public.r_meta_main a, public.r_objt_metamap b  where a.meta_id = b.meta_id and b.object_id in "+
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name='registered_by_doc' and a.meta_attr_value=? and a.meta_id=b.meta_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?) ";

	private static final String FILE_SIZE_RANGE_BY_DOC_DATE_SQL = 
			"SELECT count(*) FROM public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_id = b.meta_id and a.meta_attr_name = 'source_file_size' and b.object_id = c.data_id and to_number(a.meta_attr_value, '9999999999999999999') BETWEEN ? AND ? and b.object_id in " +
					"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b, r_data_main c  where a.meta_attr_name='registered_by_doc' and b.object_id = c.data_id and a.meta_attr_value=? and a.meta_id=b.meta_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?)";
	
	//USAGE_SUMMARY_BY_USER
	private static final String SUM_OF_DATA_BY_USER_SQL = 
			"SELECT sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b, public.r_data_main c where a.meta_attr_name = 'source_file_size' and b.object_id=c.data_id and b.object_id in " +
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b,  public.r_data_main c where a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id and b.object_id=c.data_id)";

	private static final String LARGEST_FILE_BY_USER_SQL = 
			"SELECT max(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name = 'source_file_size' and b.object_id in " +
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id)";

	private static final String AVERAGE_FILE_BY_USER_SQL = 
			"SELECT avg(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b, public.r_data_main c where a.meta_attr_name = 'source_file_size' and b.object_id=c.data_id and b.object_id in " +
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b,  public.r_data_main c where a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id and b.object_id=c.data_id)";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_USER_SQL = 
			"SELECT count(distinct c.data_id) totalObjs FROM public.r_meta_main a, public.r_objt_metamap b, public.r_data_main c where c.data_id = b.object_id and a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id"; 

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_USER_SQL = 
			"select a.meta_attr_value attr, count(a.meta_attr_name) cnt from r_meta_main a, r_coll_main b, r_objt_metamap c where b.coll_id=c.object_id and c.meta_id=a.meta_id and a.meta_attr_name='collection_type' and b.coll_id in"+
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b, r_coll_main c where c.coll_id=b.object_id and a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id) "+
			"group by a.meta_attr_value";

	private static final String TOTAL_NUM_OF_META_ATTRS_BY_USER_SQL = 
			"SELECT count(a.meta_id) totalAttrs FROM public.r_meta_main a, public.r_objt_metamap b  where a.meta_id = b.meta_id and b.object_id in "+
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id) ";

	private static final String FILE_SIZE_RANGE_BY_USER_SQL = 
			"SELECT count(*) FROM public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_id = b.meta_id and a.meta_attr_name = 'source_file_size' and b.object_id = c.data_id  and to_number(a.meta_attr_value, '9999999999999999999') BETWEEN ? AND ? and b.object_id in " +
					"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_attr_name='registered_by' and b.object_id = c.data_id and a.meta_attr_value=? and a.meta_id=b.meta_id)";
	
	//USAGE_SUMMARY_BY_USER_BY_DATE_RANGE
	private static final String SUM_OF_DATA_BY_USER_DATE_SQL = 
			"SELECT sum(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b, public.r_data_main c where a.meta_attr_name = 'source_file_size' and b.object_id=c.data_id and b.object_id  in " +
			"(select b.object_id from public.r_meta_main a, public.r_objt_metamap b, public.r_data_main c where a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id and b.object_id=c.data_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?)";

	private static final String LARGEST_FILE_BY_USER_DATE_SQL = 
			"SELECT max(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name = 'source_file_size' and b.object_id in " +
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?)";

	private static final String AVERAGE_FILE_BY_USER_DATE_SQL = 
			"SELECT avg(to_number(a.meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main a, public.r_objt_metamap b, public.r_data_main c where a.meta_attr_name = 'source_file_size' and b.object_id=c.data_id and b.object_id  in " +
			"(select b.object_id from public.r_meta_main a, public.r_objt_metamap b, public.r_data_main c where a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id and b.object_id=c.data_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?)";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_BY_USER_DATE_SQL = 
			"SELECT count(distinct c.data_id) totalObjs FROM public.r_meta_main a, public.r_objt_metamap b, public.r_data_main c where c.data_id = b.object_id and a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ? "; 

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_USER_DATE_SQL = 
			"select a.meta_attr_value attr, count(a.meta_attr_name) cnt from r_meta_main a, r_coll_main b, r_objt_metamap c where b.coll_id=c.object_id and c.meta_id=a.meta_id and a.meta_attr_name='collection_type' and b.coll_id in"+
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b, r_coll_main c where c.coll_id=b.object_id and a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ? ) "+
			"group by a.meta_attr_value";

	private static final String TOTAL_NUM_OF_META_ATTRS_BY_USER_DATE_SQL = 
			"SELECT count(a.meta_id) totalAttrs FROM public.r_meta_main a, public.r_objt_metamap b  where a.meta_id = b.meta_id and b.object_id in "+
			"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b where a.meta_attr_name='registered_by' and a.meta_attr_value=? and a.meta_id=b.meta_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?) ";

	private static final String FILE_SIZE_RANGE_BY_USER_DATE_SQL = 
			"SELECT count(*) FROM public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_id = b.meta_id and a.meta_attr_name = 'source_file_size' and b.object_id = c.data_id and to_number(a.meta_attr_value, '9999999999999999999') BETWEEN ? AND ? and b.object_id in " +
					"(select distinct b.object_id from public.r_meta_main a, public.r_objt_metamap b, r_data_main c where a.meta_attr_name='registered_by' and a.meta_attr_value=? and b.object_id = c.data_id and a.meta_id=b.meta_id and to_timestamp(CAST(a.create_ts as double precision)) BETWEEN ? AND ?)";
	
	private static final String USERS_SQL = "select \"USER_ID\" from public.\"HPC_USER\"";
	
	private static final String DOCS_SQL = "select distinct meta_attr_value from public.r_meta_main where meta_attr_name='registered_by_doc'";

	//---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mappers.
	private HpcReportRowMapper reportRowMapper = new HpcReportRowMapper();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcReportsDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcEventDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	private String getUsersSize(HpcReportCriteria criteria, Date[] dates, Object[] docArg, Object[] docDateArgs)
	{
		Long usersSize = null;
		if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_SQL, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_BY_DATE_SQL, dates, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
			usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_BY_DOC_SQL, docArg, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
			usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_BY_DOC_DATE_SQL, docDateArgs, Long.class);
		
		if(usersSize != null)
			return usersSize.toString();
		else return "0";
	}
	
	private String getTotalDataSize(HpcReportCriteria criteria, Date[] dates, String[] docArg, Object[] docDateArgs, String[] userArg, Object[] userDateArgs)
	{
		Long totalSize = null;
		if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			totalSize = jdbcTemplate.queryForObject(SUM_OF_DATA_SQL, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			totalSize = jdbcTemplate.queryForObject(SUM_OF_DATA_BY_DATE_SQL, dates, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
			totalSize = jdbcTemplate.queryForObject(SUM_OF_DATA_BY_DOC_SQL, docArg, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
			totalSize = jdbcTemplate.queryForObject(SUM_OF_DATA_BY_DOC_DATE_SQL, docDateArgs, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER))
			totalSize = jdbcTemplate.queryForObject(SUM_OF_DATA_BY_USER_SQL, userArg, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
			totalSize = jdbcTemplate.queryForObject(SUM_OF_DATA_BY_USER_DATE_SQL, userDateArgs, Long.class);
		
		if(totalSize != null)
			return totalSize.toString();
		else return "0";

	}

	private String getLargestSize(HpcReportCriteria criteria, Date[] dates, String[] docArg, Object[] docDateArgs, String[] userArg, Object[] userDateArgs)
	{
		Long largestSize = null;
		if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			largestSize = jdbcTemplate.queryForObject(LARGEST_FILE_SQL, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			largestSize = jdbcTemplate.queryForObject(LARGEST_FILE_BY_DATE_SQL, dates, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
			largestSize = jdbcTemplate.queryForObject(LARGEST_FILE_BY_DOC_SQL, docArg, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
			largestSize = jdbcTemplate.queryForObject(LARGEST_FILE_BY_DOC_DATE_SQL, docDateArgs, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER))
			largestSize = jdbcTemplate.queryForObject(LARGEST_FILE_BY_USER_SQL, userArg, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
			largestSize = jdbcTemplate.queryForObject(LARGEST_FILE_BY_USER_DATE_SQL, userDateArgs, Long.class);
		
		if(largestSize != null)
			return largestSize.toString();
		else return "0";
	}

	private String getAverageSize(HpcReportCriteria criteria, Date[] dates, String[] docArg, Object[] docDateArgs, String[] userArg, Object[] userDateArgs)
	{
		Long averageSize = null;
		if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			averageSize = jdbcTemplate.queryForObject(AVERAGE_FILE_SQL, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			averageSize = jdbcTemplate.queryForObject(AVERAGE_FILE_BY_DATE_SQL, dates, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
			averageSize = jdbcTemplate.queryForObject(AVERAGE_FILE_BY_DOC_SQL, docArg, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
			averageSize = jdbcTemplate.queryForObject(AVERAGE_FILE_BY_DOC_DATE_SQL, docDateArgs, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER))
			averageSize = jdbcTemplate.queryForObject(AVERAGE_FILE_BY_USER_SQL, userArg, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
			averageSize = jdbcTemplate.queryForObject(AVERAGE_FILE_BY_USER_DATE_SQL, userDateArgs, Long.class);
		
		if(averageSize != null)
			return averageSize.toString();
		else return "0";
	}

	private String getTotalDataObjSize(HpcReportCriteria criteria, Date[] dates, String[] docArg, Object[] docDateArgs, String[] userArg, Object[] userDateArgs)
	{
		Long dataSize = null;
		if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_SQL, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_DATE_SQL, dates, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_DOC_SQL, docArg, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_DOC_DATE_SQL, docDateArgs, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_USER_SQL, userArg, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
			dataSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_BY_USER_DATE_SQL, userDateArgs, Long.class);
		
		if(dataSize != null)
			return dataSize.toString();
		else return "0";
	}

	private List<Map<String, Object>> getTotalCollectionsSize(HpcReportCriteria criteria, Date[] dates, String[] docArg, Object[] docDateArgs, String[] userArg, Object[] userDateArgs)
	{
		List<Map<String, Object>> list = null;
		if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_SQL);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_AND_DATE_SQL, dates);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_DOC_SQL, docArg);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_DOC_DATE_SQL, docDateArgs);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_USER_SQL, userArg);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
			list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_USER_DATE_SQL, userDateArgs);
		return list;
	}

	private String getTotalMetaAttrCount(HpcReportCriteria criteria, Date[] dates, String[] docArg, Object[] docDateArgs, String[] userArg, Object[] userDateArgs)
	{
		Long metaAttrCount = null;
		if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			metaAttrCount = jdbcTemplate.queryForObject(TOTAL_NUM_OF_META_ATTRS_SQL, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			metaAttrCount = jdbcTemplate.queryForObject(TOTAL_NUM_OF_META_ATTRS_BY_DATE_SQL, dates, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
			metaAttrCount = jdbcTemplate.queryForObject(TOTAL_NUM_OF_META_ATTRS_BY_DOC_SQL, docArg, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
			metaAttrCount = jdbcTemplate.queryForObject(TOTAL_NUM_OF_META_ATTRS_BY_DOC_DATE_SQL, docDateArgs, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER))
			metaAttrCount = jdbcTemplate.queryForObject(TOTAL_NUM_OF_META_ATTRS_BY_USER_SQL, userArg, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
			metaAttrCount = jdbcTemplate.queryForObject(TOTAL_NUM_OF_META_ATTRS_BY_USER_DATE_SQL, userDateArgs, Long.class);
		
		if(metaAttrCount != null)
			return metaAttrCount.toString();
		else return "0";
	}
	
	private String getFileSize(HpcReportCriteria criteria, Object[] fileSizeArgs, Object[] filesizedateArgs, Object[] filesizedocArgs, Object[] filesizedocDateArgs, Object[] filesizeuserArgs, Object[] filesizeuserDateArgs)
	{
		Long fileSize = null;
		if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
			fileSize = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_SQL, fileSizeArgs, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			fileSize = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_BY_DATE_SQL, filesizedateArgs, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
			fileSize = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_BY_DOC_SQL, filesizedocArgs, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
			fileSize = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_BY_DOC_DATE_SQL, filesizedocDateArgs, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER))
			fileSize = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_BY_USER_SQL, filesizeuserArgs, Long.class);
		else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
			fileSize = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_BY_USER_DATE_SQL, filesizeuserDateArgs, Long.class);
		
		if(fileSize != null)
			return fileSize.toString();
		else return "0";
	}	
	
	public List<HpcReport> generatReport(HpcReportCriteria criteria)
	{
		List<HpcReport> reports = new ArrayList<HpcReport>();
		
		if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY) || criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
			reports.add(getReport(criteria));
		
		if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC) || criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
		{
			if(criteria.getDocs().isEmpty())
				criteria.getDocs().addAll(getDocs());
			
			List<String> docs = new ArrayList<String>();
			docs.addAll(criteria.getDocs());
			
			for(String doc : docs)
			{
				criteria.getDocs().clear();
				criteria.getDocs().add(doc);
				HpcReport report = getReport(criteria);
				reports.add(report);
			}
		}
			
		if((criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER) || criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE)))
		{
				if(criteria.getUsers().isEmpty())
					criteria.getUsers().addAll(getUsers());
				
				List<String> users = new ArrayList<String>();
				users.addAll(criteria.getUsers());
				
				for(String user : users)
				{
					criteria.getUsers().clear();
					criteria.getUsers().add(user);
					HpcReport report = getReport(criteria);
					reports.add(report);
				}
		}
		return reports;
	}

	private List<String> getUsers()
	{
		return jdbcTemplate.queryForList(USERS_SQL, String.class);
	}
	
	private List<String> getDocs()
	{
		return jdbcTemplate.queryForList(DOCS_SQL, String.class);
	}

	public HpcReport getReport(HpcReportCriteria criteria)
	{
		List<HpcReport> reports = new ArrayList<HpcReport>();
		HpcReport report = new HpcReport();

		//Total Users
		Date fromDate = null;
		Date toDate = null;
		if(criteria.getFromDate() != null && criteria.getToDate() != null)
		{
			fromDate = criteria.getFromDate().getTime();
			toDate = criteria.getToDate().getTime();
		}
		Date[] dateArgs = new Date[2];
		dateArgs[0] = fromDate;
		dateArgs[1] = toDate;

		String[] docArg = new String[1];
		if(criteria.getDocs() != null && criteria.getDocs().size()>0)
			docArg[0] = criteria.getDocs().get(0);
		
		Object[] docDateArgs = new Object[3];
		if(criteria.getDocs() != null && criteria.getDocs().size()>0)
		{
			docDateArgs[0] = criteria.getDocs().get(0);
			docDateArgs[1] = fromDate;
			docDateArgs[2] = toDate;
		}
		
		String[] userArg = new String[1];
		if(criteria.getUsers() != null && criteria.getUsers().size()>0)
			userArg[0] = criteria.getUsers().get(0);
		
		Object[] userDateArgs = new Object[3];
		if(criteria.getUsers() != null && criteria.getUsers().size()>0)
		{
			userDateArgs[0] = criteria.getUsers().get(0);
			userDateArgs[1] = fromDate;
			userDateArgs[2] = toDate;
		}
		
		//TOTAL_NUM_OF_REGISTERED_USERS
		HpcReportEntry userSizeEntry = new HpcReportEntry();
		userSizeEntry.setAttribute(HpcReportEntryAttribute.TOTAL_NUM_OF_REGISTERED_USERS);
		userSizeEntry.setValue(getUsersSize(criteria, dateArgs, docArg, docDateArgs));

		
		//Total Size - TOTAL_DATA_SIZE
		HpcReportEntry sizeEntry = new HpcReportEntry();
		sizeEntry.setAttribute(HpcReportEntryAttribute.TOTAL_DATA_SIZE);
		sizeEntry.setValue(getTotalDataSize(criteria, dateArgs, docArg, docDateArgs, userArg, userDateArgs));
		
		//Largest file - LARGEST_FILE_SIZE
		HpcReportEntry largestFileSizeEntry = new HpcReportEntry();
		largestFileSizeEntry.setAttribute(HpcReportEntryAttribute.LARGEST_FILE_SIZE);
		largestFileSizeEntry.setValue(getLargestSize(criteria, dateArgs, docArg, docDateArgs, userArg, userDateArgs));
		
		//Average file - AVERAGE_FILE_SIZE
		HpcReportEntry averageFileSizeEntry = new HpcReportEntry();
		averageFileSizeEntry.setAttribute(HpcReportEntryAttribute.AVERAGE_FILE_SIZE);
		averageFileSizeEntry.setValue(getAverageSize(criteria, dateArgs, docArg, docDateArgs, userArg, userDateArgs));

		//Total number of data objects - TOTAL_NUM_OF_DATA_OBJECTS
		HpcReportEntry numOfDataObjEntry = new HpcReportEntry();
		numOfDataObjEntry.setAttribute(HpcReportEntryAttribute.TOTAL_NUM_OF_DATA_OBJECTS);
		numOfDataObjEntry.setValue(getTotalDataObjSize(criteria, dateArgs, docArg, docDateArgs, userArg, userDateArgs));

		//Total number of collections - TOTAL_NUM_OF_COLLECTIONS
		List<Map<String, Object>> list = getTotalCollectionsSize(criteria, dateArgs, docArg, docDateArgs, userArg, userDateArgs);
		StringBuffer str = new StringBuffer();
        str.append("\n");
        if(list != null)
        {
                for(Map<String, Object> listEntry : list)
                {
                        String type = null;
                        String count = null;
                        Iterator<String> iter = listEntry.keySet().iterator();
                        while(iter.hasNext())
                        {
                                String name = iter.next();
                                if(name.equals("cnt"))
                                {
                                        Long value = (Long)listEntry.get(name);
                                        count = value.toString();
                                } else
                                        type = (String) listEntry.get(name);
                        }
                        str.append("\t"+type + ": "+ count + "\n");
                }
        }
		HpcReportEntry numOfCollEntry = new HpcReportEntry();
		numOfCollEntry.setAttribute(HpcReportEntryAttribute.TOTAL_NUM_OF_COLLECTIONS);
		numOfCollEntry.setValue(str.toString());

		//Total Meta attributes Size - TOTAL_NUMBER_OF_META_ATTRS
		HpcReportEntry metasizeEntry = new HpcReportEntry();
		metasizeEntry.setAttribute(HpcReportEntryAttribute.TOTAL_NUMBER_OF_META_ATTRS);
		metasizeEntry.setValue(getTotalMetaAttrCount(criteria, dateArgs, docArg, docDateArgs, userArg, userDateArgs));
		
		//Distribution of files - FILE_SIZE_BELOW_1_MB
		Long lower = Long.valueOf(0);
		Long upper = Long.valueOf(1000000);

		Object[] fileSizeArgs  = new Object[2];
		fileSizeArgs[0] = lower;
		fileSizeArgs[1] = upper;

		Object[] filesizedateArgs = new Object[4];
		filesizedateArgs[0] = fromDate;
		filesizedateArgs[1] = toDate;
		filesizedateArgs[2] = lower;
		filesizedateArgs[3] = upper;

		Object[] filesizedocArgs = new Object[3];
		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;
		if(criteria.getDocs() != null && criteria.getDocs().size()>0)
			filesizedateArgs[2] = criteria.getDocs().get(0);

		Object[] filesizedocDateArgs = new Object[5];
		filesizedocDateArgs[0] = lower;
		filesizedocDateArgs[1] = upper;
		if(criteria.getDocs() != null && criteria.getDocs().size()>0)
			filesizedocDateArgs[2] = criteria.getDocs().get(0);
		filesizedocDateArgs[3] = fromDate;
		filesizedocDateArgs[4] = toDate;

		Object[] filesizeuserArgs = new Object[3];
		filesizeuserArgs[0] = lower;
		filesizeuserArgs[1] = upper;
		if(criteria.getUsers() != null && criteria.getUsers().size()>0)
			filesizeuserArgs[2] = criteria.getUsers().get(0);

		Object[] filesizeuserDateArgs = new Object[5];
		filesizeuserDateArgs[0] = lower;
		filesizeuserDateArgs[1] = upper;
		if(criteria.getUsers() != null && criteria.getUsers().size()>0)
			filesizeuserDateArgs[2] = criteria.getUsers().get(0);
		filesizeuserDateArgs[3] = fromDate;
		filesizeuserDateArgs[4] = toDate;
		
		HpcReportEntry oneMBEntry = new HpcReportEntry();
		oneMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_BELOW_1_MB);
		oneMBEntry.setValue(getFileSize(criteria, fileSizeArgs, filesizedateArgs, filesizedocArgs, filesizedocDateArgs, filesizeuserArgs, filesizeuserDateArgs));
		
		lower = Long.valueOf(1000000);
		upper = Long.valueOf(10000000);
		fileSizeArgs[0] = lower;
		fileSizeArgs[1] = upper;

		filesizedateArgs[2] = lower;
		filesizedateArgs[3] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizeuserArgs[0] = lower;
		filesizeuserArgs[1] = upper;

		filesizeuserDateArgs[0] = lower;
		filesizeuserDateArgs[1] = upper;

		HpcReportEntry tenMBEntry = new HpcReportEntry();
		tenMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_1_MB_10_MB);
		tenMBEntry.setValue(getFileSize(criteria, fileSizeArgs, filesizedateArgs, filesizedocArgs, filesizedocDateArgs, filesizeuserArgs, filesizeuserDateArgs));
		
		lower = Long.valueOf(10000000);
		upper = Long.valueOf(50000000);
		fileSizeArgs[0] = lower;
		fileSizeArgs[1] = upper;

		filesizedateArgs[2] = lower;
		filesizedateArgs[3] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizeuserArgs[0] = lower;
		filesizeuserArgs[1] = upper;

		filesizeuserDateArgs[0] = lower;
		filesizeuserDateArgs[1] = upper;

		HpcReportEntry fiftyMBEntry = new HpcReportEntry();
		fiftyMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_10_MB_50_MB);
		fiftyMBEntry.setValue(getFileSize(criteria, fileSizeArgs, filesizedateArgs, filesizedocArgs, filesizedocDateArgs, filesizeuserArgs, filesizeuserDateArgs));

		lower = Long.valueOf(50000000);
		upper = Long.valueOf(100000000);
		fileSizeArgs[0] = lower;
		fileSizeArgs[1] = upper;

		filesizedateArgs[2] = lower;
		filesizedateArgs[3] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizeuserArgs[0] = lower;
		filesizeuserArgs[1] = upper;

		filesizeuserDateArgs[0] = lower;
		filesizeuserDateArgs[1] = upper;
		HpcReportEntry hundredMBEntry = new HpcReportEntry();
		hundredMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_50_MB_100_MB);
		hundredMBEntry.setValue(getFileSize(criteria, fileSizeArgs, filesizedateArgs, filesizedocArgs, filesizedocDateArgs, filesizeuserArgs, filesizeuserDateArgs));

		lower = Long.valueOf(100000000);
		upper = Long.valueOf(500000000);
		fileSizeArgs[0] = lower;
		fileSizeArgs[1] = upper;

		filesizedateArgs[2] = lower;
		filesizedateArgs[3] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizeuserArgs[0] = lower;
		filesizeuserArgs[1] = upper;

		filesizeuserDateArgs[0] = lower;
		filesizeuserDateArgs[1] = upper;
		HpcReportEntry fivehundredMBEntry = new HpcReportEntry();
		fivehundredMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_100_MB_500_MB);
		fivehundredMBEntry.setValue(getFileSize(criteria, fileSizeArgs, filesizedateArgs, filesizedocArgs, filesizedocDateArgs, filesizeuserArgs, filesizeuserDateArgs));

		lower = Long.valueOf(500000000);
		upper = Long.valueOf(1000000000);
		fileSizeArgs[0] = lower;
		fileSizeArgs[1] = upper;

		filesizedateArgs[2] = lower;
		filesizedateArgs[3] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizeuserArgs[0] = lower;
		filesizeuserArgs[1] = upper;

		filesizeuserDateArgs[0] = lower;
		filesizeuserDateArgs[1] = upper;
		HpcReportEntry onegbEntry = new HpcReportEntry();
		onegbEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_500_MB_1_GB);
		onegbEntry.setValue(getFileSize(criteria, fileSizeArgs, filesizedateArgs, filesizedocArgs, filesizedocDateArgs, filesizeuserArgs, filesizeuserDateArgs));

		lower = Long.valueOf(1000000000);
		upper = new Long("10000000000");
		fileSizeArgs[0] = lower;
		fileSizeArgs[1] = upper;

		filesizedateArgs[2] = lower;
		filesizedateArgs[3] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizeuserArgs[0] = lower;
		filesizeuserArgs[1] = upper;

		filesizeuserDateArgs[0] = lower;
		filesizeuserDateArgs[1] = upper;
		HpcReportEntry tengbEntry = new HpcReportEntry();
		tengbEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_1_GB_10_GB);
		tengbEntry.setValue(getFileSize(criteria, fileSizeArgs, filesizedateArgs, filesizedocArgs, filesizedocDateArgs, filesizeuserArgs, filesizeuserDateArgs));

		lower = new Long("10000000000");
		upper = new Long("100000000000000");
		fileSizeArgs[0] = lower;
		fileSizeArgs[1] = upper;

		filesizedateArgs[2] = lower;
		filesizedateArgs[3] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizedocArgs[0] = lower;
		filesizedocArgs[1] = upper;

		filesizeuserArgs[0] = lower;
		filesizeuserArgs[1] = upper;

		filesizeuserDateArgs[0] = lower;
		filesizeuserDateArgs[1] = upper;
		HpcReportEntry overtengbEntry = new HpcReportEntry();
		overtengbEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_OVER_10_GB);
		overtengbEntry.setValue(getFileSize(criteria, fileSizeArgs, filesizedateArgs, filesizedocArgs, filesizedocDateArgs, filesizeuserArgs, filesizeuserDateArgs));

		report.setGeneratedOn(Calendar.getInstance());
		if(criteria.getDocs() != null && criteria.getDocs().size()>0)
			report.setDoc(criteria.getDocs().get(0));
		if(criteria.getUsers() != null && criteria.getUsers().size()>0)
			report.setUser(criteria.getUsers().get(0));
		report.setType(criteria.getType());
		if(criteria.getFromDate() != null)
			report.setFromDate(criteria.getFromDate());
		if(criteria.getToDate() != null)
			report.setToDate(criteria.getToDate());
		
		if(!(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER) || criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE)))
			report.getReportEntries().add(userSizeEntry);
		report.getReportEntries().add(sizeEntry);
		report.getReportEntries().add(largestFileSizeEntry);
		report.getReportEntries().add(averageFileSizeEntry);
		report.getReportEntries().add(numOfDataObjEntry);
		report.getReportEntries().add(numOfCollEntry);
		report.getReportEntries().add(metasizeEntry);
		report.getReportEntries().add(oneMBEntry);
		report.getReportEntries().add(tenMBEntry);
		report.getReportEntries().add(fiftyMBEntry);
		report.getReportEntries().add(hundredMBEntry);
		report.getReportEntries().add(fivehundredMBEntry);
		report.getReportEntries().add(onegbEntry);
		report.getReportEntries().add(tengbEntry);
		report.getReportEntries().add(overtengbEntry);
		
		return report;
	}
	private HpcReport generateUsageSummaryReportByDateRange(HpcReportCriteria criteria)
	{
		return jdbcTemplate.queryForObject(SUM_OF_DATA_SQL, reportRowMapper);
	}

	private HpcReport generateUsageSummaryByDocReport(HpcReportCriteria criteria)
	{
		return jdbcTemplate.queryForObject(SUM_OF_DATA_SQL, reportRowMapper);
	}

	private HpcReport generateUsageSummaryByDocDateRangeReport(HpcReportCriteria criteria)
	{
		return jdbcTemplate.queryForObject(SUM_OF_DATA_SQL, reportRowMapper);
	}

	private HpcReport generateUsageSummaryByUserReport(HpcReportCriteria criteria)
	{
		return jdbcTemplate.queryForObject(SUM_OF_DATA_SQL, reportRowMapper);
	}

	private HpcReport generateUsageSummaryByUserDateRangeReport(HpcReportCriteria criteria)
	{
		return jdbcTemplate.queryForObject(SUM_OF_DATA_SQL, reportRowMapper);
	}
	
	// HpcEvent Row to Object mapper.
	private class HpcReportRowMapper implements RowMapper<HpcReport>
	{
		@Override
		public HpcReport mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcReport report = new HpcReport();
            
            return report;
		}
	}
}

 