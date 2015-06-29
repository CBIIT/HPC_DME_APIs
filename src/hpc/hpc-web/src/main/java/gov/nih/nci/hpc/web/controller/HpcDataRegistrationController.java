/**
 * HpcDataRegistrationController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.dataset.HpcFileUploadRequest;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetRegistrationDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;
import gov.nih.nci.hpc.web.model.HpcDatasetRegistration;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
/**
 * <p>
 * HPC DM Dataset registration controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDataRegistrationController.java 
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/registerDataset")
public class HpcDataRegistrationController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.dataset}")
    private String serviceURL;
	@Value("${gov.nih.nci.hpc.nihfnlcr.name}")
    private String destinationEndpoint;
	
	private final List<String> staticList = new ArrayList<String>(Arrays.asList("1233456", "6789123"));
	

  @RequestMapping(method = RequestMethod.GET)
  public String home(Model model){
	  HpcDatasetRegistration hpcRegistration = new HpcDatasetRegistration();
	  model.addAttribute("hpcRegistration", hpcRegistration);
      return "datasetRegistration";
  }

  /*
	if(dataset.getName() == null || dataset.getType() == null ||
	    	   dataset.getLocation() == null ||
	    	   dataset.getLocation().getFacility() == null ||
	    	   dataset.getLocation().getEndpoint() == null ||
	    	   dataset.getLocation().getDataTransfer() == null)
	 */
  @RequestMapping(method = RequestMethod.POST)
  public String register(@Valid @ModelAttribute("hpcRegistration")  HpcDatasetRegistration registration, Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
	  HpcUserDTO user = (HpcUserDTO)session.getAttribute("hpcUser");
	  //TODO: Add error message
	  if(user == null)
	  {
		return "index";  
	  }
	  List<HpcMetadataItem> eItems = getMetadataitems(request);
	  RestTemplate restTemplate = new RestTemplate();
	  HpcDatasetRegistrationDTO dto = new HpcDatasetRegistrationDTO();
	  dto.setName(registration.getDatasetName());
	  //TODO: Lookup Id
	  dto.setPrimaryInvestigatorId(registration.getInvestigatorName());
	  dto.setRegistratorId(user.getUser().getNihUserId());
	  //TODO: ID Lookup
	  dto.setCreatorId(registration.getCreatorName());
	  dto.setLabBranch(registration.getBranchName());
	  dto.setDescription(registration.getDescription());
	  dto.setComments(registration.getComments());
	  String files = registration.getOriginEndpointFilePath();
	  StringTokenizer tokens = new StringTokenizer(files, ",");
	  while(tokens.hasMoreTokens())
	  {
		  HpcFileUploadRequest upload = new HpcFileUploadRequest();
		  HpcFileType fileType;
		  HpcDataTransferLocations locations = new HpcDataTransferLocations();
		  HpcFileLocation source = new HpcFileLocation();
		  source.setEndpoint(registration.getOriginEndpoint());
		  String filePath = tokens.nextToken();
		  source.setPath(filePath);
		  HpcFileLocation destination = new HpcFileLocation();
		  destination.setEndpoint(destinationEndpoint);
		  destination.setPath(filePath);
		  locations.setDestination(destination);
		  locations.setSource(source);
		  upload.setLocations(locations);
		  //TODO: Identify file type
		  upload.setType(HpcFileType.UNKONWN);

		  //TODO: Metadata funding organization
		  HpcFilePrimaryMetadata metadata = new HpcFilePrimaryMetadata();
		  metadata.setDataEncrypted(registration.getEncrypted().equalsIgnoreCase("Yes"));
		  metadata.setDataContainsPII(registration.getPii().equalsIgnoreCase("Yes"));
		  metadata.setFundingOrganization(registration.getFundingOrganization());
		  metadata.getMetadataItems().addAll(eItems);
		  upload.setMetadata(metadata);
		  dto.getUploadRequests().add(upload);
	  }

	  try
	  {
		  HttpEntity<String> response = restTemplate.postForEntity(serviceURL,  dto, String.class);
		  String resultString = response.getBody();
		  HttpHeaders headers = response.getHeaders();
		  String location = headers.getLocation().toString();
		  String id = location.substring(location.lastIndexOf("/")+1);
		  registration.setId(id);
		  model.addAttribute("registrationStatus", true);
		  model.addAttribute("registration", registration);
	  }
	  catch(Exception e)
	  {
		  ObjectError error = new ObjectError("hpcLogin", "Failed to register: " + e.getMessage());
		  bindingResult.addError(error);
		  model.addAttribute("registrationStatus", false);
		  model.addAttribute("registrationOutput", "Failed to register your request due to: "+e.getMessage());
		  return "datasetRegistration";
	  }
	  return "datasetRegisterResult";
  }
  
  private List<HpcMetadataItem> getMetadataitems(HttpServletRequest request)
  {
	  List<HpcMetadataItem> items = new ArrayList<HpcMetadataItem>();
	  Enumeration<String> names = request.getParameterNames();
	  Field[] fields = HpcDatasetRegistration.class.getDeclaredFields();
	  List<String> fieldNames = new ArrayList<String>();
	  for(int i=0;i<fields.length;i++)
		  fieldNames.add(fields[i].getName());
	  while(names.hasMoreElements())
	  {
		  String attr = names.nextElement();
		  if(!fieldNames.contains(attr))
		  {
			  HpcMetadataItem item = new HpcMetadataItem();
			  item.setKey(attr);
			  item.setValue(request.getParameter(attr));
			  items.add(item);
		  }
	  }
	  return items;
  }
}
