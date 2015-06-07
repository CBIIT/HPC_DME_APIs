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
     * Get the registration service.
     *
     * @return A registration service proxy instance.
     */
    public HpcDataRegistrationRestService getRegistrationServiceProxy();
}

 