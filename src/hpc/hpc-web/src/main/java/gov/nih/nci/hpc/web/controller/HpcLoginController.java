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

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

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
	@Value("${gov.nih.nci.hpc.server.user.authenticate}")
	private String authenticateURL;
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String collectionURL;
	@Value("${gov.nih.nci.hpc.server.query}")
	private String queryURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(Model model) {
		HpcLogin hpcLogin = new HpcLogin();
		model.addAttribute("hpcLogin", hpcLogin);
		model.addAttribute("queryURL", queryURL);
		model.addAttribute("collectionURL", collectionURL);

		return "index";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String login(@Valid @ModelAttribute("hpcLogin") HpcLogin hpcLogin, BindingResult bindingResult, Model model,
			HttpSession session) {
		if (bindingResult.hasErrors()) {
			return "index";
		}
		try {
			RestTemplate restTemplate = HpcClientUtil.getRestTemplate(sslCertPath, sslCertPassword);
			String authToken = HpcClientUtil.getAuthenticationToken(hpcLogin.getUserId(), hpcLogin.getPasswd(),
					authenticateURL);
			if (authToken != null) {
				session.setAttribute("hpcUserToken", authToken);
				HpcUserDTO user = getUser(hpcLogin.getUserId(), authToken);
				if (user == null) {
					model.addAttribute("loginStatus", false);
					model.addAttribute("loginOutput", "Invalid login");
					ObjectError error = new ObjectError("hpcLogin", "UserId is not found!");
					bindingResult.addError(error);
					model.addAttribute("hpcLogin", hpcLogin);
					return "index";
				}
				session.setAttribute("hpcUser", user);
				// String token =
				// DatatypeConverter.printBase64Binary((hpcLogin.getUserId() +
				// ":" + hpcLogin.getPasswd()).getBytes());
				// session.setAttribute("userpasstoken", token);
			} else {
				model.addAttribute("loginStatus", false);
				model.addAttribute("loginOutput", "Invalid login");
				ObjectError error = new ObjectError("hpcLogin", "UserId is not found!");
				bindingResult.addError(error);
				model.addAttribute("hpcLogin", hpcLogin);
				return "index";
			}
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("loginStatus", false);
			model.addAttribute("loginOutput", "Invalid login" + e.getMessage());
			ObjectError error = new ObjectError("hpcLogin", "UserId is not found!");
			bindingResult.addError(error);
			model.addAttribute("hpcLogin", hpcLogin);
			return "index";
		}
		model.addAttribute("hpcLogin", hpcLogin);
		model.addAttribute("queryURL", queryURL);

		return "dashboard";
	}

	private HpcUserDTO getUser(String userId, String authToken) throws IOException {
		WebClient client = HpcClientUtil.getWebClient(serviceUserURL + "/" + userId, sslCertPath, sslCertPassword);
		client.header("Authorization", "Bearer " + authToken);

		Response restResponse = client.invoke("GET", null);
		if (restResponse.getStatus() == 200) {
			ObjectMapper mapper = new ObjectMapper();
			AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
					new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()), new JacksonAnnotationIntrospector());
			mapper.setAnnotationIntrospector(intr);
			MappingJsonFactory factory = new MappingJsonFactory(mapper);
			JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
			HpcUserDTO user = parser.readValueAs(HpcUserDTO.class);
			return user;
		}
		return null;
	}
}
