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
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcDatafileModel;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.MiscUtil;

/**
 * <p>
 * Add data file controller.
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcCreateDatafileController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/addDatafile")
public class HpcCreateDatafileController extends HpcCreateCollectionDataFileController {
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionServiceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;
	@Value("${gov.nih.nci.hpc.server}")
	private String serverURL;
	@Value("${gov.nih.nci.hpc.server.bulkregistration}")
	private String bulkRegistrationURL;
	@Value("${dme.archive.naming.forbidden.chararacters}")
	private String forbiddenChars;

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

			String path = request.getParameter("path");
			String parent = request.getParameter("parent");
			if (parent != null) {
				model.addAttribute("parent", parent);
				model.addAttribute("create", true);
			}
			if (path != null)
				model.addAttribute("datafilePath", path);
			else
				model.addAttribute("datafilePath", parent);

			String source = request.getParameter("source");
			if (source == null || source.isEmpty())
				source = (String) request.getAttribute("source");
			if (source == null || source.isEmpty())
				source = "dashboard";

			model.addAttribute("source", source);
			if (source != null)
				session.setAttribute("source", source);
			String authToken = (String) session.getAttribute("hpcUserToken");

			HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
			if (modelDTO == null) {
				modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
				session.setAttribute("userDOCModel", modelDTO);
			}

			String basePath = null;
			if (parent != null)
				basePath = HpcClientUtil.getBasePath(authToken, collectionServiceURL, parent, sslCertPath,
						sslCertPassword, modelDTO);
			else
				basePath = HpcClientUtil.getBasePath(request);
			// User Session validation

			if (basePath == null)
				basePath = (String) session.getAttribute("basePathSelected");
			else
				session.setAttribute("basePathSelected", basePath);

			if (parent == null || basePath == null)
				populateBasePaths(request, session, model, path);
			else
				setDatafilePath(model, request, parent);
			if (parent != null && !parent.isEmpty())
				checkParent(parent, session);

			String collectionType = getParentCollectionType(request, session);

			populateFormAttributes(request, session, model, basePath,
					collectionType, false, true);

			// setGlobusParameters(model, request, session, path, parent,
			// source);
			if (parent == null || parent.isEmpty())
				populateCollectionTypes(session, model, basePath, parent);

		} catch (Exception e) {
			model.addAttribute("error", "Failed to initialize add data file: " + e.getMessage());
			model.addAttribute("create", false);
			e.printStackTrace();
		}
		model.addAttribute("hpcDatafile", new HpcDatafileModel());
		model.addAttribute("serverURL", serverURL);
    model.addAttribute("invalidCharacters4PathName", forbiddenChars);
		return "adddatafile";
	}

	private void setDatafilePath(Model model, HttpServletRequest request, String parentPath) {
		String path = request.getParameter("path");
		if (path != null && !path.isEmpty()) {
			model.addAttribute("datafilePath", path);
			return;
		}

		if (parentPath == null || parentPath.isEmpty()) {
			String[] basePathValues = request.getParameterValues("basePath");
			String basePath = null;
			if (basePathValues == null || basePathValues.length == 0)
				basePath = (String) request.getAttribute("basePath");
			else
				basePath = basePathValues[0];
			model.addAttribute("datafilePath", basePath);
		} else
			model.addAttribute("datafilePath", parentPath);
	}

	/**
	 * Post operation to update metadata
	 * 
	 * @param hpcDatafile
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param response
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String createDatafile(@Valid @ModelAttribute("hpcDatafile") HpcDatafileModel hpcDataModel,
			@RequestParam("hpcDatafile") MultipartFile hpcDatafile, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request, HttpServletResponse response,
			final RedirectAttributes redirectAttributes) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		String[] action = request.getParameterValues("actionType");
		String parent = request.getParameter("parent");
		if (parent != null)
			model.addAttribute("parent", parent);
		String path = request.getParameter("path");
		if (path != null)
			model.addAttribute("datafilePath", path);
		else
			model.addAttribute("datafilePath", parent);

		String basePath = HpcClientUtil.getBasePath(request);
		if (basePath == null)
			basePath = (String) session.getAttribute("basePathSelected");

		String source = request.getParameter("source");
		if (source == null || source.isEmpty())
			source = (String) request.getAttribute("source");

		if (source == null || source.isEmpty())
			source = "dashboard";
		model.addAttribute("source", source);

		String checksum = request.getParameter("checksum");
		if (checksum == null || checksum.isEmpty())
			checksum = (String) request.getAttribute("checksum");

		model.addAttribute("checksum", checksum);

		if (action != null && action.length > 0 && action[0].equals("cancel"))
			return "redirect:/" + source;
		else if (action != null && action.length > 0 && action[0].equals("refresh"))
			return updateView(session, request, model, basePath, hpcDataModel.getPath(), true);
		else if (action != null && action.length > 0 && action[0].equals("Globus")) {
			session.setAttribute("datafilePath", hpcDataModel.getPath());
			session.setAttribute("basePathSelected", basePath);
			
		   final String percentEncodedRequestUrl = MiscUtil.performUrlEncoding(
		    request.getRequestURL().toString());
		    	return "redirect:https://app.globus.org/file-manager?method=GET&" +
		    "action=" + percentEncodedRequestUrl;
		}
		String uploadType = request.getParameter("uploadType");

		if (uploadType != null && uploadType.equals("sync") && hpcDatafile.isEmpty()) {
			model.addAttribute("hpcDataModel", hpcDataModel);
			model.addAttribute("dataFilePath", request.getParameter("path"));
			model.addAttribute("error", "Data file missing!");
			return updateView(session, request, model, basePath, hpcDataModel.getPath(), true);
		}
		boolean registered = false;

		try {
			if (hpcDataModel.getPath() == null || hpcDataModel.getPath().trim().length() == 0)
				model.addAttribute("error", "Invald Data file path");
			// Validate parent path
			String parentPath = null;
			hpcDataModel.setPath(hpcDataModel.getPath().trim());
			try {
				//MiscUtil.validateDmePathForForbiddenChars(hpcDataModel.getPath());
				parentPath = hpcDataModel.getPath().substring(0, hpcDataModel.getPath().lastIndexOf("/"));
				if (!parentPath.isEmpty())
					HpcClientUtil.getCollection(authToken, collectionServiceURL, parentPath, true, sslCertPath,
							sslCertPassword);
			} catch (HpcWebException e) {
				model.addAttribute("hpcDataModel", hpcDataModel);
				model.addAttribute("dataFilePath", request.getParameter("path"));
				model.addAttribute("error", "Invalid parent collection: " + e.getMessage());
				String collectionType = getParentCollectionType(request, session);

				populateFormAttributes(request, session, model, basePath,
						collectionType, false, true);
        model.addAttribute("invalidCharacters4PathName", forbiddenChars);
				return "adddatafile";
			}
			// if (uploadType != null && uploadType.equals("async")) {
			// HpcBulkDataObjectRegistrationRequestDTO registrationDTO =
			// constructBulkRequest(request, session,
			// hpcDataModel.getPath());
			// registered = HpcClientUtil.registerBulkDatafiles(authToken,
			// hpcDatafile, bulkRegistrationURL,
			// registrationDTO, sslCertPath, sslCertPassword);
			// } else {
			HpcDataObjectRegistrationRequestDTO registrationDTO = constructSyncRequest(request, session,
					hpcDataModel.getPath());

			registrationDTO.setChecksum(checksum);
			registered = HpcClientUtil.registerDatafile(authToken, hpcDatafile, serviceURL, registrationDTO,
					hpcDataModel.getPath(), sslCertPath, sslCertPassword);
			// }
			// if (registered) {
			// if (uploadType != null && uploadType.equals("sync"))
			// redirectAttributes.addFlashAttribute("error",
			// "Data file " + hpcDataModel.getPath() + " is registered!");
			// else
			// model.addAttribute("error", "Bulk Data file registration request
			// is submmited!");
			clearSessionAttrs(session);
			// }
		} catch (Exception e) {
			model.addAttribute("error",  e.getMessage());
			model.addAttribute("invalidCharacters4PathName", forbiddenChars);
			return "adddatafile";
		} finally {
			if (!registered) {
				if (parent == null || parent.isEmpty()) {
					populateBasePaths(request, session, model, path);
					populateCollectionTypes(session, model, basePath, null);
				} else
					setDatafilePath(model, request, parent);

				String collectionType = getParentCollectionType(request, session);
				populateFormAttributes(request, session, model, basePath,
						collectionType, false, true);
				model.addAttribute("create", true);
				model.addAttribute("serverURL", serverURL);

				model.addAttribute("hpcDatafile", hpcDatafile);
				// setGlobusParameters(model, request, session, path, parent,
				// source);
			}
		}
		// if (uploadType != null && uploadType.equals("sync"))
    final Map<String, String> queryParams = new HashMap<>();
		queryParams.put("path", hpcDataModel.getPath());
		queryParams.put("action", "view");
		return "redirect:/datafile?".concat(MiscUtil.generateEncodedQueryString(
      queryParams));
		// else
		// return "adddatafile";
	}

	private String getParentCollectionType(HttpServletRequest request, HttpSession session) {
		String collectionType = getFormAttributeValue(request, "zAttrStr_collection_type");
		if (collectionType != null)
			return collectionType;

		HpcCollectionDTO collection = (HpcCollectionDTO) session.getAttribute("parentCollection");
		if (collection != null && collection.getMetadataEntries() != null
				&& collection.getMetadataEntries().getSelfMetadataEntries() != null) {
			for (HpcMetadataEntry entry : collection.getMetadataEntries().getSelfMetadataEntries()) {
				if (entry.getAttribute().equals("collection_type"))
					return entry.getValue();
			}
		}
		return null;
	}

	private HpcDataObjectRegistrationRequestDTO constructSyncRequest(HttpServletRequest request, HttpSession session,
			String path) {

		HpcDataObjectRegistrationRequestDTO dto = new HpcDataObjectRegistrationRequestDTO();
		dto.getMetadataEntries().addAll(getMetadataEntries(request, session, path));
		dto.setSource(null);
		return dto;
	}

	// Get Collection type attributes
	// Get selected collection type
	// Get given path
	private String updateView(HttpSession session, HttpServletRequest request, Model model, String basePath,
			String path, boolean refresh) {
		populateBasePaths(request, session, model, path);
		populateCollectionTypes(session, model, basePath, null);
		String collectionType = getParentCollectionType(request, session);
		if (path != null && !path.equals(basePath))
			model.addAttribute("datafilePath", basePath);
		populateFormAttributes(request, session, model, basePath,
				collectionType, refresh, true);
		model.addAttribute("create", true);
		model.addAttribute("serverURL", serverURL);
    model.addAttribute("invalidCharacters4PathName", forbiddenChars);
		return "adddatafile";
	}

	private String getFormAttributeValue(HttpServletRequest request, String attributeName) {
		String attrValue = request.getParameter(attributeName);
		if (attrValue != null)
			return attrValue;
		else
			return (String) request.getAttribute(attributeName);
	}

}
