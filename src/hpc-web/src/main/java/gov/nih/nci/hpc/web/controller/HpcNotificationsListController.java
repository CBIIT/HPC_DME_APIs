/**
 * HpcNotificationsListController.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See https://ncisvn.nci.nih.gov/svn/HPC_Data_Management/branches/hpc-prototype-dev/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.web.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.commons.lang.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.annotation.JsonView;

import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptListDTO;
import gov.nih.nci.hpc.web.model.HpcNotificationReceipt;
import gov.nih.nci.hpc.web.model.HpcSaveSearch;
import gov.nih.nci.hpc.web.model.Views;
import gov.nih.nci.hpc.web.util.HpcClientUtil;

/**
 * <p>
 * Controller to get user notifications list
 * </p>
 *
 * @author <a href="mailto:Prasad.Konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

@Controller
@EnableAutoConfiguration
@RequestMapping("/notificationList")
public class HpcNotificationsListController extends AbstractHpcController {
	@Value("${gov.nih.nci.hpc.server.notification.receipts}")
	private String queryServiceURL;
	@Autowired
	private Environment env;

	/**
	 * GET action to get user notifications
	 * 
	 * @param search
	 * @param model
	 * @param bindingResult
	 * @param session
	 * @param request
	 * @return
	 */
	@JsonView(Views.Public.class)
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public List<HpcNotificationReceipt> get(@Valid @ModelAttribute("hpcSaveSearch") HpcSaveSearch search, Model model,
			BindingResult bindingResult, HttpSession session, HttpServletRequest request) {
		String authToken = (String) session.getAttribute("hpcUserToken");
		List<HpcNotificationReceipt> result = new ArrayList<HpcNotificationReceipt>();
		try {
			HpcNotificationDeliveryReceiptListDTO notifications = HpcClientUtil.getNotificationReceipts(authToken,
					queryServiceURL, sslCertPath, sslCertPassword);
			if (notifications != null && notifications.getNotificationDeliveryReceipts() != null
					&& notifications.getNotificationDeliveryReceipts().size() > 0) {
				for (HpcNotificationDeliveryReceiptDTO receipt : notifications.getNotificationDeliveryReceipts()) {
					HpcNotificationReceipt notification = new HpcNotificationReceipt();
					notification.setEventId(new Integer(receipt.getEventId()).toString());
					notification.setEventType(getDisplayName(receipt.getEventType().name()));
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
					notification.setEventCreated(format.format(receipt.getEventCreated().getTime()));
					notification.setDelivered(format.format(receipt.getDelivered().getTime()));
					notification.setNotificationDeliveryMethod(WordUtils.capitalizeFully(receipt.getNotificationDeliveryMethod().name()));
					result.add(notification);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// result.sort(Comparator.comparing(HpcNotificationReceipt::getEventCreated));

		Collections.sort(result, new Comparator<HpcNotificationReceipt>() {
			@Override
			public int compare(HpcNotificationReceipt h1, HpcNotificationReceipt h2) {
				return h2.getEventCreated().compareTo(h1.getEventCreated());
			}
		});
		return result;
	}
	
	/*
	 * Looks up Display Name for a given Event Name.
	 *
	 * @param eventName - Name of an Event
	 * 
	 * @return Display Name for Event
	 */
	private String getDisplayName(String eventName) {
		final String displayName = this.env.getProperty(eventName);
		return (null == displayName) ? eventName : displayName;
	}
}
