package gov.nih.nci.hpc.integration.ldap.authentication;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.Hashtable;

import javax.naming.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

public class HpcLdapAuthenticationProxyImpl implements gov.nih.nci.hpc.integration.HpcLdapAuthenticationProxy{
	LdapAuthenticationProvider ldapProvider;
	// The logger instance.
		private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
    // The LDAP Template.
	@Autowired
    private LdapTemplate ldapTemplate = null;
    
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcLdapAuthenticationProxyImpl() throws HpcException
    {
    	throw new HpcException("Constructor Disabled",
                HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }   
    	
    /**
     * Constructor for Spring Dependency Injection.
     * 
     * @param dataTransferProxy The data transfer proxy instance.
     * @param dataTransferAccountValidatorProvider 
     *        The data transfer account validator provider instance.
     */
    private HpcLdapAuthenticationProxyImpl(LdapAuthenticationProvider ldapProvider)
    {
    	this.ldapProvider = ldapProvider;
    }
	
	public boolean authenticate(String userName, String password) throws HpcException
	{
		/*
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userName, password);
		org.springframework.security.core.Authentication authentication = ldapProvider.authenticate(token);
		if(authentication == null)
			return false;
		else
			return true;
		 */
		/*
		AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("uid", userName));
        //return ldapTemplate.authenticate("ou=NCI,o=NIH", filter.toString(), password);
        
        Object obj = ldapTemplate.lookup("uid=rosenbergea,ou=NCI,o=NIH");
        if(obj != null) {
           logger.error("ERAN 1: " + obj.getClass().getName());
           logger.error("ERAN 2: " + obj.toString());
        }*/
		
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
		"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldaps://nci-ldap-prod.nci.nih.gov:636");
		// env.put(Context.SECURITY_AUTHENTICATION, search.getSecurityAthentication());
		env.put(Context.SECURITY_PRINCIPAL, "CN=NCILDAP,OU=NCI,O=NIH");
		env.put(Context.SECURITY_CREDENTIALS, "***REMOVED***");
		try {
		     return LDAPHelper.authenticate(env, userName, password.toCharArray(), null);
		} catch(Exception e) {
              throw new HpcException("LDAP failed: ", e);
		}
	}
}
