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
import gov.nih.nci.hpc.domain.model.HpcAuthenticationTokenClaims;
import gov.nih.nci.hpc.domain.model.HpcDataManagementAccount;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.integration.HpcLdapAuthenticationProxy;
import gov.nih.nci.hpc.service.HpcSecurityService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * <p>
 * HPC Security Application Service Implementation.
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
	private static final String TOKEN_LDAP_AUTHENTICATION = "LDAPAuthentication";
	private static final String TOKEN_USER_AUTHENTICATED = "UserAuthenticated";
	
	private static final String TOKEN_DM_HOST = "Host";
	private static final String TOKEN_DM_PORT = "Port";
	private static final String TOKEN_DM_USER_ZONE = "UserZone";
	private static final String TOKEN_DM_USER_NAME = "UserName";
	private static final String TOKEN_DM_PROXY_ZONE = "ProxyZone";
	private static final String TOKEN_DM_PROXY_NAME = "ProxyName";
	private static final String TOKEN_DM_PASSWORD = "DM_Password";
	private static final String TOKEN_DM_DEFAULT_RESC_STORAGE = "DefaultStorageResource";
	private static final String TOKEN_DM_HOME_DIRECTORY = "HomeDirectory";
	private static final String TOKEN_DM_AUTH_SCHEME = "AuthenticationScheme";
	
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
	
	// The authentication token expiration period in minutes.
	private int authenticationTokenExpirationPeriod = 0;
	
    // The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Constructor for Spring Dependency Injection.
     *
     * @param docValues A whitespace separated list of valid DOC values.
     * @param authenticationTokenSignatureKey The authentication token signature key.
     * @param authenticationTokenExpirationPeriod The authentication token expiration period in minutes.
     */
    private HpcSecurityServiceImpl(String docValues, 
    		                       String authenticationTokenSignatureKey,
    		                       int authenticationTokenExpirationPeriod)
    {
    	this.docValues.addAll(Arrays.asList(docValues.split("\\s+")));
    	this.authenticationTokenSignatureKey = authenticationTokenSignatureKey;
    	this.authenticationTokenExpirationPeriod = authenticationTokenExpirationPeriod;
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
    public HpcUserRole getUserRole(String nciUserId) throws HpcException
    {
    	// Input validation.
    	if(nciUserId == null) {
    	   throw new HpcException("Null NCI user ID",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	return dataManagementProxy.getUserRole(getAuthenticatedToken(), nciUserId);
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
    public String createAuthenticationToken(HpcAuthenticationTokenClaims authenticationTokenClaims)
                                           throws HpcException
    {
    	// Prepare the Claims Map.
    	Map<String, Object> claims = new HashMap<>();
    	claims.put(TOKEN_USER_NAME, authenticationTokenClaims.getUserName());
    	claims.put(TOKEN_PASSWORD, authenticationTokenClaims.getPassword());
    	claims.put(TOKEN_LDAP_AUTHENTICATION, authenticationTokenClaims.getLdapAuthentication());
    	claims.put(TOKEN_USER_AUTHENTICATED, authenticationTokenClaims.getUserAuthenticated());
    	
    	claims.put(TOKEN_DM_AUTH_SCHEME, authenticationTokenClaims.getDataManagementAccount().getAuthenticationScheme());
    	claims.put(TOKEN_DM_HOST, authenticationTokenClaims.getDataManagementAccount().getHost());
    	claims.put(TOKEN_DM_PORT, authenticationTokenClaims.getDataManagementAccount().getPort());
    	claims.put(TOKEN_DM_USER_ZONE, authenticationTokenClaims.getDataManagementAccount().getUserZone());
    	claims.put(TOKEN_DM_USER_NAME, authenticationTokenClaims.getDataManagementAccount().getUserName());
    	claims.put(TOKEN_DM_PROXY_ZONE, authenticationTokenClaims.getDataManagementAccount().getProxyZone());
    	claims.put(TOKEN_DM_PROXY_NAME, authenticationTokenClaims.getDataManagementAccount().getProxyName());
    	claims.put(TOKEN_DM_PASSWORD, authenticationTokenClaims.getDataManagementAccount().getPassword());
    	claims.put(TOKEN_DM_DEFAULT_RESC_STORAGE, authenticationTokenClaims.getDataManagementAccount().getDefaultStorageResource());
    	claims.put(TOKEN_DM_HOME_DIRECTORY, authenticationTokenClaims.getDataManagementAccount().getHomeDirectory());
    	
    	
    	// Calculate the expiration date.
    	Calendar tokenExpiration = Calendar.getInstance();
    	tokenExpiration.add(Calendar.MINUTE, authenticationTokenExpirationPeriod);
    	
    	return Jwts.builder().setSubject(TOKEN_SUBJECT).setClaims(claims).
    			              setExpiration(tokenExpiration.getTime()).
    			              signWith(SignatureAlgorithm.HS256, authenticationTokenSignatureKey).
    			              compact();
    }
    
    @Override
    public HpcAuthenticationTokenClaims parseAuthenticationToken(String authenticationToken)
                                                                throws HpcException
    {
    	try {
    	     Jws<Claims> jwsClaims = Jwts.parser().setSigningKey(authenticationTokenSignatureKey).
    	    		                            parseClaimsJws(authenticationToken);
    	     
    	     // Extract the claims.
    	     HpcAuthenticationTokenClaims tokenClaims = new HpcAuthenticationTokenClaims();
    	     tokenClaims.setUserName(jwsClaims.getBody().get(TOKEN_USER_NAME, String.class));
    	     tokenClaims.setPassword(jwsClaims.getBody().get(TOKEN_PASSWORD, String.class));
    	     tokenClaims.setLdapAuthentication(jwsClaims.getBody().get(TOKEN_LDAP_AUTHENTICATION, Boolean.class));
    	     tokenClaims.setUserAuthenticated(jwsClaims.getBody().get(TOKEN_USER_AUTHENTICATED, Boolean.class));
    	     
    	     HpcDataManagementAccount account = new HpcDataManagementAccount();
    	     account.setAuthenticationScheme(jwsClaims.getBody().get(TOKEN_DM_AUTH_SCHEME, String.class));
    	     account.setHost(jwsClaims.getBody().get(TOKEN_DM_HOST, String.class));
    	     account.setPort(jwsClaims.getBody().get(TOKEN_DM_PORT, Integer.class));
    	     account.setUserZone(jwsClaims.getBody().get(TOKEN_DM_USER_ZONE, String.class));
    	     account.setUserName(jwsClaims.getBody().get(TOKEN_DM_USER_NAME, String.class));
    	     account.setProxyZone(jwsClaims.getBody().get(TOKEN_DM_PROXY_ZONE, String.class));
    	     account.setProxyName(jwsClaims.getBody().get(TOKEN_DM_PROXY_NAME, String.class));
    	     account.setPassword(jwsClaims.getBody().get(TOKEN_DM_PASSWORD, String.class));
    	     account.setDefaultStorageResource(jwsClaims.getBody().get(TOKEN_DM_DEFAULT_RESC_STORAGE, String.class));
    	     account.setHomeDirectory(jwsClaims.getBody().get(TOKEN_DM_HOME_DIRECTORY, String.class));
    	     tokenClaims.setDataManagementAccount(account);
    	     return tokenClaims;

    	} catch(SignatureException se) {
    		    logger.error("Untrusted Token: " + se);
	    	    return null;
	    	    
    	} catch(Exception e) {
    		    logger.error("Invalid Token: " + e);
    		    return null;
    	}
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
    
    /**
     * Get the data management authenticated token from the request context.
     * If it's not in the context, get a token by authenticating.
     *
     * @throws HpcException If it failed to obtain an authentication token.
     */
    private Object getAuthenticatedToken() throws HpcException
    {
    	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    	if(invoker == null) {
	       throw new HpcException("Unknown user",
			                      HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);
    	}
    	
    	if(invoker.getDataManagementAuthenticatedToken() != null) {
    	   return invoker.getDataManagementAuthenticatedToken();
    	}
    	
    	// No authenticated token found in the request token. Authenticate the invoker.
    	if(invoker.getDataManagementAccount() == null) {
    		throw new HpcException("Unknown data management account",
                                   HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);
    	}
    	
    	// Authenticate w/ data management
    	Object token = dataManagementProxy.authenticate(invoker.getDataManagementAccount(),
    			                                        invoker.getLdapAuthenticated());
    	if(token == null) {
    	   throw new HpcException("Invalid data management account credentials",
                                  HpcRequestRejectReason.INVALID_DATA_MANAGEMENT_ACCOUNT);
    	}
    	
    	// Store token on the request context.
    	invoker.setDataManagementAuthenticatedToken(token);
    	return token;
    }    
}

