/**
 * HpcNotificationBusService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationDeliveryReceiptListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionListDTO;
import gov.nih.nci.hpc.dto.notification.HpcNotificationSubscriptionsRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Notification Business Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */
public interface HpcNotificationBusService {
  /**
   * Subscribe to notifications.
   *
   * @param notificationSubscriptions The notification subscriptions request.
   * @throws HpcException on service failure.
   */
  public void subscribeNotifications(
      HpcNotificationSubscriptionsRequestDTO notificationSubscriptions) throws HpcException;

  /**
   * Get notification subscriptions of a user.
   *
   * @return A list of notification subscriptions.
   * @throws HpcException on service failure.
   */
  public HpcNotificationSubscriptionListDTO getNotificationSubscriptions() throws HpcException;

  /**
   * Get Notification Delivery Receipts.
   *
   * @param page The requested results page.
   * @param totalCount If set to true, return the total count of collections matching the query
   *     regardless of the limit on returned entities.
   * @return A list of delivery receipts DTO.
   * @throws HpcException on service failure.
   */
  public HpcNotificationDeliveryReceiptListDTO getNotificationDeliveryReceipts(
      int page, boolean totalCount) throws HpcException;

  /**
   * Get Notification Delivery Receipt.
   *
   * @param eventId The requested receipt Id
   * @return delivery receipts DTO.
   * @throws HpcException on service failure.
   */
  public HpcNotificationDeliveryReceiptDTO getNotificationDeliveryReceipt(int eventId)
      throws HpcException;
}
