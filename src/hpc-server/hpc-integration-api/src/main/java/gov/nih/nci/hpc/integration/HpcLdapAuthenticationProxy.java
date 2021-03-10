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

import gov.nih.nci.hpc.domain.model.HpcDistinguishedNameSearchResult;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Authentication Proxy Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
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
	
	/** 
     * Get a distinguished name by performing an LDAP seach in the following way:
     * 1. Search for keywords='keywordPrefix':'id' in the 'searchBase' base (context).
     * 2. Extract the parentLink attribute from the object found.
     * 3. Search for objectSide=parentLink in the NIH full base context, and return the distinguished name found.
     * 
     * @param id The ID to search for. (this is Unix uid / gid value)
     * @param keywordPrefix prefix to use when searching for keywords attribute (uid, gid, etc)
     * @param searchBase the search base where to look for the id.
     * @return The HpcDistinguishedNameSearchResult with DN found in the search base + mapping of the DN in full AD NIH search base.
     * @throws HpcException on LDAP failure.
     */
	public HpcDistinguishedNameSearchResult getDistinguishedName(int id, String keywordPrefix, String searchBase) throws HpcException;
}
