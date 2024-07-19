/**
 * HpcGlobusTransferTaskDAOImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.oracle.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import gov.nih.nci.hpc.dao.HpcGlobusTransferTaskDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcGlobusTransferTask;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Globus Transfer Task DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public class HpcGlobusTransferTaskDAOImpl implements HpcGlobusTransferTaskDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	public static final String INSERT_REQUEST_SQL = "insert into HPC_GLOBUS_TRANSFER_TASK ( "
			+ "GLOBUS_ACCOUNT, DATA_TRANSFER_REQUEST_ID, PATH, DOWNLOAD, "
			+ "CREATED) values (?, ?, ?, ?, sysdate)";

	private static final String DELETE_REQUEST_SQL = "delete from HPC_GLOBUS_TRANSFER_TASK where DATA_TRANSFER_REQUEST_ID = ?";

	private static final String GET_GLOBUS_ACCOUNTS_USED_SQL = "select GLOBUS_ACCOUNT,count(*) from HPC_GLOBUS_TRANSFER_TASK "
			+ "group by GLOBUS_ACCOUNT order by count(*)";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// Row mapper.
	private RowMapper<String> globusAccountsRowMapper = (rs, rowNum) -> {
		return rs.getString("GLOBUS_ACCOUNT");
	};

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcGlobusTransferTaskDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcGlobusTransferTaskDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void insertRequest(HpcGlobusTransferTask request) throws HpcException {
		try {
			jdbcTemplate.update(INSERT_REQUEST_SQL, request.getGlobusAccount(), request.getDataTransferRequestId(),
					request.getPath(), request.getDownload());

		} catch (DataAccessException e) {
			throw new HpcException(
					"Failed to insert into globus transfer task table for request id: "
							+ request.getDataTransferRequestId() + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}

	}

	@Override
	public void deleteRequest(String dataTransferRequestId) throws HpcException {
		try {
			jdbcTemplate.update(DELETE_REQUEST_SQL, dataTransferRequestId);

		} catch (DataAccessException e) {
			throw new HpcException(
					"Failed to delete globus transfer request id: " + dataTransferRequestId + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}

	}

	@Override
	public List<String> getGlobusAccountsUsed() throws HpcException {
		try {
			return jdbcTemplate.query(GET_GLOBUS_ACCOUNTS_USED_SQL, globusAccountsRowMapper);

		} catch (DataAccessException e) {
			throw new HpcException(
					"Failed to get globus accounts used from globus transfer task table: " + e.getMessage(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

}
