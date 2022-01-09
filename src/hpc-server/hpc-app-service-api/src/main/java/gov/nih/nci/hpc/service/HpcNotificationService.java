/**
 * HpcNotificationService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryReceipt;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.notification.HpcSystemAdminNotificationType;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * HPC Notification Application Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcNotificationService {
  /**
   * Add/Update a notification subscription for a user.
   *
   * @param notificationSubscription The notification subscription to add/update.
   * @throws HpcException on service failure.
   */
  public void addUpdateNotificationSubscription(
      HpcNotificationSubscription notificationSubscription) throws HpcException;

  /**
   * Delete a notification subscription for a user.
   *
   * @param userId The user ID.
   * @param eventType The event type to delete.
   * @throws HpcException on service failure.
   */
  public void deleteNotificationSubscription(String userId, HpcEventType eventType) throws HpcException;

  /**
   * Get notification subscriptions of a user.
   *
   * @param userId The user ID.
   * @return A list of notification subscriptions.
   * @throws HpcException on service failure.
   */
  public List<HpcNotificationSubscription> getNotificationSubscriptions(String userId) throws HpcException;

  /**
   * Get notification subscription.
   *
   * @param userId The user ID.
   * @param eventType The event type.
   * @return A notification subscription or null if not found.
   * @throws HpcException on service failure.
   */
  public HpcNotificationSubscription getNotificationSubscription(
      String userId, HpcEventType eventType) throws HpcException;

  /**
   * Get notification subscribed users.
   *
   * @param eventType The event type.
   * @return List of subscribed user IDs.
   * @throws HpcException on service failure.
   */
  public List<String> getNotificationSubscribedUsers(HpcEventType eventType) throws HpcException;

  /**
   * Notify a user of an event.
   *
   * @param userId The user to send the notification to.
   * @param eventType The event type to notify.
   * @param payloadEntries The payload entries to use for the notification message.
   * @param deliveryMethod The delivery method.
   * @return If the notification was delivered successfully, or false otherwise.
   */
  public boolean sendNotification(
      String userId,
      HpcEventType eventType,
      List<HpcEventPayloadEntry> payloadEntries,
      HpcNotificationDeliveryMethod deliveryMethod);

  /**
   * Notify a system admin.
   *
   * @param userId The user to send the notification to.
   * @param notificationType The system notification type to notify.
   * @param payloadEntries The payload entries to use for the notification message.
   * @param deliveryMethod The delivery method.
   * @return If the notification was delivered successfully, or false otherwise.
   */
  public boolean sendNotification(
      String userId,
      HpcSystemAdminNotificationType notificationType,
      List<HpcEventPayloadEntry> payloadEntries,
      HpcNotificationDeliveryMethod deliveryMethod);

  /**
   * Create a notification delivery receipt for this event.
   *
   * @param userId The user ID.
   * @param eventId The event ID.
   * @param deliveryMethod The delivery method.
   * @param deliveryStatus The delivery status.
   */
  public void createNotificationDeliveryReceipt(
      String userId,
      int eventId,
      HpcNotificationDeliveryMethod deliveryMethod,
      boolean deliveryStatus);

  /**
   * Get Notification Delivery Receipts for a user.
   *
   * @param page The requested results page.
   * @return A list of notification delivery receipts.
   * @throws HpcException on service failure.
   */
  public List<HpcNotificationDeliveryReceipt> getNotificationDeliveryReceipts(int page)
      throws HpcException;

  /**
   * Get Notification Delivery Receipt.
   *
   * @param eventId The requested receipt event Id.
   * @return A notification delivery receipt object.
   * @throws HpcException on service failure.
   */
  public HpcNotificationDeliveryReceipt getNotificationDeliveryReceipt(int eventId)
      throws HpcException;

  /**
   * Get the notification delivery receipts page size.
   *
   * @return The notification delivery receipts page size.
   */
  public int getNotificationDeliveryReceiptsPageSize();

  /**
   * Get Notification Delivery Receipts count for a user.
   *
   * @return The count of notification delivery receipts for the user.
   * @throws HpcException on service failure.
   */
  public int getNotificationDeliveryReceiptsCount() throws HpcException;
  
  /**
   * Notify HPC-DME system-administrator of an exception
   *
   * @param exception The exception to be notified
   */
  public void sendNotification(HpcException exception);


  /**
   * Notify DME system-administrator, and optionally notify the DME storage administrators of an exception
   *
   * @param exception The exception to be notified
   * @param notifyStorageAdmins Notify the storage administrators also if true
   */
  public void sendNotification(HpcException exception, boolean notifyStorageAdmins);
}
