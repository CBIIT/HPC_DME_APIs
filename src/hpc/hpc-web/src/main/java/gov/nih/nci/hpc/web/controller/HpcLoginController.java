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

import gov.nih.nci.hpc.web.model.HpcWebUser;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
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
@RequestMapping("/login")
public class HpcLoginController extends AbstractHpcController {

	@Value("${gov.nih.nci.hpc.server.login}")
    private String serviceURL;

  @RequestMapping(method = RequestMethod.GET)
  public String home(Model model){
      return "index";
  }

  @RequestMapping(method = RequestMethod.POST)
  public String register(@Valid HpcWebUser user, Model model, BindingResult bindingResult, HttpSession session) {
	  RestTemplate restTemplate = new RestTemplate();
      if (bindingResult.hasErrors()) {
          return "index";
      }

	  try
	  {
		  //HttpEntity<String> response = restTemplate.postForEntity(uri,  null, String.class);
		  //registration.setId(id);
		  model.addAttribute("loginStatus", true);
		  session.setAttribute("hpcUser", user);
		  
	  }
	  catch(Exception e)
	  {
		  model.addAttribute("loginStatus", false);
		  model.addAttribute("loginOutput", "Invalid login"+e.getMessage());
		  return "index";
	  }
	  return "dashboard";
  }
}
