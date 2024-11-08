/**
 * HpcIPAddressRestrictionInterceptor.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.interceptor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import gov.nih.nci.hpc.bus.HpcSecurityBusService;
import gov.nih.nci.hpc.dto.security.HpcAuthenticationResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;
import io.github.bucket4j.Bucket;

/**
 * HPC User Rate Limit Interceptor.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcUserRateLimitInterceptor extends AbstractPhaseInterceptor<Message> {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Enable or disable rate limiting (default false).
	@Value("${hpc.ws.rs.auth.enableUserRateLimit:false}")
	private Boolean enableUserRateLimit = null;

	// Max user requests allowed within a duration.
	@Value("${hpc.ws.rs.auth.userRateLimitMaxRequest}")
	private Integer userRateLimitMaxRequest = null;

	// Duration in seconds.
	@Value("${hpc.ws.rs.auth.userRateLimitDurationSeconds}")
	private Integer userRateLimitDurationSeconds = null;

	// The Security Business Service instance.
	@Autowired
	private HpcSecurityBusService securityBusService = null;
	
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	public HpcUserRateLimitInterceptor() {
		super(Phase.RECEIVE);
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// AbstractPhaseInterceptor<Message> Interface Implementation
	// ---------------------------------------------------------------------//
	private final Map<String, Bucket> buckets = new HashMap<>();

	@Override
	public void handleMessage(Message message) {
		// Check if User Rate Limit is enabled
		if (enableUserRateLimit) {
			
			// Get User information
			SecurityContext sc = message.get(SecurityContext.class);
			HpcAuthenticationResponseDTO authenticationResponse = null;
			try {
				authenticationResponse = securityBusService.getAuthenticationResponse(false);

			} catch (HpcException e) {
				// Ignore this exception.
				logger.error("Failed to get authentication response", e);
			}

			String userId = authenticationResponse.getUserId();

			// Check role if system admin
			if (!sc.isUserInRole("SYSTEM_ADMIN")) {

				if (isUserRateLimitExceeded(userId)) {
					throw new AccessDeniedException("Too Many Requests.");
				}
			}
		}
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	private boolean isUserRateLimitExceeded(String userId) {
		// Get bucket for a specific user
		Bucket userBucket = getBucket(userId);
		// Check if the user can make a request
		if (userBucket.tryConsume(1)) {
			return false;
		}
		return true;
	}

	private Bucket getBucket(String userId) {
		return buckets.computeIfAbsent(userId, this::createBucket);
	}

	private Bucket createBucket(String userId) {
		// Create a new bucket for the given userId
		// Bandwidth defines the maximum count of tokens which can be held by bucket.
		// Setting capacity and refill tokens per duration to be the same which will
		// guarantee we don't go over the max requests.
		return Bucket
				.builder().addLimit(limit -> limit.capacity(userRateLimitMaxRequest)
						.refillGreedy(userRateLimitMaxRequest, Duration.ofSeconds(userRateLimitDurationSeconds)))
				.build();
	}
}
