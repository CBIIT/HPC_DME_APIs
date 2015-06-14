/**
 * HpcProxy.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.client;

import gov.nih.nci.hpc.ws.rs.HpcDataRegistrationRestService;
import gov.nih.nci.hpc.ws.rs.HpcUserRegistrationRestService;

/**
 * <p>
 * HPC Proxy Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcProxy
{    
    /**
     * Get the data registration service.
     *
     * @return A data registration service proxy instance.
     */
    public HpcDataRegistrationRestService getRegistrationServiceProxy();
    
    /**
     * Get the user registration service.
     *
     * @return A user registration service proxy instance.
     */
    public HpcUserRegistrationRestService getUserRegistrationServiceProxy();
}

 