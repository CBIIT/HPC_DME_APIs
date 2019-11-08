/**
 * HpcAuthenticationProxy.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration;

import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Authentication Proxy Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$ 
 */

public interface HpcLdapAuthenticationProxy 
{
    /** 
     * Authenticate user. 
     * 
     * @param userName The user name.
     * @param password The password
     * @return True if the user was successfully authenticated, or false otherwise.
     * @throws HpcException on LDAP failure.
     */
	public boolean authenticate(String userName, String password) throws HpcException;
	
	/** 
     * Get user's first and last name. 
     * 
     * @param userName The user name.
     * @return The user's first and last name.
     * @throws HpcException on LDAP failure.
     */
	public HpcNciAccount getUserFirstLastName(String userName) throws HpcException;
}
