/**
 * HpcProfileInterceptor.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.interceptor;

import javax.servlet.http.HttpServletRequest;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import gov.nih.nci.hpc.bus.HpcSecurityBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Profile Interceptor.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcProfileInterceptor extends AbstractPhaseInterceptor<Message> {
  //---------------------------------------------------------------------//
  // Constants
  //---------------------------------------------------------------------//

  // The attribue name to save the service invoke time.
  private static String SERVICE_INVOKE_TIME_MC_ATTRIBUTE =
      "gov.nih.nci.hpc.ws.rs.interceptor.HpcProfileInterceptor.serviceInvokeTime";
  private static String SERVICE_URI_MC_ATTRIBUTE =
      "gov.nih.nci.hpc.ws.rs.interceptor.HpcProfileInterceptor.serviceURI";
  //The invoker attribue name to saved in HpcAuthenticationInterceptor
  private static String USER_ID_MC_ATTRIBUTE =
		      "gov.nih.nci.hpc.ws.rs.interceptor.HpcAuthenticationInterceptor.userId";
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // The interceptor phase
  private String phase = null;

  // The Security Business Service instance.
  @Autowired private HpcSecurityBusService securityBusService = null;

  // The Logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /**
   * Default Constructor disabled.
   *
   * @throws HpcException Constructor disabled.
   */
  private HpcProfileInterceptor() throws HpcException {
    super(Phase.RECEIVE);
    throw new HpcException("Constructor disabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
  }

  /**
   * Constructor for Spring Dependency Injection.
   *
   * @param phase The CXF phase. It is expected to have 2 instances of this interceptor. The first
   *     with Phase.RECEIVE to capture the start time of a RS call. The second instance with
   *     Phase.SEND_ENDING to stop the timer and log performance result of the RS service call.
   * @throws HpcException If provided phase is not Phase.RECEIVE or Phase.SEND_ENDING.
   */
  private HpcProfileInterceptor(String phase) throws HpcException {
    super(phase);
    this.phase = phase;

    if (phase.equals(Phase.RECEIVE)) {
      // Start the service profiling before we authenticate the caller.
      getBefore().add(HpcAuthenticationInterceptor.class.getName());
    } else if (phase.equals(Phase.SEND_ENDING)) {
      // Stop the service profiling after we cleaned up resources post service call.
      getAfter().add(HpcCleanupInterceptor.class.getName());
    } else {
      throw new HpcException(
          "Invalid CXF phase: " + phase, HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }
  }

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // AbstractPhaseInterceptor<Message> Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public void handleMessage(Message message) {
    if (phase.equals(Phase.RECEIVE)) {
      HttpServletRequest request =
          (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);

      // Construct a string w/ the service URI.
      String serviceURI =
          request.getMethod()
              + " "
              + request.getRequestURI()
              + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

      logger.info("RS called: {}", serviceURI);
      message.getExchange().put(SERVICE_INVOKE_TIME_MC_ATTRIBUTE, System.currentTimeMillis());
      message.getExchange().put(SERVICE_URI_MC_ATTRIBUTE, serviceURI);

    } else if (phase.equals(Phase.SEND_ENDING)) {

      // Log the service invoker
      HpcAuthenticationResponseDTO authenticationResponse = null;
      try {
        authenticationResponse = securityBusService.getAuthenticationResponse(false);

      } catch (HpcException e) {
        // Ignore this exception.
        logger.error("Failed to get authentication response", e);
      }
      
      String serviceInvoker =
          authenticationResponse != null
              ? authenticationResponse.getUserId()
                  + "["
                  + authenticationResponse.getUserRole()
                  + " authenticated via "
                  + authenticationResponse.getAuthenticationType().toString()
                  + "]"
              : "Unknown';";
      
      // Log the service completion time
      Long startTime = (Long) message.getExchange().get(SERVICE_INVOKE_TIME_MC_ATTRIBUTE);
      String serviceURI = (String) message.getExchange().get(SERVICE_URI_MC_ATTRIBUTE);
      if (startTime != null && serviceURI != null) {
        logger.info(
            "RS completed: {} - Service execution time: {} milliseconds. RS invoker: {}",
            serviceURI,
            System.currentTimeMillis() - startTime,
            serviceInvoker);
      }
    }
  }
}
