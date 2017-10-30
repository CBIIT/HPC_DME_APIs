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

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.dao.HpcSystemAccountDAO;
import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcAuthenticationTokenClaims;
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
import gov.nih.nci.hpc.service.HpcSecurityService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
/**
 * <p>
 * HPC Security Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcSecurityServiceImpl implements HpcSecurityService
{
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
    // Authentication Token claim attributes.
	private static final String TOKEN_SUBJECT = "HPCAuthenticationToken";
	private static final String USER_ID_TOKEN_CLAIM = "UserName";
	private static final String DATA_MANAGEMENT_ACCOUNT_TOKEN_CLAIM = "DataManagementAccount";
	
	// JSON attributes. Used to create a JSON out of HpcIntegratedSystemAccount object.
	private static final String INTEGRATED_SYSTEM_JSON_ATTRIBUTE = "integratedSystem";
	private static final String USER_NAME_JSON_ATTRIBUTE = "username";
	private static final String PASSWORD_JSON_ATTRIBUTE = "password";
	private static final String PROPERTIES_JSON_ATTRIBUTE = "properties";
	
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
	
    // The Data Management Authenticator.
	@Autowired
    private HpcDataManagementAuthenticator dataManagementAuthenticator = null;
	
    // The Data Management Configuration Locator.
	@Autowired
    private HpcDataManagementConfigurationLocator dataManagementConfigurationLocator = null;
	
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
     * @param authenticationTokenSignatureKey The authentication token signature key.
     * @param authenticationTokenExpirationPeriod The authentication token expiration period in minutes.
     */
    private HpcSecurityServiceImpl(String authenticationTokenSignatureKey,
    		                       int authenticationTokenExpirationPeriod)
    {
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
    public void addUser(HpcNciAccount nciAccount) throws HpcException
    {
    	// Input validation.
    	if(!isValidNciAccount(nciAccount)) {
    	   throw new HpcException("Invalid add user input",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	if(!dataManagementConfigurationLocator.getDocs().contains(nciAccount.getDoc())) {
    	   throw new HpcException("Invalid Doc. Valid values: " + 
    	                          Arrays.toString(dataManagementConfigurationLocator.getDocs().toArray()),
	                              HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	if(dataManagementConfigurationLocator.get(nciAccount.getDefaultConfigurationId()) == null) {
  		  throw new HpcException("Invalid Configuration ID. Valid values: " + 
                                 Arrays.toString(dataManagementConfigurationLocator.keySet().toArray()),
                                 HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	// Check if the user already exists.
    	if(getUser(nciAccount.getUserId()) != null) {
    	   throw new HpcException("User already exists: nciUserId = " +
    	                          nciAccount.getUserId(),
    	                          HpcRequestRejectReason.USER_ALREADY_EXISTS);
    	}
    	
       	// Get the service invoker.
       	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
       	if(invoker == null) {
       	   throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
       	}

    	// Create the User domain object.
    	HpcUser user = new HpcUser();

    	user.setNciAccount(nciAccount);
    	user.setCreated(Calendar.getInstance());
    	user.setActive(true);
    	user.setActiveUpdatedBy(invoker.getNciAccount() == null ? invoker.getDataManagementAccount().getUsername() : invoker.getNciAccount().getUserId());

    	// Persist to the DB.
    	upsert(user);
    }

    @Override
    public void updateUser(String nciUserId, String firstName, String lastName, 
    		               String doc, String defaultConfigurationId, boolean active)
	                      throws HpcException
    {
    	// Input validation.
    	if(StringUtils.isEmpty(nciUserId)) {
     	   throw new HpcException("Null or empty nciUserId",
	                              HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	// Get the user.
    	HpcUser user = getUser(nciUserId);
    	if(user == null) {
    	   throw new HpcException("User not found: " + nciUserId,
    	                          HpcRequestRejectReason.INVALID_NCI_ACCOUNT);
    	}

    	// Create the User domain object.
    	if(!StringUtils.isEmpty(firstName)) {
    	   user.getNciAccount().setFirstName(firstName);
    	}
    	
    	if(!StringUtils.isEmpty(lastName)) {
    		user.getNciAccount().setLastName(lastName);
    	}
    	
    	if(!StringUtils.isEmpty(doc)) {
    	   if(!dataManagementConfigurationLocator.getDocs().contains(doc)) {
    	      throw new HpcException("Invalid Doc. Valid values: " + 
    	                             Arrays.toString(dataManagementConfigurationLocator.getDocs().toArray()),
    	 	                         HpcErrorType.INVALID_REQUEST_INPUT);
    	   }
     	   user.getNciAccount().setDoc(doc);
     	}
    	
    	if(!StringUtils.isEmpty(defaultConfigurationId)) {
    	   if(dataManagementConfigurationLocator.get(defaultConfigurationId) == null) {
    		  throw new HpcException("Invalid Configuration ID. Valid values: " + 
                                     Arrays.toString(dataManagementConfigurationLocator.keySet().toArray()),
                                     HpcErrorType.INVALID_REQUEST_INPUT);
    	   }
    	   user.getNciAccount().setDefaultConfigurationId(defaultConfigurationId);
    	}
    	else
    		user.getNciAccount().setDefaultConfigurationId(null);
    	
    	if(user.getActive() != active) {
    	   user.setActive(active);
    	   // Active indicator has changed. Update the invoker (admin) who changed it.
           HpcRequestInvoker invoker = getRequestInvoker();
           if(invoker == null) {
              throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
           }
           user.setActiveUpdatedBy(invoker.getNciAccount().getUserId());
    	}
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
    public List<HpcUser> getUsers(String nciUserId, String firstNamePattern, String lastNamePattern, 
    		                      String doc, String defaultConfigurationId, boolean active) 
                                 throws HpcException
    {
    	return userDAO.getUsers(nciUserId, firstNamePattern, lastNamePattern, 
    			                doc, defaultConfigurationId, active);
    }
                                 

    @Override
    public HpcUserRole getUserRole(String nciUserId) throws HpcException
    {
    	// Input validation.
    	if(nciUserId == null) {
    	   throw new HpcException("Null NCI user ID",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}

    	return dataManagementProxy.getUserRole(dataManagementAuthenticator.getAuthenticatedToken(), nciUserId);
    }

    @Override
    public HpcRequestInvoker getRequestInvoker()
    {
    	return HpcRequestContext.getRequestInvoker();
    }

    @Override
    public void setRequestInvoker(HpcNciAccount nciAccount, HpcAuthenticationType authenticationType,
                                  HpcIntegratedSystemAccount dataManagementAccount) throws 
                                 HpcException
    {
    	// Input validation.
    	if(nciAccount == null || authenticationType == null || dataManagementAccount == null) {
    	   throw new HpcException("Failed to set request invoker", HpcErrorType.UNEXPECTED_ERROR);
    	}
    	
    	HpcRequestInvoker invoker = new HpcRequestInvoker();
    	invoker.setNciAccount(nciAccount);
    	invoker.setDataManagementAccount(dataManagementAccount);
    	invoker.setAuthenticationType(authenticationType);
    	
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
    	invoker.setAuthenticationType(HpcAuthenticationType.SYSTEM_ACCOUNT);

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
    	claims.put(USER_ID_TOKEN_CLAIM, authenticationTokenClaims.getUserId());
    	claims.put(DATA_MANAGEMENT_ACCOUNT_TOKEN_CLAIM, toJSON(authenticationTokenClaims.getDataManagementAccount()));

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
    	     tokenClaims.setUserId(jwsClaims.getBody().get(USER_ID_TOKEN_CLAIM, String.class));
    	     tokenClaims.setDataManagementAccount(fromJSON(jwsClaims.getBody().get(DATA_MANAGEMENT_ACCOUNT_TOKEN_CLAIM, 
    	    		                                                               String.class)));
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
     * @throws HpcException on service failure.
     */
    private void upsert(HpcUser user) throws HpcException
    {
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
	private String toJSON(HpcIntegratedSystemAccount integratedSystemAccount)
	{
		if(integratedSystemAccount == null) {
		   return "";
		}
		
		JSONObject jsonIntegratedSystemAccount = new JSONObject();
		jsonIntegratedSystemAccount.put(INTEGRATED_SYSTEM_JSON_ATTRIBUTE,
				                        integratedSystemAccount.getIntegratedSystem().value());
		jsonIntegratedSystemAccount.put(USER_NAME_JSON_ATTRIBUTE,
                                        integratedSystemAccount.getUsername());
		jsonIntegratedSystemAccount.put(PASSWORD_JSON_ATTRIBUTE,
                                        integratedSystemAccount.getPassword());
		JSONObject jsonIntegratedSystemAccountProperties = new JSONObject();
		for(HpcIntegratedSystemAccountProperty property : integratedSystemAccount.getProperties()) {
			jsonIntegratedSystemAccountProperties.put(property.getName(), property.getValue());
		}
		jsonIntegratedSystemAccount.put(PROPERTIES_JSON_ATTRIBUTE, jsonIntegratedSystemAccountProperties);
		
		return jsonIntegratedSystemAccount.toJSONString();
	}
	
    /** 
     * Convert JSON string to HpcIntegratedSystemAccount.
     * 
     * @param jsonIntegratedSystemAccountStr The integrated system account JSON String.
     * @return An integrated system account object
     */
	private HpcIntegratedSystemAccount fromJSON(String jsonIntegratedSystemAccountStr)
	{
		if(StringUtils.isEmpty(jsonIntegratedSystemAccountStr)) {
		   return null;
		}
		
		// Parse the JSON string.
		JSONObject jsonIntegratedSystemAccount = null;
		try {
			 jsonIntegratedSystemAccount = (JSONObject) (new JSONParser().parse(jsonIntegratedSystemAccountStr));
			 
		} catch(ParseException e) {
			    return null;
		}
		
		// Instantiate the integrated system account object.
		HpcIntegratedSystemAccount integratedSystemAccount = new HpcIntegratedSystemAccount();
		integratedSystemAccount.setIntegratedSystem(HpcIntegratedSystem.fromValue(
				                   jsonIntegratedSystemAccount.get(INTEGRATED_SYSTEM_JSON_ATTRIBUTE).toString()));
		integratedSystemAccount.setUsername(jsonIntegratedSystemAccount.get(USER_NAME_JSON_ATTRIBUTE).toString());
		integratedSystemAccount.setPassword(jsonIntegratedSystemAccount.get(PASSWORD_JSON_ATTRIBUTE).toString());
		
		// Map account properties from JSON.
		JSONObject jsonProperties = (JSONObject) jsonIntegratedSystemAccount.get(PROPERTIES_JSON_ATTRIBUTE);
		for(Object propertyName : jsonProperties.keySet()) {
			HpcIntegratedSystemAccountProperty property = new HpcIntegratedSystemAccountProperty();
			property.setName(propertyName.toString());
			property.setValue(jsonProperties.get(propertyName).toString());
			integratedSystemAccount.getProperties().add(property);
		}

		return integratedSystemAccount;
	}	
}

