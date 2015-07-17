package gov.nih.nci.hpc.integration.ldap.authentication;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

import  org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

public class HpcLdapAuthenticationProxyImpl implements gov.nih.nci.hpc.integration.HpcLdapAuthenticationProxy{
	LdapAuthenticationProvider ldapProvider;
	
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
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userName, password);
		org.springframework.security.core.Authentication authentication = ldapProvider.authenticate(token);
		if(authentication == null)
			return false;
		else
			return true;
	}
}
