/**
 * HpcSecurityServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidIntegratedSystemAccount;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNciAccount;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import gov.nih.nci.hpc.dao.HpcApiCallsAuditDAO;
import gov.nih.nci.hpc.dao.HpcGroupDAO;
import gov.nih.nci.hpc.dao.HpcQueryConfigDAO;
import gov.nih.nci.hpc.dao.HpcSystemAccountDAO;
import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcAuthenticationTokenClaims;
import gov.nih.nci.hpc.domain.model.HpcDistinguishedNameSearch;
import gov.nih.nci.hpc.domain.model.HpcDistinguishedNameSearchResult;
import gov.nih.nci.hpc.domain.model.HpcGroup;
import gov.nih.nci.hpc.domain.model.HpcQueryConfiguration;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcAuthenticationType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccountProperty;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.integration.HpcLdapAuthenticationProxy;
import gov.nih.nci.hpc.integration.HpcSpsAuthorizationProxy;
import gov.nih.nci.hpc.service.HpcSecurityService;
import gov.nih.nci.hpc.service.HpcSystemAccountFunction;
import gov.nih.nci.hpc.service.HpcSystemAccountFunctionNoReturn;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

/**
 * HPC Security Application Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcSecurityServiceImpl implements HpcSecurityService {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// Authentication Token claim attributes.
	private static final String TOKEN_SUBJECT = "HPCAuthenticationToken";
	private static final String USER_ID_TOKEN_CLAIM = "UserName";
	private static final String DATA_MANAGEMENT_ACCOUNT_TOKEN_CLAIM = "DataManagementAccount";
	private static final String DATA_MANAGEMENT_ACCOUNT_EXPIRATION_TOKEN_CLAIM = "DataManagementAccountExpiration";

	// JSON attributes. Used to create a JSON out of HpcIntegratedSystemAccount
	// object.
	private static final String INTEGRATED_SYSTEM_JSON_ATTRIBUTE = "integratedSystem";
	private static final String USER_NAME_JSON_ATTRIBUTE = "username";
	private static final String PASSWORD_JSON_ATTRIBUTE = "password";
	private static final String PROPERTIES_JSON_ATTRIBUTE = "properties";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The User DAO instance.
	@Autowired
	private HpcUserDAO userDAO = null;

	// The User DAO instance.
	@Autowired
	private HpcGroupDAO groupDAO = null;

	// The System Account DAO instance.
	@Autowired
	private HpcSystemAccountDAO systemAccountDAO = null;

	// The Query Config DAO instance.
	@Autowired
	private HpcQueryConfigDAO queryConfigDAO = null;

	// The Query Config DAO instance.
	@Autowired
	private HpcApiCallsAuditDAO apiCallsAuditDAO = null;

	// The LDAP authenticator instance.
	@Autowired
	private HpcLdapAuthenticationProxy ldapAuthenticationProxy = null;

	// The SPS authorization instance.
	@Autowired
	private HpcSpsAuthorizationProxy spsAuthorizationProxy = null;

	@Autowired
	private HpcDataManagementProxy dataManagementProxy = null;

	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;

	// The Data Management Authenticator.
	@Autowired
	private HpcDataManagementAuthenticator dataManagementAuthenticator = null;

	// The Data Management Configuration Locator.
	@Autowired
	private HpcDataManagementConfigurationLocator dataManagementConfigurationLocator = null;

	// Query configuration locator.
	@Autowired
	private HpcQueryConfigurationLocator queryConfigurationLocator = null;

	// The authentication token signature key.
	@Value("${hpc.service.security.authenticationTokenSignatureKey}")
	private String authenticationTokenSignatureKey = null;

	// The authentication token expiration period in minutes.
	@Value("${hpc.service.security.authenticationTokenExpirationPeriod}")
	private int authenticationTokenExpirationPeriod = 0;

	// The authentication token expiration period in minutes.
	@Value("${hpc.service.security.authenticationTokenExpirationPeriodSso}")
	private int authenticationTokenExpirationPeriodSso = 0;

	// The data management account expiration period in minutes.
	@Value("${hpc.service.security.dataManagementAccountExpirationPeriod}")
	private int dataManagementExpirationPeriod = 0;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcSecurityService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void addUser(HpcNciAccount nciAccount) throws HpcException {
		// Input validation.
		if (!isValidNciAccount(nciAccount)) {
			throw new HpcException("Invalid add user input", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (!dataManagementConfigurationLocator.getDocs().contains(nciAccount.getDoc())) {
			throw new HpcException(
					"Invalid DOC: " + nciAccount.getDoc() + ". Valid values: "
							+ Arrays.toString(dataManagementConfigurationLocator.getDocs().toArray()),
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		String defaultConfigurationId = nciAccount.getDefaultConfigurationId();
		if (defaultConfigurationId != null && dataManagementConfigurationLocator.get(defaultConfigurationId) == null) {
			throw new HpcException(
					"Invalid Configuration ID. Valid values: "
							+ Arrays.toString(dataManagementConfigurationLocator.keySet().toArray()),
					HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Check if the user already exists.
		if (getUser(nciAccount.getUserId()) != null) {
			throw new HpcException("User already exists: nciUserId = " + nciAccount.getUserId(),
					HpcRequestRejectReason.USER_ALREADY_EXISTS);
		}

		// Get the service invoker.
		HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
		if (invoker == null) {
			throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Create the User domain object.
		HpcUser user = new HpcUser();

		user.setNciAccount(nciAccount);
		user.setCreated(Calendar.getInstance());
		user.setActive(true);
		user.setActiveUpdatedBy(invoker.getNciAccount() == null ? invoker.getDataManagementAccount().getUsername()
				: invoker.getNciAccount().getUserId());

		// Persist to the DB.
		upsert(user);
	}

	@Override
	public void updateUser(String nciUserId, String firstName, String lastName, String doc,
			String defaultConfigurationId, boolean active) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(nciUserId)) {
			throw new HpcException("Null or empty nciUserId", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the user.
		HpcUser user = getUser(nciUserId);
		if (user == null) {
			throw new HpcException("User not found: " + nciUserId, HpcRequestRejectReason.INVALID_NCI_ACCOUNT);
		}

		// Create the User domain object.
		if (!StringUtils.isEmpty(firstName)) {
			user.getNciAccount().setFirstName(firstName);
		}

		if (!StringUtils.isEmpty(lastName)) {
			user.getNciAccount().setLastName(lastName);
		}

		if (!StringUtils.isEmpty(doc)) {
			if (!dataManagementConfigurationLocator.getDocs().contains(doc)) {
				throw new HpcException(
						"Invalid DOC: " + doc + ". Valid values: "
								+ Arrays.toString(dataManagementConfigurationLocator.getDocs().toArray()),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			user.getNciAccount().setDoc(doc);
		}

		if (!StringUtils.isEmpty(defaultConfigurationId)) {
			if (dataManagementConfigurationLocator.get(defaultConfigurationId) == null) {
				throw new HpcException(
						"Invalid Configuration ID. Valid values: "
								+ Arrays.toString(dataManagementConfigurationLocator.keySet().toArray()),
						HpcErrorType.INVALID_REQUEST_INPUT);
			}
			user.getNciAccount().setDefaultConfigurationId(defaultConfigurationId);
		} else
			user.getNciAccount().setDefaultConfigurationId(null);

		if (user.getActive() != active) {
			user.setActive(active);
			// Active indicator has changed. Update the invoker (admin) who changed it.
			HpcRequestInvoker invoker = getRequestInvoker();
			if (invoker == null) {
				throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
			}
			user.setActiveUpdatedBy(invoker.getNciAccount().getUserId());
		}
		user.setLastUpdated(Calendar.getInstance());

		// Persist to the DB.
		upsert(user);
	}

	@Override
	public void deleteUser(String nciUserId) throws HpcException {
		// Input validation.
		if (StringUtils.isEmpty(nciUserId)) {
			throw new HpcException("Null or empty nciUserId", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		// Get the user.
		HpcUser user = getUser(nciUserId);
		if (user == null) {
			throw new HpcException("User not found: " + nciUserId, HpcRequestRejectReason.INVALID_NCI_ACCOUNT);
		}

		userDAO.deleteUser(nciUserId);
	}

	@Override
	public HpcUser getUser(String nciUserId) throws HpcException {
		// Input validation.
		if (nciUserId == null) {
			throw new HpcException("Null NCI user ID", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return userDAO.getUser(nciUserId);
	}

	@Override
	public List<HpcUser> getUsers(String nciUserId, String firstNamePattern, String lastNamePattern, String doc,
			String defaultConfigurationId, boolean active, boolean query) throws HpcException {
		if (query)
			return userDAO.queryUsers(nciUserId, firstNamePattern, lastNamePattern, doc, defaultConfigurationId,
					active);
		return userDAO.getUsers(nciUserId, firstNamePattern, lastNamePattern, doc, defaultConfigurationId, active);
	}

	@Override
	public List<HpcUser> getUsersByRole(String role, String doc, String defaultConfigurationId, boolean active)
			throws HpcException {
		return userDAO.getUsersByRole(role, doc, defaultConfigurationId, active);
	}

	@Override
	public HpcUserRole getUserRole(String nciUserId) throws HpcException {
		// Input validation.
		if (nciUserId == null) {
			throw new HpcException("Null NCI user ID", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return dataManagementProxy.getUserRole(dataManagementAuthenticator.getAuthenticatedToken(), nciUserId);
	}

	@Override
	public void addGroup(String name) throws HpcException {
		// Input validation.

		// Check if the group already exists.
		HpcGroup group = getGroup(name);
		if (group != null && group.getActive()) {
			throw new HpcException("Group already exists: name = " + name, HpcRequestRejectReason.GROUP_ALREADY_EXISTS);
		}

		// Get the service invoker.
		HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
		if (invoker == null) {
			throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Create the User domain object.
		group = new HpcGroup();
		group.setName(name);
		group.setDoc(invoker.getNciAccount().getDoc());
		group.setCreated(Calendar.getInstance());
		group.setActive(true);

		// Persist to the DB.
		group.setActiveUpdatedBy(invoker.getNciAccount() == null ? invoker.getDataManagementAccount().getUsername()
				: invoker.getNciAccount().getUserId());
		group.setLastUpdated(Calendar.getInstance());
		groupDAO.upsertGroup(group);
	}

	@Override
	public void updateGroup(String name, boolean active) throws HpcException {
		// Input validation.

		HpcGroup group = getGroup(name);
		// Check if the group already exists.
		if (group == null || !group.getActive()) {
			throw new HpcException("Group does not exist or is inactive: name = " + name,
					HpcRequestRejectReason.GROUP_DOES_NOT_EXIST);
		}

		// Get the service invoker.
		HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
		if (invoker == null) {
			throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Update the Group domain object.
		group.setActive(active);
		group.setActiveUpdatedBy(invoker.getNciAccount() == null ? invoker.getDataManagementAccount().getUsername()
				: invoker.getNciAccount().getUserId());
		group.setLastUpdated(Calendar.getInstance());

		// Persist to the DB.
		groupDAO.updateGroup(group);
	}

	@Override
	public void deleteGroup(String name) throws HpcException {
		// Input validation.

		HpcGroup group = getGroup(name);
		// Check if the group exists.
		if (group == null || !group.getActive()) {
			throw new HpcException("Group does not exist or is already inactive: name = " + name,
					HpcRequestRejectReason.GROUP_DOES_NOT_EXIST);
		}

		// Get the service invoker.
		HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
		if (invoker == null) {
			throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
		}

		// Delete from DB.
		groupDAO.deleteGroup(name);
	}

	@Override
	public HpcGroup getGroup(String name) throws HpcException {
		// Input validation.
		if (name == null) {
			throw new HpcException("Null group name", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return groupDAO.getGroup(name);
	}

	@Override
	public HpcRequestInvoker getRequestInvoker() {
		return HpcRequestContext.getRequestInvoker();
	}

	@Override
	public void setRequestInvoker(HpcNciAccount nciAccount, boolean ldapAuthentication,
			HpcAuthenticationType authenticationType, HpcIntegratedSystemAccount dataManagementAccount)
			throws HpcException {
		// Input validation.
		if (nciAccount == null || authenticationType == null || dataManagementAccount == null) {
			throw new HpcException("Failed to set request invoker", HpcErrorType.UNEXPECTED_ERROR);
		}

		HpcRequestInvoker invoker = new HpcRequestInvoker();
		invoker.setNciAccount(nciAccount);
		invoker.setDataManagementAccount(dataManagementAccount);
		invoker.setLdapAuthentication(ldapAuthentication);
		invoker.setAuthenticationType(authenticationType);

		HpcRequestContext.setRequestInvoker(invoker);
	}

	@Override
	public <T> T executeAsSystemAccount(Optional<Boolean> ldapAuthentication,
			HpcSystemAccountFunction<T> systemAccountFunction) throws HpcException {
		// Get the current request invoker, and authentication type.
		HpcRequestInvoker currentRequestInvoker = getRequestInvoker();

		// Switch to system account if needed.
		boolean switchToSystemAccount = !HpcAuthenticationType.SYSTEM_ACCOUNT
				.equals(currentRequestInvoker.getAuthenticationType());
		if (switchToSystemAccount) {
			setSystemRequestInvoker(ldapAuthentication.orElse(currentRequestInvoker.getLdapAuthentication()));
		}

		// Execute the function as system account.
		try {
			return systemAccountFunction.execute();

		} finally {
			// If we switched to system account. May need to close the connection to data
			// management.
			if (switchToSystemAccount) {
				if (getRequestInvoker().getDataManagementAuthenticatedToken() != null) {
					// Data management system account was used, so need to close this connection.
					dataManagementProxy.disconnect(getRequestInvoker().getDataManagementAuthenticatedToken());
				}

				// Switch back to the original request invoker.
				HpcRequestContext.setRequestInvoker(currentRequestInvoker);
			}
		}
	}

	@Override
	public void executeAsSystemAccount(Optional<Boolean> ldapAuthentication,
			HpcSystemAccountFunctionNoReturn systemAccountFunction) throws HpcException {
		executeAsSystemAccount(ldapAuthentication, () -> {
			systemAccountFunction.execute();
			return null;
		});
	}

	@Override
	public <T> T executeAsUserAccount(String userId, HpcSystemAccountFunction<T> userAccountFunction)
			throws HpcException {
		// Get the current request invoker, and authentication type.
		HpcRequestInvoker currentRequestInvoker = getRequestInvoker();

		// Switch to the user account
		setUserRequestInvoker(userId);

		// Execute the function as user account.
		try {
			return userAccountFunction.execute();

		} finally {
			// We may need to close the connection to data management.
			if (getRequestInvoker().getDataManagementAuthenticatedToken() != null) {
				// Data management system account was used, so need to close this connection.
				dataManagementProxy.disconnect(getRequestInvoker().getDataManagementAuthenticatedToken());
			}

			// Switch back to the original request invoker.
			HpcRequestContext.setRequestInvoker(currentRequestInvoker);
		}
	}

	@Override
	public boolean authenticate(String userName, String password) throws HpcException {
		// Input validation.
		if (userName == null || userName.trim().length() == 0) {
			throw new HpcException("User name cannot be null or empty", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if (password == null || password.trim().length() == 0) {
			throw new HpcException("Password cannot be null or empty", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return ldapAuthenticationProxy.authenticate(userName, password);
	}

	@Override
	public boolean authenticateSso(String nciUserId, String smSession) throws HpcException {
		// Input validation.
		if (smSession == null || smSession.trim().length() == 0) {
			throw new HpcException("SM session cannot be null or empty", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return spsAuthorizationProxy.authorize(nciUserId, smSession,
				systemAccountLocator.getSystemAccount(HpcIntegratedSystem.IRODS).getUsername(),
				systemAccountLocator.getSystemAccount(HpcIntegratedSystem.IRODS).getPassword());
	}

	@Override
	public void addSystemAccount(HpcIntegratedSystemAccount account, HpcDataTransferType dataTransferType,
			String classifier) throws HpcException {
		// Input validation.
		if (!isValidIntegratedSystemAccount(account)) {
			throw new HpcException("Invalid system account input", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		if (!account.getIntegratedSystem().equals(HpcIntegratedSystem.GLOBUS)
				&& systemAccountDAO.getSystemAccount(account.getIntegratedSystem()) != null
				&& !systemAccountDAO.getSystemAccount(account.getIntegratedSystem()).isEmpty()) {
			systemAccountDAO.update(account, dataTransferType, classifier);
		} else {
			systemAccountDAO.upsert(account, dataTransferType, classifier);
		}

		// Refresh the system accounts cache.
		systemAccountLocator.reload();
	}

	@Override
	public HpcIntegratedSystemAccount getSystemAccount(HpcIntegratedSystem system) throws HpcException {
		return systemAccountLocator.getSystemAccount(system);

	}

	@Override
	public String createAuthenticationToken(HpcAuthenticationType authenticationType,
			HpcAuthenticationTokenClaims authenticationTokenClaims) throws HpcException {
		// Prepare the Claims Map.
		Map<String, Object> claims = new HashMap<>();
		claims.put(USER_ID_TOKEN_CLAIM, authenticationTokenClaims.getUserId());
		claims.put(DATA_MANAGEMENT_ACCOUNT_TOKEN_CLAIM, toJSON(authenticationTokenClaims.getDataManagementAccount()));

		// Calculate the data management account expiration date.
		Calendar dataManagementAccountExpiration = Calendar.getInstance();
		dataManagementAccountExpiration.add(Calendar.MINUTE, dataManagementExpirationPeriod);
		claims.put(DATA_MANAGEMENT_ACCOUNT_EXPIRATION_TOKEN_CLAIM, dataManagementAccountExpiration.getTime());

		// Calculate the expiration date.
		Calendar tokenExpiration = Calendar.getInstance();
		if (authenticationType.equals(HpcAuthenticationType.SM)) {
			tokenExpiration.add(Calendar.MINUTE, authenticationTokenExpirationPeriodSso);
		} else {
			tokenExpiration.add(Calendar.MINUTE, authenticationTokenExpirationPeriod);
		}

		return Jwts.builder().setSubject(TOKEN_SUBJECT).setClaims(claims).setExpiration(tokenExpiration.getTime())
				.signWith(SignatureAlgorithm.HS256, authenticationTokenSignatureKey).compact();
	}

	@Override
	public HpcAuthenticationTokenClaims parseAuthenticationToken(String authenticationToken) throws HpcException {
		try {
			Jws<Claims> jwsClaims = Jwts.parser().setSigningKey(authenticationTokenSignatureKey)
					.parseClaimsJws(authenticationToken);

			// Extract the claims.
			HpcAuthenticationTokenClaims tokenClaims = new HpcAuthenticationTokenClaims();
			tokenClaims.setUserId(jwsClaims.getBody().get(USER_ID_TOKEN_CLAIM, String.class));
			tokenClaims.setDataManagementAccount(
					fromJSON(jwsClaims.getBody().get(DATA_MANAGEMENT_ACCOUNT_TOKEN_CLAIM, String.class)));

			// Check if the data management account expired.
			Date dataManagementAccountExpiration = jwsClaims.getBody()
					.get(DATA_MANAGEMENT_ACCOUNT_EXPIRATION_TOKEN_CLAIM, Date.class);
			if (dataManagementAccountExpiration.before(new Date())) {
				// Data management account expired. Remove its properties.
				tokenClaims.getDataManagementAccount().getProperties().clear();
			}

			return tokenClaims;

		} catch (SignatureException se) {
			logger.error("Untrusted Token: " + se);
			return null;

		} catch (Exception e) {
			logger.error("Invalid Token: " + e);
			return null;
		}
	}

	@Override
	public void refreshDataManagementConfigurations() throws HpcException {
		this.dataManagementConfigurationLocator.reload();
	}

	@Override
	public HpcNciAccount getUserFirstLastNameFromAD(String username) throws HpcException {

		return ldapAuthenticationProxy.getUserFirstLastName(username);
	}

	@Override
	public HpcDistinguishedNameSearch findDistinguishedNameSearch(String path) {
		if (StringUtils.isEmpty(path)) {
			return null;
		}

		for (String basePath : dataManagementConfigurationLocator.getDistinguishedNameSearchBasePaths()) {
			if (path.startsWith(basePath)) {
				return dataManagementConfigurationLocator.getDistinguishedNameSearch(basePath);
			}
		}

		return null;
	}

	@Override
	public HpcDistinguishedNameSearchResult getUserDistinguishedName(String userId, String searchBase)
			throws HpcException {
		return ldapAuthenticationProxy.getDistinguishedName(userId, "uid", searchBase);
	}

	@Override
	public HpcDistinguishedNameSearchResult getGroupDistinguishedName(String groupId, String searchBase)
			throws HpcException {
		return ldapAuthenticationProxy.getDistinguishedName(groupId, "gid", searchBase);
	}

	@Override
	public boolean isUserDataCurator(String nciUserId) throws HpcException {
		// Input validation.
		if (nciUserId == null) {
			throw new HpcException("Null NCI user ID", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return userDAO.isUserDataCurator(nciUserId);
	}

	@Override
	public void updateQueryConfig(String basePath, String encryptionKey) throws HpcException {

		queryConfigDAO.upsert(basePath, encryptionKey);
		queryConfigurationLocator.reload();
	}

	@Override
	public HpcQueryConfiguration getQueryConfig(String basePath) throws HpcException {

		return queryConfigurationLocator.getConfig(basePath);
	}

	@Override
	public void addApiCallAuditRecord(String userId, String httpRequestMethod, String endpoint, String httpResponseCode,
			String serverId, Calendar created, Calendar completed) throws HpcException {
		apiCallsAuditDAO.insert(userId, httpRequestMethod, endpoint, httpResponseCode, serverId, created, completed);
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Persist user to the DB.
	 *
	 * @param user The user to be persisted.
	 * @throws HpcException on service failure.
	 */
	private void upsert(HpcUser user) throws HpcException {
		user.setLastUpdated(Calendar.getInstance());
		userDAO.upsertUser(user);
	}

	/**
	 * Convert an integrated-system account to a JSON string
	 *
	 * @param integratedSystemAccount The integrated system account.
	 * @return A JSON representation of integrated system account.
	 */
	@SuppressWarnings("unchecked")
	private String toJSON(HpcIntegratedSystemAccount integratedSystemAccount) {
		if (integratedSystemAccount == null) {
			return "";
		}

		JSONObject jsonIntegratedSystemAccount = new JSONObject();
		jsonIntegratedSystemAccount.put(INTEGRATED_SYSTEM_JSON_ATTRIBUTE,
				integratedSystemAccount.getIntegratedSystem().value());
		jsonIntegratedSystemAccount.put(USER_NAME_JSON_ATTRIBUTE, integratedSystemAccount.getUsername());
		jsonIntegratedSystemAccount.put(PASSWORD_JSON_ATTRIBUTE, integratedSystemAccount.getPassword());
		JSONObject jsonIntegratedSystemAccountProperties = new JSONObject();
		for (HpcIntegratedSystemAccountProperty property : integratedSystemAccount.getProperties()) {
			jsonIntegratedSystemAccountProperties.put(property.getName(), property.getValue());
		}
		jsonIntegratedSystemAccount.put(PROPERTIES_JSON_ATTRIBUTE, jsonIntegratedSystemAccountProperties);

		return jsonIntegratedSystemAccount.toJSONString();
	}

	/**
	 * Convert JSON string to HpcIntegratedSystemAccount.
	 *
	 * @param jsonIntegratedSystemAccountStr The integrated system account JSON
	 *                                       String.
	 * @return An integrated system account object
	 */
	private HpcIntegratedSystemAccount fromJSON(String jsonIntegratedSystemAccountStr) {
		if (StringUtils.isEmpty(jsonIntegratedSystemAccountStr)) {
			return null;
		}

		// Parse the JSON string.
		JSONObject jsonIntegratedSystemAccount = null;
		try {
			jsonIntegratedSystemAccount = (JSONObject) (new JSONParser().parse(jsonIntegratedSystemAccountStr));

		} catch (ParseException e) {
			return null;
		}

		// Instantiate the integrated system account object.
		HpcIntegratedSystemAccount integratedSystemAccount = new HpcIntegratedSystemAccount();
		integratedSystemAccount.setIntegratedSystem(HpcIntegratedSystem
				.fromValue(jsonIntegratedSystemAccount.get(INTEGRATED_SYSTEM_JSON_ATTRIBUTE).toString()));
		integratedSystemAccount.setUsername(jsonIntegratedSystemAccount.get(USER_NAME_JSON_ATTRIBUTE).toString());
		integratedSystemAccount.setPassword(jsonIntegratedSystemAccount.get(PASSWORD_JSON_ATTRIBUTE).toString());

		// Map account properties from JSON.
		JSONObject jsonProperties = (JSONObject) jsonIntegratedSystemAccount.get(PROPERTIES_JSON_ATTRIBUTE);
		for (Object propertyName : jsonProperties.keySet()) {
			HpcIntegratedSystemAccountProperty property = new HpcIntegratedSystemAccountProperty();
			property.setName(propertyName.toString());
			property.setValue(jsonProperties.get(propertyName).toString());
			integratedSystemAccount.getProperties().add(property);
		}

		return integratedSystemAccount;
	}

	/**
	 * Set the service call invoker in the request context using system account.
	 *
	 * @param ldapAuthentication Indicator whether LDAP authentication is turned
	 *                           on/off.
	 * @throws HpcException on service failure.
	 */
	private void setSystemRequestInvoker(boolean ldapAuthentication) throws HpcException {
		HpcIntegratedSystemAccount dataManagementAccount = systemAccountLocator
				.getSystemAccount(HpcIntegratedSystem.IRODS);
		if (dataManagementAccount == null) {
			throw new HpcException("System Data Management Account not configured", HpcErrorType.UNEXPECTED_ERROR);
		}

		HpcRequestInvoker invoker = new HpcRequestInvoker();
		invoker.setNciAccount(null);
		invoker.setDataManagementAccount(dataManagementAccount);
		invoker.setDataManagementAuthenticatedToken(null);
		invoker.setLdapAuthentication(ldapAuthentication);
		invoker.setAuthenticationType(HpcAuthenticationType.SYSTEM_ACCOUNT);

		HpcRequestContext.setRequestInvoker(invoker);
	}

	/**
	 * Set the service call invoker in the request context using user account.
	 *
	 * @param userId The userId of the user account to switch to.
	 * @throws HpcException on service failure.
	 */
	private void setUserRequestInvoker(String userId) throws HpcException {
		HpcUser user = getUser(userId);
		if (user == null) {
			throw new HpcException("User is not registered with HPC-DM: " + userId, HpcErrorType.UNAUTHORIZED_REQUEST);
		}
		if (!user.getActive()) {
			throw new HpcException(
					"User is inactive. Please contact system administrator to activate account: " + userId,
					HpcErrorType.UNAUTHORIZED_REQUEST);
		}
		// Instantiate a Data Management account.
		HpcIntegratedSystemAccount dataManagementAccount = new HpcIntegratedSystemAccount();
		dataManagementAccount.setIntegratedSystem(HpcIntegratedSystem.IRODS);
		dataManagementAccount.setUsername(userId);

		HpcRequestInvoker invoker = new HpcRequestInvoker();
		invoker.setNciAccount(user.getNciAccount());
		invoker.setDataManagementAccount(dataManagementAccount);
		invoker.setDataManagementAuthenticatedToken(null);
		invoker.setUserRole(getUserRole(dataManagementAccount.getUsername()));
		invoker.setAuthenticationType(HpcAuthenticationType.TOKEN);

		HpcRequestContext.setRequestInvoker(invoker);
	}

}
