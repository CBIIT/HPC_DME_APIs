package gov.nih.nci.hpc.web.controller;

import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcWebUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import gov.nih.nci.hpc.dto.security.HpcUserDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
@EnableAutoConfiguration
@RequestMapping("/profile")
public class HpcProfileController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.acl}")
	private String serverAclURL;
	@Value("${gov.nih.nci.hpc.server.user}")
	private String serviceURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serverCollectionURL;
	@Value("${gov.nih.nci.hpc.server.dataObject}")
	private String serverDataObjectURL;
	@Value("${hpc.serviceaccount}")
	private String serviceAccount;
	@Value("${gov.nih.nci.hpc.server.user.all}")
	private String allUsersServiceURL;
	@Value("${gov.nih.nci.hpc.server.user.active}")
	private String activeUsersServiceURL;

	// The logger instance.
	private final Logger logger =
		LoggerFactory.getLogger(this.getClass().getName());


	/**
	 *
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
    public String profile(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
                          HttpSession session, HttpServletRequest request) {
    	HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login?returnPath=profile";
		}

		HpcWebUser webUser = new HpcWebUser();
		model.addAttribute("hpcWebUser", webUser);
		model.addAttribute("profile", user);
        return "profile";
    }

}
