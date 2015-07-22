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
import gov.nih.nci.hpc.web.HpcResponseErrorHandler;
import gov.nih.nci.hpc.web.model.HpcDatasetRegistration;
import gov.nih.nci.hpc.domain.metadata.HpcCompressionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcEncryptionStatus;
import gov.nih.nci.hpc.domain.metadata.HpcFilePrimaryMetadata;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataItem;
import gov.nih.nci.hpc.domain.metadata.HpcPHIContent;
import gov.nih.nci.hpc.domain.metadata.HpcPIIContent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
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
	  getPIs(model);
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
	  restTemplate.setErrorHandler(new HpcResponseErrorHandler());
	  HpcDatasetRegistrationDTO dto = new HpcDatasetRegistrationDTO();
	  dto.setName(registration.getDatasetName());
	  dto.setDescription(registration.getDescription());
	  dto.setComments(registration.getComments());
	  
	  //TODO: Lookup Id
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
		  if(registration.getEncrypted().equals("ENCRYPTED"))
			  metadata.setDataEncrypted(HpcEncryptionStatus.ENCRYPTED);
		  else if(registration.getEncrypted().equals("NOT_ENCRYPTED"))
			  metadata.setDataEncrypted(HpcEncryptionStatus.NOT_ENCRYPTED);
		  else if(registration.getEncrypted().equals("NOT_SPECIFIED"))
			  metadata.setDataEncrypted(HpcEncryptionStatus.NOT_SPECIFIED);
		  
		  if(registration.getPii().equals("PII_NOT_PRESENT"))
			  metadata.setDataContainsPII(HpcPIIContent.PII_NOT_PRESENT);
		  else if(registration.getPii().equals("PII_PRESENT"))
			  metadata.setDataContainsPII(HpcPIIContent.PII_PRESENT);
		  else if(registration.getPii().equals("NOT_SPECIFIED"))
			  metadata.setDataContainsPII(HpcPIIContent.NOT_SPECIFIED);

		  if(registration.getCompressed().equals("COMPRESSED"))
			  metadata.setDataCompressed(HpcCompressionStatus.COMPRESSED);
		  else if(registration.getCompressed().equals("NOT_COMPRESSED"))
			  metadata.setDataCompressed(HpcCompressionStatus.NOT_COMPRESSED);
		  else if(registration.getCompressed().equals("NOT_SPECIFIED"))
			  metadata.setDataCompressed(HpcCompressionStatus.NOT_SPECIFIED);

		  if(registration.getPhi().equals("PHI_NOT_PRESENT"))
			  metadata.setDataContainsPHI(HpcPHIContent.PHI_NOT_PRESENT);
		  else if(registration.getPhi().equals("PHI_PRESENT"))
			  metadata.setDataContainsPHI(HpcPHIContent.PHI_PRESENT);
		  else if(registration.getPhi().equals("NOT_SPECIFIED"))
			  metadata.setDataContainsPHI(HpcPHIContent.NOT_SPECIFIED);

		  metadata.setFundingOrganization(registration.getFundingOrganization());
		  metadata.setPrimaryInvestigatorNihUserId(registration.getInvestigatorId());
		  metadata.setRegistrarNihUserId(user.getNihAccount().getUserId());
		  metadata.setDescription(registration.getDescription());
		  //TODO: ID Lookup
		  metadata.setCreatorName(registration.getCreatorId());
		  metadata.setLabBranch(registration.getBranchName());
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
	  } catch(HttpStatusCodeException e){
		  String errorpayload = e.getResponseBodyAsString();
		  ObjectError error = new ObjectError("hpcLogin", "Failed to register: " + errorpayload);
		  bindingResult.addError(error);
		  model.addAttribute("registrationStatus", false);
		  model.addAttribute("error", "Failed to register your request due to: "+errorpayload);
		  return "datasetRegistration";
	  }
	  catch(RestClientException e)
	  {
		  ObjectError error = new ObjectError("hpcLogin", "Failed to register: " + e.getMessage());
		  bindingResult.addError(error);
		  model.addAttribute("registrationStatus", false);
		  model.addAttribute("error", "Failed to register your request due to: "+e.getMessage());
		  return "datasetRegistration";
	  }
	  catch(Exception e)
	  {
		  ObjectError error = new ObjectError("hpcLogin", "Failed to register: " + e.getMessage());
		  bindingResult.addError(error);
		  model.addAttribute("registrationStatus", false);
		  model.addAttribute("error", "Failed to register your request due to: "+e.getMessage());
		  return "datasetRegistration";
	  }
	  finally
	  {
		  getPIs(model);
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
  
  
  private void getPIs(Model model)
  {
	  Map<String, String> users = new HashMap<String, String>();
	  users.put("konkapv", "Prasad Konka");
	  users.put("narram", "Mahidhar Narra");
	  users.put("rosenbergea", "Eran Rosenberg");
	  users.put("luz6", "Zhengwu Lu");
	  users.put("stahlbergea", "Eric A Stahlberg");
	  users.put("sdavis2", "Sean R Davis");
	  users.put("maggiec", "Margaret C Cam");
	  users.put("fitzgepe", "Peter C Fitzgerald");
	  users.put("zhaoyong", "Yongmei Zhao");
	  model.addAttribute("piList", users);
	  model.addAttribute("creatorList", users);
  }
}
