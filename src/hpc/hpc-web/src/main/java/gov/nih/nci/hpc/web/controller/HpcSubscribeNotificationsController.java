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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.AjaxResponseBody;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcNotification;
import gov.nih.nci.hpc.web.model.HpcNotificationRequest;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

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
@RequestMapping("/subscribe")
public class HpcSubscribeNotificationsController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.notification}")
	private String notificationURL;

	@Autowired
	private Environment env;

	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {

		String userPasswdToken = (String) session.getAttribute("userpasstoken");
		if (userPasswdToken == null) {
			return "redirect:/";
		}

		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/";
		}

		populateNotifications(model, userPasswdToken, user, session);
		HpcNotificationRequest notificationRequest = new HpcNotificationRequest();
		model.addAttribute("notificationRequest", notificationRequest);
		return "subscribenotifications";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String search(@Valid @ModelAttribute("notificationRequest") HpcNotificationRequest notificationRequest,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			RedirectAttributes redirectAttrs) {

		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			String[] eventTypes = request.getParameterValues("eventType");
			String serviceURL = notificationURL;
			HpcNotificationSubscriptionsRequestDTO subscriptionsRequestDTO = constructRequest(eventTypes);

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("POST", subscriptionsRequestDTO);
			if (restResponse.getStatus() == 200) {
				model.addAttribute("updateStatus", "Updated successfully");
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
				  new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
				  new JacksonAnnotationIntrospector()
				);
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				
				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());
				
				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				model.addAttribute("updateStatus", "Failed to save criteria! Reason: "+exception.getMessage());
			}
		} catch (HttpStatusCodeException e) {
			model.addAttribute("updateStatus", "Failed to update changes! " + e.getMessage());
			e.printStackTrace();
		} catch (RestClientException e) {
			model.addAttribute("updateStatus", "Failed to update changes! " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			model.addAttribute("updateStatus", "Failed to update changes! " + e.getMessage());
			e.printStackTrace();
		} finally {
			String userPasswdToken = (String) session.getAttribute("userpasstoken");
			if (userPasswdToken == null) {
				return "redirect:/";
			}
			HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
			if (user == null) {
				ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
				bindingResult.addError(error);
				HpcLogin hpcLogin = new HpcLogin();
				model.addAttribute("hpcLogin", hpcLogin);
				return "redirect:/";
			}

			populateNotifications(model, userPasswdToken, user, session);
			model.addAttribute("notificationRequest", notificationRequest);
		}

		return "subscribenotifications";
	}

	private HpcNotificationSubscriptionsRequestDTO constructRequest(String[] eventTypes) {
		HpcNotificationSubscriptionsRequestDTO dto = new HpcNotificationSubscriptionsRequestDTO();
		List<HpcNotificationSubscription> addUpdateSubscriptions = new ArrayList<HpcNotificationSubscription>();
		List<HpcEventType> deleteSubscriptions = new ArrayList<HpcEventType>();

		if (eventTypes != null) {
			List<HpcEventType> types = getEventTypes();
			for (HpcEventType type : types) {
				if (subscribed(eventTypes, type.name())) {
					HpcNotificationSubscription addUpdateSubscription = new HpcNotificationSubscription();
					addUpdateSubscription.setEventType(type);
					addUpdateSubscriptions.add(addUpdateSubscription);
					addUpdateSubscription.getNotificationDeliveryMethods().add(HpcNotificationDeliveryMethod.EMAIL);
				} else
					deleteSubscriptions.add(type);

			}
			dto.getAddUpdateSubscriptions().addAll(addUpdateSubscriptions);
			dto.getDeleteSubscriptions().addAll(deleteSubscriptions);
		}

		return dto;
	}

	private boolean subscribed(String[] notifications, String type) {
		if (notifications == null || notifications.length == 0)
			return false;
		for (int i = 0; i < notifications.length; i++) {
			if (type.equals(notifications[i]))
				return true;
		}
		return false;
	}

	private void populateNotifications(Model model, String authToken, HpcUserDTO user, HttpSession session) {
		Map<String, String> eventTypes = new HashMap<String, String>();
		List<HpcNotification> notifications = new ArrayList<HpcNotification>();
		List<String> subscriptions = getUserNotifications(authToken);
		List<HpcEventType> types = getEventTypes();

		for (HpcEventType type : types) {
			HpcNotification notification = new HpcNotification();
			notification.setEventType(type.name());
			notification.setDisplayName(getDisplayName(type.name()));
			notification.setSubscribed(subscriptions.contains(type.name()));
			notifications.add(notification);
		}

		// model.addAttribute("eventTypes", eventTypes);
		model.addAttribute("notifications", notifications);
		session.setAttribute("subscribedNotifications", notifications);
	}

	private List<HpcEventType> getEventTypes() {
		HpcEventType[] types = HpcEventType.values();
		List<HpcEventType> eventTypes = new ArrayList<HpcEventType>();
		String excludeStr = env.getProperty("gov.nih.nci.notification.exclude.list");
		List<String> excludeList = new ArrayList<String>();
		if (excludeStr != null) {
			StringTokenizer tokens = new StringTokenizer(excludeStr, ",");
			while (tokens.hasMoreElements())
				excludeList.add(tokens.nextToken());
		}

		for (int i = 0; i < types.length; i++) {
			if (excludeList.contains(types[i].name()))
				continue;
			eventTypes.add(types[i]);
		}
		return eventTypes;
	}

	private String getDisplayName(String eventName) {

		String displayName = env.getProperty(eventName);
		if (displayName != null)
			return displayName;
		else
			return eventName;
		//
		// if(eventName.equals("DATA_TRANSFER_UPLOAD_IN_TEMPORARY_ARCHIVE"))
		// return env.getProperty("DATA_TRANSFER_UPLOAD_IN_TEMPORARY_ARCHIVE")
		// != null ?
		// env.getProperty("DATA_TRANSFER_UPLOAD_IN_TEMPORARY_ARCHIVE") : "Data
		// Transfer Upload in staging archive";
		// else if(eventName.equals("DATA_TRANSFER_UPLOAD_ARCHIVED"))
		// return "Data Transfer Upload archived";
		// else if(eventName.equals("DATA_TRANSFER_UPLOAD_FAILED"))
		// return "Data Transfer Upload failed";
		// else if(eventName.equals("DATA_TRANSFER_DOWNLOAD_COMPLETED"))
		// return "Data Transfer Download completed";
		// else if(eventName.equals("DATA_TRANSFER_DOWNLOAD_FAILED"))
		// return "Data Transfer Download failed";
		// else if(eventName.equals("USAGE_SUMMARY_REPORT"))
		// return "Data Archive Usage Summary report";
		// else if(eventName.equals("USAGE_SUMMARY_BY_WEEKLY_REPORT"))
		// return "Data Archive Usage Summary report of this week";
		// else
		// return eventName;
	}

	private List<String> getUserNotifications(String authToken) {
		HpcNotificationSubscriptionListDTO subscriptionListDTO = HpcClientUtil.getUserNotifications(authToken,
				notificationURL, sslCertPath, sslCertPassword);
		List<String> subscriptionList = new ArrayList<String>();

		if (subscriptionListDTO != null) {
			List<HpcNotificationSubscription> subscriptions = subscriptionListDTO.getSubscriptions();
			if (subscriptions != null && subscriptions.size() > 0) {
				for (HpcNotificationSubscription subscription : subscriptions) {
					subscriptionList.add(subscription.getEventType().name());
				}
			}
		}
		return subscriptionList;
	}

}
