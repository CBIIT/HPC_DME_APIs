/**
 * HpcSecurityContext.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.interceptor;

import org.apache.cxf.common.security.SimpleSecurityContext;

/**
 * <p>
 * HPC Authentication Interceptor.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcSecurityContext extends SimpleSecurityContext
{
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Constructor.
     * 
     * @param userRole The user's role.
     * 
     */
    public HpcSecurityContext(String userRole) 
    {
        super(userRole);
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    @Override
    public boolean isUserInRole(String role) 
    {
    	return getUserPrincipal().getName().equals(role);
    }
} 