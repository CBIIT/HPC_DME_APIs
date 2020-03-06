/**
 * HpcAuthorizationExceptionMapper.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.provider;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.cxf.interceptor.security.AccessDeniedException;

/**
 * <p>
 * Mapping CXF Exception to REST response.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcAuthorizationExceptionMapper extends HpcExceptionMapper
    implements ExceptionMapper<AccessDeniedException> {
  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /**
   * Constructor for Spring Dependency Injection.
   * 
   * @param stackTraceEnabled If set to true, stack trace will be attached to exception DTO.
   */
  public HpcAuthorizationExceptionMapper(boolean stackTraceEnabled) {
    super(stackTraceEnabled);
  }

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // ExceptionMapper<HpcAuthenticationException>
  // ---------------------------------------------------------------------//

  @Override
  public Response toResponse(AccessDeniedException exception) {
    HpcExceptionDTO exceptionDTO = new HpcExceptionDTO();
    exceptionDTO.setMessage("Access Denied: " + exception.getMessage());
    exceptionDTO.setErrorType(HpcErrorType.REQUEST_AUTHENTICATION_FAILED);

    if (getStackTraceEnabled()) {
      StringWriter writer = new StringWriter();
      exception.printStackTrace(new PrintWriter(writer));
      exceptionDTO.setStackTrace(writer.toString());
    }

    return Response.status(Response.Status.UNAUTHORIZED).entity(exceptionDTO)
        .type(getAcceptedMediaType()).build();
  }
}
