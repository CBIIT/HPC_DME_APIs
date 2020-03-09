/**
 * HpcMultipartExceptionMapper.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.provider;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.apache.cxf.io.CacheSizeExceededException;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * Mapping CXF Exception to REST response.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcWebApplicationExceptionMapper extends HpcExceptionMapper
    implements ExceptionMapper<WebApplicationException> {
  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The Default exception mapper.
  private WebApplicationExceptionMapper defaultExceptionMapper =
      new WebApplicationExceptionMapper();

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /**
   * Constructor for Spring Dependency Injection.
   * 
   * @param stackTraceEnabled If set to true, stack trace will be attached to exception DTO.
   */
  public HpcWebApplicationExceptionMapper(boolean stackTraceEnabled) {
    super(stackTraceEnabled);
  }

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // ExceptionMapper<HpcAuthenticationException>
  // ---------------------------------------------------------------------//

  @Override
  public Response toResponse(WebApplicationException exception) {
    if (exception.getCause() instanceof CacheSizeExceededException) {
      // This exception is thrown when sync upload size exceeded the configured limit.
      return toResponse(new HpcException("File size exceeds the sync upload limit",
          HpcErrorType.INVALID_REQUEST_INPUT, exception)).build();
    }

    // Else - Use the default mapper.
    return defaultExceptionMapper.toResponse(exception);
  }
}
