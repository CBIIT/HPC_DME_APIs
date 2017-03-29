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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementModelDTO;
import gov.nih.nci.hpc.dto.security.HpcGroupMembersRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcCollectionModel;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcMetadataAttrEntry;
import gov.nih.nci.hpc.web.model.HpcWebGroup;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * HPC Web Login controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcLoginController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/collection")
public class HpcCollectionController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.model}")
	private String hpcModelURL;

	@RequestMapping(method = RequestMethod.GET)
//	public String home(String path, String action, Model model, HttpSession session) {
	public String home(@RequestBody(required = false) String body, @RequestParam String path, @RequestParam String action, Model model,
				BindingResult bindingResult, HttpSession session, HttpServletRequest request) {

		try {
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (user == null || authToken == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return "index";
			}
			
			if (path == null)
				return "dashboard";

			HpcCollectionListDTO collections = HpcClientUtil.getCollection(authToken, serviceURL, path, false, sslCertPath, sslCertPassword);
			if(collections != null && collections.getCollections() != null && collections.getCollections().size() > 0)
			{
				HpcDataManagementModelDTO modelDTO = HpcClientUtil.getDOCModel(authToken, hpcModelURL, user.getDoc(), sslCertPath, sslCertPassword);
				HpcCollectionDTO collection = collections.getCollections().get(0);
				HpcCollectionModel hpcCollection = buildHpcCollection(collection, modelDTO.getCollectionSystemGeneratedMetadataAttributeNames());
				model.addAttribute("collection", hpcCollection);
				if(action != null && action.equals("edit"))
					model.addAttribute("action", "edit");
			} else {
				String message = "Collection not found!";
				model.addAttribute("error", message);
				return "dashboard";
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			model.addAttribute("error", "Failed to get Collection: " + e.getMessage());
			e.printStackTrace();
		}
		model.addAttribute("hpcCollection", new HpcCollectionModel());
		return "collection";
	}
	
	private HpcCollectionModel buildHpcCollection(HpcCollectionDTO collection, List<String> systemAttrs)
	{
		HpcCollectionModel model = new HpcCollectionModel();
		model.setCollection(collection.getCollection());
		for(HpcMetadataEntry entry : collection.getMetadataEntries().getSelfMetadataEntries())
		{
			HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
			attrEntry.setAttrName(entry.getAttribute());
			attrEntry.setAttrValue(entry.getValue());
			attrEntry.setAttrUnit(entry.getUnit());
			if(systemAttrs != null && systemAttrs.contains(entry.getAttribute()))
				attrEntry.setSystemAttr(true);
			else
				attrEntry.setSystemAttr(false);
			model.getSelfMetadataEntries().add(attrEntry);
		}

		for(HpcMetadataEntry entry : collection.getMetadataEntries().getParentMetadataEntries())
		{
			HpcMetadataAttrEntry attrEntry = new HpcMetadataAttrEntry();
			attrEntry.setAttrName(entry.getAttribute());
			attrEntry.setAttrValue(entry.getValue());
			attrEntry.setAttrUnit(entry.getUnit());
			if(systemAttrs != null && systemAttrs.contains(entry.getAttribute()))
				attrEntry.setSystemAttr(true);
			else
				attrEntry.setSystemAttr(false);
			model.getParentMetadataEntries().add(attrEntry);
		}
		return model;
	}
	/*
	 * Action for User registration
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String updateCollection(@Valid @ModelAttribute("hpcGroup") HpcCollectionModel hpcCollection,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			HttpServletResponse response) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		try {
			if(hpcCollection.getPath() == null || hpcCollection.getPath().trim().length() == 0)
				model.addAttribute("error", "Invald collection path");
			
			HpcCollectionRegistrationDTO registrationDTO = constructRequest(request, session, hpcCollection.getPath());
			
			boolean created = HpcClientUtil.updateCollection(authToken, serviceURL, registrationDTO, hpcCollection.getPath(),
					sslCertPath, sslCertPassword);
			if (created)
			{
				model.addAttribute("error", "Collection "+ hpcCollection.getPath() +" is Updated!");
				session.removeAttribute("selectedUsers");
			}
		} catch (Exception e) {
			model.addAttribute("error", "Failed to update collection: " + e.getMessage());
		}
		finally
		{
			model.addAttribute("hpcCollection.getPath()", hpcCollection);
		}
		return "redirect:/collection?path="+hpcCollection.getPath()+"&action=view";
	}
	
	private HpcCollectionRegistrationDTO constructRequest(HttpServletRequest request, HttpSession session, String path) {
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
			}
		}
		dto.getMetadataEntries().addAll(metadataEntries);
		return dto;
	}	
	
}
