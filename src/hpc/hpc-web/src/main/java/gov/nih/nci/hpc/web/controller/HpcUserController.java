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

import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcDataTransferType;
import gov.nih.nci.hpc.domain.user.HpcUser;
import gov.nih.nci.hpc.dto.userregistration.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcWebUser;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
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
@RequestMapping("/registerUser")
public class HpcUserController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.userRegistration}")
    private String serviceURL;
	@Value("${gov.nih.nci.hpc.server}")
    private String serverURL;


  @RequestMapping(method = RequestMethod.GET)
  public String home(Model model){
	  HpcWebUser hpcUser = new HpcWebUser();
	  model.addAttribute("hpcUser", hpcUser);
      return "enroll";
  }

  @RequestMapping(method = RequestMethod.POST)
  public String register(@Valid @ModelAttribute("hpcUser") HpcWebUser hpcUser, BindingResult bindingResult, Model model) {
	  RestTemplate restTemplate = new RestTemplate();
      if (bindingResult.hasErrors()) {
          return "enroll";
      }
	  
	  try
	  {
		 // HpcProxy client =  new HpcProxyImpl(serverURL);
		 // HpcUserRegistrationRestService userRegistration = client.getUserRegistrationServiceProxy();
		  HpcUserDTO userDTO = new HpcUserDTO();
		  HpcUser user = new HpcUser();
		  user.setNihUserId(hpcUser.getNihUserId());
		  user.setFirstName(hpcUser.getFirstName());
		  user.setLastName(hpcUser.getLastName());
		  HpcDataTransferAccount dtAccount = new HpcDataTransferAccount();
		  dtAccount.setUsername(hpcUser.getGlobusUserId());
		  dtAccount.setPassword(hpcUser.getGlobusPasswd());
		  dtAccount.setDataTransferType(HpcDataTransferType.GLOBUS);
		  user.setDataTransferAccount(dtAccount);
		  HttpEntity<String> response = restTemplate.postForEntity(serviceURL,  userDTO, String.class);
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
		  model.addAttribute("registrationStatus", false);
		  model.addAttribute("registrationOutput", "Failed to register your request due to: "+e.getMessage());
	  }
	  return "enrollresult";
  }
}
