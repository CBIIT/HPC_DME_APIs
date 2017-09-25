/**
 * HpcSubscribeNotificationsController.java
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
import java.util.Enumeration;
import java.util.List;
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

import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.notification.HpcNotificationTrigger;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;
import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import gov.nih.nci.hpc.web.model.HpcLogin;
import gov.nih.nci.hpc.web.model.HpcNotification;
import gov.nih.nci.hpc.web.model.HpcNotificationRequest;
import gov.nih.nci.hpc.web.model.HpcNotificationTriggerModel;
import gov.nih.nci.hpc.web.model.HpcNotificationTriggerModelEntry;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Controller to subscribe or unsubscribe notifications
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/subscribe")
public class HpcSubscribeNotificationsController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.notification}")
	private String notificationURL;

	@Autowired
	private Environment env;

	/**
	 * GET action to populate notifications with user subscriptions
	 * 
	 * @param q
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String home(@RequestBody(required = false) String q, Model model, BindingResult bindingResult,
			HttpSession session, HttpServletRequest request) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		if (authToken == null) {
			return "redirect:/";
		}

		HpcUserDTO user = (HpcUserDTO) session.getAttribute("hpcUser");
		if (user == null) {
			ObjectError error = new ObjectError("hpcLogin", "Invalid user session!");
			bindingResult.addError(error);
			HpcLogin hpcLogin = new HpcLogin();
			model.addAttribute("hpcLogin", hpcLogin);
			return "redirect:/login?returnPath=subscribe";
		}

		populateNotifications(model, authToken, user, session);
		HpcNotificationRequest notificationRequest = new HpcNotificationRequest();
		model.addAttribute("notificationRequest", notificationRequest);

		return "subscribenotifications";
	}

	/**
	 * POST action to subscribe or unsubscribe notifications
	 * 
	 * @param notificationRequest
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @param redirectAttrs
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String search(@Valid @ModelAttribute("notificationRequest") HpcNotificationRequest notificationRequest,
			Model model, BindingResult bindingResult, HttpSession session, HttpServletRequest request,
			RedirectAttributes redirectAttrs) {

		try {
			String authToken = (String) session.getAttribute("hpcUserToken");
			String[] eventTypes = request.getParameterValues("eventType");
			String serviceURL = notificationURL;
			HpcNotificationSubscriptionsRequestDTO subscriptionsRequestDTO = constructRequest(eventTypes, request);

			WebClient client = HpcClientUtil.getWebClient(serviceURL, sslCertPath, sslCertPassword);
			client.header("Authorization", "Bearer " + authToken);

			Response restResponse = client.invoke("POST", subscriptionsRequestDTO);
			if (restResponse.getStatus() == 200) {
				model.addAttribute("updateStatus", "Updated successfully");
			} else {
				ObjectMapper mapper = new ObjectMapper();
				AnnotationIntrospectorPair intr = new AnnotationIntrospectorPair(
						new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()),
						new JacksonAnnotationIntrospector());
				mapper.setAnnotationIntrospector(intr);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

				MappingJsonFactory factory = new MappingJsonFactory(mapper);
				JsonParser parser = factory.createParser((InputStream) restResponse.getEntity());

				HpcExceptionDTO exception = parser.readValueAs(HpcExceptionDTO.class);
				model.addAttribute("updateStatus", "Failed to save criteria! Reason: " + exception.getMessage());
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
			String authToken = (String) session.getAttribute("hpcUserToken");
			if (authToken == null) {
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

			populateNotifications(model, authToken, user, session);
			model.addAttribute("notificationRequest", notificationRequest);
		}

		return "subscribenotifications";
	}

	private HpcNotificationSubscriptionsRequestDTO constructRequest(String[] eventTypes, HttpServletRequest request) {
		HpcNotificationSubscriptionsRequestDTO dto = new HpcNotificationSubscriptionsRequestDTO();
		List<HpcNotificationSubscription> addUpdateSubscriptions = new ArrayList<HpcNotificationSubscription>();
		List<HpcEventType> deleteSubscriptions = new ArrayList<HpcEventType>();

		boolean collectionUpdated = false;
		Enumeration<String> params = request.getParameterNames();
		HpcNotificationSubscription collectionUpdateSubscription = new HpcNotificationSubscription();
		collectionUpdateSubscription.setEventType(HpcEventType.COLLECTION_UPDATED);
		collectionUpdateSubscription.getNotificationDeliveryMethods().add(HpcNotificationDeliveryMethod.EMAIL);
		List<HpcNotificationTrigger> triggers = new ArrayList<HpcNotificationTrigger>();

		while (params.hasMoreElements()) {
			String paramName = params.nextElement();
			if (paramName.startsWith("collectionPathAdded")) {
				String[] value = request.getParameterValues(paramName);
				if (value != null && !value[0].isEmpty()) {
					HpcNotificationTrigger trigger = new HpcNotificationTrigger();
					List<HpcEventPayloadEntry> entries = new ArrayList<HpcEventPayloadEntry>();
					HpcEventPayloadEntry pathEntry = new HpcEventPayloadEntry();
					pathEntry.setAttribute("COLLECTION_PATH");
					pathEntry.setValue(value[0]);
					entries.add(pathEntry);
					trigger.getPayloadEntries().addAll(entries);
					triggers.add(trigger);
					collectionUpdated = true;
				}
			} else if (paramName.startsWith("existingCollectionCheck")) {
				String[] value = request.getParameterValues(paramName);
				if (value != null && value[0].equals("on")) {
					String counter = paramName.substring("existingCollectionCheck".length());
					String[] existingCollectionPath = request.getParameterValues("existingCollectionPath" + counter);
					HpcNotificationTrigger trigger = new HpcNotificationTrigger();
					List<HpcEventPayloadEntry> entries = new ArrayList<HpcEventPayloadEntry>();
					HpcEventPayloadEntry pathEntry = new HpcEventPayloadEntry();
					pathEntry.setAttribute("COLLECTION_PATH");
					pathEntry.setValue(existingCollectionPath[0]);
					entries.add(pathEntry);
					String[] existingMetadataCheck = request.getParameterValues("existingMetadataCheck" + counter);

					if (existingMetadataCheck != null && existingMetadataCheck.length > 0
							&& (existingMetadataCheck[0].equals("true") || existingMetadataCheck[0].equals("on"))) {
						HpcEventPayloadEntry metadataEntry = new HpcEventPayloadEntry();
						metadataEntry.setAttribute("UPDATE");
						metadataEntry.setValue("METADATA");
						entries.add(metadataEntry);
					}

					trigger.getPayloadEntries().addAll(entries);
					triggers.add(trigger);
					collectionUpdated = true;
				}
			}
		}

		if (collectionUpdated) {
			collectionUpdateSubscription.getNotificationTriggers().addAll(triggers);
			dto.getAddUpdateSubscriptions().add(collectionUpdateSubscription);
		}

		if (eventTypes != null) {
			List<HpcEventType> types = getEventTypes();
			for (HpcEventType type : types) {
				if (subscribed(eventTypes, type.name())) {
					HpcNotificationSubscription addUpdateSubscription = new HpcNotificationSubscription();
					addUpdateSubscription.setEventType(type);
					addUpdateSubscriptions.add(addUpdateSubscription);
					addUpdateSubscription.getNotificationDeliveryMethods().add(HpcNotificationDeliveryMethod.EMAIL);
				} else {
					if (!type.equals(HpcEventType.COLLECTION_UPDATED))
						deleteSubscriptions.add(type);
					else if (!collectionUpdated)
						deleteSubscriptions.add(type);
				}

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
		List<HpcNotification> notifications = new ArrayList<HpcNotification>();
		List<HpcNotificationSubscription> subscriptions = getUserNotifications(authToken);
		List<HpcEventType> types = getEventTypes();

		for (HpcEventType type : types) {
			HpcNotification notification = new HpcNotification();
			HpcNotificationSubscription subscription = getNotificationSubscription(subscriptions, type);
			notification.setEventType(type.name());
			notification.setDisplayName(getDisplayName(type.name()));
			notification.setSubscribed(subscription == null ? false : true);
			if (type.equals(HpcEventType.COLLECTION_UPDATED) && subscription != null) {
				List<HpcNotificationTrigger> triggers = subscription.getNotificationTriggers();
				if (triggers != null && triggers.size() > 0) {
					for (HpcNotificationTrigger trigger : triggers) {
						HpcNotificationTriggerModel triggerModel = new HpcNotificationTriggerModel();
						if (trigger.getPayloadEntries() != null && trigger.getPayloadEntries().size() > 0) {
							HpcNotificationTriggerModelEntry modelEntry = new HpcNotificationTriggerModelEntry();
							for (HpcEventPayloadEntry entry : trigger.getPayloadEntries()) {
								if (entry.getAttribute().equals("COLLECTION_PATH"))
									modelEntry.setPath(entry.getValue());
								// else
								// if(entry.getAttribute().equals("UPDATE"))
								// modelEntry.setMetadata(entry.getValue());
							}
							triggerModel.getEntries().add(modelEntry);
						}
						notification.getTriggers().add(triggerModel);
					}
				}
			}
			notifications.add(notification);
		}

		model.addAttribute("notifications", notifications);
		session.setAttribute("subscribedNotifications", notifications);
	}

	private HpcNotificationSubscription getNotificationSubscription(List<HpcNotificationSubscription> subscriptions,
			HpcEventType type) {
		if (subscriptions == null || subscriptions.size() == 0)
			return null;
		for (HpcNotificationSubscription subscription : subscriptions) {
			if (subscription.getEventType().equals(type))
				return subscription;
		}
		return null;
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
	}

	private List<HpcNotificationSubscription> getUserNotifications(String authToken) {
		HpcNotificationSubscriptionListDTO subscriptionListDTO = HpcClientUtil.getUserNotifications(authToken,
				notificationURL, sslCertPath, sslCertPassword);

		if (subscriptionListDTO != null) {
			List<HpcNotificationSubscription> subscriptions = subscriptionListDTO.getSubscriptions();
			if (subscriptions != null && subscriptions.size() > 0) {
				return subscriptions;
			}
		}
		return null;
	}

}
