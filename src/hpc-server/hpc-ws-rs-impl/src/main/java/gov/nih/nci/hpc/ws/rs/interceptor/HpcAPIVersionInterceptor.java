/**
 * HpcAPIVersionInterceptor.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.interceptor;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.beans.factory.annotation.Value;

/**
 * HPC API Version Interceptor.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcAPIVersionInterceptor extends AbstractPhaseInterceptor<Message> {
  //---------------------------------------------------------------------//
  // Constants
  //---------------------------------------------------------------------//

  // API Version HEADER.
  public static final String API_VERSION_HEADER = "HPC-API-Version";

  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // The API version.
  @Value("${hpc.ws.rs.api-version}")
  private String version = null;

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /** Default Constructor disabled. */
  private HpcAPIVersionInterceptor() {
    super(Phase.MARSHAL);
  }

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  /**
   * Return a response builder w/ the custome API version header.
   *
   * @param responseBuilder The response builder to add the header to.
   * @return The response builder w/ the API version header in it.
   */
  public Response.ResponseBuilder header(Response.ResponseBuilder responseBuilder) {
    return responseBuilder.header(API_VERSION_HEADER, version);
  }

  //---------------------------------------------------------------------//
  // AbstractPhaseInterceptor<Message> Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public void handleMessage(Message message) {
    @SuppressWarnings("unchecked")
    MultivaluedMap<String, Object> headers =
        (MetadataMap<String, Object>) message.get(Message.PROTOCOL_HEADERS);

    if (headers == null) {
      headers = new MetadataMap<>();
    }

    headers.add(API_VERSION_HEADER, version);
    message.put(Message.PROTOCOL_HEADERS, headers);
  }
}
