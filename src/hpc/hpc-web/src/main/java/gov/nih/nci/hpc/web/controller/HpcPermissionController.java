/**
 * HpcUserRegistrationController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebUser;

import java.net.URI;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
/**
 * <p>
 * HPC DM User registration controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcUserRegistrationController.java 
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/permissions")
public class HpcPermissionController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server}")
    private String serverURL;
	@Value("${gov.nih.nci.hpc.server.user}")
    private String serviceUserURL;
	
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
      return "permission";
  }

  @RequestMapping(method = RequestMethod.POST)
  public String register(@Valid @ModelAttribute("hpcUser") HpcWebUser hpcUser, BindingResult bindingResult, Model model) {
	  RestTemplate restTemplate = new RestTemplate();

	  try
	  {

		  URI uri = new URI(serviceUserURL+"/"+hpcUser.getNciUserId());
/*
		  ResponseEntity<HpcUserDTO> userEntity = restTemplate.getForEntity(uri, HpcUserDTO.class);
		  if(userEntity != null && userEntity.hasBody() && userEntity.getBody() != null)
		  {
			  ObjectError error = new ObjectError("nihUserId", "UserId is already enrolled!");
			  bindingResult.addError(error);
		  }
		  
	      if (bindingResult.hasErrors()) {
	          return "enroll";
	      }
	*/  
		 // HpcProxy client =  new HpcProxyImpl(serverURL);
		 // HpcUserRegistrationRestService userRegistration = client.getUserRegistrationServiceProxy();
	      HpcUserDTO userDTO = new HpcUserDTO();
	      HpcNciAccount user = new HpcNciAccount();
		  user.setUserId(hpcUser.getNciUserId());
		  user.setFirstName(hpcUser.getFirstName());
		  user.setLastName(hpcUser.getLastName());
		  userDTO.setNciAccount(user);
/*
		  Boolean validGlobusCredentials = restTemplate.postForObject(new URI(serviceGlobusUserURL),  userDTO, Boolean.class);
		  if(validGlobusCredentials != null)
		  {
			  //Boolean valid = validGlobusCredentials.getBody();
			  if(!validGlobusCredentials)
			  {
				  ObjectError error = new ObjectError("nihUserId", "Invalid Globus credentials!");
				  bindingResult.addError(error);
				  return "enroll";
			  }
		  }
	*/	  
		  
		  HttpEntity<String> response = restTemplate.postForEntity(serviceUserURL,  userDTO, String.class);
		  String resultString = response.getBody();
		  HttpHeaders headers = response.getHeaders();
		  String location = headers.getLocation().toString();
		  String id = location.substring(location.lastIndexOf("/")+1);
		  hpcUser.setId(id);
		  model.addAttribute("registrationStatus", true);
		  model.addAttribute("hpcUser", hpcUser);
	  }
	  catch(Exception e)
	  {
		  ObjectError error = new ObjectError("nciUserId", "Failed to enroll: "+e.getMessage());
		  bindingResult.addError(error);
		  return "enroll";
	  }
	  return "enrollresult";
  }
}
