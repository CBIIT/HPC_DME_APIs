/**
 * HpcInvestigatorDAOImpl.java
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
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;

import gov.nih.nci.hpc.dao.HpcInvestigatorDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Investigator DAO Implementation.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public class HpcInvestigatorDAOImpl implements HpcInvestigatorDAO {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// SQL Queries.
	private static final String UPDATE_INVESTIGATOR_SQL = "update HPC_INVESTIGATOR set "
			+ "AD_FIRST_NAME = ?, AD_LAST_NAME = ?, AD_NIH_SAC = ? where NED_ID = ?";

	private static final String GET_INVESTIGATORS_NEDID_SQL = "select ned_id from HPC_INVESTIGATOR";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Spring JDBC Template instance.
	@Autowired
	private JdbcTemplate jdbcTemplate = null;

	// Row mapper.
	private SingleColumnRowMapper<String> rowMapper = new SingleColumnRowMapper<>();

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcInvestigatorDAOImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcGroupDAO Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void updateInvestigator(HpcNciAccount account) throws HpcException {
		try {
			jdbcTemplate.update(UPDATE_INVESTIGATOR_SQL, account.getFirstName(),
					account.getLastName(), account.getNihSac(),
					account.getNedId());
		} catch (DataAccessException e) {
			throw new HpcException("Failed to update investigator: " + account.getNedId(),
					HpcErrorType.DATABASE_ERROR, HpcIntegratedSystem.ORACLE, e);
		}
	}

	@Override
	public List<String> getAllNedIds() throws HpcException {
		try {
			return jdbcTemplate.query(GET_INVESTIGATORS_NEDID_SQL, rowMapper);

		} catch (IncorrectResultSizeDataAccessException irse) {
			return null;

		} catch (DataAccessException e) {
			throw new HpcException("Failed to get groups: " + e.getMessage(), HpcErrorType.DATABASE_ERROR,
					HpcIntegratedSystem.ORACLE, e);
		}
	}

}
