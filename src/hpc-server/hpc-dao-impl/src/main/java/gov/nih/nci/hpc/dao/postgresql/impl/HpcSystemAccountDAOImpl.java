/**
 * HpcSystemAccountDAOImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
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
 * HPC System Account DAO Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcSystemAccountDAOImpl implements HpcSystemAccountDAO {
  //---------------------------------------------------------------------//
  // Constants
  //---------------------------------------------------------------------//

  // SQL Queries.
  private static final String UPSERT_SQL =
      "insert into public.\"HPC_SYSTEM_ACCOUNT\" ( "
          + "\"USERNAME\", \"PASSWORD\", \"SYSTEM\", \"DATA_TRANSFER_TYPE\", \"CLASSIFIER\") "
          + "values (?, ?, ?, ?, ?)";



  private static final String UPDATE_SQL =
    "update public.\"HPC_SYSTEM_ACCOUNT\" set "
        + "\"USERNAME\" = ?, \"PASSWORD\" = ?, \"DATA_TRANSFER_TYPE\" = ?, \"CLASSIFIER\" = ? "
        + " where \"SYSTEM\" = ?";



  private static final String GET_BY_SYSTEM_SQL =
      "select * from public.\"HPC_SYSTEM_ACCOUNT\" where \"SYSTEM\" = ?";
  private static final String GET_BY_DATA_TRANSFER_TYPE_SQL =
      "select * from public.\"HPC_SYSTEM_ACCOUNT\" where \"DATA_TRANSFER_TYPE\" = ?";

  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // The Spring JDBC Template instance.
  @Autowired private JdbcTemplate jdbcTemplate = null;

  // Encryptor.
  @Autowired HpcEncryptor encryptor = null;

  // Row mapper.
  private RowMapper<HpcIntegratedSystemAccount> rowMapper =
      (rs, rowNum) -> {
        HpcIntegratedSystemAccount account = new HpcIntegratedSystemAccount();
        account.setUsername(rs.getString("USERNAME"));
        account.setPassword(encryptor.decrypt(rs.getBytes(("PASSWORD"))));
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

  /** Constructor for Spring Dependency Injection. */
  private HpcSystemAccountDAOImpl() {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // HpcManagedUserDAO Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public void update(
      HpcIntegratedSystemAccount account, HpcDataTransferType dataTransferType, String classifier)
      throws HpcException {
    try {
      jdbcTemplate.update(
          UPDATE_SQL,
          account.getUsername(),
          encryptor.encrypt(account.getPassword()),
          dataTransferType != null ? dataTransferType.value() : null,
          classifier != null ? classifier : null,
          account.getIntegratedSystem().value());

    } catch (DataAccessException e) {
      throw new HpcException(
          "Failed to update a system account: " + e.getMessage(),
          HpcErrorType.DATABASE_ERROR,
          HpcIntegratedSystem.POSTGRESQL,
          e);
    }
  }


  @Override
  public void upsert(
      HpcIntegratedSystemAccount account, HpcDataTransferType dataTransferType, String classifier)
      throws HpcException {
    try {
      jdbcTemplate.update(
          UPSERT_SQL,
          account.getUsername(),
          encryptor.encrypt(account.getPassword()),
          account.getIntegratedSystem().value(),
          dataTransferType != null ? dataTransferType.value() : null,
          classifier != null ? classifier : null);

    } catch (DataAccessException e) {
      throw new HpcException(
          "Failed to upsert a system account: " + e.getMessage(),
          HpcErrorType.DATABASE_ERROR,
          HpcIntegratedSystem.POSTGRESQL,
          e);
    }
  }


  @Override
  public List<HpcIntegratedSystemAccount> getSystemAccount(HpcIntegratedSystem system)
      throws HpcException {
    try {
      return jdbcTemplate.query(GET_BY_SYSTEM_SQL, rowMapper, system.value());

    } catch (IncorrectResultSizeDataAccessException notFoundEx) {
      return null;

    } catch (DataAccessException e) {
      throw new HpcException(
          "Failed to get a system account: " + e.getMessage(),
          HpcErrorType.DATABASE_ERROR,
          HpcIntegratedSystem.POSTGRESQL,
          e);
    }
  }

  @Override
  public List<HpcIntegratedSystemAccount> getSystemAccount(HpcDataTransferType dataTransferType)
      throws HpcException {
    try {
      return jdbcTemplate.query(GET_BY_DATA_TRANSFER_TYPE_SQL, rowMapper, dataTransferType.value());
    } catch (IncorrectResultSizeDataAccessException notFoundEx) {
      return null;

    } catch (DataAccessException e) {
      throw new HpcException(
          "Failed to get a system account: " + e.getMessage(),
          HpcErrorType.DATABASE_ERROR,
          HpcIntegratedSystem.POSTGRESQL,
          e);
    }
  }
}
