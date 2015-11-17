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
import gov.nih.nci.hpc.integration.HpcLdapAuthenticationProxy;
import gov.nih.nci.hpc.service.HpcLdapAuthenticationService;

/**
 * <p>
 * HPC LDAP Authentication Service Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */
public class HpcLdapAuthenticationServiceImpl implements HpcLdapAuthenticationService {

	HpcLdapAuthenticationProxy proxy;
	
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
    private HpcLdapAuthenticationServiceImpl(HpcLdapAuthenticationProxy proxy)
    {
    	this.proxy = proxy;
    }
	
	public boolean authenticate(String userName, String password) throws HpcException
	{
		if(userName == null || userName.trim().length() == 0)
			throw new HpcException("User name cannot be null or empty", HpcErrorType.INVALID_REQUEST_INPUT);
		if(password == null || password.trim().length() == 0)
			throw new HpcException("Password cannot be null or empty", HpcErrorType.INVALID_REQUEST_INPUT);
		
		return proxy.authenticate(userName, password);
		
	}
}
