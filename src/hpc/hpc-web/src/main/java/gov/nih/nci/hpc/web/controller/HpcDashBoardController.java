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

import gov.nih.nci.hpc.domain.dataset.HpcDataset;
import gov.nih.nci.hpc.domain.user.HpcUser;
import gov.nih.nci.hpc.dto.dataset.HpcDatasetDTO;
import gov.nih.nci.hpc.dto.user.HpcUserDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
/**
 * <p>
 * HPC Web Dashboard controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDashBoardController.java 
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/dashboard")
public class HpcDashBoardController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.dataset}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server}")
	private String baseURL;
	
  @RequestMapping(method = RequestMethod.GET)
  public String home(Model model, HttpSession session){
	  HpcUserDTO user = (HpcUserDTO)session.getAttribute("hpcUser");
	  if(user == null)
	  {
		  return "redirect:/";
	  }
		RestTemplate restTemplate = new RestTemplate();
		try {
			if(user.getUser().getNihUserId() == null)
				return "redirect:/";
			
			URI uri = new URI(serviceURL + "/?creatorId="+user.getUser().getNihUserId());
			ResponseEntity<HpcDatasetDTO> dataEntity = restTemplate
					.getForEntity(uri, HpcDatasetDTO.class);
			if (dataEntity == null || !dataEntity.hasBody()) {
				ObjectError error = new ObjectError("id",
						"Dataset is not found!");
				//bindingResult.addError(error);
			} else {
				HpcDatasetDTO dataDTO = dataEntity.getBody();
				if(dataDTO != null)
				{
					List<HpcDataset> datasets = dataDTO.getDatasets();
					if(datasets.size() > 0)
						model.addAttribute("datasets", datasets);
					else
						model.addAttribute("datasets", new ArrayList());
				}
				else
					model.addAttribute("dataset", new ArrayList());
				//Get file upload status
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  model.addAttribute("baseURL", baseURL);
	  return "dashboard";
  }
}
