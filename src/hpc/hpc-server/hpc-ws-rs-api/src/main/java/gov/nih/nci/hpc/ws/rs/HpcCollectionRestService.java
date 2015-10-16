/**
 * HpcCollectionRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs;

import gov.nih.nci.hpc.dto.collection.HpcCollectionRegistrationDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * <p>
 * HPC Collection REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@Path("/collection")
public interface HpcCollectionRestService
{   
    /**
     * PUT Collection registration request.
     *
     * @param path The collection path.
     */
	@PUT
	@Path("{path:.*}")
	@Consumes("application/json,application/xml")
	@Produces("application/json,application/xml")
	public Response addCollection(@PathParam("path") final String path,
			                      final HpcCollectionRegistrationDTO collectionRegistrationDTO);
}

 