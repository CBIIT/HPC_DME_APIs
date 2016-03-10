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

import gov.nih.nci.hpc.dto.user.HpcAuthenticationRequestDTO;
import gov.nih.nci.hpc.dto.user.HpcAuthenticationResponseDTO;
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
     * Update a User.
     *
     * @param userDTO The user DTO.
     * 
     * @throws HpcException
     */
    public void updateUser(HpcUserDTO userDTO) 
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
     * Authenticate user.
     *
     * @param authenticationRequest The authentication request.
     * @param ldapAuthentication Set to true if LDAP authentication should be performed.
     * @return HpcAuthenticationResponseDTO Authentication results.
     * 
     * @throws HpcException
     */
    public HpcAuthenticationResponseDTO authenticate(
    		                            HpcAuthenticationRequestDTO authenticationRequest,
    		                            boolean ldapAuthentication) 
    		                            throws HpcException;  
}

 