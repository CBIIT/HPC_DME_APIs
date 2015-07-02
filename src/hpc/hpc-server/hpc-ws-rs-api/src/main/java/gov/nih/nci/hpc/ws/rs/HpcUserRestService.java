/**
 * HpcUserRegistrationRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs;

import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Response;

/**
 * <p>
 * HPC User REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@Path("/")
public interface HpcUserRestService
{    
    /**
     * GET user by ID.
     *
     * @param userDTO The user DTO to register.
     */
    @POST
    @Path("/user")
    @Consumes("application/json,application/xml")
    public Response registerUser(HpcUserRegistrationDTO userRegistrationDTO);
    
    /**
     * Get user by NIH User ID.
     *
     * @param nihUserId The registered user ID.
     * @return gov.nih.nci.hpc.dto.user.HpcUserDTO entity.
     */
    @GET
    @Path("/user/{nihUserId}")
    @Produces("application/json,application/xml")
    public Response getUser(@PathParam("nihUserId") String nihUserId); 
}

 