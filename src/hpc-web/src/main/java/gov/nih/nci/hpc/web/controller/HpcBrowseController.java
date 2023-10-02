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

import gov.nih.nci.hpc.web.util.MiscUtil;

import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.dto.databrowse.HpcBookmarkListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcBrowserEntry;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcModelBuilder;


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

	@Value("${gov.nih.nci.hpc.server.v2.dataObject}")
	private String dataObjectURL;
	
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionURL;

	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;

	@Autowired
	private HpcModelBuilder hpcModelBuilder;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());


	/**
	 * POST Action. Invoked under the following conditions:
	 * - From the browse dialog
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
			final RedirectAttributes redirectAttributes, final String refreshNode, final String loadMore) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcBrowserEntry browserEntry = (HpcBrowserEntry) session.getAttribute("browserEntry");

		boolean getChildren = false;
		boolean refresh = false;
		boolean loadMoreEntry = false;

		if (!StringUtils.isBlank(refreshNode)) {
			refresh = true;
			getChildren = true;
		}
		
		if (!StringUtils.isBlank(loadMore)) {
			loadMoreEntry = true;
			getChildren = true;
		}

		try {
			if (hpcBrowserEntry.getSelectedNodePath() != null) {
			    hpcBrowserEntry.setSelectedNodePath(StringUtils.removeEnd(hpcBrowserEntry.getSelectedNodePath().trim(), "/"));
				// Detect if path refers to data file, and if so, redirect to data
				// file view.Do this check only if we are clicking on Browse button
				//after selecting the path. If we are here because we 
				//clicked on a folder this check is not required 
				if(hpcBrowserEntry.isPartial()) {
					final String slctdNodePath =
						hpcBrowserEntry.getSelectedNodePath().trim();
				
					if (isPathForDataFile(slctdNodePath, authToken)) {
						return genRedirectNavForDataFileView(slctdNodePath);
					}
				}

				if(!loadMoreEntry) {
					browserEntry = new HpcBrowserEntry();
			        browserEntry.setCollection(true);
			        browserEntry.setFullPath(hpcBrowserEntry.getSelectedNodePath().trim());
			        browserEntry.setId(hpcBrowserEntry.getSelectedNodePath().trim());
			        browserEntry.setName(hpcBrowserEntry.getSelectedNodePath().trim());
				}
				browserEntry = getTreeNodes(hpcBrowserEntry.getSelectedNodePath().trim(), browserEntry,
						authToken,
						model, getChildren, hpcBrowserEntry.isPartial(), refresh, loadMoreEntry);

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
			String errMsg = "Failed to browse: " + e.getMessage();
			redirectAttributes.addFlashAttribute("error", errMsg);
			model.addAttribute("error", e.getMessage());
			logger.error(errMsg, e);
		} finally {
		}
		return "browse";
	}


	
	/**
	 * POST Action. This AJAX action is invoked when:
	 * - A tree node is expanded, 
	 * - A node is refreshed
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
	@RequestMapping(value = "/collection", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody List<HpcBrowserEntry> browseCollection(@ModelAttribute("hpcBrowse") HpcBrowserEntry hpcBrowserEntry,
			Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response,
			final RedirectAttributes redirectAttributes, final String refreshNode, final String loadMore) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		HpcBrowserEntry browserEntry = (HpcBrowserEntry) session.getAttribute("browserEntry");
		if(browserEntry == null) {
			browserEntry = hpcBrowserEntry;
		}
		List<HpcBrowserEntry> entries = new ArrayList<HpcBrowserEntry>();
		boolean getChildren = false;
		boolean refresh = false;
		boolean loadMoreEntry = false;

		if (!StringUtils.isBlank(refreshNode)) {
			refresh = true;
			getChildren = false;
		}
		
		if (!StringUtils.isBlank(loadMore)) {
			loadMoreEntry = true;
			getChildren = false;
		}

		try {
			if (hpcBrowserEntry.getSelectedNodePath() != null) {

				browserEntry = getTreeNodes(hpcBrowserEntry.getSelectedNodePath().trim(), browserEntry,
						authToken,
						model, getChildren, true, refresh, loadMoreEntry);

				browserEntry = trimPath(browserEntry, browserEntry.getName());
				String name = browserEntry.getName().substring(browserEntry.getName().lastIndexOf('/') + 1);
				browserEntry.setName(name);
				entries.add(browserEntry);
			}
		} catch (Exception e) {
			String errMsg = "Failed to browse: " + e.getMessage();
			redirectAttributes.addFlashAttribute("error", errMsg);
			model.addAttribute("error", e.getMessage());
			logger.error(errMsg, e);
		} 
		return entries;
	}

	
	
	
	
  /**
   * GET operation on Browse. Invoked under the following conditions:
   * - Builds initial tree (/browse?base)
   * - When the refresh screen button is clicked (/browse?refresh)
   * - When a bookmark is selected (/browse?refresh&path=/some_path/some_collection_or_file)
   * - When the browse icon is clicked from the details page (/browse?refresh=1&path=/some_path/some_collection)
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
      //go to server only if not available in cache
      modelDTO = hpcModelBuilder.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
      session.setAttribute("userDOCModel", modelDTO);
    }

    //Get user permissioned basePaths for Browse dialog
    if(session.getAttribute("userBasePaths") == null) {
        String userId = (String) session.getAttribute("hpcUserId");
        HpcPermission[] hpcPermissions = {HpcPermission.OWN, HpcPermission.WRITE, HpcPermission.READ};
        populateUserBasePaths(modelDTO, authToken, userId, hpcPermissions, "userBasePaths",
            sslCertPath, sslCertPassword, session, hpcModelBuilder);
    }
    Set<String> userBasePaths = (Set<String>)session.getAttribute("userBasePaths");
    model.addAttribute("userBasePaths", userBasePaths);

    String partial = request.getParameter("partial");
    String refresh = request.getParameter("refresh");

    String path = null;
    if (partial != null)
      return "browsepartial";

    if (refresh != null) {
      session.removeAttribute("browserEntry");
    }

    try {
	    if (path == null || path.isEmpty()) {
	      path = request.getParameter("path");
	      if (path == null || path.isEmpty()) {
	        path = (String) request.getAttribute("path");
	      } else {
	    	  //path is present as a param, so we are either trying to browse from 
	    	  //details page or clicked on a bookmark. We want to check if path 
	    	  //points to file only if we clicked on a bookmark.If we are trying  
	    	  //to browse from details page, refresh will be 1
	    	  if(refresh != null && !refresh.equals("1")) {
	    		  //Check if path refers to file, and if so, redirect to data file view
				  if (isPathForDataFile(path, authToken)) {
					  try {
						  return genRedirectNavForDataFileView(path);
					  } catch (UnsupportedEncodingException e) {
						  String errMsg = "Failed to get file details. Reason: " + e.getMessage();
					        model.addAttribute("error", errMsg);
					        logger.error(errMsg, e);
					  }
				  }
	    	  }
	    	  
	      }
	    }
    } catch (Exception e) {
    	// Can't navigate to the selected path, so display the base path
    	path = null;
    	model.addAttribute("error", e.getMessage());
        logger.error(e.getMessage());
    }
    String selectedBrowsePath = (String) session.getAttribute("selectedBrowsePath");

    if (selectedBrowsePath != null && (path == null || path.isEmpty()))
      session.removeAttribute("browserEntry");

    // If browser tree nodes are cached, return cached data. If not, query
    // browser tree nodes based on the base path and cache it.
    try {
      if (path != null) {
        path = path.trim();
        session.setAttribute("selectedBrowsePath", path);

        // Detect if path refers to data file, and if so, redirect to data file view
				//if (isPathForDataFile(path, authToken)) {
         // return genRedirectNavForDataFileView(path);
       // }

        HpcBrowserEntry browserEntry = (HpcBrowserEntry) session.getAttribute("browserEntry");
        if (browserEntry == null) {
          browserEntry = new HpcBrowserEntry();
          browserEntry.setCollection(true);
          browserEntry.setFullPath(path);
          browserEntry.setId(path);
          browserEntry.setName(path);
          browserEntry = getTreeNodes(path, browserEntry, authToken, model, false, true, false, false);
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
        } else {
          model.addAttribute("message", "No collections found!");
        }
      } else {
          //We will get here if path is null, which means we are building initial
          //tree or clicked the refresh button - for both we want to display all
          //the base paths for the logged in user
          List<HpcBrowserEntry> entries = new ArrayList<HpcBrowserEntry>();
          for(String basePath: userBasePaths) {
              HpcBrowserEntry browserEntry = new HpcBrowserEntry();
              browserEntry.setCollection(true);
              browserEntry.setFullPath(basePath);
              browserEntry.setId(basePath);
              browserEntry.setName(basePath);
              browserEntry = getTreeNodes(basePath, browserEntry, authToken, model, false, true, false, false);
              browserEntry = addPathEntries(basePath, browserEntry);
              browserEntry = trimPath(browserEntry, browserEntry.getName());
              entries.add(browserEntry);
              model.addAttribute("browserEntryList", entries);
          }
      }
      model.addAttribute("basePath", user.getDefaultBasePath());
      model.addAttribute("userBookmarks", fetchCurrentUserBookmarks(session));
      return "browse";
    } catch (Exception e) {
    	String errMsg = "Failed to get tree. Reason: " + e.getMessage();
        model.addAttribute("message", errMsg);
        logger.error(errMsg, e);
        return "browse";
    }
  }

  
  
  /**
	 * GET operation on bookmarks
	 * 
	 * @param q
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/bookmarks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody List<HpcBookmark> getBookmarks(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		List<HpcBookmark> bookmarks = null;
		try {
			// Verify User session
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (user == null || authToken == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return bookmarks;
			}

			bookmarks = (List<HpcBookmark>) session.getAttribute("bookmarks");
			if (bookmarks == null) {
				HpcBookmarkListDTO dto = HpcClientUtil.getBookmarks(authToken, bookmarkServiceURL, sslCertPath,
						sslCertPassword);
				bookmarks = dto.getBookmarks();
				bookmarks.sort(Comparator.comparing(HpcBookmark::getName));
				session.setAttribute("bookmarks", bookmarks);
				
			}
			model.addAttribute("userBookmarks", bookmarks);

		} catch (Exception e) {
			String errMsg = "Failed to retrieve bookmarks. Reason: " + e.getMessage();
			model.addAttribute("message", errMsg);
			logger.error(errMsg, e);
			return bookmarks;
		}
		return bookmarks;
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
    //Indicate that this entry is only partially populated
    entry.setPartial(true);
    entry.setName(path);
    entry.setSelectedNodePath(fullPath);
    entry.getChildren().add(childEntry);
    return entry;
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
    final Map<String, String> paramsMap = new HashMap<>();
    paramsMap.put("action", "view");
    paramsMap.put("path", postProcPath);
    paramsMap.put("source", "browse");
    paramsMap.put("init", "1");
    final String retNavString = "redirect:/datafile?".concat(
      MiscUtil.generateEncodedQueryString(paramsMap));

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
				//Drill down childEntry, but only if this child entry
				//happens to be an ancestor of the path we are looking for
				//Else we proceed to next childEntry
				if(childEntry.getFullPath() != null && path.contains(childEntry.getFullPath())) {
					HpcBrowserEntry entry = getSelectedEntry(path, childEntry);
					if (entry != null)
						return entry;
				}
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
	private HpcBrowserEntry getTreeNodes(String path, HpcBrowserEntry browserEntry, String authToken, Model model,
			boolean getChildren, boolean partial, boolean refresh, boolean loadMore) {

		path = path.trim();
		HpcBrowserEntry selectedEntry = getSelectedEntry(path, browserEntry);

		if(refresh & selectedEntry != null) {
			selectedEntry.setPopulated(false);
		}

		// If the selected entry is already populated retry the entry
		if (!loadMore && selectedEntry != null && selectedEntry.isPopulated() && !selectedEntry.isPartial())
			return partial ? selectedEntry : browserEntry;
		if (selectedEntry != null && selectedEntry.getChildren() != null) {
			// If loading more entry is requested, remove the empty child created for child collections first.
			if (loadMore) {
				Iterator<HpcBrowserEntry> it = selectedEntry.getChildren().iterator();
				HpcBrowserEntry entry = null;
			    while (it.hasNext()) {
			    	entry = (HpcBrowserEntry) it.next();
			    	if(entry.getId().equalsIgnoreCase("empty")) {
			    		it.remove();
			    		continue;
			    	}
			    }
			}
			else
				selectedEntry.getChildren().clear();
		}
		if (selectedEntry == null)
		{
			selectedEntry = new HpcBrowserEntry();
			selectedEntry.setId(path);
			selectedEntry.setName(path);
			selectedEntry.setFullPath(path);
		}

		try
		{
			//If partial is true or refresh is true, then it means we need to
			//retrieve info on the selectedEntry also along with it's child list
			//Else, we only get the child list, since we already have the
			//info about the selectedEntry.
			HpcCollectionListDTO collections = HpcClientUtil.getCollection(
					authToken, collectionURL, path, 
					//TODO testing with the child listing only
					true, true,
					//partial || refresh ? false : true, partial || refresh,
					loadMore ? selectedEntry.getChildren().size() : 0,
					sslCertPath, sslCertPassword);
			
			for (HpcCollectionDTO collectionDTO : collections.getCollections()) {
				HpcCollection collection = collectionDTO.getCollection();
				
				//This is for displaying the total size of the selected collection
				//in the machine readable and human readable form above the file table
				Long collectionSize = getCollectionSizeFromReport(collectionDTO);
				if(!CollectionUtils.isEmpty(collectionDTO.getReports())) {
					Integer totalRecords = collection.getDataObjectsTotalRecords() + collection.getSubCollectionsTotalRecords();
					selectedEntry.setHumanReadableFileSize(MiscUtil.addHumanReadableSize(collectionSize.toString(), true));
					selectedEntry.setTotalRecords(totalRecords.toString());
				}

				if(collection.getAbsolutePath() != null) {
					selectedEntry.setFullPath(collection.getAbsolutePath());
					selectedEntry.setId(collection.getAbsolutePath());
					selectedEntry.setName(collection.getCollectionName());
				}

				//This will ensure that the next time we access this path
				//we dont read again from DB, unless an explicit refresh 
				//request has been made
				if(!refresh)
					selectedEntry.setPopulated(true);
				else
					selectedEntry.setPopulated(false);
				selectedEntry.setPartial(false);
				
				selectedEntry.setCollection(true);
				for (HpcCollectionListingEntry listEntry : collection.getSubCollections()) {
					HpcBrowserEntry listChildEntry = new HpcBrowserEntry();
					listChildEntry.setCollection(true);
					listChildEntry.setFullPath(listEntry.getPath());
					listChildEntry.setId(listEntry.getPath());
					listChildEntry.setName(listEntry.getPath());
					//Size display disabled temporarily until perf issue is fixed
					//listChildEntry.setFileSize(Long.toString(listEntry.getDataSize()));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    if(listEntry.getCreatedAt() != null)
                      listChildEntry.setLastUpdated(sdf.format(listEntry.getCreatedAt().getTime()));
					listChildEntry.setPopulated(false);
					if (getChildren)
						listChildEntry = getTreeNodes(listEntry.getPath(), listChildEntry, authToken, model, false, partial,
								refresh, false);
					else {
						HpcBrowserEntry emptyEntry = new HpcBrowserEntry();
						emptyEntry.setId("empty");
						emptyEntry.setName("");
						listChildEntry.getChildren().add(emptyEntry);
					}
					selectedEntry.getChildren().add(listChildEntry);
				}
				for (HpcCollectionListingEntry listEntry : collection.getDataObjects()) {
					selectedEntry.setCollection(true);
					HpcBrowserEntry listChildEntry = new HpcBrowserEntry();
					listChildEntry.setCollection(false);
					listChildEntry.setFullPath(listEntry.getPath());
					listChildEntry.setId(listEntry.getPath());
					listChildEntry.setName(listEntry.getPath());
					listChildEntry.setFileSize(Long.toString(listEntry.getDataSize()));
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
					if(listEntry.getCreatedAt() != null)
					  listChildEntry.setLastUpdated(sdf.format(listEntry.getCreatedAt().getTime()));
					listChildEntry.setPopulated(true);
					selectedEntry.getChildren().add(listChildEntry);
				}
				if (selectedEntry.getChildren() == null || selectedEntry.getChildren().isEmpty()) {
					HpcBrowserEntry listChildEntry = new HpcBrowserEntry();
					listChildEntry.setCollection(false);
					listChildEntry.setFullPath("");
					listChildEntry.setId("empty");
					listChildEntry.setName("");
					listChildEntry.setPopulated(true);
					selectedEntry.getChildren().add(listChildEntry);
				}
			}
		}
		catch(HpcWebException e)
		{
			model.addAttribute("error", e.getMessage());
			logger.error(e.getMessage(), e);
		}
		return partial ? selectedEntry : browserEntry;
	}


  /**
   * Determines whether given DME path refers to data file.
   * @param argPath The DME path.
   * @param argAuthToken The DME auth token.
   * @return boolean true if path refers to a data file, false otherwise
   */
	private boolean isPathForDataFile(String argPath, String argAuthToken) {

    String theItemPath = argPath.trim();
    try {
      //Try getting the data object
    	HpcDataObjectDTO datafile = HpcClientUtil.getDatafilesWithoutAttributes(argAuthToken, this.dataObjectURL, theItemPath, false, false, 
		  sslCertPath, sslCertPassword);
	  if (datafile != null && datafile.getMetadataEntries() != null) {
		  return true;
	  }
      
    } catch (HpcWebException e) {
    	//This could be collection or path doesn't exist
    	return false;
    }
    return false;
  }


	private HpcBrowserEntry trimPath(HpcBrowserEntry entry, String parentPath) {
		for (HpcBrowserEntry child : entry.getChildren()) {
			String childPath = child.getFullPath();
			if (childPath == null || childPath.equals(""))
				continue;
			if (childPath.indexOf(parentPath) != -1) {
				String childName = Paths.get(childPath).getFileName().toString();
				child.setName(childName);
			}
			trimPath(child, childPath);
		}
		return entry;
	}

}
