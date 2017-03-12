/**
 * HpcDataManagementSecurityServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNciAccount;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcDataManagementAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcDataManagementSecurityService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Data Management Security Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataManagementSecurityServiceImpl implements HpcDataManagementSecurityService
{   
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
    // The Data Management Authenticator.
	@Autowired
    private HpcDataManagementAuthenticator dataManagementAuthenticator = null;
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Constructor for Spring Dependency Injection.
     */
    private HpcDataManagementSecurityServiceImpl()
    {
    }   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDataManagementSecurityService Interface Implementation
    //---------------------------------------------------------------------//  
    
    @Override
    public void addUser(HpcNciAccount nciAccount, HpcUserRole userRole) throws HpcException
    {
    	// Input validation.
    	if(!isValidNciAccount(nciAccount)) {	
    	   throw new HpcException("Invalid NCI Account: Null user ID or name or DOC", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
       	
    	dataManagementProxy.addUser(dataManagementAuthenticator.getAuthenticatedToken(), 
    			                    nciAccount, userRole);
    }
    
    @Override
    public void updateUser(String nciUserId, String firstName, String lastName,
                           HpcUserRole userRole) throws HpcException
    {
    	// Input validation.
    	if(nciUserId == null || firstName == null || lastName == null ||
    	   userRole == null) {	
    	   throw new HpcException("Invalid update user input", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	dataManagementProxy.updateUser(dataManagementAuthenticator.getAuthenticatedToken(), 
    			                       nciUserId, firstName, lastName, userRole);
    }    
    
    @Override
    public void deleteUser(String nciUserId) throws HpcException
    {
    	// Input validation.
    	if(nciUserId == null) {	
    	   throw new HpcException("Invalid NCI user ID", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
       	
    	dataManagementProxy.deleteUser(dataManagementAuthenticator.getAuthenticatedToken(), 
    			                       nciUserId);
    }
    
    @Override
    public HpcUserRole getUserRole(String nciUserId) throws HpcException
    {
    	// Input validation.
    	if(nciUserId == null) {	
    	   throw new HpcException("Invalid NCI user ID", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return dataManagementProxy.getUserRole(dataManagementAuthenticator.getAuthenticatedToken(), 
    			                               nciUserId);
    }

    @Override
    public void addGroup(String groupName) throws HpcException
    {
    	// Input validation.
    	if(groupName == null) {	
    	   throw new HpcException("Null group name", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	dataManagementProxy.addGroup(dataManagementAuthenticator.getAuthenticatedToken(), groupName);
    }
    
    @Override
    public void deleteGroup(String groupName) throws HpcException
    {
    	// Input validation.
    	if(groupName == null) {	
    	   throw new HpcException("Null group name", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	dataManagementProxy.deleteGroup(dataManagementAuthenticator.getAuthenticatedToken(), groupName);
    }
    
    @Override
    public boolean groupExists(String groupName) throws HpcException
    {
    	// Input validation.
    	if(groupName == null) {	
    	   throw new HpcException("Null group name", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return dataManagementProxy.groupExists(dataManagementAuthenticator.getAuthenticatedToken(), groupName);
    }
    
    @Override
    public void addGroupMember(String groupName, String userId) throws HpcException
    {
    	// Input validation.
    	if(groupName == null || userId == null) {	
    	   throw new HpcException("Null group name or user id", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	dataManagementProxy.addGroupMember(dataManagementAuthenticator.getAuthenticatedToken(), 
    			                           groupName, userId);
    }
    
    @Override
    public void deleteGroupMember(String groupName, String userId) throws HpcException
    {
    	// Input validation.
    	if(groupName == null || userId == null) {	
    	   throw new HpcException("Null group name or user id", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	dataManagementProxy.deleteGroupMember(dataManagementAuthenticator.getAuthenticatedToken(), 
    			                              groupName, userId);
    }
    
    @Override
    public List<String> getGroupMembers(String groupName) throws HpcException
    {
    	// Input validation.
    	if(groupName == null) {	
    	   throw new HpcException("Null group name", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return dataManagementProxy.getGroupMembers(dataManagementAuthenticator.getAuthenticatedToken(), 
    			                                   groupName);   	
    }
    
    @Override
    public List<String> getGroups(String groupSearchCriteria) throws HpcException
    {
    	// Input validation.
    	if(groupSearchCriteria == null) {	
    	   throw new HpcException("Null group search criteria", 
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
    	}
    	
    	return dataManagementProxy.getGroups(dataManagementAuthenticator.getAuthenticatedToken(), 
    			                             groupSearchCriteria);      	
    }
    
    @Override
    public HpcDataManagementAccount getHpcDataManagementAccount(Object irodsAccount) throws HpcException
    {
   		return dataManagementProxy.getHpcDataManagementAccount(irodsAccount);
    }
    
    @Override
    public Object getProxyManagementAccount(HpcDataManagementAccount irodsAccount) throws HpcException
    {
   		return dataManagementProxy.getProxyManagementAccount(irodsAccount);
    }  
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
}