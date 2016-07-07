/**
 * HpcSecurityRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs;

import gov.nih.nci.hpc.dto.security.HpcGroupRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcSystemAccountDTO;
import gov.nih.nci.hpc.dto.security.HpcUpdateUserRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * <p>
 * HPC Security REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcSecurityRestService.java 1013 2016-03-26 23:06:30Z rosenbergea $
 */

@Path("/")
public interface HpcSecurityRestService
{
    /**
     * Register user.
     *
     * @param userRegistrationDTO The user DTO to register.
     */
    @PUT
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response registerUser(HpcUserDTO userRegistrationDTO);

    /**
     * Update an existing user.
     *
     * @param nciUserId The user ID to update.
     * @param updateUserRequestDTO The update request DTO.
     */
    @POST
    @Path("/user/{nciUserId}")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response updateUser(@PathParam("nciUserId") String nciUserId,
    		                   HpcUpdateUserRequestDTO updateUserRequestDTO);

    /**
     * Get user by NCI User ID.
     *
     * @param nciUserId The registered user ID.
     * @return gov.nih.nci.hpc.dto.security.HpcUserDTO entity.
     */
    @GET
    @Path("/user/{nciUserId}")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getUser(@PathParam("nciUserId") String nciUserId);

    /**
     * Authenticate a user.
     *
     * @return gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO entity.
     */
    @GET
    @Path("/authenticate")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response authenticate();

    /**
     * POST Set (create or update) a group and assign/remove users.
     *
     * @param groupRequest The request DTO to create/update a group.
     */
	@POST
	@Path("/group")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response setGroup(HpcGroupRequestDTO groupRequest);

    /**
     * Register system account.
     *
     * @param systemAccountRegistrationDTO The system account DTO to register.
     */
    @PUT
    @Path("/systemAccount")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response registerSystemAccount(HpcSystemAccountDTO systemAccountRegistrationDTO);
}

