/**
 * HpcRequestContext.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.model.HpcUser;

/**
 * <p>
 * HPC Request Context. Holds specific request (service call) data.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcRequestContext 
{      
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	private static final ThreadLocal<HpcUser> requestInvoker = 
			new ThreadLocal<HpcUser>() 
			{
				@Override protected HpcUser initialValue() 
				{
					return new HpcUser();
				}
	        };
	
    /**
     * Get user who invoked this service-call.
     *
     * @return The HPC user who invoked this service.
     */
    public static HpcUser getRequestInvoker() 
    {
        return requestInvoker.get();
    }
    
    /**
     * Set the user who invoked this service-call.
     *
     * @param user The HPC user who invoked this service.
     */
    public static void setRequestInvoker(HpcUser user)
    {
        requestInvoker.set(user);
    }
}

 