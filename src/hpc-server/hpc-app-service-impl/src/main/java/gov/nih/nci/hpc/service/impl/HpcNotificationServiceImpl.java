/**
 * HpcNotificationServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static gov.nih.nci.hpc.service.impl.HpcDomainValidator.isValidNotificationSubscription;
import static gov.nih.nci.hpc.service.impl.HpcEventServiceImpl.COLLECTION_PATH_PAYLOAD_ATTRIBUTE;
import static gov.nih.nci.hpc.service.impl.HpcEventServiceImpl.DATA_OBJECT_PATH_PAYLOAD_ATTRIBUTE;
import gov.nih.nci.hpc.dao.HpcNotificationDAO;
import gov.nih.nci.hpc.dao.HpcUserDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryReceipt;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.notification.HpcNotificationTrigger;
import gov.nih.nci.hpc.domain.notification.HpcSystemAdminNotificationType;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcNotificationService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

/**
 * HPC Notification Application Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcNotificationServiceImpl implements HpcNotificationService {
  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // Map notification delivery method to its notification sender impl.
  private Map<HpcNotificationDeliveryMethod, HpcNotificationSender> notificationSenders =
      new EnumMap<>(HpcNotificationDeliveryMethod.class);

  // The Notification DAO instance.
  @Autowired
  private HpcNotificationDAO notificationDAO = null;

  //The User DAO instance.
   @Autowired
  private HpcUserDAO userDAO = null;
	
  // The Data Management Proxy instance.
  @Autowired
  private HpcDataManagementProxy dataManagementProxy = null;

  // The Data Management Authenticator.
  @Autowired
  private HpcDataManagementAuthenticator dataManagementAuthenticator = null;

  // Pagination support.
  @Autowired
  @Qualifier("hpcNotificationPagination")
  private HpcPagination pagination = null;

  // The system administrator NCI user ID.
  @Value("${hpc.service.notification.systemAdministratorUserId}")
  private String systemAdministratorUserId = null;

  //The storage administrators' NCI user IDs.
  @Value("${hpc.service.notification.storageAdministratorUserIds}")
  private String storageAdministratorUserIds = null;

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /**
   * Default constructor disabled.
   *
   * @throws HpcException Constructor disabled.
   */
  private HpcNotificationServiceImpl() throws HpcException {
    throw new HpcException("Constructor Dosabled", HpcErrorType.SPRING_CONFIGURATION_ERROR);
  }

  /**
   * Constructor for Spring Dependency Injection.
   *
   * @param notificationSenders The notification senders.
   */
  private HpcNotificationServiceImpl(
      Map<HpcNotificationDeliveryMethod, HpcNotificationSender> notificationSenders) {
    this.notificationSenders.putAll(notificationSenders);
  }

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // HpcNotificationService Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public void addUpdateNotificationSubscription(
      HpcNotificationSubscription notificationSubscription) throws HpcException {
    // Input validation.
    if (!isValidNotificationSubscription(notificationSubscription)) {
      throw new HpcException("Invalid add/update notification subscription request",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Get the service invoker.
    HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    if (invoker == null) {
      throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
    }

    // Validate subscription for usage summary report is allowed for system admin only
    if (!invoker.getUserRole().equals(HpcUserRole.SYSTEM_ADMIN)) {
      if (notificationSubscription.getEventType().equals(HpcEventType.USAGE_SUMMARY_REPORT)
          || notificationSubscription.getEventType()
              .equals(HpcEventType.USAGE_SUMMARY_BY_WEEKLY_REPORT)
          || notificationSubscription.getEventType()
              .equals(HpcEventType.USAGE_SUMMARY_BY_DOC_REPORT)
          || notificationSubscription.getEventType()
              .equals(HpcEventType.USAGE_SUMMARY_BY_DOC_BY_WEEKLY_REPORT)
          || notificationSubscription.getEventType()
              .equals(HpcEventType.USAGE_SUMMARY_BY_USER_REPORT)
          || notificationSubscription.getEventType()
              .equals(HpcEventType.USAGE_SUMMARY_BY_USER_BY_WEEKLY_REPORT)) {
        throw new HpcException(
            "Not authorized to subscribe to the report. Please contact system administrator",
            HpcRequestRejectReason.NOT_AUTHORIZED);
      }
    }

    // Validate the notification triggers.
    validateNotificationTriggers(notificationSubscription.getNotificationTriggers());

    // Upsert to DB.
    notificationDAO.upsertSubscription(invoker.getNciAccount().getUserId(),
        notificationSubscription);
  }

  @Override
  public void deleteNotificationSubscription(String userId, HpcEventType eventType) throws HpcException {
    // Input validation.
    if (eventType == null) {
      throw new HpcException("Null event type", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Delete from DB.
    notificationDAO.deleteSubscription(userId, eventType);
  }

  @Override
  public List<HpcNotificationSubscription> getNotificationSubscriptions(String userId) throws HpcException {
    return notificationDAO.getSubscriptions(userId);
  }

  @Override
  public List<String> getNotificationSubscribedUsers(HpcEventType eventType) throws HpcException {
    return notificationDAO.getSubscribedUsers(eventType);
  }

  @Override
  public HpcNotificationSubscription getNotificationSubscription(String userId,
      HpcEventType eventType) throws HpcException {
    // Input validation.
    if (userId == null || eventType == null) {
      throw new HpcException("Invalid user ID or event type", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Query the DB.
    return notificationDAO.getSubscription(userId, eventType);
  }

  @Override
  public boolean sendNotification(String userId, HpcEventType eventType,
      List<HpcEventPayloadEntry> payloadEntries, HpcNotificationDeliveryMethod deliveryMethod) {
    // Input validation.
    if (userId == null || eventType == null || deliveryMethod == null) {
      return false;
    }

    // Locate the notification sender for this delivery method.
    HpcNotificationSender notificationSender = notificationSenders.get(deliveryMethod);
    if (notificationSender == null) {
      logger.error("Could not locate notification sender for: " + deliveryMethod);
      return false;
    }
    
    
    // Send the notification.
    try {
      // Get user's doc to see if specific template should be used.
      HpcUser user = userDAO.getUser(userId);
      HpcEventPayloadEntry payloadEntry = new HpcEventPayloadEntry();
      payloadEntry.setAttribute("FIRST_NAME");
      payloadEntry.setValue(user.getNciAccount().getFirstName());
      payloadEntries.add(payloadEntry);

      payloadEntry = new HpcEventPayloadEntry();
      payloadEntry.setAttribute("LAST_NAME");
      payloadEntry.setValue(user.getNciAccount().getLastName());
      payloadEntries.add(payloadEntry);
      
      // If payload entry has doc, then send it for doc specific template
      HpcEventPayloadEntry docPayloadEntry = payloadEntries.stream()
    		  .filter(entry -> "DOC".equals(entry.getAttribute()))
    		  .findAny()
    		  .orElse(null);
      
      // If doc payload entry is GAU, add another payloadEntry for request ID.
      notificationSender.sendNotification(userId, eventType, docPayloadEntry == null ? null: docPayloadEntry.getValue(), payloadEntries);

    } catch (HpcException e) {
      logger.error("failed to send user notification", e);
      return false;
    }

    return true;
  }

  @Override
  public boolean sendNotification(String userId, HpcSystemAdminNotificationType notificationType,
      List<HpcEventPayloadEntry> payloadEntries, HpcNotificationDeliveryMethod deliveryMethod) {
    // Input validation.
    if (userId == null || notificationType == null || deliveryMethod == null) {
      return false;
    }

    // Locate the notification sender for this delivery method.
    HpcNotificationSender notificationSender = notificationSenders.get(deliveryMethod);
    if (notificationSender == null) {
      logger.error("Could not locate notification sender for: " + deliveryMethod);
      return false;
    }

    // Send the notification.
    try {
      notificationSender.sendNotification(userId, notificationType, payloadEntries);

    } catch (HpcException e) {
      logger.error("failed to send system admin notification", e);
      return false;
    }

    return true;
  }

  @Override
  public void createNotificationDeliveryReceipt(String userId, int eventId,
      HpcNotificationDeliveryMethod deliveryMethod, boolean deliveryStatus) {
    if (userId == null || deliveryMethod == null) {
      return;
    }

    HpcNotificationDeliveryReceipt deliveryReceipt = new HpcNotificationDeliveryReceipt();
    deliveryReceipt.setUserId(userId);
    deliveryReceipt.setEventId(eventId);
    deliveryReceipt.setNotificationDeliveryMethod(deliveryMethod);
    deliveryReceipt.setDeliveryStatus(deliveryStatus);
    deliveryReceipt.setDelivered(Calendar.getInstance());

    try {
      notificationDAO.upsertDeliveryReceipt(deliveryReceipt);

    } catch (HpcException e) {
      logger.error("Failed to create a delivery receipt", e);
    }
  }

  @Override
  public List<HpcNotificationDeliveryReceipt> getNotificationDeliveryReceipts(int page)
      throws HpcException {
    // Get the service invoker.
    HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    if (invoker == null) {
      throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
    }

    return notificationDAO.getDeliveryReceipts(invoker.getNciAccount().getUserId(),
        pagination.getOffset(page), pagination.getPageSize());
  }

  @Override
  public HpcNotificationDeliveryReceipt getNotificationDeliveryReceipt(int eventId)
      throws HpcException {
    // Get the service invoker.
    HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    if (invoker == null) {
      throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
    }

    return notificationDAO.getDeliveryReceipt(invoker.getNciAccount().getUserId(), eventId);
  }

  @Override
  public int getNotificationDeliveryReceiptsPageSize() {
    return pagination.getPageSize();
  }

  @Override
  public int getNotificationDeliveryReceiptsCount() throws HpcException {
    // Get the service invoker.
    HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    if (invoker == null) {
      throw new HpcException("Unknown service invoker", HpcErrorType.UNEXPECTED_ERROR);
    }

    return notificationDAO.getDeliveryReceiptsCount(invoker.getNciAccount().getUserId());
  }


  public void sendNotification(HpcException exception) {
	  sendNotification(exception, false);
  }


  @Override
  public void sendNotification(HpcException exception, boolean notifyStorageAdmins) {
    if (exception.getIntegratedSystem() != null) {
      logger.info("Sending a notification to system admin: {}", exception.getMessage());

      // Create a payload containing the exception data.
      List<HpcEventPayloadEntry> payloadEntries = new ArrayList<>();

      HpcEventPayloadEntry integratedSystemPayloadEntry = new HpcEventPayloadEntry();
      integratedSystemPayloadEntry.setAttribute("INTEGRATED_SYSTEM");
      integratedSystemPayloadEntry.setValue(exception.getIntegratedSystem().value());
      payloadEntries.add(integratedSystemPayloadEntry);

      HpcEventPayloadEntry errorMessage = new HpcEventPayloadEntry();
      errorMessage.setAttribute("ERROR_MESSAGE");
      errorMessage.setValue(exception.getMessage());
      payloadEntries.add(errorMessage);

      HpcEventPayloadEntry stackTrace = new HpcEventPayloadEntry();
      stackTrace.setAttribute("STACK_TRACE");
      stackTrace.setValue(exception.getStackTraceString());
      payloadEntries.add(stackTrace);

      // Send the notification.
      this.sendNotification(systemAdministratorUserId,
          HpcSystemAdminNotificationType.INTEGRATED_SYSTEM_ERROR, payloadEntries,
          HpcNotificationDeliveryMethod.EMAIL);

      if(notifyStorageAdmins && storageAdministratorUserIds != null) {
          for(String userId: storageAdministratorUserIds.split(",")) {
              this.sendNotification(userId,
              HpcSystemAdminNotificationType.INTEGRATED_SYSTEM_ERROR, payloadEntries,
              HpcNotificationDeliveryMethod.EMAIL);
          }
      }
    }
  }


  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

  /**
   * Validate notification triggers include collection/data-objects that exist. In addition, event
   * payload for collections/data-objects are referencing the relative path of the
   * collection/data-object.For this reason, we make sure the triggers are referencing relative path
   * as well.
   *
   * @param notificationTriggers The notification triggers to validate.
   * @throws HpcException if found an invalid notification trigger.
   */
  private void validateNotificationTriggers(List<HpcNotificationTrigger> notificationTriggers)
      throws HpcException {
    for (HpcNotificationTrigger notificationTrigger : notificationTriggers) {
      for (HpcEventPayloadEntry payloadEntry : notificationTrigger.getPayloadEntries()) {
        if (payloadEntry.getAttribute().equals(COLLECTION_PATH_PAYLOAD_ATTRIBUTE)) {
          String collectionPath = payloadEntry.getValue();
          if (!dataManagementProxy
              .getPathAttributes(dataManagementAuthenticator.getAuthenticatedToken(),
                  collectionPath)
              .getIsDirectory()) {
            throw new HpcException("Collection doesn't exist: " + collectionPath,
                HpcErrorType.INVALID_REQUEST_INPUT);
          }
          payloadEntry.setValue(dataManagementProxy.getRelativePath(collectionPath));
          break;
        }
        if (payloadEntry.getAttribute().equals(DATA_OBJECT_PATH_PAYLOAD_ATTRIBUTE)) {
          String dataObjectPath = payloadEntry.getValue();
          if (!dataManagementProxy.getPathAttributes(
              dataManagementAuthenticator.getAuthenticatedToken(), dataObjectPath).getIsFile()) {
            throw new HpcException("Data object doesn't exist: " + dataObjectPath,
                HpcErrorType.INVALID_REQUEST_INPUT);
          }
          payloadEntry.setValue(dataManagementProxy.getRelativePath(dataObjectPath));
        }
      }
    }
  }
}
