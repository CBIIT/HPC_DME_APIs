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
import gov.nih.nci.hpc.dto.security.HpcUserRequestDTO;

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
     * @param nciUserId The NCI user ID to register.
     * @param userRegistrationRequest The user registration request DTO.
     * @return The REST service response. 
     */
    @PUT
    @Path("/user/{nciUserId}")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response registerUser(@PathParam("nciUserId") String nciUserId,
    		                     HpcUserRequestDTO userRegistrationRequest);

    /**
     * Update a user.
     *
     * @param nciUserId The user ID to update.
     * @param userUpdateRequest The user update request DTO.
     * @return The REST service response.
     */
    @POST
    @Path("/user/{nciUserId}")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response updateUser(@PathParam("nciUserId") String nciUserId,
    		                   HpcUserRequestDTO userUpdateRequest);

    /**
     * Get a user by NCI user id.
     *
     * @param nciUserId The registered user ID.
     * @return The REST service response w/ HpcUserDTO entity.
     */
    @GET
    @Path("/user/{nciUserId}")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getUser(@PathParam("nciUserId") String nciUserId);
    
    /**
     * Get the invoker user.
     *
     * @return The REST service response w/ HpcUserDTO entity.
     */
    @GET
    @Path("/user")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getInvoker();
    
    /**
     * Get users by search criterias. Note: only active users are returned.
     *
     * @param nciUserId (Optional) The user ID to search for (using case insensitive comparison).
     * @param firstNamePattern (Optional) The first-name pattern to search for (using case insensitive matching).
     *                                    SQL LIKE wildcards ('%', '_') are supported. 
     * @param lastNamePattern (Optional) The last-name pattern to search for (using case insensitive matching).
     *                                   SQL LIKE wildcards ('%', '_') are supported. 
     * @param defaultBasePath The default base path.                      
     * @return The REST service response w/ HpcUserListDTO entity.
     */
    @GET
    @Path("/user/active")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getActiveUsers(@QueryParam("nciUserId") String nciUserId,
    		                       @QueryParam("firstNamePattern") String firstNamePattern,
    		                       @QueryParam("lastNamePattern") String lastNamePattern,
    		                       @QueryParam("defaultBasePath") String defaultBasePath);
    
    /**
     * Get users by search criterias. Note: All users are returned, both active and inactive
     *
     * @param nciUserId (Optional) The user ID to search for (using case insensitive comparison).
     * @param firstNamePattern (Optional) The first-name pattern to search for (using case insensitive matching).
     *                                    SQL LIKE wildcards ('%', '_') are supported. 
     * @param lastNamePattern (Optional) The last-name pattern to search for (using case insensitive matching).
     *                                   SQL LIKE wildcards ('%', '_') are supported. 
     * @param doc User DOC 
     * @return The REST service response w/ HpcUserListDTO entity.
     */
    @GET
    @Path("/user/all")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getAllUsers(@QueryParam("nciUserId") String nciUserId,
    		                    @QueryParam("firstNamePattern") String firstNamePattern,
    		                    @QueryParam("lastNamePattern") String lastNamePattern,
    		                    @QueryParam("doc") String doc);
    
    /**
     * Authenticate a user.
     *
     * @return The REST service response w/ HpcAuthenticationResponseDTO entity.
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
     * @return The REST service response w/ HpcGroupMembersResponseDTO entity.
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
     * @return The REST service response w/ HpcGroupMembersResponseDTO entity.
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
     * @return The REST service response w/ HpcGroupMembersDTO entity.
     */
    @GET
    @Path("/group/{groupName}")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getGroup(@PathParam("groupName") String groupName);
    
    /**
     * Get groups by search criteria.
     *
     * @param groupPattern (Optional) The group pattern to search for (using case insensitive matching).
     *                                SQL LIKE wildcards ('%', '_') are supported. 
     *                                If null - then all groups are returned.
     * @return The REST service response w/ HpcGroupListDTO entity.
     */
    @GET
    @Path("/group")
    @Produces(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response getGroups(@QueryParam("groupPattern") String groupPattern);
    
    /**
     * Delete a group.
     *
     * @param groupName The group name
     * @return The REST service response.
     */
    @DELETE
    @Path("/group/{groupName}")
    public Response deleteGroup(@PathParam("groupName") String groupName);

    /**
     * Register system account.
     *
     * @param systemAccountRegistration The system account DTO to register.
     * @return The REST service response.
     */
    @PUT
    @Path("/systemAccount")
    @Consumes(MediaType.APPLICATION_JSON + "," + MediaType.APPLICATION_XML)
    public Response registerSystemAccount(HpcSystemAccountDTO systemAccountRegistration);
}

