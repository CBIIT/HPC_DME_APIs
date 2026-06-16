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

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
			+ "GLOBUS_ACCOUNT, DATA_TRANSFER_REQUEST_ID, PATH, DOWNLOAD, USER_ID, "
			+ "CREATED) values (?, ?, ?, ?, ?, sysdate)";

	private static final String DELETE_REQUEST_SQL = "delete from HPC_GLOBUS_TRANSFER_TASK where DATA_TRANSFER_REQUEST_ID = ?";

	private static final String GET_GLOBUS_ACCOUNTS_USED_SQL = "select GLOBUS_ACCOUNT,count(*) from HPC_GLOBUS_TRANSFER_TASK "
			+ "group by GLOBUS_ACCOUNT order by count(*)";

	private static final String GET_USERS_BY_DOWNLOAD_TYPE_SQL = "select DISTINCT USER_ID from HPC_GLOBUS_TRANSFER_TASK where USER_ID is NOT NULL and DOWNLOAD = ?";

	private static final String GET_REQUEST_COUNT_BY_USER_AND_DOWNLOAD_TYPE_SQL = "select count(*) from HPC_GLOBUS_TRANSFER_TASK where USER_ID = ? and DOWNLOAD = ?";

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

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

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
					request.getPath(), request.getDownload(), request.getUserId());

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


	@Override
	public int getGlobusRequestCountByUser(String userId, boolean download) throws HpcException {
		try {
			Integer count = jdbcTemplate.queryForObject(
			    GET_REQUEST_COUNT_BY_USER_AND_DOWNLOAD_TYPE_SQL,
			    Integer.class,
			    userId,
			    download
			);

			return count != null ? count : 0;

		} catch (Exception e) {
			String errorMessage = "Failed to get count of globus requests for user: " + userId;
	        logger.error(errorMessage, e);
	        throw new HpcException(errorMessage, HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<String> getGlobusUsersAllocated(boolean download) throws HpcException {
	    try {
	        List<String> users = jdbcTemplate.queryForList(
	            GET_USERS_BY_DOWNLOAD_TYPE_SQL,
	            String.class,
	            download
	        );

	        return users != null ? users : Collections.emptyList();

	    } catch (Exception e) {
	        String errorMessage = "Failed to retrieve distinct users from Globus task queue.";
	        logger.error(errorMessage, e);
	        throw new HpcException(errorMessage, HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
	    }
	}


}
