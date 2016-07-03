/**
 * HpcSecurityServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidIntegratedSystemAccount;
import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNciAccount;
import gov.nih.nci.hpc.dao.HpcSystemAccountDAO;
import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.integration.HpcLdapAuthenticationProxy;
import gov.nih.nci.hpc.service.HpcSecurityService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
/**
 * <p>
 * HPC User Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcSecurityServiceImpl.java 1013 2016-03-26 23:06:30Z rosenbergea $
 */

public class HpcSecurityServiceImpl implements HpcSecurityService
{
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
    // Authentication Token claim attributes.
	private static final String TOKEN_SUBJECT = "HPCAuthenticationToken";
	private static final String TOKEN_USER_NAME = "UserName";
	private static final String TOKEN_PASSWORD = "Password";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The User DAO instance.
	@Autowired
    private HpcUserDAO userDAO = null;

    // The System Account DAO instance.
	@Autowired
    private HpcSystemAccountDAO systemAccountDAO = null;

	// The LDAP authenticator instance.
	@Autowired
	private HpcLdapAuthenticationProxy ldapAuthenticationProxy = null;

	@Autowired
	private HpcDataManagementProxy dataManagementProxy = null;

	// System Accounts locator.
	@Autowired
	private HpcSystemAccountLocator systemAccountLocator = null;

	// The valid DOC values.
	private Set<String> docValues = new HashSet<>();
	
	// The authentication token signature key.
	private String authenticationTokenSignatureKey = null;

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Constructor for Spring Dependency Injection.
     *
     * @param docValues A whitespace separated list of valid DOC values.
     * @param authenticationTokenSignatureKey The authentication token signature key.
     */
    private HpcSecurityServiceImpl(String docValues, 
    		                       String authenticationTokenSignatureKey)
    {
    	this.docValues.addAll(Arrays.asList(docValues.split("\\s+")));
    	this.authenticationTokenSignatureKey = authenticationTokenSignatureKey;
    }

    /**
     * Default constructor disabled.
     *
     * @throws HpcException Constructor is disabled.
     */
    private HpcSecurityServiceImpl() throws HpcException
    {
    	throw new HpcException("Constructor disabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }

    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//

    //---------------------------------------------------------------------//
    // HpcSecurityService Interface Implementation
    //---------------------------------------------------------------------//

    @Override
    public void addUser(HpcNciAccount nciAccount,
	                    HpcIntegratedSystemAccount dataManagementAccount)
	                   throws HpcException
    {
    	// Input validation.
    	if(!isValidNciAccount(nciAccount) ||
    	   !isValidIntegratedSystemAccount(dataManagementAccount)) {
    	   throw new HpcException("Invalid add user input",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	if(!docValues.contains(nciAccount.getDOC())) {
    	   throw new HpcException("Invalid DOC: " + nciAccount.getDOC() +
    			                  ". Valid values: " + docValues,
	                              HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Check if the user already exists.
    	if(getUser(nciAccount.getUserId()) != null) {
    	   throw new HpcException("User already exists: nciUserId = " +
    	                          nciAccount.getUserId(),
    	                          HpcRequestRejectReason.USER_ALREADY_EXISTS);
    	}

    	// Create the User domain object.
    	HpcUser user = new HpcUser();

    	user.setNciAccount(nciAccount);
    	user.setDataManagementAccount(dataManagementAccount);
    	user.setCreated(Calendar.getInstance());

    	// Persist to the DB.
    	upsert(user);
    }

    @Override
    public void updateUser(String nciUserId, String firstName, String lastName, String doc)
	                      throws HpcException
    {
    	// Input validation.
    	if(nciUserId == null || firstName == null || lastName == null || doc == null) {
    	   throw new HpcException("Invalid update user input",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	if(!docValues.contains(doc)) {
    	   throw new HpcException("Invalid DOC: " + doc +
    			                  ". Valid values: " + docValues,
	                              HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Get the user.
    	HpcUser user = getUser(nciUserId);
    	if(user == null) {
    	   throw new HpcException("User not found: " + nciUserId,
    	                          HpcRequestRejectReason.INVALID_NCI_ACCOUNT);
    	}

    	// Create the User domain object.
    	user.getNciAccount().setFirstName(firstName);
    	user.getNciAccount().setLastName(lastName);
    	user.getNciAccount().setDOC(doc);
    	user.setLastUpdated(Calendar.getInstance());

    	// Persist to the DB.
    	upsert(user);
    }

    @Override
    public HpcUser getUser(String nciUserId) throws HpcException
    {
    	// Input validation.
    	if(nciUserId == null) {
    	   throw new HpcException("Null NCI user ID",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	return userDAO.getUser(nciUserId);
    }

    @Override
    public HpcRequestInvoker getRequestInvoker()
    {
    	return HpcRequestContext.getRequestInvoker();
    }

    @Override
    public void setRequestInvoker(HpcUser user, boolean ldapAuthenticated)
    {
    	HpcRequestInvoker invoker = new HpcRequestInvoker();
    	if(user != null) {
    	   invoker.setNciAccount(user.getNciAccount());
    	   invoker.setDataManagementAccount(user.getDataManagementAccount());
    	   invoker.setDataManagementAuthenticatedToken(null);
    	   invoker.setLdapAuthenticated(ldapAuthenticated);
    	}

    	HpcRequestContext.setRequestInvoker(invoker);
    }
    
    @Override
    public void setSystemRequestInvoker() throws HpcException
    {
    	HpcIntegratedSystemAccount dataManagementAccount = 
    	   systemAccountLocator.getSystemAccount(HpcIntegratedSystem.IRODS);
    	if(dataManagementAccount == null) {
    	   throw new HpcException("System Data Management Account not configured",
    			                  HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
    	HpcRequestInvoker invoker = new HpcRequestInvoker();
    	invoker.setNciAccount(null);
    	invoker.setDataManagementAccount(dataManagementAccount);
    	invoker.setDataManagementAuthenticatedToken(null);
    	invoker.setLdapAuthenticated(false);

    	HpcRequestContext.setRequestInvoker(invoker);
    }

    @Override
	public boolean authenticate(String userName, String password) throws HpcException
	{
    	// Input validation.
		if(userName == null || userName.trim().length() == 0) {
		   throw new HpcException("User name cannot be null or empty",
				                  HpcErrorType.INVALID_REQUEST_INPUT);
		}
		if(password == null || password.trim().length() == 0) {
		   throw new HpcException("Password cannot be null or empty",
				                  HpcErrorType.INVALID_REQUEST_INPUT);
		}

		return ldapAuthenticationProxy.authenticate(userName, password);
	}

    @Override
    public void addSystemAccount(HpcIntegratedSystemAccount account,
                                 HpcDataTransferType dataTransferType)
                                throws HpcException
    {
    	// Input validation.
    	if(!isValidIntegratedSystemAccount(account)) {
    	   throw new HpcException("Invalid system account input",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	systemAccountDAO.upsert(account, dataTransferType);

    	// Refresh the system accounts cache.
    	systemAccountLocator.reload();
    }
    
    @Override
    public String getAuthenticationToken(String userName, String password)
                                        throws HpcException
    {
    	// Prepare the Claims MAp.
    	Map<String, Object> claims = new HashMap<>();
    	claims.put(TOKEN_USER_NAME, userName);
    	claims.put(TOKEN_PASSWORD, password);
    	
    	return Jwts.builder().setSubject(TOKEN_SUBJECT).setClaims(claims).
    			              signWith(SignatureAlgorithm.HS256, authenticationTokenSignatureKey).
    			              compact();
    }

    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//

    /**
     * Persist user to the DB.
     *
     * @param user The user to be persisted.
     *
     * @throws HpcException
     */
    private void upsert(HpcUser user) throws HpcException
    {
    	user.setLastUpdated(Calendar.getInstance());
    	userDAO.upsert(user);
    }
}

