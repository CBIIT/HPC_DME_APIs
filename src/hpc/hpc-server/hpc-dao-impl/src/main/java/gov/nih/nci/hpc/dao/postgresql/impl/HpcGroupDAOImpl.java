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
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;

/**
 * <p>
 * HPC Group DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id:$
 */

public class HpcGroupDAOImpl implements HpcGroupDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	private static final String GET_GROUPS_SQL = "select user_name from public.r_user_main where user_type_name = 'rodsgroup'";
    
	private static final String GET_GROUPS_GROUP_NAME_PATTERN_FILTER = " and lower(user_name) like lower(?) ";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mapper.
	private SingleColumnRowMapper<String> rowMapper = new SingleColumnRowMapper<>();
	
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
		    	    	               HpcErrorType.DATABASE_ERROR, e);
		}		
    }
}

 