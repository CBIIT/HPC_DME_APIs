/**
 * HpcLdapAuthenticationProvider.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
/**
 * <p>
 * HPC LDAP Authentication Service Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id:  $
 */
public class HpcLdapAuthenticationServiceImpl {

	LdapAuthenticationProvider ldapProvider;
	
    /**
     * Default Constructor.
     * 
     * @throws HpcException Constructor is disabled.
     */
    private HpcLdapAuthenticationServiceImpl() throws HpcException
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
    private HpcLdapAuthenticationServiceImpl(LdapAuthenticationProvider ldapProvider)
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
