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

import gov.nih.nci.hpc.web.util.MiscUtil;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcBulkMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDirectoryScanRegistrationItemDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.HpcCollectionModel;
import gov.nih.nci.hpc.web.model.HpcDatafileModel;
import gov.nih.nci.hpc.web.model.HpcMetadataAttrEntry;
import gov.nih.nci.hpc.web.service.HpcAuthorizationService;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import gov.nih.nci.hpc.web.util.HpcExcelUtil;

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
@RequestMapping("/addbulk")
public class HpcCreateBulkDatafileController extends HpcCreateCollectionDataFileController {
    @Autowired
    private HpcAuthorizationService hpcAuthorizationService;
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionServiceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;
	@Value("${gov.nih.nci.hpc.server}")
	private String serverURL;
	@Value("${gov.nih.nci.hpc.server.v2.bulkregistration}")
	private String bulkRegistrationURL;
	@Value("${gov.nih.nci.hpc.web.server}")
	private String webServerName;
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
				session.setAttribute("bulkType", "globus");
			} else
				model.addAttribute("create", true);

			String bulkType = request.getParameter("bulkType");
			if (StringUtils.isBlank(bulkType))
				bulkType = (String) session.getAttribute("bulkType");
			model.addAttribute("bulkType", bulkType);

			String path = request.getParameter("path");
			String endPoint = request.getParameter("endpoint_id");
			String code = request.getParameter("code");
	        if (code != null) {
	            //Return from Google Drive Authorization
	            final String returnURL = this.webServerName + "/addbulk";
	            try {
				  if(StringUtils.equals(bulkType, GOOGLE_DRIVE_BULK_TYPE) ){
					String accessToken = hpcAuthorizationService.getToken(code, returnURL, HpcAuthorizationService.ResourceType.GOOGLEDRIVE);
					session.setAttribute("accessToken", accessToken);
					model.addAttribute("accessToken", accessToken);
					model.addAttribute("authorized", "true");
				  } else if (StringUtils.equals(bulkType, GOOGLE_CLOUD_BULK_TYPE) ) {
					String accessTokenGoogleCloud = hpcAuthorizationService.getToken(code, returnURL, HpcAuthorizationService.ResourceType.GOOGLECLOUD);
					session.setAttribute("accessTokenGoogleCloud", accessTokenGoogleCloud);
					model.addAttribute("accessTokenGoogleCloud", accessTokenGoogleCloud);
					model.addAttribute("authorizedGC", "true");
				  }
	            } catch (Exception e) {
	              model.addAttribute("error", "Failed to redirect to Google for authorization: " + e.getMessage());
	              e.printStackTrace();
	            } 
	        }
			String parent = request.getParameter("parent");
			if (parent == null || parent.isEmpty())
				parent = (String) session.getAttribute("parent");
			else
				session.setAttribute("parent", parent);
			String selectedPath = (String) session.getAttribute("datafilePath");
			if (parent != null)
				model.addAttribute("parent", parent);

			if (selectedPath != null && !selectedPath.isEmpty())
				model.addAttribute("datafilePath", selectedPath);
			else if (path != null && (endPoint == null || endPoint.isEmpty()))
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
				setDatafilePath(model, request, session, parent);
			if (parent != null && !parent.isEmpty())
				checkParent(parent, session);

			String collectionType = getParentCollectionType(request, session);

			populateFormAttributes(request, session, model, basePath, collectionType, false, false);
			setInputParameters(model, request, session, path, parent, source, false);
			if (parent == null || parent.isEmpty())
				populateCollectionTypes(session, model, basePath, parent);

		} catch (Exception e) {
			String errormsg = "Failed to initialize add data file: " + e.getMessage();
			log.error(errormsg, e);
			model.addAttribute("error", errormsg);
		}
		model.addAttribute("hpcDatafile", new HpcDatafileModel());
		model.addAttribute("serverURL", serverURL);
    model.addAttribute("invalidCharacters4PathName", forbiddenChars);
		return "adddatafilebulk";
	}

	private void setDatafilePath(Model model, HttpServletRequest request, HttpSession session, String parentPath) {
		// String selectedPath = (String) session.getAttribute("datafilePath");
		// if(selectedPath != null && !selectedPath.isEmpty())
		// {
		// model.addAttribute("datafilePath", selectedPath);
		// return;
		// }

		String path = request.getParameter("path");
		String endPoint = request.getParameter("endpoint_id");
		if (path != null && !path.isEmpty() && (endPoint == null || endPoint.isEmpty())) {
			model.addAttribute("datafilePath", path);
			session.setAttribute("datafilePath", path);
			return;
		}

		if (parentPath == null || parentPath.isEmpty()) {
			String selectedPath = (String) session.getAttribute("datafilePath");
			if (selectedPath != null && !selectedPath.isEmpty()) {
				model.addAttribute("datafilePath", selectedPath);
				return;
			}

			String[] basePathValues = request.getParameterValues("basePath");
			String basePath = null;
			if (basePathValues == null || basePathValues.length == 0)
				basePath = (String) request.getAttribute("basePath");
			else
				basePath = basePathValues[0];
		} else {
			model.addAttribute("datafilePath", parentPath);
			session.setAttribute("datafilePath", parentPath);
		}
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
	public String createDatafile(@Valid @ModelAttribute("hpcDataModel") HpcDatafileModel hpcDataModel, Model model,
			BindingResult bindingResult, HttpSession session, @RequestParam(value = "hpcMetaDatafile", required = false) MultipartFile hpcMetaDatafile,
			HttpServletRequest request, HttpServletResponse response,
			final RedirectAttributes redirectAttributes) {
		
		String authToken = (String) session.getAttribute("hpcUserToken");
		String[] action = request.getParameterValues("actionType");
		String parent = request.getParameter("parent");
		String basePath = HpcClientUtil.getBasePath(request);
		String bulkType = request.getParameter("bulkType");
		model.addAttribute("bulkType", bulkType);
		String bucketName = (String)request.getParameter("bucketName");
		model.addAttribute("bucketName", bucketName);
		String s3Path = (String)request.getParameter("s3Path");
		model.addAttribute("s3Path", s3Path);
		String gcPath = (String)request.getParameter("gcPath");
		model.addAttribute("gcPath", gcPath);
		String accessKey = (String)request.getParameter("accessKey");
		model.addAttribute("accessKey", accessKey);
		String secretKey = (String)request.getParameter("secretKey");
		model.addAttribute("secretKey", secretKey);
		String region = (String)request.getParameter("region");
		model.addAttribute("region", region);
		String s3File = (String)request.getParameter("s3File");
		model.addAttribute("s3File", s3File != null && s3File.equals("on"));
		String gcFile = (String)request.getParameter("gcFile");
		model.addAttribute("gcFile", gcFile != null && gcFile.equals("on"));
	
		if (basePath == null)
			basePath = (String) session.getAttribute("basePathSelected");

		if (parent != null && !parent.isEmpty())
			model.addAttribute("parent", parent);
		String path = request.getParameter("path");
		setDatafilePath(model, request, session, parent);

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
		setInputParameters(model, request, session, path, parent, source, true);
		if (action != null && action.length > 0 && action[0].equals("cancel"))
			return "redirect:/" + source;
		else if (action != null && action.length > 0 && action[0].equals("refresh"))
		{
			model.addAttribute("useraction", "refresh");
			return updateView(session, request, model, basePath, hpcDataModel.getPath(), true);
		}
		else if (action != null && action.length > 0 && action[0].equals("Globus")) {
			session.setAttribute("datafilePath", hpcDataModel.getPath());
			session.setAttribute("basePathSelected", basePath);
			model.addAttribute("useraction", "globus");
			session.setAttribute("bulkType", "globus");
			session.removeAttribute("GlobusEndpoint");
			session.removeAttribute("GlobusEndpointPath");
			session.removeAttribute("GlobusEndpointFiles");
			session.removeAttribute("GlobusEndpointFolders");
			setCriteria(model, request, session);
			populateFormAttributes(request, session, model, basePath, getParentCollectionType(request, session), true,
					false);
			
			final String percentEncodedReturnURL = MiscUtil.performUrlEncoding(
					this.webServerName + "/addbulk");
			return "redirect:https://app.globus.org/file-manager?method=GET&" +
	        "action=" + percentEncodedReturnURL;
			
		} else if (action != null && action.length > 0 && action[0].toLowerCase().equals(GOOGLE_DRIVE_BULK_TYPE)) {
          session.setAttribute("datafilePath", hpcDataModel.getPath());
          session.setAttribute("basePathSelected", basePath);
          model.addAttribute("useraction", GOOGLE_DRIVE_BULK_TYPE);
          session.setAttribute("bulkType", GOOGLE_DRIVE_BULK_TYPE);
          setCriteria(model, request, session);
          populateFormAttributes(request, session, model, basePath, getParentCollectionType(request, session), true,
                  false);
          
          String returnURL = this.webServerName + "/addbulk";
          try {
            return "redirect:" + hpcAuthorizationService.authorize(returnURL, HpcAuthorizationService.ResourceType.GOOGLEDRIVE);
          } catch (Exception e) {
            model.addAttribute("error", "Failed to redirect to Google for authorization: " + e.getMessage());
            e.printStackTrace();
          }
          
        } else if (action != null && action.length > 0 && action[0].equals(GOOGLE_CLOUD_BULK_TYPE)) {
			session.setAttribute("datafilePath", hpcDataModel.getPath());
			session.setAttribute("basePathSelected", basePath);
			model.addAttribute("useraction", GOOGLE_CLOUD_BULK_TYPE);
			session.setAttribute("bulkType", GOOGLE_CLOUD_BULK_TYPE);
			setCriteria(model, request, session);
			populateFormAttributes(request, session, model, basePath, getParentCollectionType(request, session), true,
					false);
			String returnURL = this.webServerName + "/addbulk";

			try {
			  return "redirect:" + hpcAuthorizationService.authorize(returnURL, HpcAuthorizationService.ResourceType.GOOGLECLOUD);
			} catch (Exception e) {
			  model.addAttribute("error", "Failed to redirect to Google for authorization: " + e.getMessage());
			  e.printStackTrace();
			}
		  }

		try {
			if (hpcDataModel.getPath() == null || hpcDataModel.getPath().trim().length() == 0)
				model.addAttribute("error", "Invald Data file path");
			// Validate parent path
			String parentPath = null;
			hpcDataModel.setPath(hpcDataModel.getPath().trim());
			HpcBulkDataObjectRegistrationRequestDTO registrationDTO = constructV2BulkRequest(request, session,
					hpcDataModel.getPath().trim());
			
			HpcBulkMetadataEntries hpcBulkMetadataEntries = HpcExcelUtil.parseBulkMatadataEntries(hpcMetaDatafile, hpcDataModel.getPath().trim());
			
			if(hpcBulkMetadataEntries != null)
			{
				for(HpcDirectoryScanRegistrationItemDTO itemDTO : registrationDTO.getDirectoryScanRegistrationItems())
					itemDTO.setBulkMetadataEntries(hpcBulkMetadataEntries);
			}

			if(registrationDTO.getDataObjectRegistrationItems() != null && !registrationDTO.getDataObjectRegistrationItems().isEmpty())
			{
				for(HpcDataObjectRegistrationItemDTO dto : registrationDTO.getDataObjectRegistrationItems())
				{
					if(hpcBulkMetadataEntries != null && !hpcBulkMetadataEntries.getPathsMetadataEntries().isEmpty())
					{
						for(HpcBulkMetadataEntry bulkMeta : hpcBulkMetadataEntries.getPathsMetadataEntries())
						{
							if(dto.getPath().equals(bulkMeta.getPath()))
								dto.getDataObjectMetadataEntries().addAll(bulkMeta.getPathMetadataEntries());
						}
					}
				}
			}
			
			if(registrationDTO.getDataObjectRegistrationItems().size() == 0 && registrationDTO.getDirectoryScanRegistrationItems().size() == 0)
				throw new HpcWebException("No input file(s) / folder(s) are selected");
			Set<String> basePaths = (Set<String>) session.getAttribute("basePaths");
			
			if (basePaths == null || basePaths.isEmpty()) {
				HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute("userDOCModel");
				if (modelDTO == null) {
					modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
					session.setAttribute("userDOCModel", modelDTO);
				}
				String userId = (String) session.getAttribute("hpcUserId");
				HpcClientUtil.populateBasePaths(session, model, modelDTO, authToken, userId, serviceURL, sslCertPath,
						sslCertPassword);
				basePaths = (Set<String>) session.getAttribute("basePaths");
			}
			
			if (!registrationDTO.getDryRun() && !basePaths.contains(hpcDataModel.getPath().trim())) {
				HpcCollectionRegistrationDTO collectionRegistrationDTO = constructRequest(request, session,
						hpcDataModel.getPath().trim(), null);

				if (collectionRegistrationDTO.getMetadataEntries().isEmpty()) {
					String collectionType = getParentCollectionType(request, session);
					HpcMetadataEntry entry = new HpcMetadataEntry();
					entry.setAttribute("collection_type");
					entry.setValue(collectionType);
					collectionRegistrationDTO.getMetadataEntries().add(entry);
				}
				HpcClientUtil.updateCollection(authToken, collectionServiceURL, collectionRegistrationDTO,
						hpcDataModel.getPath().trim(), sslCertPath, sslCertPassword);
			}
			HpcBulkDataObjectRegistrationResponseDTO responseDTO = HpcClientUtil.registerBulkDatafiles(authToken,
					bulkRegistrationURL, registrationDTO, sslCertPath, sslCertPassword);
			if (responseDTO != null) {
				StringBuffer info = new StringBuffer();
				for (HpcDataObjectRegistrationItemDTO responseItem : responseDTO.getDataObjectRegistrationItems()) {
					info.append(responseItem.getPath()).append("<br/>");
				}
				if (registrationDTO.getDryRun())
				{
					if(info.toString().isEmpty())
						model.addAttribute("message", "No files found! ");
					else
						model.addAttribute("message", "Here are the dry run list of files: <br/> " + info.toString());
				}
				else {				
				    String taskId = responseDTO.getTaskId();
					model.addAttribute("message",
							"Bulk Data file registration request is submitted! Task Id: <a href='uploadtask?type=&taskId=" + taskId +"'>"+taskId+"</a>");
					model.addAttribute("dryRun", false); 
					session.removeAttribute("dryRun");
		
				}
			}

			// clearSessionAttrs(session);
		} catch (Exception e) {
		  log.error("failed to create data file: " + e.getMessage(), e);
		  String msg = e.getMessage().replace("\n", "<br/>");
		  model.addAttribute("error", msg);
          model.addAttribute("invalidCharacters4PathName", forbiddenChars);
			return "adddatafilebulk";
		} finally {
			if (parent == null || parent.isEmpty()) {
				populateBasePaths(request, session, model, path);
				populateCollectionTypes(session, model, basePath, null);
			} else
				setDatafilePath(model, request, session, parent);

			String collectionType = getParentCollectionType(request, session);
			populateFormAttributes(request, session, model, basePath, collectionType, false, false);
			model.addAttribute("create", true);
			model.addAttribute("serverURL", serverURL);

			
		}
    model.addAttribute("invalidCharacters4PathName", forbiddenChars);
		return "adddatafilebulk";
	}

	protected HpcCollectionRegistrationDTO constructParentCollectionRequest(HttpServletRequest request,
			HttpSession session, String path, HpcCollectionModel hpcCollection) throws HpcWebException {
		Enumeration<String> params = request.getParameterNames();
		HpcCollectionRegistrationDTO dto = new HpcCollectionRegistrationDTO();
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
		List<HpcMetadataAttrEntry> selfMetadataEntries = new ArrayList<>();
		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			if (paramName.startsWith("zAttrStr_")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
				String attrName = paramName.substring("zAttrStr_".length());
				String[] attrValue = request.getParameterValues(paramName);
				if (attrValue.length == 0 || attrValue[0].isEmpty())
					continue;
				entry.setAttribute(attrName);
				entry.setValue(attrValue[0]);
				metadataEntries.add(entry);
				attrEntry.setAttrName(attrName);
				attrEntry.setAttrValue(attrValue[0]);
				attrEntry.setSystemAttr(false);
				selfMetadataEntries.add(attrEntry);
			} else if (paramName.startsWith("_addAttrName")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrId = paramName.substring("_addAttrName".length());
				String[] attrName = request.getParameterValues(paramName);
				String[] attrValue = request.getParameterValues("_addAttrValue" + attrId);
				if (attrName.length > 0 && !attrName[0].isEmpty())
					entry.setAttribute(attrName[0]);
				else
					throw new HpcWebException("Invalid (empty) metadata attribute name for param " + paramName);
				if (attrValue.length > 0 && !attrValue[0].isEmpty())
					entry.setValue(attrValue[0]);
				else
					throw new HpcWebException("Invalid (empty) metadata attribute value for param " + paramName);
				metadataEntries.add(entry);
				HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
				attrEntry.setAttrName(attrName[0]);
				attrEntry.setAttrValue(attrValue[0]);
				attrEntry.setSystemAttr(false);
				selfMetadataEntries.add(attrEntry);
			}
		}
		hpcCollection.setSelfMetadataEntries(selfMetadataEntries);
		dto.getMetadataEntries().addAll(metadataEntries);
		return dto;
	}

	private String getParentCollectionType(HttpServletRequest request, HttpSession session) {
		String collectionType = getFormAttributeValue(request, "zAttrStr_collection_type");
		if (collectionType != null) {
			session.setAttribute("collection_type", collectionType);
			return collectionType;
		} else
			collectionType = (String) session.getAttribute("collection_type");

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

	// Get Collection type attributes
	// Get selected collection type
	// Get given path
	private String updateView(HttpSession session, HttpServletRequest request, Model model, String basePath,
			String path, boolean refresh) {
		populateBasePaths(request, session, model, path);
		populateCollectionTypes(session, model, basePath, null);
		String collectionType = getParentCollectionType(request, session);
		if (collectionType.equals("_select_null"))
			model.addAttribute("collection_type", "Folder");
		else
			model.addAttribute("collection_type", collectionType);
		if (path != null && !path.isEmpty() && !path.equals(basePath))
			model.addAttribute("datafilePath", path);
		populateFormAttributes(request, session, model, basePath, collectionType, refresh, false);
		setInputParameters(model, request, session, path, null, null, true);
		model.addAttribute("create", true);
		model.addAttribute("serverURL", serverURL);
    model.addAttribute("invalidCharacters4PathName", forbiddenChars);
		return "adddatafilebulk";
	}

	private String getFormAttributeValue(HttpServletRequest request, String attributeName) {
		String attrValue = request.getParameter(attributeName);
		if (attrValue != null)
			return attrValue;
		else
			return (String) request.getAttribute(attributeName);
	}

}