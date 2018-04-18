/**
 * HpcLoginController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcBrowserEntry;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.util.HpcClientUtil;


/**
 * <p>
 * HPC Web Browse controller. Builds tree nodes based on user DOC basepath and
 * then builds up the tree based on expanded nodes
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcBrowseController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/browse")
public class HpcBrowseController extends AbstractHpcController {

  @Value("${gov.nih.nci.hpc.server.bookmark}")
  private String bookmarkServiceURL;

	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionURL;

	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;

	@Value("${gov.nih.nci.hpc.server.pathreftype}")
  private String hpcPathRefTypeURL;


	/**
	 * POST Action. When a tree node is expanded, this action fetches its child
	 * nodes
	 * 
	 * @param hpcBrowserEntry
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String browse(@Valid @ModelAttribute("hpcBrowse") HpcBrowserEntry hpcBrowserEntry,
			Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response,
			final RedirectAttributes redirectAttributes, final String refreshNode) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcBrowserEntry browserEntry = (HpcBrowserEntry) session.getAttribute("browserEntry");

		boolean getChildren = false;
		boolean refresh = false;

		if (!StringUtils.isBlank(refreshNode)) {
			refresh = true;
			getChildren = true;
		}

		try {
			if (hpcBrowserEntry.getSelectedNodePath() != null) {

        // Detect if path refers to data file, and if so, redirect to data
        // file view
        final String slctdNodePath =
          hpcBrowserEntry.getSelectedNodePath().trim();
        if (isPathForDataFile(slctdNodePath, authToken)) {
          return genRedirectNavForDataFileView(slctdNodePath);
        }

				// session.setAttribute("selectedBrowsePath",
				// hpcBrowserEntry.getSelectedNodePath());
				browserEntry = getTreeNodes(hpcBrowserEntry.getSelectedNodePath().trim(), browserEntry,
						authToken,
						model, getChildren, hpcBrowserEntry.isPartial(), refresh);
				if (hpcBrowserEntry.isPartial()) {
					browserEntry = addPathEntries(hpcBrowserEntry.getSelectedNodePath().trim(), browserEntry);
				}

				browserEntry = trimPath(browserEntry, browserEntry.getName());
				List<HpcBrowserEntry> entries = new ArrayList<HpcBrowserEntry>();
				entries.add(browserEntry);
				model.addAttribute("userBookmarks", fetchCurrentUserBookmarks(session));
				model.addAttribute("browserEntryList", entries);
				model.addAttribute("browserEntry", browserEntry);
				model.addAttribute("scrollLoc", hpcBrowserEntry.getScrollLoc());
				session.setAttribute("browserEntry", browserEntry);
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Failed to browse: " + e.getMessage());
			model.addAttribute("error", e.getMessage());
		} finally {
		}
		return "browse";
	}


  /**
   * GET operation on Browse. Builds initial tree
   *
   * @param q
   * @param model
   * @param bindingResult
   * @param session
   * @param request
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public String get(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
      HttpSession session, HttpServletRequest request) {

    // Verify User session
    HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
    String authToken = (String) session.getAttribute("hpcUserToken");
    if (user == null || authToken == null) {
      ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
      bindingResult.addError(error);
      HpcLogin hpcLogin = new HpcLogin();
      model.addAttribute("hpcLogin", hpcLogin);
      return "redirect:/login?returnPath=browse";
    }

    // Get User DOC model for base path
    HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
    if (modelDTO == null) {
      modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
      session.setAttribute("userDOCModel", modelDTO);
    }
    String partial = request.getParameter("partial");
    String path = null;
    if (partial != null)
      return "browsepartial";

    if (request.getParameter("refresh") != null) {
      session.removeAttribute("browserEntry");
    }

    if (path == null || path.isEmpty()) {
      path = request.getParameter("path");
      if (path == null || path.isEmpty()) {
        path = (String) request.getAttribute("path");
      }
    }
    String selectedBrowsePath = (String) session.getAttribute("selectedBrowsePath");

    if (selectedBrowsePath != null && (path == null || path.isEmpty()))
      session.removeAttribute("browserEntry");
    if (path == null || path.isEmpty() || request.getParameter("base") != null)
      path = user.getDefaultBasePath();

    // If browser tree nodes are cached, return cached data. If not, query
    // browser tree nodes based on the base path and cache it.
    try {
      if (path != null) {
        path = path.trim();
        session.setAttribute("selectedBrowsePath", path);

        // Detect if path refers to data file, and if so, redirect to data file view
				if (isPathForDataFile(path, authToken)) {
          return genRedirectNavForDataFileView(path);
        }

        HpcBrowserEntry browserEntry = (HpcBrowserEntry) session.getAttribute("browserEntry");
        if (browserEntry == null) {
          browserEntry = new HpcBrowserEntry();
          browserEntry.setCollection(true);
          browserEntry.setFullPath(path);
          browserEntry.setId(path);
          browserEntry.setName(path);
          browserEntry = getTreeNodes(path, browserEntry, authToken, model, false, true, false);
          if (request.getParameter("base") == null)
            browserEntry = addPathEntries(path, browserEntry);
          browserEntry = trimPath(browserEntry, browserEntry.getName());
          session.setAttribute("browserEntry", browserEntry);
        }

        if (browserEntry != null) {
          List<HpcBrowserEntry> entries = new ArrayList<HpcBrowserEntry>();
          entries.add(browserEntry);
          model.addAttribute("browserEntryList", entries);
          model.addAttribute("browserEntry", browserEntry);
        } else
          model.addAttribute("message", "No collections found!");
      }
      model.addAttribute("basePath", user.getDefaultBasePath());
      model.addAttribute("userBookmarks", fetchCurrentUserBookmarks(session));
      return "browse";
    } catch (Exception e) {
      model.addAttribute("message", "Failed to get tree. Reason: " + e.getMessage());
      e.printStackTrace();
      return "browse";
    }
  }


  private HpcBrowserEntry addPathEntries(String path, HpcBrowserEntry browserEntry) {
    if (path.indexOf("/") != -1) {
      String[] paths = path.split("/");
      for (int i = paths.length - 2; i >= 0; i--) {
        if (paths[i].isEmpty())
          continue;
        browserEntry = addPathEntry(path, paths[i], browserEntry);
      }
    }
    return browserEntry;
  }


  private HpcBrowserEntry addPathEntry(String fullPath, String path, HpcBrowserEntry childEntry) {
    HpcBrowserEntry entry = new HpcBrowserEntry();
    String entryPath = fullPath.substring(0, (fullPath.indexOf("/" + path) + ("/" + path).length()));
    entry.setCollection(true);
    entry.setId(entryPath);
    entry.setFullPath(entryPath);
    entry.setPopulated(true);
    entry.setName(path);
    entry.getChildren().add(childEntry);
    return entry;
  }


  private List<HpcBookmark> fetchCurrentUserBookmarks(HttpSession session) {
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
    }
    return retBookmarkList;
  }


  /**
   * Generates navigation outcome string for redirecting to the data
   * file/object view.
   *
   * @param path The path of the data file/object
   * @return navigation outcome string
   */
  private String genRedirectNavForDataFileView(String path)
  throws UnsupportedEncodingException {
    String postProcPath = path.trim();
    if (!postProcPath.startsWith("/")) {
      postProcPath = "/".concat(postProcPath);
    }
    final String encodedPath = URLEncoder.encode(postProcPath, "UTF-8");
    final String retNavString = String.format(
      "redirect:/datafile?action=view&path=%s&source=browse&init",
      encodedPath);
    return retNavString;
  }


	private HpcBrowserEntry getSelectedEntry(String path, HpcBrowserEntry browserEntry) {
		if (browserEntry == null)
			return null;

		if (browserEntry.getFullPath() != null && browserEntry.getFullPath().equals(path))
			return browserEntry;

		for (HpcBrowserEntry childEntry : browserEntry.getChildren()) {
			if (childEntry.getFullPath() != null && childEntry.getFullPath().equals(path))
				return childEntry;
			else {
				HpcBrowserEntry entry = getSelectedEntry(path, childEntry);
				if (entry != null)
					return entry;
			}
		}
		return null;
	}


	/**
	 * Get child Tree nodes for selected tree node and merge it with cached
	 * nodes
	 *
	 * @param path
	 * @param browserEntry
	 * @param authToken
	 * @param model
	 * @param getChildren
	 * @param refresh
	 * @return
	 */
	private HpcBrowserEntry getTreeNodes(
    String path,
    HpcBrowserEntry browserEntry,
    String authToken,
    Model model,
    boolean getChildren,
    boolean partial,
    boolean refresh) {
    final String effPath = path.trim();
		HpcBrowserEntry selectedEntry = getSelectedEntry(effPath, browserEntry);
    if (null == selectedEntry) {
      selectedEntry = new HpcBrowserEntry();
      selectedEntry.setName(effPath);
    } else {
      if (refresh) {
        selectedEntry.setPopulated(false);
      }
      if (selectedEntry.isPopulated()) {
        return partial ? selectedEntry : browserEntry;
      }
      if (null != selectedEntry.getChildren()) {
        selectedEntry.getChildren().clear();
      }
    }
		try {
			final HpcCollectionListDTO collDtos = HpcClientUtil.getCollection(
        authToken,
        this.collectionURL,
        effPath,
        true,
        false,
        this.sslCertPath,
        this.sslCertPassword);
			for (HpcCollectionDTO someCollDto : collDtos.getCollections()) {
				HpcCollection coll = someCollDto.getCollection();
        selectedEntry.setId(coll.getAbsolutePath());
        selectedEntry.setName(coll.getCollectionName());
				selectedEntry.setFullPath(coll.getAbsolutePath());
        selectedEntry.setCollection(true);
        selectedEntry.setPopulated(getChildren);
        processSubCollections(
          coll,
          selectedEntry,
          model,
          authToken,
          getChildren,
          partial);
        processDataObjects(coll, selectedEntry);
        addStubDataObjectChildIfChildless(selectedEntry);
			}
		} catch (HpcWebException e) {
			model.addAttribute("error", e.getMessage());
		}

		return partial ? selectedEntry : browserEntry;
	}


	private void addStubDataObjectChildIfChildless(HpcBrowserEntry
    theBrowseEntry) {
    if (theBrowseEntry.getChildren() == null ||
        theBrowseEntry.getChildren().isEmpty()) {
      theBrowseEntry.getChildren().add(genPopHpcBrowserEntry4StubDataObj());
    }
  }


	private void processDataObjects(
    HpcCollection theColl,
    HpcBrowserEntry theBrowseEntry
  ) {
    for (HpcCollectionListingEntry someEntry : theColl.getDataObjects()) {
      theBrowseEntry.setCollection(true);
      theBrowseEntry.getChildren().add(
        genPopHpcBrowserEntry4DataObj(someEntry));
    }
  }


	private void processSubCollections(
    HpcCollection theColl,
    HpcBrowserEntry theBrowseEntry,
    Model theModel,
    String theAuthToken,
    boolean fetchChildren,
    boolean fetchPartial) {
    for (HpcCollectionListingEntry someEntry : theColl.getSubCollections()) {
      HpcBrowserEntry listChildEntry = genUnpopHpcBrowserEntry4Coll(someEntry);
      if (fetchChildren) {
        listChildEntry = getTreeNodes(someEntry.getPath(), listChildEntry,
          theAuthToken, theModel,false, fetchPartial,false);
      } else {
        listChildEntry.getChildren().add(genStubNamelessHpcBrowserEntry());
      }
      theBrowseEntry.getChildren().add(listChildEntry);
    }
  }


	private static HpcBrowserEntry genStubNamelessHpcBrowserEntry() {
    final HpcBrowserEntry emptyEntry = new HpcBrowserEntry();
    emptyEntry.setName("");
    return emptyEntry;
  }


	private static HpcBrowserEntry genPopHpcBrowserEntry4StubDataObj() {
    final HpcBrowserEntry browserEntry = new HpcBrowserEntry();
    browserEntry.setId(" ");
    browserEntry.setName(" ");
    browserEntry.setFullPath(" ");
    browserEntry.setCollection(false);
    browserEntry.setPopulated(true);
    return browserEntry;
  }


  private static HpcBrowserEntry genPopHpcBrowserEntry4DataObj(
    HpcCollectionListingEntry collListingEntry) {
    final HpcBrowserEntry browserEntry = new HpcBrowserEntry();
    browserEntry.setId(collListingEntry.getPath());
    browserEntry.setName(collListingEntry.getPath());
    browserEntry.setFullPath(collListingEntry.getPath());
    browserEntry.setCollection(false);
    browserEntry.setPopulated(true);
    return browserEntry;
  }


  private static HpcBrowserEntry genUnpopHpcBrowserEntry4Coll(
    HpcCollectionListingEntry collListingEntry) {
    final HpcBrowserEntry browserEntry = new HpcBrowserEntry();
    browserEntry.setId(collListingEntry.getPath());
    browserEntry.setName(collListingEntry.getPath());
    browserEntry.setFullPath(collListingEntry.getPath());
    browserEntry.setCollection(true);
    browserEntry.setPopulated(false);
    return browserEntry;
  }


  /**
   * Determines whether given DME path refers to data file.
   * @param argPath The DME path.
   * @param argAuthToken The DME auth token.
   * @return boolean true if path refers to a data file, false otherwise
   * @throws HpcWebException on determining that path does not exist
   */
	private boolean isPathForDataFile(String argPath, String argAuthToken)
      throws HpcWebException {
    final Optional<String> pathElementType = HpcClientUtil.getPathElementType(
      argAuthToken, this.hpcPathRefTypeURL, argPath,
      sslCertPath, sslCertPassword);
    return "data file".equals(pathElementType.orElse(""));
  }


	private HpcBrowserEntry trimPath(HpcBrowserEntry entry, String parentPath) {
		for (HpcBrowserEntry child : entry.getChildren()) {
			String childPath = child.getFullPath();
			if (childPath == null || childPath.equals(""))
				continue;
			if (childPath.indexOf(parentPath) != -1) {
				String childName = childPath.substring(childPath.indexOf(parentPath) + parentPath.length() + 1);
				child.setName(childName);
			}
			trimPath(child, childPath);
		}
		return entry;
	}

}
