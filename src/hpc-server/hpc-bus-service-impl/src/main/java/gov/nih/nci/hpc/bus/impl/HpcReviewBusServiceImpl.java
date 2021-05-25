/**
 * HpcReviewBusServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import gov.nih.nci.hpc.bus.HpcReviewBusService;
import gov.nih.nci.hpc.bus.aspect.HpcExecuteAsSystemAccount;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.review.HpcReviewEntry;
import gov.nih.nci.hpc.domain.user.HpcAuthenticationType;
import gov.nih.nci.hpc.dto.review.HpcReviewDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcReviewService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC Review Business Service Implementation.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcReviewBusServiceImpl implements HpcReviewBusService {

	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Review Application Service instance.
	@Autowired
	private HpcReviewService reviewService = null;

	// Notification Application service instance.

	@Autowired
	private HpcNotificationService notificationService = null;
	// The security service instance.
	@Autowired
	private HpcSecurityService securityService = null;

	// On off switch to send the review emails to actual users.
	@Value("${hpc.bus.sendReviewNotificationToUser}")
	private Boolean sendReviewNotificationToUser = null;
		
	// The system administrator NCI user ID.
    @Value("${hpc.service.notification.systemAdministratorUserId}")
    private String systemAdministratorUserId = null;
	  
	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcReviewBusServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcReviewBusService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public HpcReviewDTO getReview(String projectStatus, String dataCurator) throws HpcException {

		// Execute the query and package the results into a DTO.
		HpcReviewDTO reviewDTO = new HpcReviewDTO();

		List<HpcReviewEntry> hpcReviewEntries = reviewService.getReview(projectStatus, dataCurator);

		reviewDTO.getReviewEntries().addAll(hpcReviewEntries);

		return reviewDTO;

	}

	@Override
	@HpcExecuteAsSystemAccount
	public void sendAnnualReview() throws HpcException {
		// Send Annual Review emails to data curators
		List<String> curators = reviewService.getCuratorsForAnnualReview();
		
		// For each curator, send an annual review email if the curator have active projects 
		// and annual review email has never been sent 
		// or it has been one year since the last annual review email was sent.
		for (String curator: curators) {
			sendReview(curator);
		}
	}
	
	@Override
	@HpcExecuteAsSystemAccount
	public void sendAnnualReviewReminder() throws HpcException {
		// Send Annual Review Reminder emails to data curators
		List<String> curators = reviewService.getCuratorsForAnnualReviewReminder();
		
		// For each curator, send a reminder email if annual review email has been sent,
		// and the curator have active projects that has a blank review date 
		// or a last review date that is before the last annual review email sent date.
		for (String curator: curators) {
			sendReminder(curator);
		}
	}
	
	/**
	 * Send an 'Annual Review' email to the user.
	 *
	 * @param nciUserId
	 *            The NCI user ID.
	 */
	private void sendReview(String nciUserId) throws HpcException {

		if (nciUserId == null) {
			throw new HpcException("Null nciUserId", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcUser user = securityService.getUser(nciUserId);
		if (user == null) {
			logger.error("User {} doesn't exist", nciUserId);
			return;
		}
		List<HpcEventPayloadEntry> payloadEntries = new ArrayList<>();
		HpcEventPayloadEntry payloadEntry = new HpcEventPayloadEntry();
		payloadEntry.setAttribute("FIRST_NAME");
		payloadEntry.setValue(user.getNciAccount().getFirstName());
		payloadEntries.add(payloadEntry);

		payloadEntry = new HpcEventPayloadEntry();
		payloadEntry.setAttribute("LAST_NAME");
		payloadEntry.setValue(user.getNciAccount().getLastName());
		payloadEntries.add(payloadEntry);

		// For lower tiers, don't send it to the actual user but to system admin

		logger.info("Sending annual review notification for: {} sending to {}", nciUserId, sendReviewNotificationToUser.booleanValue() ? nciUserId : systemAdministratorUserId);
		notificationService.sendNotification(sendReviewNotificationToUser.booleanValue() ? nciUserId : systemAdministratorUserId, HpcEventType.REVIEW_SENT, payloadEntries,
				HpcNotificationDeliveryMethod.EMAIL);

		reviewService.addReviewSentNotification(nciUserId);
	}
	
	/**
	 * Send a 'Review reminder' email to the user.
	 *
	 * @param nciUserId
	 *            The NCI user ID.
	 */
	@Override
	public void sendReminder(String nciUserId) throws HpcException {

		if (nciUserId == null) {
			throw new HpcException("Null nciUserId", HpcErrorType.INVALID_REQUEST_INPUT);
		}

		HpcUser user = securityService.getUser(nciUserId);
		if (user == null) {
			throw new HpcException("User doesn't exist", HpcErrorType.INVALID_REQUEST_INPUT);
		}
		List<HpcEventPayloadEntry> payloadEntries = new ArrayList<>();
		HpcEventPayloadEntry payloadEntry = new HpcEventPayloadEntry();
		payloadEntry.setAttribute("FIRST_NAME");
		payloadEntry.setValue(user.getNciAccount().getFirstName());
		payloadEntries.add(payloadEntry);

		payloadEntry = new HpcEventPayloadEntry();
		payloadEntry.setAttribute("LAST_NAME");
		payloadEntry.setValue(user.getNciAccount().getLastName());
		payloadEntries.add(payloadEntry);

		// For lower tiers, don't send it to the actual user but to the invoker.
		HpcRequestInvoker invoker = securityService.getRequestInvoker();
		if (invoker == null) {
			throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
		}
		String invokerUserId = null;
		if (HpcAuthenticationType.SYSTEM_ACCOUNT
				.equals(invoker.getAuthenticationType()))
			invokerUserId = systemAdministratorUserId;
		else
			invokerUserId = invoker.getNciAccount().getUserId();

		logger.info("Sending review reminder notification to: {} triggered by {}", nciUserId, invokerUserId);
		notificationService.sendNotification(sendReviewNotificationToUser.booleanValue() ? nciUserId : invokerUserId, HpcEventType.REVIEW_REMINDER_SENT, payloadEntries,
				HpcNotificationDeliveryMethod.EMAIL);

		reviewService.addReviewReminderSentNotification(nciUserId);
	}

}
