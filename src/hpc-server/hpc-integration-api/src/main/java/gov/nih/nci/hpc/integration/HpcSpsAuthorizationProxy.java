/**
 * HpcSpsAuthorizationProxy.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC SPS Authorization Proxy Interface.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$ 
 */

public interface HpcSpsAuthorizationProxy 
{
    /** 
     * Authorize smSession. 
     * 
     * @param nciUserId The user's nciUserId.
     * @param smSession The smSession cookie.
     * @param username The user name to authenticate to SPS.
     * @param password The password to authenticate to SPS.
     * @return True if the smSession was successfully authorized, or false otherwise.
     * @throws HpcException on SPS authorization failure.
     */
	public boolean authorize(String nciUserId, String smSession, String username, String password) throws HpcException;
}
