/**
 * HpcSecurityBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.datamanagement.HpcGroupRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcGroupResponseDTO;
import gov.nih.nci.hpc.dto.user.HpcAuthenticationRequestDTO;
import gov.nih.nci.hpc.dto.user.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.user.HpcUpdateUserRequestDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC User Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcSecurityBusService.java 1013 2016-03-26 23:06:30Z rosenbergea $
 */

public interface HpcSecurityBusService 
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
     * @param nciUserId The user ID to update.
     * @param updateUserRequestDTO The update request DTO.
     * 
     * @throws HpcException
     */
    public void updateUser(String nciUserId, 
    		               HpcUpdateUserRequestDTO updateUserRequestDTO)  
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
    
    /**
     * Set (create or update) a group and assign/remove users.
     *
     * @param groupRequest The request DTO to create/update a group.
     * @return A list of result for each user.
     * 
     * @throws HpcException
     */
	public HpcGroupResponseDTO setGroup(HpcGroupRequestDTO groupRequest) throws HpcException;

}

 