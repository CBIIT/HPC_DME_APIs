/**
 * HpcOidcAuthorizationProxy.java
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
 * HPC OIDC Authorization Proxy Interface.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$ 
 */

public interface HpcOidcAuthorizationProxy 
{
	/** 
     * Get username from user info endpoint
     * 
     * @param accessToken The access token.
     * @return The user's username.
     * @throws HpcException on OIDC user info endpoint error
     */
	public String getUsername(String accessToken) throws HpcException;

	/** 
     * Get JWK Set from jwks endpoint
     * 
     * @return The JWK Set
     */
	public String getJWKSet();
	
}
