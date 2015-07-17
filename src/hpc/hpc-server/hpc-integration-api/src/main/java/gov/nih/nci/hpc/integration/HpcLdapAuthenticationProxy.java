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

import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Authentication Proxy Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id:  $ 
 */

public interface HpcLdapAuthenticationProxy {
	public boolean authenticate(String userName, String password) throws HpcException;
}
