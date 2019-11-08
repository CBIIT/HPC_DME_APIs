package gov.nih.nci.hpc.integration.ldap.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcLdapAuthenticationProxy;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * LDAP Authentication Proxy Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcLdapAuthenticationProxyImpl implements HpcLdapAuthenticationProxy
{
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//    
	
	private static final String INITIAL_CONTEXT = "com.sun.jndi.ldap.LdapCtxFactory";
	private static final String SECURITY_AUTHENTICATION = "simple";
	private static final String SECURITY_PROTOCOL = "ssl";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// LDAP Environment.
	Hashtable<String, String> environment = new Hashtable<>();
	
	// Search base.
	String base = null;
	
	// User ID filter.
	String userIdFilter = null;
	
	// Last Name filter.
	String lastNameFilter = null;
	
	// First Name filter.
	String firstNameFilter = null;
	
	//User domain name
	String userDomainName = null;
	
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for spring injection.
     * 
     * @param url The LDAP provider URL.
     * @param username The account to access the LDAP server.
     * @param password The password to access the LDAP server.
     * @param base The LDAP search base.
     * @param userIdFilter The user ID filter.
     * @param userDomainName The user domain name.
     * @param lastNameFilter The last name filter.
     * @param firstNameFilter The first name filter.
     */
	private HpcLdapAuthenticationProxyImpl(String url, String username, String password,
			                               String base, String userIdFilter, String userDomainName,
			                               String lastNameFilter, String firstNameFilter) 
    {
		environment.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT);
		environment.put(Context.PROVIDER_URL, url);
		environment.put(Context.SECURITY_AUTHENTICATION, SECURITY_AUTHENTICATION);
		environment.put(Context.SECURITY_PROTOCOL, SECURITY_PROTOCOL);
		environment.put(Context.SECURITY_PRINCIPAL, username);
		environment.put(Context.SECURITY_CREDENTIALS, password);
		
		this.base = base;
		this.userIdFilter = userIdFilter;
		this.userDomainName = userDomainName;
		this.lastNameFilter = lastNameFilter;
		this.firstNameFilter = firstNameFilter;
    }
	
    /**
     * Default Constructor is disabled
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcLdapAuthenticationProxyImpl() throws HpcException
    {
    	throw new HpcException("Default Constructor Disabled", 
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
	
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcLdapAuthenticationProxy Interface Implementation
    //---------------------------------------------------------------------//  
 
    @Override
	public boolean authenticate(String userName, String password) throws HpcException
	{
		String fullyDistinguishedName = getFullyDistinguishedName(userName);
		if(fullyDistinguishedName == null) {
		   // user not found.
		   return false;
		}
		
		DirContext dirContext = null;
		try {
			 Hashtable<String, String> authEnv = new Hashtable<>();
			 authEnv.putAll(environment);
			 authEnv.put(Context.SECURITY_PRINCIPAL, userName+"@"+userDomainName);
			 authEnv.put(Context.SECURITY_CREDENTIALS, password);
			 dirContext = new InitialDirContext(authEnv);			
			 return true;
			 
		} catch(NamingException ne) {
			    logger.info("Incorrect password: " + userName, ne);
			    return false;
			    
		} finally {
		        try {
		        	 if(dirContext != null) {
		                dirContext.close();
		        	 }
		            
		       } catch(NamingException ne) {
				       logger.error("Failed to close LDAP context", ne);
		       }
		}
	}
    

	@Override
	public HpcNciAccount getUserFirstLastName(String userName) throws HpcException {
		
		String[] attributeIDs = { lastNameFilter, firstNameFilter }; 
		String searchFilter = "(" + userIdFilter + "=" + userName + ")";

		DirContext dirContext = null;
		try	{
			 dirContext = new InitialDirContext(environment);
		
		} catch(NamingException ne) {
			    throw new HpcException("Error occured in connecting to the directory server", 
			    		               HpcErrorType.LDAP_ERROR, HpcIntegratedSystem.LDAP, ne);
		}
		
		SearchControls searchControls = new SearchControls();
		searchControls.setReturningAttributes(attributeIDs);
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		try {		 
			 NamingEnumeration<SearchResult> searchEnum = 
					                         dirContext.search(base, searchFilter, searchControls);
			 HpcNciAccount account = null;
			 if(searchEnum.hasMore()) {
				 account = new HpcNciAccount();
				 Attributes attrs = searchEnum.next().getAttributes();
				 account.setLastName(attrs.get(lastNameFilter).toString().substring(lastNameFilter.length() + 1).trim());
				 account.setFirstName(attrs.get(firstNameFilter).toString().substring(firstNameFilter.length() + 1).trim());
			 }
			 return account;

		} catch(NamingException ne) {
				throw new HpcException("User name not found: " + userName, ne);
			    
		} finally {
			       try {
			            dirContext.close();
			            
			       } catch(NamingException ne) {
					       logger.error("Failed to close LDAP context", ne);
			       }
		}
	}
	
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
	/**
	 * Get obtains the fully Distinguished User Name for the user id provided from the LDAP server.
	 * 
	 * @param userName The user name which is to be authenticated.
	 * @return The Fully Distinguished User Name obtained from the LDAP for the passed user name.
	 *         Null is returned if the user doesn't exist.
	 * @throws HpcException on LDAP failure.
	 */
	private String getFullyDistinguishedName(String userName) throws HpcException 
	{
		String[] attributeIDs = { userIdFilter }; 
		String searchFilter = "(" + userIdFilter + "=" + userName + ")";

		DirContext dirContext = null;
		try	{
			 dirContext = new InitialDirContext(environment);
		
		} catch(NamingException ne) {
			    throw new HpcException("Error occured in connecting to the directory server", 
			    		               HpcErrorType.LDAP_ERROR, HpcIntegratedSystem.LDAP, ne);
		}
		
		SearchControls searchControls = new SearchControls();
		searchControls.setReturningAttributes(attributeIDs);
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		try {
			 NamingEnumeration<SearchResult> searchEnum = 
					                         dirContext.search(base, searchFilter, searchControls);
			 return searchEnum.hasMore() ? searchEnum.next().getName() + "," + base : null;

		} catch(NamingException ne) {
			    logger.error("User name not found: " + userName, ne);
			    return null;
			    
		} finally {
			       try {
			            dirContext.close();
			            
			       } catch(NamingException ne) {
					       logger.error("Failed to close LDAP context", ne);
			       }
		}
	}
}
