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

import gov.nih.nci.hpc.dto.project.HpcProjectDTO;

import java.net.URI;
import java.net.URISyntaxException;

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
 * HPC Web Login controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcLoginController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/project")
public class HpcProjectController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.project}")
	private String serviceURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home( String id, Model model,  HttpSession session
			) {
		RestTemplate restTemplate = new RestTemplate();
		try {
			if(id == null)
				return "dashboard";
			URI uri = new URI(serviceURL + "/"+id);
			ResponseEntity<HpcProjectDTO> dataEntity = restTemplate
					.getForEntity(uri, HpcProjectDTO.class);
			if (dataEntity == null || !dataEntity.hasBody()) {
				ObjectError error = new ObjectError("id",
						"Project is not found!");
				model.addAttribute("error", "Failed to get Dataset: "+ id);
				//bindingResult.addError(error);
			} else {
				HpcProjectDTO dataDTO = dataEntity.getBody();
				if(dataDTO != null)
					model.addAttribute("project", dataDTO);
				else
					model.addAttribute("project", new HpcProjectDTO());
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			model.addAttribute("error", "Failed to get Dataset: "+e.getMessage());
			e.printStackTrace();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		model.addAttribute("error", "Failed to get Dataset: "+e.getMessage());
		e.printStackTrace();
	}

		return "project";
	}
}
