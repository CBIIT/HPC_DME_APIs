/**
 * HpcMetadataDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import gov.nih.nci.hpc.dao.HpcMetadataDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * <p>
 * HPC Metadata DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcMetadataDAOImpl implements HpcMetadataDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	public static final String ASSOCIATE_METADATA_SQL = 
		   "insert into public.\"r_objt_metamap\" ( " +
                   "\"object_id\", \"meta_id\", \"create_ts\", \"modify_ts\" ) " +
                   "values (?, ?, ?, ?) " +
           "on conflict(\"object_id\", \"meta_id\") do nothing";

    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcMetadataDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcMetadataDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void associateMetadata(int objectId, int metadataId) throws HpcException
    {
		try {
			 String now = String.valueOf((new Date()).getTime());
		     jdbcTemplate.update(ASSOCIATE_METADATA_SQL, objectId, metadataId, now, now);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to associate metadata: " + e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, e);
		}
    }
}

 