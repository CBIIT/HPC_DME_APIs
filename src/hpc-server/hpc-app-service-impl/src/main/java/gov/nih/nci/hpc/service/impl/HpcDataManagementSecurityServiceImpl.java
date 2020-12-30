/**
 * HpcDataManagementSecurityServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNciAccount;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.dao.HpcGroupDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataManagementSecurityService;

/**
 * HPC Data Management Security Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcDataManagementSecurityServiceImpl implements HpcDataManagementSecurityService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Data Management Proxy instance.
	@Autowired
	private HpcDataManagementProxy dataManagementProxy = null;

	// The Data Management Authenticator.
	@Autowired
	private HpcDataManagementAuthenticator dataManagementAuthenticator = null;

	// The Group DAO instance.
	@Autowired
	private HpcGroupDAO groupDAO = null;

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcDataManagementSecurityServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataManagementSecurityService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void addUser(HpcNciAccount nciAccount, HpcUserRole userRole) throws HpcException {
		// Input validation.
		if (!isValidNciAccount(nciAccount)) {
			throw new HpcException("Invalid NCI Account: Null user ID or name or DOC",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();

		// Validate the data management account doesn't exist.
		if (dataManagementProxy.userExists(authenticatedToken, nciAccount.getUserId())) {
			throw new HpcException("Data management account already exists: " + nciAccount.getUserId(),
					HpcRequestRejectReason.USER_ALREADY_EXISTS);
		}

		dataManagementProxy.addUser(authenticatedToken, nciAccount, userRole);
	}

	@Override
	public boolean userExists(String nciUserId) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(nciUserId)) {
			throw new HpcException("Null or empty NCI user Id", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return dataManagementProxy.userExists(dataManagementAuthenticator.getAuthenticatedToken(), nciUserId);
	}

	@Override
	public void deleteUser(String nciUserId) throws HpcException {
		// Input validation.
		validateUserExists(nciUserId);

		dataManagementProxy.deleteUser(dataManagementAuthenticator.getAuthenticatedToken(), nciUserId);
	}

	@Override
	public void updateUser(String nciUserId, String firstName, String lastName, HpcUserRole userRole)
			throws HpcException {
		// Input validation.
		validateUserExists(nciUserId);
		if (StringUtils.isEmpty(firstName) || StringUtils.isEmpty(lastName) || userRole == null) {
			throw new HpcException("Invalid update user input", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		dataManagementProxy.updateUser(dataManagementAuthenticator.getAuthenticatedToken(), nciUserId, firstName,
				lastName, userRole);
	}

	@Override
	public HpcUserRole getUserRole(String nciUserId) throws HpcException {
		// Input validation.
		validateUserExists(nciUserId);

		return dataManagementProxy.getUserRole(dataManagementAuthenticator.getAuthenticatedToken(), nciUserId);
	}

	@Override
	public void addGroup(String groupName) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(groupName)) {
			throw new HpcException("Null or empty group name", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate group name. Only alphanumeric + '_' and '-' are supported
		if (!groupName.matches("[a-zA-Z0-9\\-_]+")) {
			throw new HpcException("Invalid group name. Only alphanumeric, '_' and '-' are supported.",
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		Object authenticatedToken = dataManagementAuthenticator.getAuthenticatedToken();

		// Validate the group doesn't already exists.
		if (dataManagementProxy.groupExists(authenticatedToken, groupName)) {
			throw new HpcException("Group already exists: " + groupName, HpcRequestRejectReason.GROUP_ALREADY_EXISTS);
		}

		// Validate the group name doesn't exist as a user-id.
		if (dataManagementProxy.userExists(authenticatedToken, groupName)) {
			throw new HpcException("Group name exists as a user-id: " + groupName,
					HpcRequestRejectReason.GROUP_ALREADY_EXISTS);
		}

		// Add the group.
		dataManagementProxy.addGroup(authenticatedToken, groupName);
	}

	@Override
	public boolean groupExists(String groupName) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(groupName)) {
			throw new HpcException("Null or empty group name", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return dataManagementProxy.groupExists(dataManagementAuthenticator.getAuthenticatedToken(), groupName);
	}

	@Override
	public void deleteGroup(String groupName) throws HpcException {
		// Input validation.
		validateGroupExists(groupName);

		dataManagementProxy.deleteGroup(dataManagementAuthenticator.getAuthenticatedToken(), groupName);
	}

	@Override
	public void addGroupMember(String groupName, String userId) throws HpcException {
		// Input validation.
		validateGroupExists(groupName);
		if (StringUtils.isEmpty(userId)) {
			throw new HpcException("Null or empty user id", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		dataManagementProxy.addGroupMember(dataManagementAuthenticator.getAuthenticatedToken(), groupName, userId);
	}

	@Override
	public void deleteGroupMember(String groupName, String userId) throws HpcException {
		// Input validation.
		validateGroupExists(groupName);
		if (StringUtils.isEmpty(userId)) {
			throw new HpcException("Null or empty user id", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		dataManagementProxy.deleteGroupMember(dataManagementAuthenticator.getAuthenticatedToken(), groupName, userId);
	}

	@Override
	public List<String> getGroupMembers(String groupName) throws HpcException {
		// Input validation.
		validateGroupExists(groupName);

		return dataManagementProxy.getGroupMembers(dataManagementAuthenticator.getAuthenticatedToken(), groupName);
	}

	@Override
	public List<String> getGroups(String groupPattern) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(groupPattern)) {
			throw new HpcException("Null or empty group search criteria", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Note: The Jargon API doesn't support case insensitive query. To workaround,
		// we query the iRODS DB.
		// Once the Jargon API is enhanced to support case insensitive search, it needs
		// to be used and the DAO retired.
		return groupDAO.getGroups(groupPattern);
	}

	@Override
	public List<String> getUserGroups(String userId) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(userId)) {
			throw new HpcException("Null or empty userId", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return groupDAO.getUserGroups(userId);
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Validate that a group exists.
	 *
	 * @param groupName The group name to validate.
	 * @throws HpcException If the group doesn't exist.
	 */
	private void validateGroupExists(String groupName) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(groupName)) {
			throw new HpcException("Null or empty group name", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the group exists.
		if (!dataManagementProxy.groupExists(dataManagementAuthenticator.getAuthenticatedToken(), groupName)) {
			throw new HpcException("Group doesn't exist: " + groupName, HpcErrorType.INVALID_REQUEST_INPUT);
		}
	}

	/**
	 * Validate a data management account exists.
	 *
	 * @param nciUserId The user id to check if a data management account exists.
	 * @throws HpcException If the data management account doesn't exist.
	 */
	private void validateUserExists(String nciUserId) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(nciUserId)) {
			throw new HpcException("Null or empty user ID", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Validate the group exists.
		if (!dataManagementProxy.userExists(dataManagementAuthenticator.getAuthenticatedToken(), nciUserId)) {
			throw new HpcException("Data management account doesn't exist: " + nciUserId,
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
	}
}
