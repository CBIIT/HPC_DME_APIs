/**
 * HpcDataRegistrationRestService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs;

import gov.nih.nci.hpc.dto.HpcDataRegistrationInput;
import gov.nih.nci.hpc.dto.HpcDataRegistrationOutput;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Response;

/**
 * <p>
 * HPC Data Registration REST Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcDatasetsRegistrationRestService.java 58 2015-05-15 14:56:07Z rosenbergea $
 */

@Path("/")
public interface HpcDataRegistrationRestService
{    
    /**
     * GET registration request by ID.
     *
     * @param id The registered data ID.
     * @return The registered data.
     */
    @GET
    @Path("/registration/{id}")
    @Produces("application/json,application/xml")
    public Response getRegisterdData(@PathParam("id") String id); 
    
    /**
     * POST registration request.
     *
     * @param registrationInput The data registration input DTO.
     */
    @POST
    @Path("/registration")
    @Consumes("application/json,application/xml")
    public Response registerData(
    		                HpcDataRegistrationInput registrationInput);
}

 