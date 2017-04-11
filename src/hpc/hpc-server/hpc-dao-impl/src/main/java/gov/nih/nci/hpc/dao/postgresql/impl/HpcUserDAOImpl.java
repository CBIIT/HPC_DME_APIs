/**
 * HpcUserDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * <p>
 * HPC User DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcUserDAOImpl implements HpcUserDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	private static final String UPSERT_USER_SQL = 
		    "insert into public.\"HPC_USER\" ( " +
                    "\"USER_ID\", \"FIRST_NAME\", \"LAST_NAME\", \"DOC\", " +
                    "\"ACTIVE\", \"CREATED\", \"LAST_UPDATED\", \"ACTIVE_UPDATED_BY\") " +
                    "values (?, ?, ?, ?, ?, ?, ?, ?) " +
            "on conflict(\"USER_ID\") do update set \"FIRST_NAME\"=excluded.\"FIRST_NAME\", " +
                                                   "\"DOC\"=excluded.\"DOC\", " +
                                                   "\"LAST_NAME\"=excluded.\"LAST_NAME\", " +
                                                   "\"ACTIVE\"=excluded.\"ACTIVE\", " +
                                                   "\"ACTIVE_UPDATED_BY\"=excluded.\"ACTIVE_UPDATED_BY\", " +
                                                   "\"CREATED\"=excluded.\"CREATED\", " +
                                                   "\"LAST_UPDATED\"=excluded.\"LAST_UPDATED\"";
	
	private static final String GET_USER_SQL = "select * from public.\"HPC_USER\" where \"USER_ID\" = ?";

	private static final String GET_USERS_SQL = "select * from public.\"HPC_USER\" where ?";
    
	private static final String GET_USERS_USER_ID_FILTER = " and lower(\"USER_ID\") = lower(?) ";
    
	private static final String GET_USERS_FIRST_NAME_FILTER = " and lower(\"FIRST_NAME\") like lower(?) ";
    
	private static final String GET_USERS_LAST_NAME_FILTER = " and lower(\"LAST_NAME\") like lower(?) ";
	
	private static final String GET_USERS_ACTIVE_FILTER = " and \"ACTIVE\" = true ";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Encryptor.
	@Autowired
	HpcEncryptor encryptor = null;
	
	// Row mapper.
	private HpcUserRowMapper rowMapper = new HpcUserRowMapper();
	
    // The logger instance.
	private static final Logger logger = 
			LoggerFactory.getLogger(HpcUserDAOImpl.class.getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcUserDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcUserDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void upsertUser(HpcUser user) throws HpcException
    {
		try {
		     jdbcTemplate.update(UPSERT_USER_SQL,
		                         user.getNciAccount().getUserId(),
		                         user.getNciAccount().getFirstName(),
		                         user.getNciAccount().getLastName(),
		                         user.getNciAccount().getDoc(),
		                         user.getActive(),
		                         user.getCreated(),
		                         user.getLastUpdated(),
		                         user.getActiveUpdatedBy());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a user: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}
    }
	
	@Override 
	public HpcUser getUser(String nciUserId) throws HpcException
	{
		try {
		     return jdbcTemplate.queryForObject(GET_USER_SQL, rowMapper, nciUserId);
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    logger.error("Multiple users with the same ID found", irse);
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a user: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}
	}
	
	public List<HpcUser> getUsers(String nciUserId, String firstName, String lastName, boolean active) 
                                 throws HpcException
    {
		// Build the query based on provided search criteria.
		StringBuilder sqlQueryBuilder = new StringBuilder();
    	List<Object> args = new ArrayList<>();
    	
    	sqlQueryBuilder.append(GET_USERS_SQL);
    	args.add(true);
    	
    	if(nciUserId != null) {
    	   sqlQueryBuilder.append(GET_USERS_USER_ID_FILTER);
    	   args.add(nciUserId);
    	}
    	if(firstName != null) {
     	   sqlQueryBuilder.append(GET_USERS_FIRST_NAME_FILTER);
     	   args.add(firstName);
     	}
    	if(lastName != null) {
     	   sqlQueryBuilder.append(GET_USERS_LAST_NAME_FILTER);
     	   args.add(lastName);
     	}
    	if(active) {
      	   sqlQueryBuilder.append(GET_USERS_ACTIVE_FILTER);
      	}
    	
		try {
		     return jdbcTemplate.query(sqlQueryBuilder.toString(), rowMapper, args.toArray());
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get users: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}		
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
	// HpcUser Table to Object mapper.
	private class HpcUserRowMapper implements RowMapper<HpcUser>
	{
		@Override
		public HpcUser mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcNciAccount nciAccount = new HpcNciAccount();
			nciAccount.setUserId(rs.getString("USER_ID"));
			nciAccount.setFirstName(rs.getString("FIRST_NAME"));
			nciAccount.setLastName(rs.getString("LAST_NAME"));
			nciAccount.setDoc(rs.getString("DOC"));
			
        	HpcUser user = new HpcUser();
        	Calendar created = new GregorianCalendar();
        	created.setTime(rs.getDate("CREATED"));
        	user.setCreated(created);
        	
        	Calendar lastUpdated = new GregorianCalendar();
        	lastUpdated.setTime(rs.getDate("LAST_UPDATED"));
        	user.setLastUpdated(lastUpdated);
        	
        	user.setActive(rs.getBoolean("ACTIVE"));
        	user.setActiveUpdatedBy(rs.getString("ACTIVE_UPDATED_BY"));
        	
        	user.setNciAccount(nciAccount);
            
            return user;
		}
	}
	
    /**
     * Verify connection to DB. (Invoked by spring init-method).
     * 
     * @throws HpcException If it failed to connect to the database.
     */
	@SuppressWarnings("unused")
	private void dbConnect() throws HpcException
    {
    	try {
    	     jdbcTemplate.getDataSource().getConnection();
    	     
    	} catch(Exception e) {
    		    throw new HpcException(
    		    		     "Failed to connect to PostgreSQL DB. Check credentials config", 
    		    		     HpcErrorType.DATABASE_ERROR, e);
    	}
    } 
}

 