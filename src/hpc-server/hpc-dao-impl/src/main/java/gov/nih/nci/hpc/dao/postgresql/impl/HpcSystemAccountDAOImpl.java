/**
 * HpcSystemAccountDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.postgresql.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import gov.nih.nci.hpc.dao.HpcSystemAccountDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccountProperty;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC System Account DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcSystemAccountDAOImpl implements HpcSystemAccountDAO
{ 
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
    
    // SQL Queries.
/*
	private final static String UPSERT_SQL =
		    "insert into public.\"HPC_SYSTEM_ACCOUNT\" ( " +
                    "\"USERNAME\", \"PASSWORD\", \"SYSTEM\", \"DATA_TRANSFER_TYPE\", \"CLASSIFIER\") " +
                    "values (?, ?, ?, ?, ?) " +
            "on conflict(\"SYSTEM\") do update set \"USERNAME\"=excluded.\"USERNAME\", " +
                                                   "\"PASSWORD\"=excluded.\"PASSWORD\", " +
                                                   "\"DATA_TRANSFER_TYPE\"=excluded.\"DATA_TRANSFER_TYPE\"";
*/
  private final static String UPSERT_SQL =
      "insert into public.\"HPC_SYSTEM_ACCOUNT\" ( " +
          "\"USERNAME\", \"PASSWORD\", \"SYSTEM\", \"DATA_TRANSFER_TYPE\", \"CLASSIFIER\") " +
          "values (?, ?, ?, ?, ?)";

	private final static String GET_BY_SYSTEM_SQL = 
		    "select * from public.\"HPC_SYSTEM_ACCOUNT\" where \"SYSTEM\" = ?";
	private final static String GET_BY_DATA_TRANSFER_TYPE_SQL = 
		    "select * from public.\"HPC_SYSTEM_ACCOUNT\" where \"DATA_TRANSFER_TYPE\" = ?";

//	private final static String GET_SHARED_GLOBUS_APP_ACCOUNTS =
//        "select * from public.\"HPC_SYSTEM_ACCOUNT\" where \"SYSTEM\" = 'P_GLOBUS'";
	
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
  private RowMapper<HpcIntegratedSystemAccount> rowMapper = (rs, rowNum) ->
  {
    HpcIntegratedSystemAccount account = new HpcIntegratedSystemAccount();
    account.setUsername(rs.getString("USERNAME"));
    account.setPassword(encryptor.decrypt(rs.getBytes(("PASSWORD"))));
/*
		final String rawSystem = rs.getString("SYSTEM");
		if (!"P_GLOBUS".equals(rawSystem)) {
			account.setIntegratedSystem(HpcIntegratedSystem.fromValue(rawSystem));
		}
*/
    account.setIntegratedSystem(HpcIntegratedSystem.fromValue(rs.getString("SYSTEM")));

    final String classifierValue = rs.getString("CLASSIFIER");
    HpcIntegratedSystemAccountProperty theProperty = new HpcIntegratedSystemAccountProperty();
    theProperty.setName("classifier");
    theProperty.setValue(classifierValue);
    account.getProperties().add(theProperty);

    return account;
  };
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection. 
     * 
     */
    private HpcSystemAccountDAOImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcManagedUserDAO Interface Implementation
    //---------------------------------------------------------------------//  

  @Override
  public void upsert(HpcIntegratedSystemAccount account,
      HpcDataTransferType dataTransferType,
      String classifier) throws HpcException {
    try {
      jdbcTemplate.update(UPSERT_SQL,
          account.getUsername(),
          encryptor.encrypt(account.getPassword()),
          account.getIntegratedSystem().value(),
          dataTransferType != null ? dataTransferType.value() : null,
          classifier != null ? classifier : null);

    } catch (DataAccessException e) {
      throw new HpcException("Failed to upsert a system account: " + e.getMessage(),
          HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
    }
  }
	
	@Override 
	public List<HpcIntegratedSystemAccount> getSystemAccount(HpcIntegratedSystem system)
                                                      throws HpcException
	{
		try {
//      return jdbcTemplate.queryForObject(GET_BY_SYSTEM_SQL, rowMapper, system.value());
       return jdbcTemplate.query(GET_BY_SYSTEM_SQL, rowMapper, system.value());

		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a system account: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}
	
	@Override 
	public List<HpcIntegratedSystemAccount> getSystemAccount(HpcDataTransferType dataTransferType)
                                                      throws HpcException
	{
		try {
//		     return jdbcTemplate.queryForObject(GET_BY_DATA_TRANSFER_TYPE_SQL, rowMapper,
//		    		                            dataTransferType.value());
      return jdbcTemplate.query(GET_BY_DATA_TRANSFER_TYPE_SQL, rowMapper, dataTransferType.value());
		} catch(IncorrectResultSizeDataAccessException notFoundEx) {
			    return null;
			    
		} catch(DataAccessException e) {
		        throw new HpcException("Failed to get a system account: " + e.getMessage(),
		    	    	               HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, e);
		}
	}

/*
  @Override
  public List<HpcIntegratedSystemAccount> getGlobusPooledAccounts() throws HpcException {
    try {
      return jdbcTemplate.query(GET_SHARED_GLOBUS_APP_ACCOUNTS, rowMapper);
    } catch (DataAccessException daEx) {
      throw new HpcException("Failed to get shared/pooled Globus app accounts: " + daEx.getMessage(),
          HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.POSTGRESQL, daEx);
    }
  }
*/

}

 