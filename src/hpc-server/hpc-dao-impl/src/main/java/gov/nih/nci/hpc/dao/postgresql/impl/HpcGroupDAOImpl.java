/**
 * HpcGroupDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import gov.nih.nci.hpc.dao.HpcGroupDAO;
import gov.nih.nci.hpc.domain.model.HpcGroup;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.RowMapper;

/**
 * <p>
 * HPC Group DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcGroupDAOImpl implements HpcGroupDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    // SQL Queries.
	private static final String UPSERT_GROUP_SQL =
	"insert into public.\"HPC_GROUP\" ( " +
	         "\"GROUP_NAME\", \"DOC\", " +
	         "\"ACTIVE\", \"CREATED\", \"LAST_UPDATED\", \"ACTIVE_UPDATED_BY\") " +
	         "values (?, ?, ?, ?, ?, ?) " +
	         "on conflict(\"GROUP_NAME\") do update set \"DOC\"=excluded.\"DOC\", " +
	                                                   "\"ACTIVE\"=excluded.\"ACTIVE\", " +
	                                                   "\"ACTIVE_UPDATED_BY\"=excluded.\"ACTIVE_UPDATED_BY\", " +
	                                                   "\"CREATED\"=excluded.\"CREATED\", " +
	                                                   "\"LAST_UPDATED\"=excluded.\"LAST_UPDATED\"";

	 private static final String UPDATE_GROUP_SQL =
	"update public.\"HPC_GROUP\" set "
			        + "\"ACTIVE\" = ?, \"LAST_UPDATED\" = ?, \"ACTIVE_UPDATED_BY\" = ? "
			        + " where \"GROUP_NAME\" = ?";


	private static final String DELETE_GROUP_SQL = "delete from public.\"HPC_GROUP\" where \"GROUP_NAME\" = ?";

	private static final String GET_GROUP_SQL = "select * from public.\"HPC_GROUP\" where \"GROUP_NAME\" = ?";

	private static final String GET_GROUPS_SQL = "select user_name from public.r_user_main where " +
                                                 "user_type_name = 'rodsgroup' and user_name <> 'rodsadmin'";
	
	//Get all groups to which the given user belongs
	private static final String GET_USER_GROUPS_SQL ="select m.user_name from r_user_main m, r_user_group g, r_user_main u "
	                                                 + "where m.user_type_name = 'rodsgroup' and "
	                                                 + "m.user_id = g.group_user_id and "
	                                                 + "g.user_id = u.user_id and "
			                                         + "u.user_name = ?";
	
    
	private static final String GET_GROUPS_GROUP_NAME_PATTERN_FILTER = " and lower(user_name) like lower(?) ";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mapper.
	private SingleColumnRowMapper<String> rowMapper = new SingleColumnRowMapper<>();
	
	// Row mapper.
	private RowMapper<HpcGroup> groupRowMapper = (rs, rowNum) ->
	{
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
	private static final Logger logger =
			LoggerFactory.getLogger(HpcGroupDAOImpl.class.getName());

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcGroupDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcGroupDAO Interface Implementation
    //---------------------------------------------------------------------//  

    @Override
	public void upsertGroup(HpcGroup group) throws HpcException
    {
		try {
		     jdbcTemplate.update(UPSERT_GROUP_SQL,
		                         group.getName(),
		                         group.getDoc(),
		                         group.getActive(),
		                         group.getCreated(),
		                         group.getLastUpdated(),
		                         group.getActiveUpdatedBy());

		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a user: " + e.getMessage(),
			        HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
    }


    @Override
    public void updateGroup(HpcGroup group) throws HpcException {
      try {
        jdbcTemplate.update(
            UPDATE_GROUP_SQL,
            group.getActive(),
            group.getLastUpdated(),
            group.getActiveUpdatedBy(),
            group.getName());

      } catch (DataAccessException e) {
        throw new HpcException(
            "Failed to update a group: " + group.getName(),
            HpcErrorType.DATABASE_ERROR,
            HpcIntegratedSystem.POSTGRESQL,
            e);
      }
    }

    @Override
	public void deleteGroup(String name) throws HpcException
    {
        try {
            jdbcTemplate.update(DELETE_GROUP_SQL, name);

        } catch(DataAccessException e) {
            throw new HpcException("Failed to delete group " + name + ": " + e.getMessage(),
                HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
        }
    }


	@Override
	public HpcGroup getGroup(String name) throws HpcException
	{
		try {
		     return jdbcTemplate.queryForObject(GET_GROUP_SQL, groupRowMapper, name);
		} catch(IncorrectResultSizeDataAccessException e) {
			    logger.error("Multiple groups with the same name found", e);
			    return null;
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a group: " + e.getMessage(),
		            HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}


	@Override
	public List<String> getGroups(String groupPattern) throws HpcException
    {
		// Build the query based on provided search criteria.
		StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();

    	sqlQueryBuilder.append(GET_GROUPS_SQL);

    	if(groupPattern != null) {
     	   sqlQueryBuilder.append(GET_GROUPS_GROUP_NAME_PATTERN_FILTER);
     	   args.add(groupPattern);
     	}

		try {
		     return jdbcTemplate.query(sqlQueryBuilder.toString(), rowMapper, args.toArray());
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get groups: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}		
    }
	
	
	@Override
	public List<String> getUserGroups(String userId) throws HpcException
    {
		// Build the query based on provided search criteria.
		StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    	
    	sqlQueryBuilder.append(GET_USER_GROUPS_SQL);
    	args.add(userId);
    	
    	
		try {
		     return jdbcTemplate.queryForList(sqlQueryBuilder.toString(), String.class, args.toArray());
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get groups for user: " + userId + ": " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}		
    }

}

 