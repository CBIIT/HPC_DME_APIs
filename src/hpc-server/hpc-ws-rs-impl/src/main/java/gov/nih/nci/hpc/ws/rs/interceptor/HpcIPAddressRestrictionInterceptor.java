/**
 * HpcIPAddressRestrictionInterceptor.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.interceptor;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
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
 * HPC IP Address Restriction Interceptor.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcIPAddressRestrictionInterceptor extends AbstractPhaseInterceptor<Message> {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Restricted list of IP address.
	@Value("#{'${hpc.ws.rs.auth.restrictedIPAddress}'.split(',')}")
	private List<String> restrictedIPAddress = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	public HpcIPAddressRestrictionInterceptor() {
		super(Phase.RECEIVE);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// AbstractPhaseInterceptor<Message> Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public void handleMessage(Message message) {
		// Validate the caller's IP address and restrict user's role if required.
		if (!CollectionUtils.isEmpty(restrictedIPAddress) && StringUtils.isNotEmpty(restrictedIPAddress.get(0))) {
			try {
				HttpServletRequest request = (HttpServletRequest) message.get(AbstractHTTPDestination.HTTP_REQUEST);
				String remoteAddress = request.getRemoteAddr();

				if (StringUtils.isNotBlank(request.getHeader("X-Forwarded-For"))) {
					remoteAddress = request.getHeader("X-Forwarded-For");
				}

				if (isRestrictedIPAddress(remoteAddress)) {
					// Set a security context with the restricted user's role.
					HpcSecurityContext sc = new HpcSecurityContext(HpcUserRole.RESTRICTED.value());
					message.put(SecurityContext.class, sc);
				}
			} catch (Exception e) {
				logger.error("Error occurred during IP address restriction validation.", e);
				throw new AccessDeniedException("Error occurred during IP address restriction validation.");
			}
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	private boolean isRestrictedIPAddress(String remoteAddress) {
		for (String ip : restrictedIPAddress) {
			IpAddressMatcher ipAddressMatcher = new IpAddressMatcher(ip);
			if (ipAddressMatcher.matches(remoteAddress))
				return true;
		}
		return false;
	}
}
