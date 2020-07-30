/**
 * HpcUserInterceptor.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://github.com/CBIIT/HPC_DME_APIs/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * HPC User Interceptor
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */
@Component
public class HpcUserInterceptor extends HandlerInterceptorAdapter {
	private static final Logger log = LoggerFactory.getLogger(HpcUserInterceptor.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.web.servlet.handler.HandlerInterceptorAdapter#preHandle(
	 * javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse, java.lang.Object)
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		HttpSession session = request.getSession();
		String user = (String) session.getAttribute("hpcUserId");

		if (StringUtils.isBlank(user)) {
			user = request.getHeader("SM_USER");
			HpcUserDTO userDto = (HpcUserDTO) session.getAttribute("hpcUser");

			if (StringUtils.isBlank(user) || userDto == null) {
				log.error(
						"redirect to login - no authorized user to work with in SM_USER "
								+ "header");
				response.sendRedirect("/login");
				return false;
			}

			session.setAttribute("hpcUserId", user);
		}

		return super.preHandle(request, response, handler);
	}

}
