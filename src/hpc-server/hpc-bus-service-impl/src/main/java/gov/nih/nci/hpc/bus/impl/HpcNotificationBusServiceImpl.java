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
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryReceipt;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcNotificationService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HPC Notification Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcNotificationBusServiceImpl implements HpcNotificationBusService {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // Application service instances.

  @Autowired private HpcNotificationService notificationService = null;

  @Autowired private HpcEventService eventService = null;

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

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
  public void subscribeNotifications(
      HpcNotificationSubscriptionsRequestDTO notificationSubscriptions) throws HpcException {
    // Input validation.
    if (notificationSubscriptions == null
        || notificationSubscriptions.getAddUpdateSubscriptions() == null) {
      throw new HpcException(
          "Null List<HpcNotificationSubscription>", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Add/Update subscriptions for the user.
    for (HpcNotificationSubscription notificationSubscription :
        notificationSubscriptions.getAddUpdateSubscriptions()) {
      notificationService.addUpdateNotificationSubscription(notificationSubscription);
    }

    // Delete subscriptions for the user.
    if (notificationSubscriptions.getDeleteSubscriptions() != null) {
      for (HpcEventType eventType : notificationSubscriptions.getDeleteSubscriptions()) {
        notificationService.deleteNotificationSubscription(eventType);
      }
    }
  }

  @Override
  public HpcNotificationSubscriptionListDTO getNotificationSubscriptions() throws HpcException {
    // Get the subscriptions for the user.
    List<HpcNotificationSubscription> subscriptions =
        notificationService.getNotificationSubscriptions();
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
  //---------------------------------------------------------------------//

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
}
