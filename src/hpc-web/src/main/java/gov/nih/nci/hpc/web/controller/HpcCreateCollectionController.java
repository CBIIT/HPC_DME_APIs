/**
 * HpcCollectionController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.util.HashMap;
import java.util.List;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcCollectionModel;
import gov.nih.nci.hpc.web.model.HpcMetadataAttrEntry;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.MiscUtil;

/**
 * <p>
 * Collection controller. Gets selected collection details. Updates collection
 * metadata.
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcCollectionController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/addCollection")
public class HpcCreateCollectionController extends HpcCreateCollectionDataFileController {
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;
	@Value("${dme.archive.naming.forbidden.chararacters}")
  private String forbiddenCharacters;

	/**
	 * Get selected collection details from its path
	 * 
	 * @param body
	 * @param path
	 * @param action
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String body, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		try {
			if (login(model, bindingResult, session, request) != null)
				return "redirect:/login";

			String init = request.getParameter("init");
			if (init != null) {
				clearSessionAttrs(session);
				model.addAttribute("create", false);
			} else
				model.addAttribute("create", true);

			String parent = request.getParameter("parent");
			if (parent != null)
				model.addAttribute("parent", parent);
			String path = request.getParameter("path");

			String source = request.getParameter("source");
			if (source == null || source.isEmpty())
				source = (String) request.getAttribute("source");
			if (source == null || source.isEmpty())
				source = "dashboard";

			model.addAttribute("source", source);

			String authToken = (String) session.getAttribute("hpcUserToken");

			HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
			if (modelDTO == null) {
				modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
				session.setAttribute("userDOCModel", modelDTO);
			}
			String basePath = null;
			if (parent != null)
				basePath = HpcClientUtil.getBasePath(authToken, serviceURL, parent, sslCertPath, sslCertPassword,
						modelDTO);
			else
				basePath = HpcClientUtil.getBasePath(request);

			if (parent == null || basePath == null)
				populateBasePaths(request, session, model, path);
			if (parent != null && !parent.isEmpty())
				checkParent(parent, session);

			populateCollectionTypes(session, model, basePath, parent);
			setCollectionPath(model, request, parent);
			model.addAttribute("basePath", basePath);
			model.addAttribute("create", false);
		} catch (Exception e) {
			model.addAttribute("error", "Failed to add Collection: " + e.getMessage());
			e.printStackTrace();
		}

		model.addAttribute("hpcCollection", new HpcCollectionModel());
		model.addAttribute("invalidCharacters4PathName", forbiddenCharacters);
		return "addcollection";
	}

	private String getFormAttributeValue(HttpServletRequest request, String attributeName) {
		String attrValue = request.getParameter(attributeName);
		if (attrValue != null)
			return attrValue;
		else
			return (String) request.getAttribute(attributeName);
	}

	// Get Collection type attributes
	// Get selected collection type
	// Get given path
	private String updateView(HttpSession session, HttpServletRequest request, Model model, String basePath,
			String path, String parent, boolean refresh) {
		if (parent == null) {
			populateBasePaths(request, session, model, path);
			model.addAttribute("collectionPath", basePath);
		} else
			setCollectionPath(model, request, parent);
		populateCollectionTypes(session, model, basePath, parent);
		String collectionType = getFormAttributeValue(request, "zAttrStr_collection_type");
		populateFormAttributes(request, session, model, basePath,
				collectionType, refresh, false);
		model.addAttribute("create", true);
    model.addAttribute("invalidCharacters4PathName", forbiddenCharacters);
		return "addcollection";
	}
	
	
	private String setErrorAndUpdateView(HttpSession session, HttpServletRequest request, Model model, String basePath,
			String parent, HpcCollectionModel hpcCollection, String errorMsg) {
		model.addAttribute("hpcCollection", hpcCollection);
		setCollectionPath(model, request, hpcCollection.getPath());
		model.addAttribute("error", errorMsg);
		return updateView(session, request, model, basePath, hpcCollection.getPath(), parent, false);
	}

	/**
	 * Create collection
	 * 
	 * @param hpcCollection
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String createCollection(@Valid @ModelAttribute("hpcCollection") HpcCollectionModel hpcCollection,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response, final RedirectAttributes redirectAttributes) {
		String[] action = request.getParameterValues("actionType");
		String[] type = request.getParameterValues("zAttrStr_collection_type");
		String[] parent = request.getParameterValues("parent");
		String authToken = (String) session.getAttribute("hpcUserToken");

		String originPath = null;
		String[] sourceParam = request.getParameterValues("source");
		String source = "Dashboard";
		if (sourceParam == null || sourceParam.length == 0)
			source = (String) request.getAttribute("source");
		else {
			if (sourceParam.length > 1) {
				source = sourceParam[0].isEmpty() ? sourceParam[1] : sourceParam[0];
			} else
				source = sourceParam[0];
		}
		if (source == null || source.isEmpty())
			source = "dashboard";
		if (parent != null && !parent[0].isEmpty())
			originPath = parent[0];

		if (originPath != null)
			model.addAttribute("parent", originPath);

		model.addAttribute("source", source);
		HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
		if (modelDTO == null) {
			modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
			session.setAttribute("userDOCModel", modelDTO);
		}

		String basePath = null;
		if (originPath != null)
			basePath = HpcClientUtil.getBasePath(authToken, serviceURL, originPath, sslCertPath, sslCertPassword,
					modelDTO);
		else
			basePath = HpcClientUtil.getBasePath(request);

		String collectionType = getFormAttributeValue(request, "zAttrStr_collection_type");

		if (action != null && action.length > 0 && action[0].equals("cancel"))
			return "redirect:/" + source;
		else if (action != null && action.length > 0 && action[0].equals("refresh"))
			return updateView(session, request, model, basePath, hpcCollection.getPath(), originPath, true);

		HpcCollectionRegistrationDTO registrationDTO = null;

		try {
			registrationDTO = constructRequest(request, session, hpcCollection.getPath(), hpcCollection);
		} catch (HpcWebException e) {
			//model.addAttribute("hpcCollection", hpcCollection);
			//setCollectionPath(model, request, originPath);
			//model.addAttribute("error", e.getMessage());
			//return updateView(session, request, model, basePath, hpcCollection.getPath(), originPath, false);
			return setErrorAndUpdateView(session, request, model, basePath, originPath, hpcCollection, 
					e.getMessage());
		}

		// Validate parent path
		String parentPath = null;
		try {
			hpcCollection.setPath(hpcCollection.getPath().trim());
			if (hpcCollection.getPath().lastIndexOf("/") != -1)
				parentPath = hpcCollection.getPath().substring(0, hpcCollection.getPath().lastIndexOf("/"));
			else
				parentPath = hpcCollection.getPath();
			if (!parentPath.isEmpty())
				HpcClientUtil.getCollection(authToken, serviceURL, parentPath, true, sslCertPath, sslCertPassword);
			else
				return setErrorAndUpdateView(session, request, model, basePath, originPath, hpcCollection, 
						"Invalid parent in Collection Path");
		} catch (HpcWebException e) {
			//model.addAttribute("hpcCollection", hpcCollection);
			//setCollectionPath(model, request, originPath);
			//model.addAttribute("error", "Invalid parent collection: " + e.getMessage());
			//return updateView(session, request, model, basePath, hpcCollection.getPath(), originPath, false);
			
			return setErrorAndUpdateView(session, request, model, basePath, originPath, hpcCollection, 
					"Invalid parent in Collection Path: " + e.getMessage());
			
		}

		// Validate Collection path
		try {
			HpcCollectionListDTO collection = HpcClientUtil.getCollection(authToken, serviceURL,
					hpcCollection.getPath(), false, true, sslCertPath, sslCertPassword);
			if (collection != null && collection.getCollections() != null && collection.getCollections().size() > 0) {
				//model.addAttribute("hpcCollection", hpcCollection);
				//setCollectionPath(model, request, originPath);
				//model.addAttribute("error", "Collection already exists: " + hpcCollection.getPath());
				//return updateView(session, request, model, basePath, hpcCollection.getPath(), originPath, false);
				
				return setErrorAndUpdateView(session, request, model, basePath, originPath, hpcCollection, 
						"Collection already exists: " + hpcCollection.getPath());
			}
		} catch (HpcWebException e) {
			// Collection does not exist. We will create the collection
		}

		try {
			if (hpcCollection.getPath() == null || hpcCollection.getPath().trim().length() == 0)
				model.addAttribute("error", "Invald collection path");

			boolean created = HpcClientUtil.updateCollection(authToken, serviceURL, registrationDTO,
					hpcCollection.getPath(), sslCertPath, sslCertPassword);
			if (created) {
				redirectAttributes.addFlashAttribute("error", "Collection " + hpcCollection.getPath() + " is created!");
				session.removeAttribute("selectedUsers");
			}
		} catch (Exception e) {
			model.addAttribute("error", e.getMessage());
			model.addAttribute("invalidCharacters4PathName", forbiddenCharacters);
			return "addcollection";
		} finally {
			if (originPath == null)
				populateBasePaths(request, session, model, hpcCollection.getPath());
			populateCollectionTypes(session, model, basePath, originPath);
			populateFormAttributes(request, session, model, basePath,
					collectionType, false, false);
			model.addAttribute("hpcCollection", hpcCollection);
			setCollectionPath(model, request, originPath);
		}
		final Map<String, String> qParams = new HashMap<>();
		qParams.put("path", hpcCollection.getPath());
		qParams.put("action", "view");
		return "redirect:/collection?".concat(MiscUtil.generateEncodedQueryString(
      qParams));
	}

	
	private void setCollectionPath(Model model, HttpServletRequest request, String parentPath) {
		String path = request.getParameter("path");
		if (path != null && !path.isEmpty()) {
			model.addAttribute("collectionPath", request.getParameter("path"));
			return;
		}

		if (parentPath == null || parentPath.isEmpty()) {
			String[] basePathValues = request.getParameterValues("basePath");
			String basePath = null;
			if (basePathValues == null || basePathValues.length == 0)
				basePath = (String) request.getAttribute("basePath");
			else
				basePath = basePathValues[0];
			model.addAttribute("collectionPath", basePath);
		} else
			model.addAttribute("collectionPath", parentPath);
	}
}
