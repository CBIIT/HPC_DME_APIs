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
    
	private static final String SUM_OF_DATA_DATE_RANGE_SQL = 
			"SELECT sum(to_number(meta_attr_value, '9999999999999999999')) FROM public.r_meta_main where meta_attr_name = 'source_file_size' and to_timestamp(CAST(create_ts as double precision)) BETWEEN ? AND ?";

	private static final String SUM_OF_DATA_SQL = 
			"SELECT sum(to_number(meta_attr_value, '9999999999999999999')) totalSize FROM public.r_meta_main where meta_attr_name = 'source_file_size'";

	private static final String LARGEST_FILE_SQL = 
			"SELECT max(to_number(meta_attr_value, '9999999999999999999')) maxSize FROM public.r_meta_main where meta_attr_name = 'source_file_size'";
	
	private static final String AVERAGE_FILE_SQL = 
			"SELECT avg(to_number(meta_attr_value, '9999999999999999999')) maxSize FROM public.r_meta_main where meta_attr_name = 'source_file_size'";

	private static final String TOTAL_NUM_OF_USERS_SQL = 
			"SELECT count(*) totalUsers FROM public.\"HPC_USER\"";

	private static final String TOTAL_NUM_OF_DATA_OBJECTS_SQL = 
			"SELECT count(*) totalObjs FROM public.r_data_main";

	private static final String TOTAL_NUM_OF_COLLECTIONS_SQL = 
			"SELECT count(*) totalcolls FROM public.r_coll_main";

	private static final String TOTAL_NUM_OF_COLLECTIONS_BY_NAME_SQL = 
			"select a.meta_attr_value, count(a.meta_attr_name) from r_meta_main a, r_coll_main b, r_objt_metamap c where b.coll_id=c.object_id and c.meta_id=a.meta_id and a.meta_attr_name='collection_type' group by a.meta_attr_value;";

	private static final String TOTAL_NUM_OF_META_ATTRS_SQL = 
			"SELECT count(*) totalAttrs FROM public.r_meta_main";
	
	private static final String FILE_SIZE_RANGE_SQL = 
			"SELECT count(*) FROM public.r_meta_main where meta_attr_name = 'source_file_size' and to_number(meta_attr_value, '9999999999999999999') BETWEEN ? AND ?";
	
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
    
	@Override
	public HpcReport generatReport(HpcReportCriteria criteria) throws HpcException
    {
		try {
			if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY))
				return generateUsageSummaryReport(criteria);
			else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
				return generateUsageSummaryReportByDateRange(criteria);
			else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
				return generateUsageSummaryByDocReport(criteria);
			else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
				return generateUsageSummaryByDocDateRangeReport(criteria);
			else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER))
				return generateUsageSummaryByUserReport(criteria);
			else if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
				return generateUsageSummaryByUserDateRangeReport(criteria);
			
			return null;
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to generate report: " + 
		                               e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}	    	
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  

	private HpcReport generateUsageSummaryReport(HpcReportCriteria criteria)
	{
		HpcReport report = new HpcReport();
		
		//Total Users
		Integer usersSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_USERS_SQL, Integer.class);
		HpcReportEntry userSizeEntry = new HpcReportEntry();
		userSizeEntry.setAttribute(HpcReportEntryAttribute.TOTAL_NUM_OF_REGISTERED_USERS);
		userSizeEntry.setValue(Integer.toString(usersSize == null ? 0 : usersSize));

		//Total Size
		Long totalSize = jdbcTemplate.queryForObject(SUM_OF_DATA_SQL, Long.class);
		HpcReportEntry sizeEntry = new HpcReportEntry();
		sizeEntry.setAttribute(HpcReportEntryAttribute.TOTAL_DATA_SIZE);
		sizeEntry.setValue(Long.toString(totalSize == null ? 0 : totalSize));
		
		//Largest file
		Long largestFileSize = jdbcTemplate.queryForObject(LARGEST_FILE_SQL, Long.class);
		HpcReportEntry largestFileSizeEntry = new HpcReportEntry();
		largestFileSizeEntry.setAttribute(HpcReportEntryAttribute.LARGEST_FILE_SIZE);
		largestFileSizeEntry.setValue(Long.toString(largestFileSize == null ? 0 : largestFileSize));
		
		//Average file
		Long averageFileSize = jdbcTemplate.queryForObject(AVERAGE_FILE_SQL, Long.class);
		HpcReportEntry averageFileSizeEntry = new HpcReportEntry();
		averageFileSizeEntry.setAttribute(HpcReportEntryAttribute.AVERAGE_FILE_SIZE);
		averageFileSizeEntry.setValue(Long.toString(averageFileSize == null ? 0 : averageFileSize));

		//Total number of data objects
		Long numOfDataObj = jdbcTemplate.queryForObject(TOTAL_NUM_OF_DATA_OBJECTS_SQL, Long.class);
		HpcReportEntry numOfDataObjEntry = new HpcReportEntry();
		numOfDataObjEntry.setAttribute(HpcReportEntryAttribute.TOTAL_NUM_OF_DATA_OBJECTS);
		numOfDataObjEntry.setValue(Long.toString(numOfDataObj == null ? 0 : numOfDataObj));

		//Total number of collections
		List<Map<String, Object>> list = jdbcTemplate.queryForList(TOTAL_NUM_OF_COLLECTIONS_BY_NAME_SQL);
		List<String> entryList = new ArrayList<String>();
		StringBuffer str = new StringBuffer();
		if(list != null)
		{
			for(Map<String, Object> listEntry : list)
			{
				Iterator<String> iter = listEntry.keySet().iterator();
				while(iter.hasNext())
				{
					String name = iter.next();
					String value = (String)listEntry.get(name);
					str.append(name + ": " + value +"\n");
				}
			}
		}
		HpcReportEntry numOfCollEntry = new HpcReportEntry();
		numOfCollEntry.setAttribute(HpcReportEntryAttribute.TOTAL_NUM_OF_COLLECTIONS);
		numOfCollEntry.setValue(str.toString());

		//Total Meta attributes Size
		Long totalMetaSize = jdbcTemplate.queryForObject(TOTAL_NUM_OF_META_ATTRS_SQL, Long.class);
		HpcReportEntry metasizeEntry = new HpcReportEntry();
		metasizeEntry.setAttribute(HpcReportEntryAttribute.TOTAL_NUMBER_OF_META_ATTRS);
		metasizeEntry.setValue(Long.toString(totalMetaSize == null ? 0 : totalMetaSize));
		
		//Distribution of files
		Long[] onembargs = new Long[2];
		onembargs[0] = Long.valueOf(0);
		onembargs[1] = Long.valueOf(1000000);
		Long oneMBCount = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_SQL, Long.class, onembargs );
		HpcReportEntry oneMBEntry = new HpcReportEntry();
		oneMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_BELOW_1_MB);
		oneMBEntry.setValue(Long.toString(oneMBCount == null ? 0 : oneMBCount));
		
		Long[] tenmbargs = new Long[2];
		tenmbargs[0] = Long.valueOf(1000000);
		tenmbargs[1] = Long.valueOf(10000000);
		Long tenMBCount = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_SQL, Long.class, tenmbargs);
		HpcReportEntry tenMBEntry = new HpcReportEntry();
		tenMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_1_MB_10_MB);
		tenMBEntry.setValue(Long.toString(tenMBCount == null ? 0 : tenMBCount));
		
		Long[] fiftymbargs = new Long[2];
		fiftymbargs[0] = Long.valueOf(10000000);
		fiftymbargs[1] = Long.valueOf(50000000);
		Long fiftyMBCount = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_SQL, Long.class, fiftymbargs);
		HpcReportEntry fiftyMBEntry = new HpcReportEntry();
		fiftyMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_10_MB_50_MB);
		fiftyMBEntry.setValue(Long.toString(fiftyMBCount == null ? 0 : fiftyMBCount));

		Long[] hundredmbargs = new Long[2];
		hundredmbargs[0] = Long.valueOf(50000000);
		hundredmbargs[1] = Long.valueOf(100000000);
		Long hundredMBCount = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_SQL, Long.class, hundredmbargs);
		HpcReportEntry hundredMBEntry = new HpcReportEntry();
		hundredMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_50_MB_100_MB);
		hundredMBEntry.setValue(Long.toString(hundredMBCount == null ? 0 : hundredMBCount));

		Long[] fivehundredmbargs = new Long[2];
		fivehundredmbargs[0] = Long.valueOf(100000000);
		fivehundredmbargs[1] = Long.valueOf(500000000);
		Long fivehundredMBCount = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_SQL, Long.class, fivehundredmbargs);
		HpcReportEntry fivehundredMBEntry = new HpcReportEntry();
		fivehundredMBEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_100_MB_500_MB);
		fivehundredMBEntry.setValue(Long.toString(fivehundredMBCount == null ? 0 : fivehundredMBCount));

		Long[] onegbargs = new Long[2];
		onegbargs[0] = Long.valueOf(500000000);
		onegbargs[1] = Long.valueOf(1000000000);
		Long onegbCount = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_SQL, Long.class, onegbargs);
		HpcReportEntry onegbEntry = new HpcReportEntry();
		onegbEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_500_MB_1_GB);
		onegbEntry.setValue(Long.toString(onegbCount == null ? 0 : onegbCount));

		Long[] tengbargs = new Long[2];
		tengbargs[0] = Long.valueOf(1000000000);
		tengbargs[1] = new Long("10000000000");
		Long tengbCount = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_SQL, Long.class, tengbargs);
		HpcReportEntry tengbEntry = new HpcReportEntry();
		tengbEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_1_GB_10_GB);
		tengbEntry.setValue(Long.toString(tengbCount == null ? 0 : tengbCount));

		Long[] overtengbargs = new Long[2];
		overtengbargs[1] = new Long("10000000000");
		overtengbargs[1] = new Long("100000000000000");
		Long overtengbCount = jdbcTemplate.queryForObject(FILE_SIZE_RANGE_SQL, Long.class, overtengbargs);
		HpcReportEntry overtengbEntry = new HpcReportEntry();
		overtengbEntry.setAttribute(HpcReportEntryAttribute.FILE_SIZE_OVER_10_GB);
		overtengbEntry.setValue(Long.toString(overtengbCount == null ? 0 : overtengbCount));

		report.setGeneratedOn(Calendar.getInstance());
		report.setType(criteria.getType());
		
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

 