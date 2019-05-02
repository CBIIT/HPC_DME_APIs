/**
 * HpcSecurityRestService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * HPC Security Single Sign On Service Interface.
 *
 * @author dinhys
 */
@Path("/")
public interface HpcSecuritySSOService {

  /**
   * Authenticate a user.
   *
   * @return The REST service response w/ HpcAuthenticationResponseDTO entity.
   */
  @GET
  @Path("/authenticate")
  @Produces("application/json; charset=UTF-8, application/xml; charset=UTF-8")
  public Response authenticate();

}
