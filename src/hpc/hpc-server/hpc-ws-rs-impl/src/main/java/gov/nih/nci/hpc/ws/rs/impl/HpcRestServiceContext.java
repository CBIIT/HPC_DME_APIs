/**
 * HpcRestServiceContext.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * <p>
 * HPC REST Service Context.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcRestServiceContext 
{   
    /**
     * Set the URI Info.
     *
     * @param The context URI Info instance..
     */
	@Context 
    public void setUriInfo(UriInfo uriInfo); 
}

 