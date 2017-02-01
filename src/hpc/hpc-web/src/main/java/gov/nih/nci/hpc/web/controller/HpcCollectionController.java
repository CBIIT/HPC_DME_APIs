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

import java.io.InputStream;

import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionListDTO;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

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
@RequestMapping("/collection")
public class HpcCollectionController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.collection}")
	private String serviceURL;

	@RequestMapping(method = RequestMethod.GET)
	public String home(String path, Model model, 
			HttpSession session) {

		try {
			if (path == null)
				return "dashboard";

			String authToken = (String) session.getAttribute("hpcUserToken");
			WebClient client = HpcClientUtil.getWebClient(serviceURL + path, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("GET", null);
			//System.out.println("restResponse.getStatus():" +restResponse.getStatus());
			if (restResponse.getStatus() == 200) {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
				  new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
				  new JacksonAnnotationIntrospector()
				);
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				
				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
				
				HpcCollectionListDTO collections = parser.readValueAs(HpcCollectionListDTO.class);
				HpcCollectionDTO collection = collections.getCollections().get(0);
				model.addAttribute("collection", collection);
			} else {
				String message = "Collection not found!";
				ObjectError error = new ObjectError("hpcSearch", message);
				model.addAttribute("error", message);
				return "dashboard";
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			model.addAttribute("error", "Failed to get Collection: " + e.getMessage());
			e.printStackTrace();
		}

		return "collection";
	}
}
