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

import gov.nih.nci.hpc.dto.userregistration.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;

import java.net.URI;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
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
	@Value("${gov.nih.nci.hpc.server.login}")
    private String serviceURL;


  @RequestMapping(method = RequestMethod.GET)
  public String home(Model model){
	  HpcLogin hpcLogin = new HpcLogin();
	  model.addAttribute("hpcLogin", hpcLogin);
      return "index";
  }

  @RequestMapping(method = RequestMethod.POST)
  public String register(@Valid @ModelAttribute("hpcLogin") HpcLogin hpcLogin, BindingResult bindingResult, Model model, HttpSession session) {
	  RestTemplate restTemplate = new RestTemplate();
      if (bindingResult.hasErrors()) {
          return "index";
      }
	  
	  try
	  {
		  URI uri = new URI(serviceURL+"/"+hpcLogin.getUserId());
		  ResponseEntity<HpcUserDTO> userEntity = restTemplate.getForEntity(uri, HpcUserDTO.class);
		  if(userEntity == null || !userEntity.hasBody())
		  {
			  ObjectError error = new ObjectError("nihUserId", "UserId is not found!");
			  bindingResult.addError(error);
			  model.addAttribute("hpcLogin", hpcLogin);
			  return "index";
		  }
		  HpcUserDTO userDTO = userEntity.getBody();
		  model.addAttribute("loginStatus", true);
		  session.setAttribute("hpcUser", userDTO);
	  }
	  catch(Exception e)
	  {
		  model.addAttribute("loginStatus", false);
		  model.addAttribute("loginOutput", "Invalid login"+e.getMessage());
		  ObjectError error = new ObjectError("nihUserId", "UserId is not found!");
		  bindingResult.addError(error);
		  model.addAttribute("hpcLogin", hpcLogin);
		  return "index";
	  }
	  return "dashboard";
  }
}
