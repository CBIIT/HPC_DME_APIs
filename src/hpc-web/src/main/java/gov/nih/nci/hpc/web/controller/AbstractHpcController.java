package gov.nih.nci.hpc.web.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermissionsForCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcPermsForCollectionsDTO;
import gov.nih.nci.hpc.dto.security.HpcGroup;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcAuthorizationException;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcModelBuilder;

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
	@Value("${dme.globus.public.endpoints:}")
	private String globusPublicEndpoints;
	@Value("${gov.nih.nci.hpc.server.collection.acl}")
	private String collectionAclsURL;
	@Value("${gov.nih.nci.hpc.server.user.group}")
	private String userGroupServiceURL;

	//Attribute constants
	protected static final String ATTR_USER_LOGIN = "hpcLogin";
	protected static final String ATTR_USER_TOKEN = "hpcUserToken";
	protected static final String ATTR_USER_DOC_MODEL = "userDOCModel";
	protected static final String ATTR_USER_PERMISSION = "userpermission";
	protected static final String ATTR_ERROR = "error";
	protected static final String ATTR_MESSAGE = "message";
	protected static final String ATTR_BOOKMARKS = "bookmarks";
	
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
		log.info("Converting Uncaught exception to RestResponse : {}", ex.getMessage());

		response.setHeader("Content-Type", "application/json");
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		return new HpcResponse("Error occurred", ex.toString());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public @ResponseBody HpcResponse handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request,
			HttpServletResponse response) {
		log.info("Converting IllegalArgumentException to RestResponse : {}", ex.getMessage());

		response.setHeader("Content-Type", "application/json");
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		return new HpcResponse("Error occurred", ex.toString());
	}


	protected HpcDataManagementModelDTO getModelDTO(HttpSession session) {
		String authToken = (String) session.getAttribute(ATTR_USER_TOKEN);
		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute(ATTR_USER_DOC_MODEL);
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute(ATTR_USER_DOC_MODEL, modelDTO);
		}
		return modelDTO;
	}

	protected void populateUserBasePaths(HpcDataManagementModelDTO modelDTO, String authToken, String userId,
			String sessionAttribute, String sslCertPath, String sslCertPassword, HttpSession session, 
			HpcModelBuilder hpcModelBuilder) {

		Set<String> userBasePaths = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        //Get the groups the user belongs to.
        HpcGroupListDTO groups = null;
        List<String> userGroupNames = new ArrayList<String>();
        if (session.getAttribute("userGroups") == null) {
            groups =
              HpcClientUtil.getUserGroup(
                  authToken, userGroupServiceURL, sslCertPath, sslCertPassword);
            if(groups != null) {
                session.setAttribute("userGroups", groups);
            }
        } else {
            groups = (HpcGroupListDTO) session.getAttribute("userGroups");
        }
        if(groups != null && CollectionUtils.isEmpty(groups.getGroups())) {
           for(HpcGroup group: groups.getGroups()) {
               userGroupNames.add(group.getGroupName());
           }
        }

        //Get all permissions for all base paths, hpcModelBuilder goes to server only if not available in cache
        HpcPermsForCollectionsDTO permissions = hpcModelBuilder.getModelPermissions(
            modelDTO, authToken, collectionAclsURL, sslCertPath, sslCertPassword);

        //Now extract the base paths that this user has permissions to
        if (permissions != null && !CollectionUtils.isEmpty(permissions.getCollectionPermissions())) {
            for (HpcPermissionsForCollection collectionPermissions : permissions.getCollectionPermissions()) {
                if (collectionPermissions != null && !CollectionUtils.isEmpty(collectionPermissions.getCollectionPermissions())) {
                    for(HpcSubjectPermission permission: collectionPermissions.getCollectionPermissions()) {
                        if( (permission.getSubject().contentEquals(userId) ||
                              userGroupNames.contains(permission.getSubject()))
                          && (permission.getPermission() != null
                          && !permission.getPermission().equals(HpcPermission.NONE))) {
                            userBasePaths.add(collectionPermissions.getCollectionPath());
                            break;
                        }
                    }
                }
            }
        }
        session.setAttribute("userBasePaths", userBasePaths);
	}
	
	protected void populateDOCs(Model model, String authToken, HpcUserDTO user, HttpSession session) {
		List<String> userDOCs = new ArrayList<>();
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
	    if (session.getAttribute(ATTR_BOOKMARKS) instanceof List) {
	      // if "bookmarks" session attribute of type List is present, assume component type is HpcBookmark
	      retBookmarkList = (List<HpcBookmark>) session.getAttribute("bookmarks");
	    } else if (null == session.getAttribute(ATTR_USER_TOKEN)) {
	      throw new HpcWebException("No user token is session, so unable to resolve which user.");
	    } else {
	      final String authToken = session.getAttribute(ATTR_USER_TOKEN).toString();
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
	
	protected String isPublicEndpoint(String endpointUUID) {
		List<String> endpointEntry = Arrays.stream(globusPublicEndpoints.split(","))
				.filter(pairString -> pairString.contains(endpointUUID)).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(endpointEntry)) {
			String[] endpoint = endpointEntry.get(0).split("\\|");
			return "The UUID <b>" + endpoint[0] + "</b> you selected is for public endpoint <b>" + endpoint[1]
					+ "</b>. "
					+ "<br/>Use a guest collection on this endpoint or create one using the instructions described in "
					+ "<a href='https://wiki.nci.nih.gov/x/cAyKFg'>Preparing to Use Globus</a>.";
		}
		return null;
	}
}