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
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import gov.nih.nci.hpc.web.util.HpcCollectionUtil;
import gov.nih.nci.hpc.web.util.HpcEncryptionUtil;
import gov.nih.nci.hpc.web.util.HpcIdentityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;
import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.hpc.domain.databrowse.HpcBookmark;
import gov.nih.nci.hpc.domain.datamanagement.HpcPermission;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataValidationRule;
import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportEntry;
import gov.nih.nci.hpc.domain.report.HpcReportEntryAttribute;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementRulesDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcUserPermissionDTO;
import gov.nih.nci.hpc.dto.security.HpcGroup;
import gov.nih.nci.hpc.dto.security.HpcGroupListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.HpcWebException;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcCollectionModel;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataAttrEntry;
import gov.nih.nci.hpc.web.model.HpcSecuredRequest;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;


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
@RequestMapping("/collection")
public class HpcCollectionController extends HpcCreateCollectionDataFileController {
    private static final String
      ERROR_MSG__$DELETE_FAILED = "Failed to delete collection.",
      FEEDBACK_MSG__$DELETE_SUCCEEDED = "Collection has been deleted!",
      KEY_PREFIX = "fdc-",
      NAV_OUTCOME_FORWARD_PREFIX = "forward:",
      NAV_OUTCOME_REDIRECT_PREFIX = "redirect:";
    
    private static final String ATTR_CAN_DELETE = "canDelete";

	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;
	@Value("${gov.nih.nci.hpc.server.user.group}")
    private String userGroupServiceURL;


	//The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	  
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
	public String home(@RequestBody(required = false) String body, @RequestParam String path,
			@RequestParam String action, Model model, BindingResult bindingResult, HttpSession session,
			HttpServletRequest request, RedirectAttributes redirAttrs) {
		try {
			// Prepend forward slash if user doesn't enter
			path = path.charAt(0) != '/' ? '/' + path : path;
			// User Session validation
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");			
			String authToken = (String) session.getAttribute(ATTR_USER_TOKEN);
			if (user == null || authToken == null) {
				ObjectError error = new ObjectError("error", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute(ATTR_USER_LOGIN, hpcLogin);
				final Map<String, String> paramsMap = new HashMap<>();
				paramsMap.put("returnPath", "collection");
				paramsMap.put("action", action);
				paramsMap.put("path", path);
				return "redirect:/login?".concat(MiscUtil.generateEncodedQueryString(
          paramsMap));
			}

			if (path == null) {
                return RET_DASHBOARD;
            } else if (copyInputFlashMap2Model(request, model, KEY_PREFIX)) {
                // This point reached if arrived at this controller action via
                // forward from controller action handling collection item
                // delete.  In this case, copyInputFlashMap2Model method
                // has populated Model with state found in request object that
                // was provided by the other controller action.
                return "collection";
            }

			// Get collection
			HpcCollectionListDTO collections = HpcClientUtil.getCollection(authToken, serviceURL, path, false,
					false, true, sslCertPath, sslCertPassword);
			if (collections != null && collections.getCollections() != null
					&& !collections.getCollections().isEmpty()) {
				HpcDataManagementModelDTO modelDTO = (HpcDataManagementModelDTO) session.getAttribute(ATTR_USER_DOC_MODEL);
				if (modelDTO == null) {
					modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, sslCertPath, sslCertPassword);
					session.setAttribute(ATTR_USER_DOC_MODEL, modelDTO);
				}
				//Find out if user belongs to the SEC_GROUP for this DOC
				HpcGroupListDTO groups = (HpcGroupListDTO) session.getAttribute("hpcSecGroup");
                if (groups == null) {
                    groups = HpcClientUtil.getUserGroup(authToken, userGroupServiceURL, sslCertPath, sslCertPassword);
                    session.setAttribute("hpcSecGroup", groups);
                }
				
                String basePath = path.substring(0, StringUtils.ordinalIndexOf(path, "/", 2) < 0 ? path.length() : StringUtils.ordinalIndexOf(path, "/", 2));
		        HpcDataManagementRulesDTO basePathRules = HpcClientUtil.getBasePathManagementRules(modelDTO, basePath);
		        
		        String doc = HpcClientUtil.getDocByBasePath(modelDTO, basePath);
		        boolean userInSecGroup = false;
		        if(groups != null && !CollectionUtils.isEmpty(groups.getGroups())) {
		          for (HpcGroup group : groups.getGroups()) {
		            if (group.getGroupName().equalsIgnoreCase(doc + "_SEC_GROUP")) {
		              userInSecGroup = true;
		              break;
		            }
		          }
		        }
		        
				HpcCollectionDTO collection = collections.getCollections().get(0);
				HpcCollectionModel hpcCollection = buildHpcCollection(collection,
						modelDTO.getCollectionSystemGeneratedMetadataAttributeNames(), basePathRules.getCollectionMetadataValidationRules(), userInSecGroup);
				model.addAttribute("collection", hpcCollection);
				model.addAttribute(ATTR_USER_PERMISSION, (collection.getPermission() != null)
						? collection.getPermission().toString() : "null");
				model.addAttribute("attributeNames", getMetadataAttributeNames(collection));
                
				//Add the delete icon only if user has permission to delete this collection 
				boolean canDeleteFlag = determineIfCollectionCanBeDelete(session, collection);
				model.addAttribute(ATTR_CAN_DELETE, Boolean.toString(canDeleteFlag));
				
				//Get the collection size if present
				List<HpcReport> reports = collection.getReports();
				if(!CollectionUtils.isEmpty(reports)) {
					for(HpcReport report: reports) {
						if(report.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH)) {
							for(HpcReportEntry reportEntry: report.getReportEntries()) {
								if(reportEntry.getAttribute().equals(HpcReportEntryAttribute.TOTAL_DATA_SIZE)) {
									model.addAttribute("collectionSize", MiscUtil.addHumanReadableSize(reportEntry.getValue(), true));
									break;
								}
							}
						}
					}
				}

				if (action != null && action.equals("edit")) {
					if (collection.getPermission() == null || collection.getPermission().equals(HpcPermission.NONE)
							|| collection.getPermission().equals(HpcPermission.READ)) {
						model.addAttribute(ATTR_ERROR,
								"No edit permission. Please contact collection owner for write access.");
						model.addAttribute("action", "view");
					} else {
						String collectionType = getCollectionType(collection);
						populateFormAttributes(request, session, model, basePath,
								collectionType, false, false);
						List<HpcMetadataAttrEntry> userMetadataEntries = (List<HpcMetadataAttrEntry>) session.getAttribute("metadataEntries");
						List<HpcMetadataAttrEntry> mergedMetadataEntries = mergeMatadataEntries(hpcCollection.getSelfMetadataEntries(), userMetadataEntries);
						hpcCollection.setSelfMetadataEntries(mergedMetadataEntries);
						model.addAttribute("collection", hpcCollection);
						model.addAttribute("action", "edit");						
					}
				}
			} else {
				String message = "Could not find collection " + path;
				model.addAttribute(ATTR_ERROR, message);
				return RET_DASHBOARD;
			}
		} catch (HpcWebException e) {
			model.addAttribute(ATTR_ERROR, e.getMessage());
		}
		model.addAttribute("hpcCollection", new HpcCollectionModel());
		return "collection";
	}

    /**
     * Finds out whether delete icon should be displayed in the detailed
     * view for the (authenticated) User.
     *
     * Delete icon will be displayed if the following 2 conditions are
     * satisfied:
     *   1. Current user is in the role of either Group Admin or System
     *      Admin 
     *   2. The user has Own permission on the Collection.
     *   Note: Additional criteria may be applied at the server, in 
     *   which case, failure to meet these will cause an error msg to
     *   be returned.
     *
     * @param theUserPermDto  HpcUserPermissionDTO instance representing
     *                        current user
     * @param collection  HpcCollectionDTO instance representing specified
     *                       collection
     * @return true if specified collection may be deleted by current user;
     *         false otherwise
     */
	private boolean determineIfCollectionCanBeDelete(
      HttpSession session, HpcCollectionDTO collection) {
      
          return HpcIdentityUtil.iUserSystemAdminOrGroupAdmin(session) &&
          HpcCollectionUtil.isUserCollectionOwner(session, collection);

    }


    private List<String> getMetadataAttributeNames(HpcCollectionDTO collection) {
		List<String> names = new ArrayList<>();
		if (collection == null || collection.getMetadataEntries() == null
				|| collection.getMetadataEntries().getSelfMetadataEntries() == null
				|| collection.getMetadataEntries().getParentMetadataEntries() == null)
			return names;
		for (HpcMetadataEntry entry : collection.getMetadataEntries().getSelfMetadataEntries())
			names.add(entry.getAttribute());
		return names;
	}
    

	/**
	 * Update collection
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
	public String updateCollection(@Valid @ModelAttribute("hpcGroup") HpcCollectionModel hpcCollection, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request, HttpServletResponse response,
			final RedirectAttributes redirectAttributes) {
		String[] action = request.getParameterValues("action");
		if (action != null && action.length > 0 && action[0].equals("cancel")) {
      final Map<String, String> paramsMap = new HashMap<>();
      paramsMap.put("path", hpcCollection.getPath());
      paramsMap.put("action", "view");
      return "redirect:/collection?".concat(MiscUtil.generateEncodedQueryString(
        paramsMap));
    }
		String authToken = (String) session.getAttribute(ATTR_USER_TOKEN);
		try {
			if (hpcCollection.getPath() == null || hpcCollection.getPath().trim().length() == 0)
				model.addAttribute(ATTR_ERROR, "Invald collection path");

			HpcCollectionRegistrationDTO registrationDTO = constructRequest(request);

			boolean updated = HpcClientUtil.updateCollection(authToken, serviceURL, registrationDTO,
					hpcCollection.getPath(), sslCertPath, sslCertPassword);
			if (updated) {
				redirectAttributes.addFlashAttribute(ATTR_ERROR, "Collection " + hpcCollection.getPath() + " is Updated!");
				session.removeAttribute("selectedUsers");
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute(ATTR_ERROR, "Failed to update metadata. " + e.getMessage());
		}
		final Map<String, String> paramsMap = new HashMap<>();
		paramsMap.put("path", hpcCollection.getPath());
		paramsMap.put("action", "view");
		return "redirect:/collection?".concat(MiscUtil.generateEncodedQueryString(
      paramsMap));
	}

	
	private HpcCollectionModel buildHpcCollection(HpcCollectionDTO collection, List<String> systemAttrs, List<HpcMetadataValidationRule> rules, boolean userInSecGroup) {
		HpcCollectionModel model = new HpcCollectionModel();
		systemAttrs.add("collection_type");
		
		String collectionType = getCollectionType(collection);
		
		model.setCollection(collection.getCollection());
		if (collection.getMetadataEntries() == null)
			return model;

		for (HpcMetadataEntry entry : collection.getMetadataEntries().getSelfMetadataEntries()) {
			HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
			attrEntry.setAttrName(entry.getAttribute());
			attrEntry.setAttrValue(entry.getValue());
			attrEntry.setAttrUnit(entry.getUnit());
			attrEntry.setEncrypted(isEncryptedAttribute(entry.getAttribute(), collectionType, rules));
            
			if (systemAttrs.contains(entry.getAttribute())) {
				attrEntry.setSystemAttr(true);
				model.getSelfSystemMetadataEntries().add(attrEntry);
			}
			else {
				attrEntry.setSystemAttr(false);
				if(!attrEntry.isEncrypted() || userInSecGroup)
					  model.getSelfMetadataEntries().add(attrEntry);
			}
		}

		for (HpcMetadataEntry entry : collection.getMetadataEntries().getParentMetadataEntries()) {
			HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
			attrEntry.setAttrName(entry.getAttribute());
			attrEntry.setAttrValue(entry.getValue());
			attrEntry.setAttrUnit(entry.getUnit());
			attrEntry.setLevelLabel(entry.getLevelLabel());
			attrEntry.setSystemAttr(systemAttrs.contains(entry.getAttribute()));
            attrEntry.setEncrypted(isEncryptedAttribute(entry.getAttribute(), null, rules));
           
			if(!attrEntry.isEncrypted())
			    model.getParentMetadataEntries().add(attrEntry);
		}
		
		return model;
	}
	
    private boolean isEncryptedAttribute(String attribute, String collectionType, List<HpcMetadataValidationRule> rules) {
      for(HpcMetadataValidationRule rule: rules) {
        if (StringUtils.equals(rule.getAttribute(), attribute) && collectionType != null && rule.getCollectionTypes().contains(collectionType))
          return rule.getEncrypted();
        if (StringUtils.equals(rule.getAttribute(), attribute) && collectionType == null)
          return rule.getEncrypted();
      }
      return false;
    }
    
    private String getCollectionType(HpcCollectionDTO collection) {
      if(collection.getMetadataEntries() != null) {
    	  for (HpcMetadataEntry entry : collection.getMetadataEntries().getSelfMetadataEntries()) {
    	    if (StringUtils.equals(entry.getAttribute(), "collection_type"))
    	      return entry.getValue();
    	  }
      }
	  return null;
	}

	private HpcCollectionRegistrationDTO constructRequest(HttpServletRequest request) {
		Enumeration<String> params = request.getParameterNames();
		HpcCollectionRegistrationDTO dto = new HpcCollectionRegistrationDTO();
		List<HpcMetadataEntry> metadataEntries = new ArrayList<>();

		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			if (paramName.startsWith("zAttrStr_")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrName = paramName.substring("zAttrStr_".length());
				String[] attrValue = request.getParameterValues(paramName);
				entry.setAttribute(attrName);
				entry.setValue(attrValue[0]);
				metadataEntries.add(entry);
			} else if (paramName.startsWith("addAttrName")) {
				HpcMetadataEntry entry = new HpcMetadataEntry();
				String attrId = paramName.substring("addAttrName".length());
				String[] attrName = request.getParameterValues(paramName);
				String[] attrValue = request.getParameterValues("addAttrValue" + attrId);
				if (attrName.length > 0 && !attrName[0].isEmpty()) {
					entry.setAttribute(attrName[0]);
					if (attrValue.length > 0 && !attrValue[0].isEmpty()) {
						entry.setValue(attrValue[0]);
					} else {
						throw new HpcWebException("Invalid value for metadata attribute " + attrName[0] + ": Value cannot be empty");
					}
				} else if (attrValue.length > 0 && !attrValue[0].isEmpty()) {
					throw new HpcWebException("Invalid metadata attribute name for value " + attrValue[0] + ": Name cannot be empty");
				} else {
					//If both attrName and attrValue are empty, then we just
					//ignore it and move to the next element
					continue;
				}

				metadataEntries.add(entry);
			}
		}
		dto.getMetadataEntries().addAll(metadataEntries);
		return dto;
	}


	@RequestMapping(
			path = "/delete", method = RequestMethod.POST,
			produces = MediaType.TEXT_HTML_VALUE
	)
	public String processDeleteCollection(
			@RequestBody(required = false) String body,
			@RequestParam("collectionPath4Delete") String collPath,
			@RequestParam("action4Delete") String collAction,
			Model model,
			BindingResult bindingResult,
			HttpServletRequest request,
			RedirectAttributes redirAttrs,
			HttpSession session)
	{
		String retNavOutcome = null;
		try {
			retNavOutcome = prelimCheckForAuthSessionAndPath(
					collPath, collAction, model, session, bindingResult);
			if (retNavOutcome.isEmpty()) {
				retNavOutcome = doDeleteCollection(
                    collPath, collAction, model, redirAttrs, session);
			}
		} catch (Exception e) {
			logger.error("Error deleting collection ",e);
			model.addAttribute(ATTR_ERROR, e.getMessage());
			copyModelState2FlashScope(model, redirAttrs, KEY_PREFIX);
			final Map<String, String> qParams = new HashMap<>();
			qParams.put("path", collPath);
			qParams.put("action", collAction);
			retNavOutcome = NAV_OUTCOME_REDIRECT_PREFIX.concat("/collection?").concat(
        MiscUtil.generateEncodedQueryString(qParams));
		}
		return retNavOutcome;
	}


	private String doDeleteCollection(
      @RequestParam("collectionPath4Delete") String collPath,
      @RequestParam("action") String collAction,
      Model model,
      RedirectAttributes redirAttrs,
      HttpSession session)
    {
        String retNavOutcome = "";
        final String authToken = (String) session.getAttribute(ATTR_USER_TOKEN);
        // Populate model based on collection item's state
        populateModelForCollectionDetailView(
                authToken, collPath, collAction, session, model);
        if (HpcClientUtil.deleteCollection(authToken, serviceURL, collPath,
                                              sslCertPath, sslCertPassword)) {
            model.addAttribute(ATTR_ERROR, FEEDBACK_MSG__$DELETE_SUCCEEDED);
            // At this point, retain populated model attributes based on just
            // deleted collection item.
            // After successful deletion,
            //   1) user permission not applicable
            //   2) can no longer delete
            //   3) can only view info about just deleted collection
            model.addAttribute(ATTR_USER_PERMISSION, "null");
            model.addAttribute(ATTR_CAN_DELETE, Boolean.FALSE.toString());
            model.addAttribute("action", "view");
        } else {
            model.addAttribute(ATTR_ERROR, ERROR_MSG__$DELETE_FAILED);
        }
        copyModelState2FlashScope(model, redirAttrs, KEY_PREFIX);
        final String allowedAction = (String) model.asMap().get("action");
        final Map<String, String> qParams = new HashMap<>();
        qParams.put("path", collPath);
        qParams.put("action", allowedAction);
        retNavOutcome = NAV_OUTCOME_REDIRECT_PREFIX.concat("/collection?")
          .concat(MiscUtil.generateEncodedQueryString(qParams));
        return retNavOutcome;
    }

    /**
     * Decrypt an encrypted attribute Ajax POST
     * 
     * @param hpcWebUser
     * @param model
     * @param bindingResult
     * @param session
     * @param request
     * @param response
     * @return
     */
    @JsonView(Views.Public.class)
    @RequestMapping(path = "/show", method = RequestMethod.POST)
    @ResponseBody
    public AjaxResponseBody decrypt(@Valid @ModelAttribute("hpcSecuredRequest") HpcSecuredRequest hpcSecuredRequest, Model model,
            BindingResult bindingResult, HttpSession session, HttpServletRequest request,
            HttpServletResponse response) {
        AjaxResponseBody result = new AjaxResponseBody();

        try {
            if (StringUtils.isEmpty(hpcSecuredRequest.getUserKey()) || StringUtils.isEmpty(hpcSecuredRequest.getTextString())) {
              String errMsg = "error: Invalid user input";
              result.setCode(errMsg);
            } else {
                String decryptedString = HpcEncryptionUtil.decrypt(hpcSecuredRequest.getUserKey(), Base64.getDecoder().decode(hpcSecuredRequest.getTextString()));
                if (decryptedString != null) {
                    result.setCode("success");
                    result.setMessage(decryptedString);
                }
            }
        } catch (Exception e) {
            String errMsg = "error: " + e.getMessage();
            result.setCode(errMsg);
        }
        return result;
    }
    
    private boolean copyInputFlashMap2Model(
      HttpServletRequest req, Model model, String attrNmPrefix)
    {
        final Map<String,?> inputFlashMap =
                              RequestContextUtils.getInputFlashMap(req);
        int numAttrsCopied = 0;
        if(inputFlashMap != null)
        {
        final Set<String> inputFlashAttrKeys = inputFlashMap.keySet();

        for (String someKey : inputFlashAttrKeys) {
            Object someVal = inputFlashMap.get(someKey);
            if (null == attrNmPrefix) {
                model.addAttribute(someKey, someVal);
                numAttrsCopied += 1;
            } else if (someKey.startsWith(attrNmPrefix)) {
                final String altKey = someKey.substring(attrNmPrefix.length());
                model.addAttribute(altKey, someVal);
                numAttrsCopied += 1;
            }
        }
        }
        return (numAttrsCopied > 0);
    }


    private void copyModelState2FlashScope(
      Model model, RedirectAttributes redirAttrs, String keyPrefix)
    {
        for (Map.Entry<String, Object> modelItem : model.asMap().entrySet()) {
            final String targetReqAttrName = (null == keyPrefix) ?
                    modelItem.getKey() : keyPrefix.concat(modelItem.getKey());
            redirAttrs.addFlashAttribute(targetReqAttrName, modelItem.getValue());
        }
    }


    /**
     * Checks if authenticated session is present and if so, returns empty string.
     * Otherwise, returns controller navigation outcome for going to applicable page.
     *
     * @param collectionPath  Path of collection item
     * @param collectionAction  Action on collection item (edit, view)
     * @param model  The model instance
     * @param session  The HTTP session
     * @param bindingResult  The binding result instance
     * @return Empty string if authenticated session is present; otherwise, string representing navigation outcome to take
     */
	private String checkIfAuthenticatedSessionExists(
      String collectionPath,
      String collectionAction,
      Model model,
      HttpSession session,
      BindingResult bindingResult)
    {
	    String retNavOutcome = "";
        if (!(session.getAttribute("hpcUser") instanceof HpcUserDTO) ||
              !(session.getAttribute(ATTR_USER_TOKEN) instanceof String))
        {
            bindingResult.addError(new ObjectError("error",
                                        "Invalid user session!"));
            model.addAttribute(ATTR_USER_LOGIN, new HpcLogin());
            final Map<String, String> qParams = new HashMap<>();
            qParams.put("returnPath", "collection");
            qParams.put("action", collectionAction);
            qParams.put("path", collectionPath);
            retNavOutcome = "redirect:/login?".concat(MiscUtil
              .generateEncodedQueryString(qParams));
        }
        return retNavOutcome;
    }


    /**
     * Checks if given collection path is missing (null) or blank (empty).  If so, returns controller navigation
     * outcome for going to applicable page.  If not, returns empty string.
     *
     * @param collectionPath  Path of collection item
     * @return String representing navigation outcome to take if missing or blank collection path; otherwise, empty string
     */
    private String checkIfMissingOrBlankPath(String collectionPath) {	   
          return (null == collectionPath || collectionPath.isEmpty()) ?
          RET_DASHBOARD : "";
       
    }


    /**
     * Checks for authenticated session and provided, non-empty collection path.
     * If both are detected, returns empty string.  Otherwise, returns string representing
     * navigation outcome to take.
     *
     * @param aCollectionPath  Path of collection item
     * @param aCollectionAction  Action on collection item (edit, view)
     * @param theModel  The model instance
     * @param theSession  The HTTP session
     * @param theBindingResult  The binding result instance
     * @return Empty string if checks pass; otherwise, string representing navigation outcome to take
     */
    private String prelimCheckForAuthSessionAndPath(
      String aCollectionPath,
      String aCollectionAction,
      Model theModel,
      HttpSession theSession,
      BindingResult theBindingResult)
    {
        String retNavOutcome = checkIfAuthenticatedSessionExists(
          aCollectionPath,
          aCollectionAction,
          theModel,
          theSession,
          theBindingResult
        );
        if (retNavOutcome.isEmpty()) {
            retNavOutcome = checkIfMissingOrBlankPath(aCollectionPath);
        }
        return retNavOutcome;
    }


    /**
     * Selectively puts into HTTP session attribute named "userDOCModel" which
     * is an instance of HpcDataManagementModelDTO.  If attribute already
     * exists, does nothing.  If attribute does not exist, fetches DTO from
     * applicable HPC API rest service and creates the session attribute.
     *
     * @param authToken  Auth token
     * @param session  HTTP session
     */
    private void putUserDocModelIntoSession(
      String authToken, HttpSession session) {
        putUserDocModelIntoSession(authToken, session, false);
    }


    /**
     * Puts into HTTP session the attribute named "userDOCModel" which is an
     * instance of HpcDataManagementModelDTO, with control over whether to
     * retain the attribute if it already exists.
     *
     * If the parameter fetchFreshModel is true, then the session attribute is
     * set to new value using freshly fetched HpcDataManagementModelDTO object.
     * Any existing value is replaced.
     *
     * If the parameter fetchFreshModel is false, then the session attribute is
     * set to new value using freshly fetched HpcDataManagementModelDTO object
     * only if the attribute does not already exist.
     *
     * Any freshly fetched HpcDataManagementModelDTO object is obtained via
     * request to applicable HPC API rest service.
     *
     * @param authToken  Auth token
     * @param session  HTTP session
     * @param fetchFreshModel  boolean to indicate whether to set session
     *                         attribute using freshly obtained DTO
     */
    private void putUserDocModelIntoSession(
      String authToken, HttpSession session, boolean fetchFreshModel) {
        if (!(session.getAttribute(ATTR_USER_DOC_MODEL) instanceof
                HpcDataManagementModelDTO) || fetchFreshModel) {
            HpcDataManagementModelDTO retDtoObj = HpcClientUtil.getDOCModel(
              authToken, hpcModelURL, sslCertPath, sslCertPassword);
            session.setAttribute(ATTR_USER_DOC_MODEL, retDtoObj);
        }
    }


    /**
     * Attempts to obtain 1 collection item's data in 2 forms and returns
     * the 2 forms as a 2-element Object array.  First form would be at
     * index 0 in the array and is a HpcCollectionModel object.  Second form
     * would be at index 1 in the array and is a HpcCollectionDTO object.
     *
     * @param authToken  Auth token
     * @param collectionPath  Path to collection item
     * @param session  HTTP session
     * @return Object array of size 2.  If collection item could not be
     *          resolved, both elements in the array are null.  Otherwise, the
     *          element at index 0 is a HpcCollectionModel instance
     *          representing the collection item, while the element at index 1
     *          is a HpcCollectionDTO instance representing the collection
     *          item.
     */
    private Object[] obtainHpcCollectionIn2Forms(
      String authToken, String collectionPath, HttpSession session) {
        HpcCollectionModel retHpcCollObj = null;
        HpcCollectionDTO theCollectionDto = null;
        putUserDocModelIntoSession(authToken, session);
        HpcCollectionListDTO collections = HpcClientUtil.getCollection(
          authToken, serviceURL, collectionPath, false,
          sslCertPath, sslCertPassword);
        if (null != collections && null != collections.getCollections() &&
              !collections.getCollections().isEmpty()) {
            theCollectionDto = collections.getCollections().get(0);
            final HpcDataManagementModelDTO modelDTO =
              (HpcDataManagementModelDTO) session.getAttribute(ATTR_USER_DOC_MODEL);
            String basePath = collectionPath.substring(0, StringUtils.ordinalIndexOf(collectionPath, "/", 2) < 0 ? collectionPath.length() : StringUtils.ordinalIndexOf(collectionPath, "/", 2));
            HpcDataManagementRulesDTO basePathRules = HpcClientUtil.getBasePathManagementRules(modelDTO, basePath);
            retHpcCollObj = buildHpcCollection(theCollectionDto,
              modelDTO.getCollectionSystemGeneratedMetadataAttributeNames(), basePathRules.getCollectionMetadataValidationRules(), false);
        }

        return new Object[] { retHpcCollObj, theCollectionDto };
    }


    private boolean determineWhetherCollectionHasChildren(
                      String theAuthToken, String theCollectionPath) {
        boolean retVal = false;
        final HpcCollectionListDTO someCollection = HpcClientUtil.getCollection(
                theAuthToken, serviceURL, theCollectionPath, true, false,
                sslCertPath, sslCertPassword);
        if (null != someCollection && null != someCollection.getCollections() &&
                0 < someCollection.getCollections().size()) {
            retVal = HpcCollectionUtil.isCollectionEmpty(
                       someCollection.getCollections().get(0));
        }
        return retVal;
    }


    /**
     * Converts a HpcUserPermissionDTO instance to a String form conveying
     * simple permission text (ex. "READ", "WRITE", "OWN").
     *
     * @param permDto  HpcUserPermissionDTO object
     * @return  String conveying simple permission text
     */
    private String convertPermDto2String(HpcUserPermissionDTO permDto) {
        return (null == permDto || null == permDto.getPermission()) ?
          "null" : permDto.getPermission().toString();
       
    }


    /**
     * Determines action on collection item that user should have, returning
     * Map<String,String> for which there is at least one entry indicating
     * that action.  That entry has key "action", and its value is the action
     * user should have.
     *
     * Also, if attempted action is forbidden, the returned Map contains entry
     * having key "error" and whose value is string conveying error message.
     *
     * @param collAction  Attempted action on collection item
     * @param permDto  HpcUserPermissionDTO instance representing user's
     *                 permission on collection item
     * @return Map<String,String> as described in description above
     */
    private Map<String, String> determineActionWithOptError(
      String collAction, HpcUserPermissionDTO permDto) {
        String theAction = "view";
        String errMsg = "";
        if (null != collAction && "edit".equals(collAction)) {
            if (null == permDto ||
                  HpcPermission.NONE.equals(permDto.getPermission()) ||
                  HpcPermission.READ.equals(permDto.getPermission())) {
                errMsg = "No edit permission. Please contact ".concat(
                  "collection owner for write access.");
            }
            else { theAction = "edit"; }
        }
        final Map<String, String> retMap = new HashMap<>();
        retMap.put("action", theAction);
        if (!errMsg.isEmpty()) { retMap.put("error", errMsg); }
        return retMap;
    }


    /**
     * Populates Model instance for passing state to view object with info
     * about collection item, if resolvable.  If so, returns Object array of
     * length 2 such that element at index 0 is HpcCollectionModel instance
     * representing the collection item and element at index 1 is
     * HpcCollectionDTO instance representing the collection item.  If
     * collection item not resolvable, the returns Object array of length 2
     * with both elements set to null.
     *
     * @param model  Model instance
     * @param authToken  Auth token
     * @param collPath  Path to collection
     * @param session  HTTP session
     * @return  Object array of length 2 as described above
     */
    private Object[] populateModelWithCollectionInfo(
      Model model, String authToken, String collPath, HttpSession session) {
        final Object[] hpcCollReps =
                obtainHpcCollectionIn2Forms(authToken, collPath, session);
        if (hpcCollReps[0] instanceof HpcCollectionModel) {
            model.addAttribute("collection", hpcCollReps[0]);
        }
        if (hpcCollReps[1] instanceof HpcCollectionDTO) {
            final List<String> attribNames = getMetadataAttributeNames(
                                        (HpcCollectionDTO) hpcCollReps[1]);
            model.addAttribute("attributeNames", attribNames);
        }
        model.addAttribute("hpcCollection", new HpcCollectionModel());
        return hpcCollReps;
    }


    /**
     * Populates Model instance for passing state to view object with info
     * related to current user's permission on specified collection item.
     * Returns HpcUserPermissionDto instance representing current user's
     * permission on collection item.
     *
     * @param model  Model instance
     * @param authToken  Auth token
     * @param collPath  Path to collection
     * @param collDto  HpcCollectionDTO instance
     * @param session  HTTP session
     * @return HpcUserPermissionDTO instance representing current user's
     *          permission on specified collection item
     */
    private HpcUserPermissionDTO populateModelWithPermInfo(
      Model model,
      String authToken,
      String collPath,
      HpcCollectionDTO collDto,
      HttpSession session)
    {
        final String userId = (String) session.getAttribute("hpcUserId");
        final HpcUserPermissionDTO permissionDto =
          HpcClientUtil.getPermissionForUser(authToken, collPath, userId,
                          serviceURL, sslCertPath, sslCertPassword);
        final String permAsStr = convertPermDto2String(permissionDto);
        final String canDeleteFlag = Boolean.toString(
          determineIfCollectionCanBeDelete(session, collDto)
        );
        model.addAttribute(ATTR_USER_PERMISSION, permAsStr);
        model.addAttribute(ATTR_CAN_DELETE, canDeleteFlag);
        return permissionDto;
    }


    /**
     * Populates Model instance for passing state to view object with info
     * about the action current user may take on collection item in context.
     *
     * This method could add "error" attribute to Model instance.
     *
     * @param model  Model instance
     * @param collAction  Desired action on collection item in context
     * @param permDto  Current user's permission on collection item in context
     * @return Map<String,String> containing 2 entries having keys "action" and
     *          "error" such that former entry has value conveying current
     *          user's allowed action and latter entry has value conveying
     *          error message, if any.
     */
    private Map<String,String> populateModelWithActionInfo(
      Model model, String collAction, HpcUserPermissionDTO permDto) {
        final Map<String,String> actionCheckResult =
                determineActionWithOptError(collAction, permDto);
        final String actionAllowed = actionCheckResult.get("action");
        final String actionErrorMsg = actionCheckResult.get("error");
        model.addAttribute("action", actionAllowed);
        if (null != actionErrorMsg) {
            model.addAttribute(ATTR_ERROR, actionErrorMsg);
        }
        return actionCheckResult;
    }


    /**
     * Populates Model instance state that provides state for the
     * view object.
     *
     * @param authToken  Auth token
     * @param collPath  Path to collection item
     * @param session  HTTP session
     * @param model  Model instance
     */
    private void populateModelForCollectionDetailView(
      String authToken,
      String collPath,
      String collAction,
      HttpSession session,
      Model model)
    {
        final Object[] hpcCollReps =
          populateModelWithCollectionInfo(model, authToken, collPath, session);
        final HpcCollectionDTO collDto = (HpcCollectionDTO) hpcCollReps[1];
        final HpcUserPermissionDTO permDto = populateModelWithPermInfo(
          model, authToken, collPath, collDto, session);
        populateModelWithActionInfo(model, collAction, permDto);
    }

}
