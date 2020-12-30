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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcModelBuilder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.Cookie;
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
  
    @Value("${gov.nih.nci.hpc.ssl.cert}")
    protected String sslCertPath;
    @Value("${gov.nih.nci.hpc.ssl.cert.password}")
    protected String sslCertPassword;
    @Value("${gov.nih.nci.hpc.server.model}")
    private String hpcModelURL;
    @Value("${gov.nih.nci.hpc.server.user}")
    private String serviceUserURL;
    @Value("${gov.nih.nci.hpc.server.user.authenticate}")
    private String authenticateURL;
    @Value("${app.version:}")
    protected String version;
    @Value("${app.env:}")
    protected String env;
    @Value("${dme.token.expiration.period:120}")
    private int tokenExpirationPeriod;
    
    @Autowired
    private HpcModelBuilder hpcModelBuilder;
    
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
		String userId = (String) session.getAttribute("hpcUserId");
		String smUser = request.getHeader("SM_USER");
		Date tokenExpiration = (Date) session.getAttribute("tokenExpiration");

        if (StringUtils.isBlank(userId) || StringUtils.isNotBlank(smUser) && isTokenExpired(tokenExpiration)) {
            //Expired session or token expiration
            userId = smUser;

            if (StringUtils.isBlank(userId)) {
			    //SM Header not available, redirect to login page
				log.error(
						"redirect to login - no authorized user to work with in SM_USER "
								+ "header");
				response.sendRedirect("/login");
				return false;
			} else {
			    HpcUserDTO user = null;
			    try {
		            String smSession = getCookieValue(request, "NIHSMSESSION");
		            String action = request.getRequestURI();
    	            String authToken = HpcClientUtil.getAuthenticationTokenSso(userId, smSession,
    	                    authenticateURL);
    	            session.setAttribute("hpcUserToken", authToken);
    	            try {
    	              user = HpcClientUtil.getUser(authToken, serviceUserURL, sslCertPath, sslCertPassword);
    	                if (user == null)
    	                    throw new HpcWebException("Invalid User");
    	                log.info("getting DOCModel for user: " + user.getFirstName() + " " + user.getLastName());            
    	                //Get DOC Models, go to server only if not available in cache
    	                HpcDataManagementModelDTO modelDTO = hpcModelBuilder.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
    	                
    	                if (modelDTO != null)
    	                    session.setAttribute("userDOCModel", modelDTO);
    	                
    	            } catch (HpcWebException e) {
    	                log.error("Authentication failed. " + e.getMessage());
    	                throw new HpcAuthorizationException("You are not authorized to view this page.");
    	            }
    	            
    	            // Calculate the token expiration date.
    	    		Calendar tokenExpirationDate = Calendar.getInstance();
    	    		tokenExpirationDate.add(Calendar.MINUTE, tokenExpirationPeriod);
    	    		session.setAttribute("tokenExpiration", tokenExpirationDate.getTime());

    	            session.setAttribute("hpcUserId", userId);
    	            session.setAttribute("hpcUser", user);
    	            session.setAttribute("env", env);
    	            session.setAttribute("version", version);
    	            
    	            if(action.equals("/")) {
    	                response.sendRedirect("/login");
    	                return false;
    	            }
    	        } catch (Exception e) {
    	            e.printStackTrace();
    	            log.error("Authentication failed. " + e.getMessage());
    	            throw new HpcAuthorizationException("You are not authorized to view this page.");
    	        }
			}
		}

		return super.preHandle(request, response, handler);
	}
	
	private String getCookieValue(HttpServletRequest req, String cookieName) {
	    if(req.getCookies() != null)
	      return Arrays.stream(req.getCookies())
	            .filter(c -> c.getName().equals(cookieName))
	            .findFirst()
	            .map(Cookie::getValue)
	            .orElse(null);
	    return "";
	}
	
	private boolean isTokenExpired(Date tokenExpiration) {
		// Check the expiration date.
		return tokenExpiration == null || tokenExpiration.before(new Date());
	}
}
