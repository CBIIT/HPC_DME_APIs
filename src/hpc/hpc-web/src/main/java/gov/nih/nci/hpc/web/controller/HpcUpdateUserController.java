/**
 * HpcSearchProjectController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.hpc.dto.datamanagement.HpcDataManagementDocListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.dto.security.HpcUserListDTO;
import gov.nih.nci.hpc.dto.security.HpcUserRequestDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebUser;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * HPC DM Project Search controller
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id: HpcDataRegistrationController.java
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/updateuser")
public class HpcUpdateUserController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.user}")
	private String userServiceURL;
	@Value("${gov.nih.nci.hpc.server.docs}")
	private String docsServiceURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q,  @RequestParam String userId, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (user == null || authToken == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		init(userId, model, authToken, user);
		return "updateuser";
	}

	private void init(String userId, Model model, String authToken, HpcUserDTO user)
	{
		getUser(userId, model, authToken);
		HpcWebUser webUser = new HpcWebUser();
		webUser.setNciUserId(userId);
		model.addAttribute("hpcWebUser", webUser);
		populateDOCs(model, authToken, user);
		populateRoles(model, user);
	}
	
	private void getUser(String userId, Model model, String authToken)
	{
		HpcUserDTO userDTO = HpcClientUtil.getUser(authToken, userServiceURL, userId,
				sslCertPath, sslCertPassword);
		model.addAttribute("userDTO", userDTO);
	}
	
	private void populateDOCs(Model model, String authToken, HpcUserDTO user)
	{
		List<String> userDOCs = new ArrayList<String>();
		if(user.getUserRole().equals("SYSTEM_ADMIN"))
		{
			HpcDataManagementDocListDTO docs = HpcClientUtil.getDOCs(authToken, docsServiceURL, 
					sslCertPath, sslCertPassword);
			model.addAttribute("docs", docs.getDocs());
		}
		else
		{
			userDOCs.add(user.getDoc());
			model.addAttribute("docs", userDOCs);
		}
	}

	private void populateRoles(Model model, HpcUserDTO user)
	{
		List<String> roles = new ArrayList<String>();
		if(user.getUserRole().equals("SYSTEM_ADMIN"))
		{
			roles.add("SYSTEM_ADMIN");
			roles.add("GROUP_ADMIN");
			roles.add("USER");
		}
		else
			roles.add("USER");
		model.addAttribute("roles", roles);
	}

	@JsonView(Views.Public.class)
	@RequestMapping(method = RequestMethod.POST)
	@ResponseBody
	public AjaxResponseBody updateUser(@Valid @ModelAttribute("hpcUser") HpcWebUser hpcWebUser, BindingResult bindingResult,
			Model model, HttpSession session, HttpServletRequest request) {
		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		String authToken = (String) session.getAttribute("hpcUserToken");
		AjaxResponseBody result = new AjaxResponseBody();

		try {
			if(hpcWebUser.getNciUserId() == null || hpcWebUser.getNciUserId().trim().length() == 0 
					|| hpcWebUser.getFirstName() == null || hpcWebUser.getFirstName().trim().length() == 0 
					|| hpcWebUser.getLastName() == null && hpcWebUser.getLastName().trim().length() == 0
					|| hpcWebUser.getDoc() == null && hpcWebUser.getDoc().trim().length() == 0
					|| hpcWebUser.getUserRole() == null && hpcWebUser.getUserRole().trim().length() == 0)
				model.addAttribute("message", "Invald user input");
			
			HpcUserRequestDTO dto = new HpcUserRequestDTO();
			dto.setDoc(hpcWebUser.getDoc());
			dto.setFirstName(hpcWebUser.getFirstName());
			dto.setLastName(hpcWebUser.getLastName());
			dto.setUserRole(hpcWebUser.getUserRole());
			dto.setActive((hpcWebUser.getActive() != null && hpcWebUser.getActive().equals("on"))?true:false); 
			
			boolean created = HpcClientUtil.updateUser(authToken, userServiceURL, dto, hpcWebUser.getNciUserId(),
					sslCertPath, sslCertPassword);
			if (created)
				result.setMessage("User account updated ");
		} catch (Exception e) {
			result.setMessage("Failed to update user: " + e.getMessage());
		}
		finally
		{
			init(hpcWebUser.getNciUserId(), model, authToken, user);			
		}
		return result;
	}
	
}
