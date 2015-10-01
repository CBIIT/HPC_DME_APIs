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

import gov.nih.nci.hpc.dto.user.HpcUserDTO;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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
	@Value("${gov.nih.nci.hpc.server.dataset}")
	private String datasetURL;
	@Value("${gov.nih.nci.hpc.server.project.query.registrar}")
	private String projectURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(Model model, HttpSession session) {
		model.addAttribute("datasetURL", datasetURL);
		model.addAttribute("projectURL", projectURL);
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			return "redirect:/";
		}

		return "dashboard";
	}
}
