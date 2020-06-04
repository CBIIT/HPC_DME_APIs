/**
 * HpcNotificationBusServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import gov.nih.nci.hpc.bus.HpcNotificationBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryReceipt;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.dto.notification.HpcAddOrUpdateNotificationSubscriptionProblem;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsResponseDTO;
import gov.nih.nci.hpc.dto.notification.HpcRemoveNotificationSubscriptionProblem;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcSecurityService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HPC Notification Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcNotificationBusServiceImpl implements HpcNotificationBusService {

  private static final String MSG__NULL_OBJ_4_ACTIONS =
    "Failed to perform notification subscription action(s) because " +
    " there was missing representation of action(s).";


  private static final String MSG__OBJ_4_ACTIONS_HAS_NO_ACTIONS =
    "Failed to perform notification subscription action(s) because " +
    " the representation of the action(s) contained no actions.";


  private static final String  MSG_TEMPLATE__ACTION_FAILED_MORE_2_COME =
    "Notification subscription could not be <action>; details follow" +
    " on next lines.";

  private static final String NULL_REF_IN_LOGGING = "<null ref>";
  public static final String ERROR_MSG__INVALID_ADD_UPDT_NOTIF_SUBSCRPTN_RQST = "Invalid add/update notification subscription request";
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // Application service instances.

  @Autowired private HpcNotificationService notificationService = null;

  @Autowired private HpcEventService eventService = null;

  @Autowired private HpcSecurityService securityService = null;

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  private List<HpcAddOrUpdateNotificationSubscriptionProblem> addOrUpdateNotifSubProblems;
  private List<HpcRemoveNotificationSubscriptionProblem> removeNotifSubProblems;

  private List<HpcEventType> removedSubs;
  private List<HpcNotificationSubscription> addedOrUpdatedNotifSubs;

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcNotificationBusServiceImpl() {}

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  //---------------------------------------------------------------------//
  // HpcNotificationBusService Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public HpcNotificationSubscriptionsResponseDTO subscribeNotifications(
    HpcNotificationSubscriptionsRequestDTO notificationSubscriptions)
    throws HpcException {
    validateSubscriptionReqDto(notificationSubscriptions);
    helpProcessSubscriptionAddsAndUpdates(notificationSubscriptions);
    helpProcessSubscriptionRemovals(notificationSubscriptions);
    HpcNotificationSubscriptionsResponseDTO responseDto =
      produceSubscriptionProcessingResponse();
    return responseDto;
  }


  @Override
  public HpcNotificationSubscriptionListDTO getNotificationSubscriptions() throws HpcException {

	// Get the service invoker.
	HpcRequestInvoker invoker = securityService.getRequestInvoker();
	if (invoker == null) {
	      throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
	}

	// Get the subscriptions for the user.
    List<HpcNotificationSubscription> subscriptions =
        notificationService.getNotificationSubscriptions(invoker.getNciAccount().getUserId());
    if (subscriptions == null || subscriptions.isEmpty()) {
      return null;
    }

    // Construct and return a DTO.
    HpcNotificationSubscriptionListDTO subscriptionsDTO = new HpcNotificationSubscriptionListDTO();
    subscriptionsDTO.getSubscriptions().addAll(subscriptions);
    return subscriptionsDTO;
  }

  @Override
  public HpcNotificationDeliveryReceiptListDTO getNotificationDeliveryReceipts(
      int page, boolean totalCount) throws HpcException {
    // Get the delivery receipts for the user and package in a DTO.
    HpcNotificationDeliveryReceiptListDTO deliveryReceiptsDTO =
        new HpcNotificationDeliveryReceiptListDTO();
    for (HpcNotificationDeliveryReceipt deliveryReceipt :
        notificationService.getNotificationDeliveryReceipts(page)) {
      deliveryReceiptsDTO
          .getNotificationDeliveryReceipts()
          .add(toNotificationDeliveryReceiptDTO(deliveryReceipt));
    }

    int limit = notificationService.getNotificationDeliveryReceiptsPageSize();
    deliveryReceiptsDTO.setPage(page);
    deliveryReceiptsDTO.setLimit(limit);
    if (totalCount) {
      int count = deliveryReceiptsDTO.getNotificationDeliveryReceipts().size();
      deliveryReceiptsDTO.setTotalCount(
          (page == 1 && count < limit)
              ? count
              : notificationService.getNotificationDeliveryReceiptsCount());
    }

    return deliveryReceiptsDTO;
  }

  @Override
  public HpcNotificationDeliveryReceiptDTO getNotificationDeliveryReceipt(int eventId)
      throws HpcException {
    // Get the delivery receipts for the user and package in a DTO.
    HpcNotificationDeliveryReceipt receipt =
        notificationService.getNotificationDeliveryReceipt(eventId);
    if (receipt != null) return toNotificationDeliveryReceiptDTO(receipt);
    else return null;
  }
  //---------------------------------------------------------------------//
  // Helper Methods
  //---------------------------------------------------


  /*
   * Helps with processing adds and updates to notification subscriptions.
   *
   * @param notificationSubscriptions - data transfer object conveying the
   *         notification actions (adds, updates, and removes)
   */
  private void helpProcessSubscriptionAddsAndUpdates(
      HpcNotificationSubscriptionsRequestDTO notificationSubscriptions) {
    if (null == this.addedOrUpdatedNotifSubs) {
      this.addedOrUpdatedNotifSubs = new ArrayList<>();
    } else {
      this.addedOrUpdatedNotifSubs.clear();
    }
    if (null == this.addOrUpdateNotifSubProblems) {
      this.addOrUpdateNotifSubProblems = new ArrayList<>();
    } else {
      this.addOrUpdateNotifSubProblems.clear();
    }
    if (null != notificationSubscriptions) {
      for (HpcNotificationSubscription someSub : notificationSubscriptions
        .getAddUpdateSubscriptions()) {
        try {
          notificationService.addUpdateNotificationSubscription(someSub);
          this.addedOrUpdatedNotifSubs.add(someSub);
        } catch (HpcException hpce) {
          HpcAddOrUpdateNotificationSubscriptionProblem problem =
            new HpcAddOrUpdateNotificationSubscriptionProblem();
          problem.setSubscription(someSub);
          problem.setProblem(hpce.getMessage());
          this.addOrUpdateNotifSubProblems.add(problem);
          recordErrorToLog(hpce, Optional.empty());
        }
      }
    }
  }


  /*
   * Helps with processing removals of notification subscriptions.
   *
   * @param notificationSubscriptions - data transfer object conveying the
   *         notification actions (adds, updates, and removes)
   */
  private void helpProcessSubscriptionRemovals(
      HpcNotificationSubscriptionsRequestDTO notificationSubscriptions) throws HpcException {

	// Get the service invoker.
	HpcRequestInvoker invoker = securityService.getRequestInvoker();
	if (invoker == null) {
	      throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
	}

    if (null == this.removedSubs) {
      this.removedSubs = new ArrayList<>();
    } else {
      this.removedSubs.clear();
    }
    if (null == this.removeNotifSubProblems) {
      this.removeNotifSubProblems = new ArrayList<>();
    } else {
      this.removeNotifSubProblems.clear();
    }
    if (notificationSubscriptions.getDeleteSubscriptions() != null) {
      for (HpcEventType removeSubEvent : notificationSubscriptions
          .getDeleteSubscriptions()) {
        try {
          notificationService.deleteNotificationSubscription(invoker.getNciAccount().getUserId(), removeSubEvent);
          this.removedSubs.add(removeSubEvent);
        } catch (HpcException hpce) {
          HpcRemoveNotificationSubscriptionProblem problem =
            new HpcRemoveNotificationSubscriptionProblem();
          problem.setRemoveSubscriptionEvent(removeSubEvent);
          problem.setProblem(hpce.getMessage());
          this.removeNotifSubProblems.add(problem);
          recordErrorToLog(hpce, Optional.of("removed"));
        }
      }
    }
  }


  private HpcNotificationSubscriptionsResponseDTO
            produceSubscriptionProcessingResponse() {
    HpcNotificationSubscriptionsResponseDTO respDTO = new
      HpcNotificationSubscriptionsResponseDTO();

    if (null != this.addedOrUpdatedNotifSubs && !this.addedOrUpdatedNotifSubs
        .isEmpty()) {
      respDTO.getAddedOrUpdatedSubscriptions().addAll(
        this.addedOrUpdatedNotifSubs);
    }
    if (null != this.addOrUpdateNotifSubProblems && !this
        .addOrUpdateNotifSubProblems.isEmpty()) {
      respDTO.getSubscriptionsCouldNotBeAddedOrUpdated().addAll(
        this.addOrUpdateNotifSubProblems);
    }

    if (null != this.removedSubs && !this.removedSubs.isEmpty()) {
      respDTO.getRemovedSubscriptions().addAll(this.removedSubs);
    }
    if (null != this.removeNotifSubProblems && !this.removeNotifSubProblems
        .isEmpty()) {
      respDTO.getSubscriptionsCouldNotBeRemoved().addAll(
        this.removeNotifSubProblems);
    }

    return respDTO;
  }


  private String extractValForLogging(Object obj) {
    String retVal;
    if (null == obj) {
      retVal = NULL_REF_IN_LOGGING;
    } else if (obj.getClass().isEnum()) {
      try {
        retVal = obj.getClass().getMethod("value").invoke(obj).toString();
      } catch (Exception e) {
        retVal = "???";
      }
    } else {
      retVal = obj.toString();
    }

    return retVal;
  }

  private void recordErrorToLog(HpcException hpce, Optional<String>
    subscriptionAction) {
    final String firstErrorMsg = MSG_TEMPLATE__ACTION_FAILED_MORE_2_COME
      .replace("<action>", subscriptionAction.orElse("added/updated"));
    logger.error(firstErrorMsg);
    logger.error("HpcException received: ");
    logger.error("*****    message = " +
      extractValForLogging(hpce.getMessage()));
    logger.error("*****    errorType = " +
      extractValForLogging(hpce.getErrorType()));
    logger.error("*****    integratedSystem = " +
      extractValForLogging(hpce.getIntegratedSystem()));
    logger.error("*****    requestRejectReason = " +
      extractValForLogging(hpce.getRequestRejectReason()));
  }


  /**
   * Construct a notification delivery receipt DTO.
   *
   * @param deliveryReceipt The delivery receipt domain object.
   * @return A notification delivery receipt DTO.
   */
  private HpcNotificationDeliveryReceiptDTO toNotificationDeliveryReceiptDTO(
      HpcNotificationDeliveryReceipt deliveryReceipt) {
    HpcNotificationDeliveryReceiptDTO deliveryReceiptDTO = new HpcNotificationDeliveryReceiptDTO();
    deliveryReceiptDTO.setEventId(deliveryReceipt.getEventId());
    deliveryReceiptDTO.setDeliveryStatus(deliveryReceipt.getDeliveryStatus());
    deliveryReceiptDTO.setNotificationDeliveryMethod(
        deliveryReceipt.getNotificationDeliveryMethod());
    deliveryReceiptDTO.setDelivered(deliveryReceipt.getDelivered());

    // Get the archived event that triggered this notification and populate some DTO fields.
    try {
      HpcEvent event = eventService.getArchivedEvent(deliveryReceipt.getEventId());
      if (event != null) {
        deliveryReceiptDTO.setEventType(event.getType());
        deliveryReceiptDTO.setEventCreated(event.getCreated());
        deliveryReceiptDTO.getEventPayloadEntries().addAll(event.getPayloadEntries());
      }

    } catch (HpcException e) {
      logger.error("Failed to get archived event: " + deliveryReceipt.getEventId(), e);
    }

    return deliveryReceiptDTO;
  }


  /*
   * Validate whether received <code>HpcNotificationSubscriptionsRequestDTO
   * </code> instance contains adequate data for processing notification
   * subscription operations (add, modify, or remove).
   *
   * @param vsrDto - <code>HpcNotificationSubscriptionsRequestDTO</code>
   *                 instance
   * @throws HpcException if instance contains inadequate data
   */
  private void validateSubscriptionReqDto(
      HpcNotificationSubscriptionsRequestDTO vsrDto) throws HpcException {

    if (null == vsrDto) {
      logger.info(MSG__NULL_OBJ_4_ACTIONS);
      throw new HpcException(MSG__NULL_OBJ_4_ACTIONS,
        HpcErrorType.INVALID_REQUEST_INPUT);
    } else {
      final List<HpcNotificationSubscription> nsList =
        vsrDto.getAddUpdateSubscriptions();
      final List<HpcEventType> eventList =
        vsrDto.getDeleteSubscriptions();
      if ( (null == nsList || nsList.isEmpty()) &&
          (null == eventList || eventList.isEmpty()) ) {
        logger.info(MSG__OBJ_4_ACTIONS_HAS_NO_ACTIONS);
        logger.info("Representation of notification subscription action(s): " +
            vsrDto.toString());
        throw new HpcException(MSG__OBJ_4_ACTIONS_HAS_NO_ACTIONS,
          HpcErrorType.INVALID_REQUEST_INPUT);
      } else {
        validateSubscriptionAddUpdateRequests(nsList);
        validateSubscriptionRemoveRequests(eventList);
      }
    }
  }


  private void validateSubscriptionAddUpdateRequests(
    List<HpcNotificationSubscription> subAddUpdateReqList) throws HpcException {
    boolean missingEventType = false;
    boolean missingNotifMethods = false;
    boolean notifMethodUnresolved = false;
    for (HpcNotificationSubscription req : subAddUpdateReqList) {
      if (null == req.getEventType()) {
        missingEventType = true;
      } else {
        List<HpcNotificationDeliveryMethod> notifDms =
          req.getNotificationDeliveryMethods();
        if (null == notifDms || notifDms.isEmpty()) {
          missingNotifMethods = true;
        } else {
          for (HpcNotificationDeliveryMethod dm : notifDms) {
            if (null == dm) {
              notifMethodUnresolved = true;
            }
          }
        }
      }
      if (missingEventType || missingNotifMethods || notifMethodUnresolved) {
        throw new HpcException(
          ERROR_MSG__INVALID_ADD_UPDT_NOTIF_SUBSCRPTN_RQST,
          HpcErrorType.INVALID_REQUEST_INPUT);
      }
    }
  }


  private void validateSubscriptionRemoveRequests(
    List<HpcEventType> removeReqList) throws HpcException {
    for (HpcEventType someEventType : removeReqList) {
      if (null == someEventType) {
        throw new HpcException(
          ERROR_MSG__INVALID_ADD_UPDT_NOTIF_SUBSCRPTN_RQST,
          HpcErrorType.INVALID_REQUEST_INPUT);
      }
    }
  }

}
