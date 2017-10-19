/**
 * HpcAuthentcationTestInterceptor.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.test;

import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.user.HpcAuthenticationType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcSecurityService;

import org.apache.cxf.interceptor.security.SecureAnnotationsInterceptor;
import org.apache.cxf.interceptor.security.SimpleAuthorizingInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Authentication Interceptor for JUnit Tests.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcAuthenticationTestInterceptor 
             extends AbstractPhaseInterceptor<Message> 
{
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Security Business Service instance.
	@Autowired
    private HpcSecurityService securityService = null;
	
    // The logger instance.
	protected final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Constructor for Spring Dependency Injection.
     * 
     */
    private HpcAuthenticationTestInterceptor() 
    {
        super(Phase.RECEIVE);

        // We need to authenticate first, and then authorize.
        getBefore().add(SecureAnnotationsInterceptor.class.getName());
        getBefore().add(SimpleAuthorizingInterceptor.class.getName());
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // AbstractPhaseInterceptor<Message> Interface Implementation
    //---------------------------------------------------------------------//  

    @Override
    public void handleMessage(Message message) 
    {
    	HpcNciAccount nciAccount = new HpcNciAccount();
    	nciAccount.setDefaultConfigurationId("DEFAULT_CONFIG_ID");
    	nciAccount.setFirstName("Unit");
    	nciAccount.setLastName("Test");
    	nciAccount.setUserId("unittest");
    	
    	HpcIntegratedSystemAccount dataManagementAccount = new HpcIntegratedSystemAccount();
    	dataManagementAccount.setIntegratedSystem(HpcIntegratedSystem.IRODS);
    	dataManagementAccount.setUsername("unittest");
    	dataManagementAccount.setPassword("pwd");
    	
    	HpcUser user = new HpcUser();
    	user.setNciAccount(nciAccount);
    	
    	try {
    	     securityService.setRequestInvoker(user.getNciAccount(), HpcAuthenticationType.LDAP, dataManagementAccount);
    	     
    	} catch(HpcException e) {
    		    logger.error("Authentication failed", e);
    	}
    }
} 
