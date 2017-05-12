/**
 * HpcDocConfigurationDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import gov.nih.nci.hpc.dao.HpcDocConfigurationDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcDocConfiguration;
import gov.nih.nci.hpc.exception.HpcException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * <p>
 * HPC DOC Configuration DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDocConfigurationDAOImpl implements HpcDocConfigurationDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	private static final String GET_DOC_CONFIGURATIONS_SQL = 
			                    "select * from public.\"HPC_DOC_CONFIGURATION\"";
	
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mapper.
	private HpcDocConfigRowMapper rowMapper = new HpcDocConfigRowMapper();
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcDocConfigurationDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDocConfigDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public List<HpcDocConfiguration> getDocConfigurations() throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_DOC_CONFIGURATIONS_SQL, rowMapper);
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get DOC configurations: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}		
    }
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
	
	// HpcUser Table to Object mapper.
	private class HpcDocConfigRowMapper implements RowMapper<HpcDocConfiguration>
	{
		@Override
		public HpcDocConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			HpcDocConfiguration docConfiguration = new HpcDocConfiguration();
			docConfiguration.setDoc(rs.getString("DOC"));
			docConfiguration.setBasePath(rs.getString("BASE_PATH"));

			return docConfiguration;
		}
	}
	
    /**
     * Verify connection to DB. Since this DAO is called at the time the server starts,
     * we are making a DB connection test after this DAO was constructed to ensure DB credentials
     * are configured properly.
     * 
     * @throws HpcException If it failed to connect to the database.
     */
	@PostConstruct
	private void dbConnect() throws HpcException
    {
		logger.error("ERAN: DB CONNECT");
    	try {
    	     jdbcTemplate.getDataSource().getConnection();
    	     
    	} catch(Exception e) {
    		    throw new HpcException(
    		    		     "Failed to connect to PostgreSQL DB. Check credentials config", 
    		    		     HpcErrorType.DATABASE_ERROR, e);
    	}
    } 
}

 