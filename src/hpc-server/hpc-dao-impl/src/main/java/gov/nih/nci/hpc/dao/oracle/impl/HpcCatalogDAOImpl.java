/**
 * HpcCatalogDAOImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;

import gov.nih.nci.hpc.dao.HpcCatalogDAO;
import gov.nih.nci.hpc.domain.catalog.HpcCatalogCriteria;
import gov.nih.nci.hpc.domain.catalog.HpcCatalogMetadataEntry;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Catalog DAO Implementation.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcCatalogDAOImpl implements HpcCatalogDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String GET_CATALOG_DOC_EQUAL_SQL = " \"DOC\" = ?";

	private static final String GET_CATALOG_BASEPATH_EQUAL_SQL = " \"BASE_PATH\" = ?";
	
    private static final String GET_CATALOG_IDS_SQL = "select distinct object_id from r_catalog_meta_main ";

    private static final String GET_CATALOG_ID_IN_SQL = " object_id in (";
    
    private static final String GET_CATALOG_ID_IN_END_SQL = ") order by \"DOC\", \"BASE_PATH\", object_path";

	private static final String LIMIT_OFFSET_SQL = " order by object_id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private static final String GET_CATALOG_SQL = "select \"DOC\", \"BASE_PATH\", object_path, meta_attr_name, meta_attr_value, meta_attr_unit "
        + "from r_catalog_meta_main ";

	private static final String GET_CATALOG_COUNT_SQL = "select count(distinct object_id) from r_catalog_meta_main ";
	

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// Row mappers.
	private RowMapper<HpcCatalogMetadataEntry> catalogMetadataEntryRowMapper = (rs, rowNum) -> {
	    HpcCatalogMetadataEntry catalogMetadataEntry = new HpcCatalogMetadataEntry();
	    catalogMetadataEntry.setDoc(rs.getString(1));
	    catalogMetadataEntry.setBasePath(rs.getString(2));
	    catalogMetadataEntry.setPath(rs.getString(3));
	    catalogMetadataEntry.setAttribute(rs.getString(4));
	    catalogMetadataEntry.setValue(rs.getString(5));

		return catalogMetadataEntry;
	};

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcCatalogDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcCatalogDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
    public List<HpcCatalogMetadataEntry> getCatalog(HpcCatalogCriteria criteria, int offset, int limit) throws HpcException
    {
        // Build the query based on provided search criteria.
        StringBuilder sqlQueryBuilder = new StringBuilder();
        List<Object> args = new ArrayList<>();
        
        sqlQueryBuilder.append(GET_CATALOG_SQL);
        sqlQueryBuilder.append(" where ");
        sqlQueryBuilder.append(GET_CATALOG_ID_IN_SQL);

        sqlQueryBuilder.append(GET_CATALOG_IDS_SQL);
        if(!StringUtils.isEmpty(criteria.getDoc())) {
           sqlQueryBuilder.append(" where ");
           sqlQueryBuilder.append(GET_CATALOG_DOC_EQUAL_SQL);
           args.add(criteria.getDoc());
        }
        if(!StringUtils.isEmpty(criteria.getBasePath())) {
           if(StringUtils.isEmpty(criteria.getDoc()))
               sqlQueryBuilder.append(" where ");
           else
               sqlQueryBuilder.append(" and ");
           sqlQueryBuilder.append(GET_CATALOG_BASEPATH_EQUAL_SQL);
           args.add(criteria.getBasePath());
        }
        sqlQueryBuilder.append(LIMIT_OFFSET_SQL);
        args.add(offset);
        args.add(limit);
        sqlQueryBuilder.append(GET_CATALOG_ID_IN_END_SQL);
        
        try {
             return jdbcTemplate.query(sqlQueryBuilder.toString(), catalogMetadataEntryRowMapper, args.toArray());
             
        } catch(IncorrectResultSizeDataAccessException irse) {
                return Collections.emptyList();
                
        } catch(DataAccessException e) {
                throw new HpcException("Failed to get catalog entries: " + e.getMessage(),
                                       HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
        }       
    }
	
	@Override
    public int getCatalogCount(HpcCatalogCriteria criteria) throws HpcException {
      // Build the query based on provided search criteria.
        StringBuilder sqlQueryBuilder = new StringBuilder();
        List<Object> args = new ArrayList<>();
        
        sqlQueryBuilder.append(GET_CATALOG_COUNT_SQL);
   
        if(!StringUtils.isEmpty(criteria.getDoc())) {
           sqlQueryBuilder.append(" where ");
           sqlQueryBuilder.append(GET_CATALOG_DOC_EQUAL_SQL);
           args.add(criteria.getDoc());
        }
        if(!StringUtils.isEmpty(criteria.getBasePath())) {
           if(StringUtils.isEmpty(criteria.getDoc()))
               sqlQueryBuilder.append(" where ");
           else
               sqlQueryBuilder.append(" and ");
           sqlQueryBuilder.append(GET_CATALOG_BASEPATH_EQUAL_SQL);
           args.add(criteria.getBasePath());
        }
        try {
            return jdbcTemplate.queryForObject(sqlQueryBuilder.toString(), Integer.class, args.toArray());

        } catch (DataAccessException e) {
            throw new HpcException("Failed to count catalog: " + e.getMessage(),
                  HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
        }
    }

}
