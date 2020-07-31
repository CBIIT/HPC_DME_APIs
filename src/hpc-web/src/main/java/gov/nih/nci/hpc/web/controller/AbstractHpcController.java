package gov.nih.nci.hpc.web.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcAuthorizationException;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

public abstract class AbstractHpcController {
	@Value("${gov.nih.nci.hpc.ssl.cert}")
	protected String sslCertPath;
	@Value("${gov.nih.nci.hpc.ssl.cert.password}")
	protected String sslCertPassword;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;
	@Value("${hpc.SYSTEM_ADMIN}")
	protected String SYSTEM_ADMIN;
	@Value("${hpc.GROUP_ADMIN}")
	protected String GROUP_ADMIN;
	@Value("${hpc.USER}")
	protected String USER;
	
	@Value("${gov.nih.nci.hpc.server.bookmark}")
	private String bookmarkServiceURL;
	
	//Attribute constants
	protected static final String ATTR_USER_LOGIN = "hpcLogin";
	protected static final String ATTR_USER_TOKEN = "hpcUserToken";
	protected static final String ATTR_USER_DOC_MODEL = "userDOCModel";
	protected static final String ATTR_USER_PERMISSION = "userpermission";
	protected static final String ATTR_ERROR = "error";
	
	//Return constants
	protected static final String RET_DASHBOARD = "dashboard";
	

	protected Logger log = LoggerFactory.getLogger(this.getClass());

	/**
     * Handler for authorization exceptions thrown by HpcUserInterceptor
     *
     * @param req the req
     * @param e   the e
     * @return model and view
     */
    @ExceptionHandler({HpcAuthorizationException.class})
    public ModelAndView handleUnauthorizedException(HttpServletRequest req, Exception e) {
        ModelAndView mav = new ModelAndView();

        mav.setViewName("notauthorized");

        return mav;
    }
    
	@ExceptionHandler({ Exception.class, java.net.ConnectException.class })
	public @ResponseBody HpcResponse handleUncaughtException(Exception ex, WebRequest request,
			HttpServletResponse response) {
		log.info("Converting Uncaught exception to RestResponse : " + ex.getMessage());

		response.setHeader("Content-Type", "application/json");
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		return new HpcResponse("Error occurred", ex.toString());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public @ResponseBody HpcResponse handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request,
			HttpServletResponse response) {
		log.info("Converting IllegalArgumentException to RestResponse : " + ex.getMessage());

		response.setHeader("Content-Type", "application/json");
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		return new HpcResponse("Error occurred", ex.toString());
	}
	
	
	protected void populateDOCs(Model model, String authToken, HpcUserDTO user, HttpSession session) {
		List<String> userDOCs = new ArrayList<String>();
		if (user.getUserRole().equals("SYSTEM_ADMIN")) {
			List<String> docs = HpcClientUtil.getDOCs(authToken, hpcModelURL, sslCertPath, sslCertPassword, session);
			model.addAttribute("docs", docs);
		} else {
			userDOCs.add(user.getDoc());
			model.addAttribute("docs", userDOCs);
		}
	}
	
	
	protected List<HpcBookmark> fetchCurrentUserBookmarks(HttpSession session) {
	    List<HpcBookmark> retBookmarkList;
	    if (session.getAttribute("bookmarks") instanceof List) {
	      // if "bookmarks" session attribute of type List is present, assume component type is HpcBookmark
	      retBookmarkList = (List<HpcBookmark>) session.getAttribute("bookmarks");
	    } else if (null == session.getAttribute("hpcUserToken")) {
	      throw new HpcWebException("No user token is session, so unable to resolve which user.");
	    } else {
	      final String authToken = session.getAttribute("hpcUserToken").toString();
	      final HpcBookmarkListDTO dto = HpcClientUtil.getBookmarks(
	          authToken, bookmarkServiceURL, sslCertPath,
	          sslCertPassword);
	      if (null == dto || null == dto.getBookmarks()) {
	        retBookmarkList = Collections.emptyList();
	      } else {
	        retBookmarkList = dto.getBookmarks();
	        retBookmarkList.sort(Comparator.comparing(HpcBookmark::getName));
	      }
	      session.setAttribute("bookmarks", retBookmarkList);
	    }
	    return retBookmarkList;
	  }
}