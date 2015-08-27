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

import gov.nih.nci.hpc.dto.user.HpcUserCredentialsDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;
import gov.nih.nci.hpc.dto.user.HpcUserRegistrationDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.util.RestClient;

import java.net.URI;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

import test.gov.nih.nci.hpc.web.ClientResponseLoggingFilter;
/**
 * <p>
 * HPC DM User Login controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcUserRegistrationController.java 
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/login")
public class HpcLoginController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.user}")
    private String serviceUserURL;
	@Value("${gov.nih.nci.hpc.server.dataset.query.registrar}")
	private String datasetURL;
	@Value("${gov.nih.nci.hpc.server.user.authenticate.hpc}")
	private String authenticateHpcURL;
	@Value("${gov.nih.nci.hpc.server.user.authenticate.ldap}")
	private String authenticateLdapURL;
	@Value("${gov.nih.nci.hpc.login.module}")
	private String loginModule;
	@Value("${gov.nih.nci.hpc.server.project.query.registrar}")
	private String projectURL;

  @RequestMapping(method = RequestMethod.GET)
  public String home(Model model){
	  HpcUserCredentialsDTO hpcLogin = new HpcUserCredentialsDTO();
	  model.addAttribute("hpcLogin", hpcLogin);
	  model.addAttribute("ldap", loginModule.equals("ldap")?"true":"false");
      return "index";
  }

  @RequestMapping(method = RequestMethod.POST)
  public String login(@Valid @ModelAttribute("hpcLogin") HpcUserCredentialsDTO hpcLogin, BindingResult bindingResult, Model model, HttpSession session) {
      if (bindingResult.hasErrors()) {
          return "index";
      }
//      RestTemplate restTemplate = new RestTemplate(RestClient.getSSLRequestFactory());
      RestTemplate restTemplate = new RestTemplate();
   	  HpcUserDTO userDTO = null;
   	  try
	  {

		  Client client = ClientBuilder.newClient().register(ClientResponseLoggingFilter.class);
    	  String authURL = authenticateHpcURL;
    	  if(loginModule == null || !loginModule.equals("ldap"))
    	  {
    	   	  URI uri = new URI(serviceUserURL+"/"+hpcLogin.getUserName());
    		  ResponseEntity<HpcUserDTO> userEntity = restTemplate.getForEntity(uri, HpcUserDTO.class);
    		  
    		  userDTO = userEntity.getBody();
    		  if(userDTO == null){
    			  model.addAttribute("loginStatus", false);
    			  model.addAttribute("loginOutput", "Invalid login");
    			  ObjectError error = new ObjectError("hpcLogin", "Invalid login!");
    			  bindingResult.addError(error);
    			  model.addAttribute("hpcLogin", hpcLogin);
    			  model.addAttribute("ldap", loginModule.equals("ldap")?"true":"false");
    			  return "index";
    		  }

    	  }
    	  else
    	  {
    		  authURL = authenticateLdapURL;
			  Response res = client
  					.target(authURL)
  					.request()
  					.post(Entity.entity(hpcLogin, MediaType.APPLICATION_XML));
  			if (res.getStatus() != 200) {
  				throw new RuntimeException("Failed : HTTP error code : "
  						+ res.getStatus());
  			}
    	  }
	  }
	  catch(Exception e)
	  {
		  model.addAttribute("loginStatus", false);
		  model.addAttribute("loginOutput", "Invalid login"+e.getMessage());
		  ObjectError error = new ObjectError("hpcLogin", "Invalid login!");
		  bindingResult.addError(error);
		  model.addAttribute("hpcLogin", hpcLogin);
		  model.addAttribute("ldap", loginModule.equals("ldap")?"true":"false");
		  return "index";
	  }		
      
      try{
    	  if(userDTO == null)
    	  {
	    	  URI uri = new URI(serviceUserURL+"/"+hpcLogin.getUserName());
			  ResponseEntity<HpcUserDTO> userEntity = restTemplate.getForEntity(uri, HpcUserDTO.class);
	
			  userDTO = userEntity.getBody();
    	  }
		  model.addAttribute("loginStatus", true);
		  session.setAttribute("hpcUser", userDTO);
	  }
	  catch(Exception e)
	  {
		  model.addAttribute("loginStatus", false);
		  model.addAttribute("loginOutput", "Invalid login"+e.getMessage());
		  ObjectError error = new ObjectError("hpcLogin", "UserId is not found!");
		  bindingResult.addError(error);
		  model.addAttribute("hpcLogin", hpcLogin);
		  model.addAttribute("ldap", loginModule.equals("ldap")?"true":"false");
		  return "index";
	  }
	  model.addAttribute("datasetURL", datasetURL);
	  model.addAttribute("projectURL", projectURL);
	  return "dashboard";
  }
}
