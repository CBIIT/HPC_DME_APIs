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

import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;
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
     * @return The registered user ID.
     * 
     * @throws HpcException
     */
    public String registerUser(HpcUserRegistrationDTO userRegistrationDTO) 
    		                  throws HpcException;
    
    /**
     * Get a user by its NIH user id.
     *
     * @param nihUserId The user's NIH user id.
     * @return The registered user DTO or null if not found.
     * 
     * @throws HpcException
     */
    public HpcUserDTO getUser(String nihUserId) throws HpcException;
}

 