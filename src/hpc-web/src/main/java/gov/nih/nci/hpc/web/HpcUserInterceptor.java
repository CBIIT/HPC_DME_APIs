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
import org.springframework.web.servlet.HandlerInterceptor;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcIdentityUtil;
import gov.nih.nci.hpc.web.util.HpcModelBuilder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * <p>
 * HPC User Interceptor
 * </p>
 *
 * @author <a href="mailto:Yuri.Dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */
@Component
public class HpcUserInterceptor implements HandlerInterceptor {
  
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
    @Value("${contact.email:}")
    protected String contactEmail;
    @Value("${dme.token.expiration.period:120}")
    private int tokenExpirationPeriod;
    @Value("${gov.nih.nci.hpc.server.childCollections.acl.user}")
	private String childCollectionsAclURL;
    @Value("${gov.nih.nci.hpc.oidc.header}")
    private String oidcHeader;
    
    @Autowired
    private HpcModelBuilder hpcModelBuilder;
    
	private static final Logger log = LoggerFactory.getLogger(HpcUserInterceptor.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.web.servlet.HandlerInterceptor#preHandle(
	 * jakarta.servlet.http.HttpServletRequest,
	 * jakarta.servlet.http.HttpServletResponse, java.lang.Object)
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		HttpSession session = request.getSession();
		String userId = (String) session.getAttribute("hpcUserId");
		String smUser = request.getHeader("SM_USER");
		String oidcAccessToken = request.getHeader(oidcHeader);
		Date tokenExpiration = (Date) session.getAttribute("tokenExpiration");

        if (StringUtils.isBlank(userId) || StringUtils.isNotBlank(smUser) && isTokenExpired(tokenExpiration)) {
            //Expired session or token expiration
            userId = smUser;

            if (StringUtils.isBlank(userId) && StringUtils.isBlank(oidcAccessToken)) {
			    //SM Header not available, redirect to login page
				log.error(
						"redirect to login - no authorized user to work with in SM_USER "
								+ "header");
				response.sendRedirect("/login");
				return false;
			} else {
			    HpcUserDTO user = null;
			    String authToken = null;
			    String action = request.getRequestURI();
			    try {
			    	if(StringUtils.isBlank(oidcAccessToken)) {
			    		// This is to support Site Minder Web agent and no longer used.
			            String smSession = getCookieValue(request, "NIHSMSESSION");
	    	            authToken = HpcClientUtil.getAuthenticationTokenSso(userId, smSession,
	    	                    authenticateURL);
			    	} else {
			    		authToken = HpcClientUtil.getAuthenticationTokenOIDC(oidcAccessToken, authenticateURL);
			    	}
			    	session.setAttribute("hpcUserToken", authToken);
    	            try {
                        user = HpcClientUtil.getUser(authToken, serviceUserURL, sslCertPath, sslCertPassword);
                        if (user == null) {
                           throw new HpcWebException("Invalid User ");
                        }
                        
                        userId = user.getUserId();
                        log.info("getting DOCModel for user: " + user.getFirstName() + " " + user.getLastName());
    	                //Get DOC Models, go to server only if not available in cache
    	                HpcDataManagementModelDTO modelDTO = hpcModelBuilder.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);

                        if (modelDTO != null) {
                            session.setAttribute("userDOCModel", modelDTO);
                        }

                        //Cache this user's permissions for all base paths
                        HpcUserPermsForCollectionsDTO permissions = (HpcUserPermsForCollectionsDTO) session.getAttribute("userDOCPermissions");
                        if(permissions == null) {
                            permissions = HpcClientUtil.getPermissionsForBasePaths(modelDTO, authToken,
                            userId, childCollectionsAclURL, sslCertPath, sslCertPassword);
                            session.setAttribute("userDOCPermissions", permissions);
                        }

    	            } catch (HpcWebException e) {
                        log.error("Authentication failed. ", e);
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
    	            session.setAttribute("contactEmail", contactEmail);
    	            session.setAttribute("isCurator", HpcIdentityUtil.isUserCurator(session));
    	            
    	            if(action.equals("/")) {
    	                response.sendRedirect("/login");
    	                return false;
    	            }
    	        } catch (Exception e) {
                    log.error("Authentication failed. ", e);
    	            throw new HpcAuthorizationException("You are not authorized to view this page.");
    	        }
			}
		}

		return true;
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
