/**
 * HpcDataManagementController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.dto.user.HpcAuthenticationRequestDTO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * <p>
 * HPC Data management Web Application root Controller 
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDataManagementController.java 
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/")
public class HpcDataManagementController extends AbstractHpcController {

	@Value("${gov.nih.nci.hpc.login.module}")
	private String loginModule;


  @RequestMapping(method = RequestMethod.GET)
  public String index(Model model){
	  HpcAuthenticationRequestDTO hpcLogin = new HpcAuthenticationRequestDTO();
	  model.addAttribute("hpcLogin", hpcLogin);
	  model.addAttribute("ldap", loginModule.equals("ldap")?"true":"false");
      return "index";
  }
}
