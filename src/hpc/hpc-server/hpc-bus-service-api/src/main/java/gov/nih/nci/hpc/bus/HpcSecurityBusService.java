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

import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupResponseDTO;
import gov.nih.nci.hpc.dto.security.HpcSystemAccountDTO;
import gov.nih.nci.hpc.dto.security.HpcUpdateUserRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
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
     * @param userName The user's name.
     * @param password The user's password.
     * @param ldapAuthentication Perform LDAP authentication indicator.
     * @return HpcAuthenticationResponseDTO.
     * 
     * @throws HpcException
     */
    public HpcAuthenticationResponseDTO 
           authenticate(String userName, String password, 
        		        boolean ldapAuthentication) throws HpcException;  
    
    /**
     * Authenticate user.
     *
     * @param authenticationToken An Authentication token.
     * @return HpcAuthenticationResponseDTO.
     * 
     * @throws HpcException
     */
    public HpcAuthenticationResponseDTO authenticate(String authenticationToken) 
    		                                         throws HpcException;  
    
    /**
     * Get the authentication response for the current request invoker.
     *
     * @return HpcAuthenticationResponseDTO.
     * 
     * @throws HpcException
     */
    public HpcAuthenticationResponseDTO getAuthenticationResponse() throws HpcException; 
    
    /**
     * Set (create or update) a group and assign/remove users.
     *
     * @param groupRequest The request DTO to create/update a group.
     * @return A list of result for each user.
     * 
     * @throws HpcException
     */
	public HpcGroupResponseDTO setGroup(HpcGroupRequestDTO groupRequest) throws HpcException;
	
    /**
     * Register a System Account.
     *
     * @param systemAccountRegistrationDTO The system account registration DTO.
     * 
     * @throws HpcException
     */
    public void registerSystemAccount(HpcSystemAccountDTO systemAccountRegistrationDTO) 
    		                         throws HpcException;
}

 