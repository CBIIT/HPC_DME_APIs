package gov.nih.nci.hpc.integration.ldap.authentication;

import gov.nih.nci.hpc.exception.HpcException;

public class HpcLdapAuthenticationProxyImpl implements gov.nih.nci.hpc.integration.HpcLdapAuthenticationProxy
{
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcLdapAuthenticationProxyImpl() throws HpcException
    {
    }   
    	
	public boolean authenticate(String userName, String password) throws HpcException
	{
		try {
		     return LDAPHelper.authenticate(userName, password.toCharArray());
		} catch(Exception e) {
              throw new HpcException("LDAP failed: ", e);
		}
	}
}
