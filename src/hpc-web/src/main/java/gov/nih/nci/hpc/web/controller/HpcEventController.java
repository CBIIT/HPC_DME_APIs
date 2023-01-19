/**
 * HpcEventController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptListDTO;
import gov.nih.nci.hpc.web.util.HpcClientUtil;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * <p>
 * Controller to display user notification event details
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/event")
public class HpcEventController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.notification.receipt}")
	private String receiptServiceURL;

	/**
	 * GET action to display event details
	 * 
	 * @param body
	 * @param id
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String body, @RequestParam String id, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			final String serviceURL = UriComponentsBuilder.fromHttpUrl(
        this.receiptServiceURL).queryParam("eventId", id).build().encode()
				.toUri().toURL().toExternalForm();
			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("GET", serviceURL);
			if (restResponse.getStatus() == 200) {
				MappingJsonFactory factory = new MappingJsonFactory();
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
				HpcNotificationDeliveryReceiptListDTO receiptsDTO = parser
						.readValueAs(HpcNotificationDeliveryReceiptListDTO.class);
				if (receiptsDTO != null) {
					List<HpcNotificationDeliveryReceiptDTO> receipts = receiptsDTO.getNotificationDeliveryReceipts();
					if (receipts != null && receipts.size() > 0) {
					    for(HpcNotificationDeliveryReceiptDTO receipt: receipts) {
					      for(HpcEventPayloadEntry entry : receipt.getEventPayloadEntries()) {
					        if(entry.getValue().startsWith("<br>"))
					          entry.setValue(entry.getValue().substring("<br>".length()));
					      }
					    }
						model.addAttribute("receipt", receipts.get(0));
					}
				}
			} else {
				String message = "No matching results!";
				ObjectError error = new ObjectError("hpcSearch", message);
				bindingResult.addError(error);
				model.addAttribute("error", message);
				return "dashboard";
			}
		} catch (com.fasterxml.jackson.databind.JsonMappingException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "dashboard";
		} catch (HttpStatusCodeException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "dashboard";
		} catch (RestClientException e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "dashboard";
		} catch (Exception e) {
			e.printStackTrace();
			ObjectError error = new ObjectError("hpcLogin", "Failed to search project: " + e.getMessage());
			bindingResult.addError(error);
			model.addAttribute("error", "Failed to search projects due to: " + e.getMessage());
			return "dashboard";
		}

		return "receiptdetails";
	}
}
