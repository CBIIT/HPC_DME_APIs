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

import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;
import gov.nih.nci.hpc.ws.rs.HpcUserRestService;

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
     * Get the user registration service.
     *
     * @return A user rest service proxy instance.
     */
    public HpcUserRestService getUserRestServiceProxy();
    
    /**
     * Get the data management service.
     *
     * @return A dataset rest service proxy instance.
     */
    public HpcDataManagementRestService getDataManagementRestServiceProxy();
}

 