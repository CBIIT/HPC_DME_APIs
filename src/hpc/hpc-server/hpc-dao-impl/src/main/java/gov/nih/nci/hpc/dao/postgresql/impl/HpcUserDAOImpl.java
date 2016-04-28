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
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;

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
	public final static String INSERT_SQL = 
		   "insert into public.\"HPC_USER\" ( " +
                    "\"USER_ID\", \"FIRST_NAME\", \"LAST_NAME\", \"DOC\", " +
                    "\"IRODS_USERNAME\", \"IRODS_PASSWORD\", " +
                    "\"CREATED\", \"LAST_UPDATED\") " +
                    "values (?, ?, ?, ?, ?, ?, ?, ?) ";
	
	/* Add this back in when we upgrade to PostgreSQL 9.5
            + "on conflict(\"USER_ID\") do update set \"FIRST_NAME\"=excluded.\"FIRST_NAME\", \"DOC\", " +
                                                  "\"LAST_NAME\"=excluded.\"LAST_NAME\", " +
                                                  "\"IRODS_USERNAME\"=excluded.\"IRODS_USERNAME\", " +
                                                  "\"IRODS_PASSWORD\"=excluded.\"IRODS_PASSWORD\", " +
                                                  "\"CREATED\"=excluded.\"CREATED\", " +
                                                  "\"LAST_UPDATED\"=excluded.\"LAST_UPDATED\"";*/

	public final static String GET_SQL = "select * from public.\"HPC_USER\" where \"USER_ID\" = ?";
	
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
    // HpcManagedUserDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void insert(HpcUser user) throws HpcException
    {
		try {
		     jdbcTemplate.update(INSERT_SQL,
		                         user.getNciAccount().getUserId(),
		                         user.getNciAccount().getFirstName(),
		                         user.getNciAccount().getLastName(),
		                         user.getNciAccount().getDOC(),
		                         user.getDataManagementAccount().getUsername(),
		                         encryptor.encrypt(user.getDataManagementAccount().getPassword()),
		                         user.getCreated(),
		                         user.getLastUpdated());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a user: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}
    }
	
	@Override
	public void update(HpcUser user) throws HpcException
    {
		try {
			String UPDATE_SQL = 
					   "update public.\"HPC_USER\" set " +
			                    "\"FIRST_NAME\"=?, \"LAST_NAME\"=?, \"DOC\"=?, ";
			if(user.getDataManagementAccount() != null)
				UPDATE_SQL = UPDATE_SQL + "\"IRODS_USERNAME\"=?, \"IRODS_PASSWORD\"=?, ";
			UPDATE_SQL = UPDATE_SQL + "\"LAST_UPDATED\" =? " +
				"WHERE \"USER_ID\"=?";
			
			if(user.getDataManagementAccount() != null)
		     jdbcTemplate.update(UPDATE_SQL,
		                         user.getNciAccount().getFirstName(),
		                         user.getNciAccount().getLastName(),
		                         user.getNciAccount().getDOC(),
		                         user.getDataManagementAccount().getUsername(),
		                         encryptor.encrypt(user.getDataManagementAccount().getPassword()),
		                         user.getLastUpdated(),
		                         user.getNciAccount().getUserId());
			else
			     jdbcTemplate.update(UPDATE_SQL,
                         user.getNciAccount().getFirstName(),
                         user.getNciAccount().getLastName(),
                         user.getNciAccount().getDOC(),
                         user.getLastUpdated(),
                         user.getNciAccount().getUserId());
				
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a user: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}
    }

	@Override 
	public HpcUser getUser(String nciUserId) throws HpcException
	{
		try {
		     return jdbcTemplate.queryForObject(GET_SQL, rowMapper, nciUserId);
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a user: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
	// HpcUser Table to Object mapper.
	private class HpcUserRowMapper implements RowMapper<HpcUser>
	{
		public HpcUser mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcNciAccount nciAccount = new HpcNciAccount();
			nciAccount.setUserId(rs.getString("USER_ID"));
			nciAccount.setFirstName(rs.getString("FIRST_NAME"));
			nciAccount.setLastName(rs.getString("LAST_NAME"));
			nciAccount.setDOC(rs.getString("DOC"));
			
			HpcIntegratedSystemAccount dataManagementAccount = new HpcIntegratedSystemAccount();
			dataManagementAccount.setIntegratedSystem(HpcIntegratedSystem.IRODS);
			dataManagementAccount.setUsername(rs.getString("IRODS_USERNAME"));
			dataManagementAccount.setPassword(encryptor.decrypt(rs.getBytes(("IRODS_PASSWORD"))));
			
        	HpcUser user = new HpcUser();
        	Calendar created = new GregorianCalendar();
        	created.setTime(rs.getDate("CREATED"));
        	user.setCreated(created);
        	
        	Calendar lastUpdated = new GregorianCalendar();
        	lastUpdated.setTime(rs.getDate("LAST_UPDATED"));
        	user.setLastUpdated(lastUpdated);
        	
        	user.setNciAccount(nciAccount);
        	user.setDataManagementAccount(dataManagementAccount);
            
            return user;
		}
	}
	
    /**
     * Verify connection to DB. (Invoked by spring init-method).
     * 
     * Throws HpcException If it failed to connect to the database.
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

 