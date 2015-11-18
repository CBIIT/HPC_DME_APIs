/**
 * HpcUserBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.user.HpcUserCredentialsDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC User Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcUserBusService 
{         
    /**
     * Register a User.
     *
     * @param userRegistrationDTO The user registration DTO.
     * 
     * @throws HpcException
     */
    public void registerUser(HpcUserDTO userRegistrationDTO) 
    		                throws HpcException;
    
    /**
     * Get a user by its NCI user id.
     *
     * @param nciUserId The user's NCI user id.
     * @return The registered user DTO or null if not found.
     * 
     * @throws HpcException
     */
    public HpcUserDTO getUser(String nciUserId) throws HpcException;
    
    
    /**
     * Authenticate User by NCI LDAP credentials.
     *
     * @param credentials The user's NCI user id and password.
     * @param ldapAuthentication Set to true if LDAP authentication should be performed.
     * @return boolean Returns true if the user was successfully authenticated.
     * 
     * @throws HpcException
     */
    public boolean authenticate(HpcUserCredentialsDTO credentials) throws HpcException;  
    
    /**
     * Authenticate User and populate the request context with the user.
     * LDAP authentication is optional.
     *
     * @param nciUserId The user ID.
     * @param password The password.
     * @param ldapAuthentication LDAP authentication will be skipped if this is set to false
     * @return boolean Returns true if the user was successfully authenticated.
     * 
     * @throws HpcException
     */
    public boolean authenticate(String nciUserId, String password, boolean ldapAuthentication)
    		                   throws HpcException; 
}

 