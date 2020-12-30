/**
 * HpcUserBookmarkDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import gov.nih.nci.hpc.dao.HpcUserBookmarkDAO;
import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC User Bookmark DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcUserBookmarkDAOImpl implements HpcUserBookmarkDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
	private static final String UPSERT_USER_BOOKMARK_SQL = 
			"insert into public.\"HPC_USER_BOOKMARK\" ( " +
	                "\"USER_ID\", \"BOOKMARK_NAME\", \"PATH\", \"BOOKMARK_GROUP\", " +
	                "\"CREATED\", \"UPDATED\" ) " +
	                "values (?, ?, ?, ?, ?, ?) " + 
	        "on conflict(\"USER_ID\", \"BOOKMARK_NAME\") do update " +
            "set \"PATH\"=excluded.\"PATH\", \"BOOKMARK_GROUP\"=excluded.\"BOOKMARK_GROUP\", " +
	        "\"CREATED\"=excluded.\"CREATED\", \"UPDATED\"=excluded.\"UPDATED\"";
	        
	private static final String DELETE_USER_BOOKMARK_SQL = 
		    "delete from public.\"HPC_USER_BOOKMARK\" where \"USER_ID\" = ? and \"BOOKMARK_NAME\" = ?";
	
	private static final String GET_USER_BOOKMARKS_SQL = 
		    "select * from public.\"HPC_USER_BOOKMARK\" where \"USER_ID\" = ?";
	
	private static final String GET_USER_BOOKMARK_SQL = 
		    "select * from public.\"HPC_USER_BOOKMARK\" where \"USER_ID\" = ? and \"BOOKMARK_NAME\" = ?";
	
	private static final String GET_USER_BOOKMARKS_BY_PATH_SQL = 
			"select * from public.\"HPC_USER_BOOKMARK\" where \"USER_ID\" = ? and \"PATH\" =?";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// The Spring JDBC Template instance.
	@Autowired
	@Qualifier("hpcPostgreSQLJdbcTemplate")
	private JdbcTemplate jdbcTemplate = null;
	
	// Row mappers.
	private RowMapper<HpcBookmark> userBookmarkRowMapper = (rs, rowNum) ->
	{
		HpcBookmark bookmark = new HpcBookmark();
		bookmark.setName(rs.getString("BOOKMARK_NAME"));
		bookmark.setGroup(rs.getString("BOOKMARK_GROUP"));
		bookmark.setPath(rs.getString("PATH"));
		Calendar created = Calendar.getInstance();
		created.setTime(rs.getTimestamp("CREATED"));
		bookmark.setCreated(created);
		Calendar updated = Calendar.getInstance();
		updated.setTime(rs.getTimestamp("UPDATED"));
		bookmark.setUpdated(updated);
		return bookmark;
	};
	
    // The logger instance.
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcUserBookmarkDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcUserBookmarkDAO Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void upsertBookmark(String nciUserId, HpcBookmark bookmark) 
                              throws HpcException
	{
		try {
		     jdbcTemplate.update(UPSERT_USER_BOOKMARK_SQL,
		    		             nciUserId, bookmark.getName(), bookmark.getPath(),
		    		             bookmark.getGroup(), bookmark.getCreated(),
		    		             bookmark.getUpdated());
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to upsert a user bookmark " + 
		                               e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
	
	@Override
	public void deleteBookmark(String nciUserId, String bookmarkName) throws HpcException
	{
		try {
		     jdbcTemplate.update(DELETE_USER_BOOKMARK_SQL, nciUserId, bookmarkName);
		     
		} catch(DataAccessException e) {
			    throw new HpcException("Failed to delete a bookmark" + 
		                               e.getMessage(),
			    		               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}   
	}
	
	@Override
	public List<HpcBookmark> getBookmarks(String nciUserId) throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_USER_BOOKMARKS_SQL, userBookmarkRowMapper, 
		    		                   nciUserId);
		     
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get user bookmarks: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}	    	
    }
    
    @Override
    public HpcBookmark getBookmark(String nciUserId, String bookmarkName) 
    		                      throws HpcException
    {
		try {
		     return jdbcTemplate.queryForObject(GET_USER_BOOKMARK_SQL, userBookmarkRowMapper, 
		    		                            nciUserId, bookmarkName);
		     
		} catch(IncorrectResultSizeDataAccessException irse) {
			    logger.error("Multiple bookmarks with the same name found", irse);
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a user bookamrk: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
    }
    
    
    @Override
    public List<HpcBookmark> getBookmarksByPath(String nciUserId, String bookmarkPath) 
    		                      throws HpcException
    {
		try {
		     return jdbcTemplate.query(GET_USER_BOOKMARKS_BY_PATH_SQL, userBookmarkRowMapper, 
		    		                            nciUserId, bookmarkPath);
		     
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get bookamrks for user " 
		        		+ nciUserId + " for the given path " + bookmarkPath + ": " + e.getMessage(),
		    	    	HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
    }
}

 