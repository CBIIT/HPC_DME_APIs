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

import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcSystemAccountDTO;
import gov.nih.nci.hpc.dto.security.HpcUpdateUserRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
     * User registration.
     *
     * @param userRegistrationDTO The user DTO to register.
     * @return The REST service response.
     */
    @PUT
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response registerUser(HpcUserDTO userRegistrationDTO);

    /**
     * Update a user.
     *
     * @param nciUserId The user ID to update.
     * @param updateUserRequestDTO The update request DTO.
     * @return The REST service response.
     */
    @POST
    @Path("/user/{nciUserId}")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response updateUser(@PathParam("nciUserId") String nciUserId,
    		                   HpcUpdateUserRequestDTO updateUserRequestDTO);

    /**
     * Get a user by NCI user id.
     *
     * @param nciUserId The registered user ID.
     * @return gov.nih.nci.hpc.dto.security.HpcUserDTO entity.
     * @return The REST service response.
     */
    @GET
    @Path("/user/{nciUserId}")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getUser(@PathParam("nciUserId") String nciUserId);
    
    /**
     * Get users by search criterias.
     *
     * @param nciUserId The user ID to search for.
     * @param firstName The first name to search for.
     * @param lastName The last name to search for.
     * @return gov.nih.nci.hpc.dto.security.HpcUserListDTO entity.
     * @return The REST service response.
     */
    @GET
    @Path("/user")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getUsers(@QueryParam("nciUserId") String nciUserId,
    		                 @QueryParam("firstName") String firstName,
    		                 @QueryParam("lastName") String lastName);

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
     * Group registration.
     *
     * @param groupName The group name.
     * @param groupMembersRequest (Optional) request to add users to the registered group.
     * @return The REST service response.
     */
	@PUT
	@Path("/group/{groupName}")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response registerGroup(@PathParam("groupName") String groupName,
			                     HpcGroupMembersRequestDTO groupMembersRequest);
	
    /**
     * Group update.
     *
     * @param groupName The group name.
     * @param groupMembersRequest Request to add/remove users to/from a group.
     * @return The REST service response.
     */
	@POST
	@Path("/group/{groupName}")
	@Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
	public Response updateGroup(@PathParam("groupName") String groupName,
			                    HpcGroupMembersRequestDTO groupMembersRequest);
	
    /**
     * Get a group by name.
     *
     * @param groupName The group name
     * @return The REST service response.
     */
    @GET
    @Path("/group/{groupName}")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getGroup(@PathParam("groupName") String groupName);
    
    /**
     * Get groups by search criteria.
     *
     * @param groupSearchCriteria (Optional) The group search criteria (In the form of SQL 'LIKE').
     *                            If null - then all groups are returned.
     * @return The REST service response.
     */
    @GET
    @Path("/group")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getGroups(@QueryParam("groupSearchCriteria") String groupSearchCriteria);
    
    /**
     * Delete a group.
     *
     * @param groupName The group name
     * @return The REST service response.
     */
    @DELETE
    @Path("/group/{groupName}")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response deleteGroup(@PathParam("groupName") String groupName);

    /**
     * Register system account.
     *
     * @param systemAccountRegistrationDTO The system account DTO to register.
     * @return The REST service response.
     */
    @PUT
    @Path("/systemAccount")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response registerSystemAccount(HpcSystemAccountDTO systemAccountRegistrationDTO);
}

