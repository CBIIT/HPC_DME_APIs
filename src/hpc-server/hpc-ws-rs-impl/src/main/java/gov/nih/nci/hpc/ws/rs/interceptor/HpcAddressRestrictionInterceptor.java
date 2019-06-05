/**
 * HpcAddressRestrictionInterceptor.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.interceptor;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.util.CollectionUtils;
import gov.nih.nci.hpc.domain.user.HpcUserRole;

/**
 * HPC Address Restriction Interceptor.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcAddressRestrictionInterceptor extends AbstractPhaseInterceptor<Message> {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  //Restricted list of IP address.
  @Value("#{'${hpc.ws.rs.restrictedAddress}'.split(',')}")
  private List<String> restrictedAddress = null;

  //The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  public HpcAddressRestrictionInterceptor() {
    super(Phase.RECEIVE);
  }

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // AbstractPhaseInterceptor<Message> Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public void handleMessage(Message message) {
    // Validate the caller's IP address and restrict user's role if required.
    if (!CollectionUtils.isEmpty(restrictedAddress) && StringUtils.isNotEmpty(restrictedAddress.get(0))) {
      try {
        HttpServletRequest request =
            (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
        String remoteAddress = request.getRemoteAddr();

        if (StringUtils.isNotBlank(request.getHeader("X-Forwarded-For"))) {
          remoteAddress = request.getHeader("X-Forwarded-For");
        }

        if (isRestrictedAddress(remoteAddress)) {
          // Set a security context with the restricted user's role.
          HpcSecurityContext sc = new HpcSecurityContext(HpcUserRole.RESTRICTED.value());
          message.put(SecurityContext.class, sc);
        }
      } catch (Exception e) {
        logger.error("Error occurred during address restriction validation.", e);
        throw new AccessDeniedException("Error occurred during address restriction validation.");
      }
    }
  }

  //---------------------------------------------------------------------//
  // Helper Methods
  //---------------------------------------------------------------------//

  private boolean isRestrictedAddress(String remoteAddress) {
    for (String ip : restrictedAddress) {
      IpAddressMatcher ipAddressMatcher = new IpAddressMatcher(ip);
      if (ipAddressMatcher.matches(remoteAddress)) return true;
    }
    return false;
  }
}
