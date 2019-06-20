/**
 * HpcSystemBusServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.bus.HpcSystemBusService;
import gov.nih.nci.hpc.bus.aspect.HpcExecuteAsSystemAccount;
import gov.nih.nci.hpc.domain.datamanagement.HpcBulkDataObjectRegistrationTaskStatus;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollectionListingEntry;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectRegistrationTaskItem;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectUploadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferDownloadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadReport;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusUploadSource;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3UploadSource;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationTask;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataObjectRegistrationRequest;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.domain.model.HpcUser;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationDeliveryMethod;
import gov.nih.nci.hpc.domain.notification.HpcNotificationSubscription;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcReportService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC System Business Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcSystemBusServiceImpl implements HpcSystemBusService {
  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // Security Application Service Instance.
  @Autowired
  private HpcSecurityService securityService = null;

  // Data Transfer Application Service Instance.
  @Autowired
  private HpcDataTransferService dataTransferService = null;

  // Data Management Application Service Instance.
  @Autowired
  private HpcDataManagementService dataManagementService = null;

  // Data Management Bus Service Instance.
  @Autowired
  private HpcDataManagementBusService dataManagementBusService = null;

  // Notification Application Service Instance.
  @Autowired
  private HpcNotificationService notificationService = null;

  // Event Application Service Instance.
  @Autowired
  private HpcEventService eventService = null;

  // Metadata Application Service Instance
  @Autowired
  private HpcMetadataService metadataService = null;

  // Reports Application Service Instance
  @Autowired
  private HpcReportService reportsService = null;

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcSystemBusServiceImpl() {}

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // HpcSystemBusService Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  @HpcExecuteAsSystemAccount
  public void processDataTranferUploadReceived() throws HpcException {
    // Iterate through the data objects that their data transfer is in-progress.
    List<HpcDataObject> dataObjectsReceived = dataManagementService.getDataObjectsUploadReceived();
    for (HpcDataObject dataObject : dataObjectsReceived) {
      String path = dataObject.getAbsolutePath();
      logger.info("Processing data object upload received: {}", path);
      try {
        // Get the system metadata.
        HpcSystemGeneratedMetadata systemGeneratedMetadata =
            metadataService.getDataObjectSystemGeneratedMetadata(path);

        // Transfer the data file.
        HpcDataObjectUploadResponse uploadResponse = dataTransferService.uploadDataObject(
            toGlobusUploadSource(systemGeneratedMetadata.getSourceLocation()), null, null, false,
            null, path, systemGeneratedMetadata.getRegistrarId(),
            systemGeneratedMetadata.getCallerObjectId(),
            systemGeneratedMetadata.getConfigurationId());

        // Update system metadata of the data object.
        metadataService.updateDataObjectSystemGeneratedMetadata(path,
            uploadResponse.getArchiveLocation(), uploadResponse.getDataTransferRequestId(), null,
            uploadResponse.getDataTransferStatus(), uploadResponse.getDataTransferType(), null,
            uploadResponse.getDataTransferCompleted(), null);

      } catch (HpcException e) {
        logger.error("Failed to process queued data transfer upload :" + path, e);

        // Delete the data object.
        deleteDataObject(path);
      }
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void processDataTranferUploadInProgress() throws HpcException {
    // Iterate through the data objects that their data transfer is in-progress.
    List<HpcDataObject> dataObjectsInProgress =
        dataManagementService.getDataObjectsUploadInProgress();
    for (HpcDataObject dataObject : dataObjectsInProgress) {
      String path = dataObject.getAbsolutePath();
      logger.info("Processing data object upload in-progress: {}", path);
      try {
        // Get the system metadata.
        HpcSystemGeneratedMetadata systemGeneratedMetadata =
            metadataService.getDataObjectSystemGeneratedMetadata(path);

        // Get the data transfer upload request status.
        HpcDataTransferUploadReport dataTransferUploadReport = dataTransferService
            .getDataTransferUploadStatus(systemGeneratedMetadata.getDataTransferType(),
                systemGeneratedMetadata.getDataTransferRequestId(),
                systemGeneratedMetadata.getConfigurationId());

        HpcDataTransferUploadStatus dataTransferStatus = dataTransferUploadReport.getStatus();
        Calendar dataTransferCompleted = null;
        switch (dataTransferStatus) {
          case ARCHIVED:
            // Data object is archived. 

            // Generate archive (File System) system generated metadata.
            String checksum = dataTransferService.addSystemGeneratedMetadataToDataObject(
                systemGeneratedMetadata.getArchiveLocation(),
                systemGeneratedMetadata.getConfigurationId(), systemGeneratedMetadata.getObjectId(),
                systemGeneratedMetadata.getRegistrarId());

            // Update data management w/ data transfer status, checksum and completion time.
            dataTransferCompleted = Calendar.getInstance();
            metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, checksum,
                dataTransferStatus, null, null, dataTransferCompleted, null);
            break;
            
          case FAILED:
            // Data transfer failed.
            throw new HpcException("Data transfer failed: " + dataTransferUploadReport.getMessage(),
                HpcErrorType.DATA_TRANSFER_ERROR);

          default:
            // Transfer is still in progress.
            continue;
        }

        // Data transfer upload completed (successfully or failed). Add an event if
        // needed.
        if (systemGeneratedMetadata.getRegistrationCompletionEvent()) {
          addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path,
              dataTransferStatus, systemGeneratedMetadata.getSourceLocation(),
              dataTransferCompleted, systemGeneratedMetadata.getDataTransferType(),
              systemGeneratedMetadata.getConfigurationId(), HpcDataTransferType.GLOBUS);
        }

      } catch (HpcException e) {
        logger.error("Failed to process data transfer upload in progress:" + path, e);

        // Delete the data object.
        deleteDataObject(path);
      }
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void processDataTranferUploadInProgressWithGeneratedURL() throws HpcException {
    // Iterate through the data objects that their data transfer is in-progress.
    List<HpcDataObject> dataObjectsInProgress =
        dataManagementService.getDataTranferUploadInProgressWithGeneratedURL();
    for (HpcDataObject dataObject : dataObjectsInProgress) {
      String path = dataObject.getAbsolutePath();
      logger.info("Processing data object uploaded via URL: {}", path);

      // Get the system metadata.
      HpcSystemGeneratedMetadata systemGeneratedMetadata =
          metadataService.getDataObjectSystemGeneratedMetadata(path);

      try {
        if (!updateS3UploadStatus(path, systemGeneratedMetadata)
            && generatedURLExpired(systemGeneratedMetadata.getDataTransferStarted(),
                systemGeneratedMetadata.getConfigurationId())) {
          // The data object was not found in archive. i.e. user did not complete the
          // upload and the upload URL has expired.

          // Delete the data object.
          dataManagementService.delete(path, true);

          // Add event.
          addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path,
              HpcDataTransferUploadStatus.URL_GENERATED, null, null,
              systemGeneratedMetadata.getDataTransferType(),
              systemGeneratedMetadata.getConfigurationId(), HpcDataTransferType.S_3);
        }

      } catch (HpcException e) {
        logger.error("Failed to process data transfer upload in progress with URL:" + path, e);

        // Delete the data object.
        deleteDataObject(path);
      }
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void processDataTranferUploadStreamingInProgress(boolean streamingStopped)
      throws HpcException {
    // Iterate through the data objects that their data transfer is in-progress.
    List<HpcDataObject> dataObjectsInProgress =
        dataManagementService.getDataTranferUploadStreamingInProgress();
    for (HpcDataObject dataObject : dataObjectsInProgress) {
      String path = dataObject.getAbsolutePath();
      logger.info("Processing data object uploaded via Streaming: {}", path);
      try {
        if (!streamingStopped) {
          // Get the system metadata.
          HpcSystemGeneratedMetadata systemGeneratedMetadata =
              metadataService.getDataObjectSystemGeneratedMetadata(path);

          // Check if the S3 upload completed, and update upload status accordingly.
          updateS3UploadStatus(path, systemGeneratedMetadata);
        } else {
          // Streaming stopped (server shutdown). We just update the status accordingly.
          logger.info("Upload streaming stopped for: {}", path);
          metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null,
              HpcDataTransferUploadStatus.STREAMING_STOPPED, null, null, null, null);
        }

      } catch (HpcException e) {
        logger.error("Failed to process data transfer upload streaming in progress:" + path, e);

        // Delete the data object.
        deleteDataObject(path);
      }
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void processDataTranferUploadStreamingInProgress() throws HpcException {
    processDataTranferUploadStreamingInProgress(false);
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void processDataTranferUploadStreamingStopped() throws HpcException {
    // Iterate through the data objects that their data transfer (S3 streaming) has stopped.
    List<HpcDataObject> dataObjectsStreamingStopped =
        dataManagementService.getDataTranferUploadStreamingStopped();
    for (HpcDataObject dataObject : dataObjectsStreamingStopped) {
      String path = dataObject.getAbsolutePath();
      logger.info("Processing restart upload streaming for data object: {}", path);
      try {
        // Get the system metadata.
        HpcSystemGeneratedMetadata systemGeneratedMetadata =
            metadataService.getDataObjectSystemGeneratedMetadata(path);

        // Transfer the data file.
        HpcDataObjectUploadResponse uploadResponse = dataTransferService.uploadDataObject(null,
            toS3UploadSource(systemGeneratedMetadata.getSourceLocation(),
                systemGeneratedMetadata.getSourceURL(), systemGeneratedMetadata.getSourceSize()),
            null, false, null, path, systemGeneratedMetadata.getRegistrarId(),
            systemGeneratedMetadata.getCallerObjectId(),
            systemGeneratedMetadata.getConfigurationId());

        // Update the transfer status and request id.
        metadataService.updateDataObjectSystemGeneratedMetadata(path, null,
            uploadResponse.getDataTransferRequestId(), null, uploadResponse.getDataTransferStatus(),
            null, null, null, null);

      } catch (HpcException e) {
        logger.error("Failed to process restart upload streaming for data object:" + path, e);

        // Delete the data object.
        deleteDataObject(path);
      }
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void completeDataObjectDownloadTasks() throws HpcException {
    // Iterate through all the data object download tasks that are in-progress.
    for (HpcDataObjectDownloadTask downloadTask : dataTransferService
        .getDataObjectDownloadTasks()) {
      try {
        switch (downloadTask.getDataTransferStatus()) {
          case RECEIVED:
            logger.info("Continuing download task: {}", downloadTask.getId());
            dataTransferService.continueDataObjectDownloadTask(downloadTask);
            break;

          case IN_PROGRESS:
            logger.info("Completing download task: {}", downloadTask.getId());
            completeDataObjectDownloadTaskInProgress(downloadTask);
            break;

          default:
            throw new HpcException("Unexpected data transfer download status ["
                + downloadTask.getDataTransferStatus() + "] for task: " + downloadTask.getId(),
                HpcErrorType.UNEXPECTED_ERROR);
        }

      } catch (HpcException e) {
        logger.error("Failed to complete download task: " + downloadTask.getId(), e);
      }
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void restartDataObjectDownloadTasks() throws HpcException {
    // Iterate through all the data object download tasks that are in-progress w/ S3 transfer.
    for (HpcDataObjectDownloadTask downloadTask : dataTransferService
        .getDataObjectDownloadTasks()) {
      try {
        if (downloadTask.getDataTransferType().equals(HpcDataTransferType.S_3) && downloadTask
            .getDataTransferStatus().equals(HpcDataTransferDownloadStatus.IN_PROGRESS)) {
          logger.info("Resetting download task: {}", downloadTask.getId());
          dataTransferService.resetDataObjectDownloadTask(downloadTask);
        }

      } catch (HpcException e) {
        logger.error("Failed to restart download task: " + downloadTask.getId(), e);
      }
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void processCollectionDownloadTasks() throws HpcException {
    // Iterate through all the collection download requests that were submitted (not
    // processed yet).
    for (HpcCollectionDownloadTask downloadTask : dataTransferService
        .getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus.RECEIVED)) {
      try {
        List<HpcCollectionDownloadTaskItem> downloadItems = null;

        if (downloadTask.getType().equals(HpcDownloadTaskType.COLLECTION)) {
          // Get the collection to be downloaded.
          HpcCollection collection =
              dataManagementService.getCollection(downloadTask.getPath(), true);
          if (collection == null) {
            throw new HpcException("Collection not found", HpcErrorType.INVALID_REQUEST_INPUT);
          }

          // Download all files under this collection.
          downloadItems =
              downloadCollection(collection, downloadTask.getGlobusDownloadDestination(),
                  downloadTask.getS3DownloadDestination(), downloadTask.getUserId());

        } else if (downloadTask.getType().equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
          downloadItems = downloadDataObjects(downloadTask.getDataObjectPaths(),
              downloadTask.getGlobusDownloadDestination(), downloadTask.getS3DownloadDestination(),
              downloadTask.getUserId());
        }

        // Verify data objects found under this collection.
        if (downloadItems == null || downloadItems.isEmpty()) {
          // No data objects found under this collection.
          throw new HpcException("No data objects found under collection",
              HpcErrorType.INVALID_REQUEST_INPUT);
        }

        // 'Activate' the collection download request.
        downloadTask.setStatus(HpcCollectionDownloadTaskStatus.ACTIVE);
        downloadTask.getItems().addAll(downloadItems);

        // Persist the collection download task.
        dataTransferService.updateCollectionDownloadTask(downloadTask);

      } catch (HpcException e) {
        logger.error("Failed to process a collection download: " + downloadTask.getId(), e);
        completeCollectionDownloadTask(downloadTask, false, e.getMessage());
      }
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void completeCollectionDownloadTasks() throws HpcException {
    // Iterate through all the active collection download requests.
    for (HpcCollectionDownloadTask downloadTask : dataTransferService
        .getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus.ACTIVE)) {
      boolean downloadCompleted = true;

      // Update status of individual download items in this collection download task.
      for (HpcCollectionDownloadTaskItem downloadItem : downloadTask.getItems()) {
        try {
          if (downloadItem.getResult() == null) {
            // This download item in progress - check its status.
            HpcDownloadTaskStatus downloadItemStatus =
                downloadItem.getDataObjectDownloadTaskId() != null
                    ? dataTransferService.getDownloadTaskStatus(
                        downloadItem.getDataObjectDownloadTaskId(), HpcDownloadTaskType.DATA_OBJECT)
                    : null;

            if (downloadItemStatus == null) {
              throw new HpcException("Data object download task status is unknown",
                  HpcErrorType.UNEXPECTED_ERROR);
            }

            if (!downloadItemStatus.getInProgress()) {
              // This download item is now complete. Update the result.
              downloadItem.setResult(downloadItemStatus.getResult().getResult());
              downloadItem.setMessage(downloadItemStatus.getResult().getMessage());
              downloadItem.setPercentComplete(downloadItemStatus.getResult().getResult() ? 100 : 0);
              downloadItem.setEffectiveTransferSpeed(
                  downloadItemStatus.getResult().getEffectiveTransferSpeed() > 0
                      ? downloadItemStatus.getResult().getEffectiveTransferSpeed()
                      : null);
            } else {
              // Update the progress on this download item.
              downloadItem.setSize(downloadItemStatus.getDataObjectDownloadTask().getSize());
              downloadItem.setPercentComplete(
                  downloadItemStatus.getDataObjectDownloadTask().getPercentComplete());

              // This item still in progress, so overall download not completed just yet.
              downloadCompleted = false;
            }
          }

        } catch (HpcException e) {
          logger.error("Failed to check collection download item status", e);
          downloadItem.setResult(false);
          downloadItem.setMessage(e.getMessage());
        }
      }

      // Update the collection download task.
      if (downloadCompleted) {
        // The collection download task finished. Determine if the collection download was
        // successful.
        // It is successful if and only if all items (data objects under the collection) were
        // completed successfully.
        int completedItemsCount = 0;
        for (HpcCollectionDownloadTaskItem downloadItem : downloadTask.getItems()) {
          if (downloadItem.getResult()) {
            completedItemsCount++;
          }
        }

        int itemsCount = downloadTask.getItems().size();
        boolean result = completedItemsCount == itemsCount;
        completeCollectionDownloadTask(downloadTask, result, result ? null
            : completedItemsCount + " items downloaded successfully out of " + itemsCount);

      } else {
        dataTransferService.updateCollectionDownloadTask(downloadTask);
      }
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void processBulkDataObjectRegistrationTasks() throws HpcException {
    // Iterate through all the bulk data object registration requests that were submitted (not
    // processed yet).
    dataManagementService
        .getBulkDataObjectRegistrationTasks(HpcBulkDataObjectRegistrationTaskStatus.RECEIVED)
        .forEach(bulkRegistrationTask -> {
          try {
            // 'Activate' the registration task.
            bulkRegistrationTask.setStatus(HpcBulkDataObjectRegistrationTaskStatus.ACTIVE);

            // Register all items in this bulk registration task.
            bulkRegistrationTask.getItems()
                .forEach(item -> registerDataObject(item, bulkRegistrationTask.getUserId()));

            // Persist the bulk data object registration task.
            dataManagementService.updateBulkDataObjectRegistrationTask(bulkRegistrationTask);

          } catch (HpcException e) {
            logger.error("Failed to process a bulk data object registration: "
                + bulkRegistrationTask.getId(), e);
            completeBulkDataObjectRegistrationTask(bulkRegistrationTask, false, e.getMessage());
          }
        });
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void completeBulkDataObjectRegistrationTasks() throws HpcException {
    // Iterate through all the bulk data object registration requests that are
    // active.
    dataManagementService
        .getBulkDataObjectRegistrationTasks(HpcBulkDataObjectRegistrationTaskStatus.ACTIVE)
        .forEach(bulkRegistrationTask -> {
          // Update status of items in this bulk registration task.
          bulkRegistrationTask.getItems().forEach(this::updateRegistrationItemStatus);

          // Check if registration task completed.
          int completedItemsCount = 0;
          for (HpcBulkDataObjectRegistrationItem registrationItem : bulkRegistrationTask
              .getItems()) {
            if (registrationItem.getTask().getResult() == null) {
              // Task still in progress. Update progress.
              try {
                dataManagementService.updateBulkDataObjectRegistrationTask(bulkRegistrationTask);

              } catch (HpcException e) {
                logger.error(
                    "Failed to update data object list task: " + bulkRegistrationTask.getId());
              }
              return;
            }

            if (registrationItem.getTask().getResult()) {
              completedItemsCount++;
            }
          }

          // Bulk registration task completed.
          int itemsCount = bulkRegistrationTask.getItems().size();
          boolean result = completedItemsCount == itemsCount;
          completeBulkDataObjectRegistrationTask(bulkRegistrationTask, result, result ? null
              : completedItemsCount + " items registered successfully out of " + itemsCount);
        });
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void processEvents() throws HpcException {
    // Get and process the pending notification events.
    for (HpcEvent event : eventService.getEvents()) {
      // Notify all users associated with this event.
      try {
        for (String userId : event.getUserIds()) {
          try {
            // Get the subscription.
            HpcEventType eventType = event.getType();
            HpcNotificationSubscription subscription =
                notificationService.getNotificationSubscription(userId, eventType);
            if (subscription != null) {
              // Iterate through all the delivery methods the user is subscribed to.
              for (HpcNotificationDeliveryMethod deliveryMethod : subscription
                  .getNotificationDeliveryMethods()) {
                // Send notification via this delivery method.
                boolean notificationSent = notificationService.sendNotification(userId, eventType,
                    event.getPayloadEntries(), deliveryMethod);

                // Create a delivery receipt for this delivery method.
                notificationService.createNotificationDeliveryReceipt(userId, event.getId(),
                    deliveryMethod, notificationSent);
              }
            }

          } catch (Exception e) {
            logger.error("Failed to deliver notifications to: " + userId);
          }
        }

      } finally {
        eventService.archiveEvent(event);
      }
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void generateSummaryReportEvent() throws HpcException {
    List<String> summaryReportUsers =
        notificationService.getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_REPORT);
    if (summaryReportUsers != null && !summaryReportUsers.isEmpty()) {
      HpcReportCriteria criteria = new HpcReportCriteria();
      criteria.setType(HpcReportType.USAGE_SUMMARY);
      eventService.generateReportsEvents(summaryReportUsers, criteria);
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void generateWeeklySummaryReportEvent() throws HpcException {
    List<String> summaryReportByDateUsers = notificationService
        .getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_WEEKLY_REPORT);
    if (summaryReportByDateUsers != null && !summaryReportByDateUsers.isEmpty()) {
      HpcReportCriteria criteria = new HpcReportCriteria();
      criteria.setType(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE);
      Calendar today = Calendar.getInstance();
      Calendar oneWeekbefore = Calendar.getInstance();
      oneWeekbefore.add(Calendar.DATE, -7);
      criteria.setFromDate(oneWeekbefore);
      criteria.setToDate(today);
      eventService.generateReportsEvents(summaryReportByDateUsers, criteria);
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void generateDocReportEvent() throws HpcException {
    List<String> summaryReportUsers = notificationService
        .getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_DOC_REPORT);
    if (summaryReportUsers != null && !summaryReportUsers.isEmpty()) {
      HpcReportCriteria criteria = new HpcReportCriteria();
      criteria.setType(HpcReportType.USAGE_SUMMARY_BY_DOC);
      eventService.generateReportsEvents(summaryReportUsers, criteria);
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void generateWeeklyDocReportEvent() throws HpcException {
    List<String> summaryReportByDateUsers = notificationService
        .getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_DOC_BY_WEEKLY_REPORT);
    if (summaryReportByDateUsers != null && !summaryReportByDateUsers.isEmpty()) {
      HpcReportCriteria criteria = new HpcReportCriteria();
      criteria.setType(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE);
      Calendar today = Calendar.getInstance();
      Calendar oneWeekbefore = Calendar.getInstance();
      oneWeekbefore.add(Calendar.DATE, -7);
      criteria.setFromDate(oneWeekbefore);
      criteria.setToDate(today);
      eventService.generateReportsEvents(summaryReportByDateUsers, criteria);
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void generateUserReportEvent() throws HpcException {
    List<String> summaryReportUsers = notificationService
        .getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_USER_REPORT);
    if (summaryReportUsers != null && !summaryReportUsers.isEmpty()) {
      HpcReportCriteria criteria = new HpcReportCriteria();
      criteria.setType(HpcReportType.USAGE_SUMMARY_BY_USER);
      eventService.generateReportsEvents(summaryReportUsers, criteria);
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void generateWeeklyUserReportEvent() throws HpcException {
    List<String> summaryReportByDateUsers = notificationService
        .getNotificationSubscribedUsers(HpcEventType.USAGE_SUMMARY_BY_USER_BY_WEEKLY_REPORT);
    if (summaryReportByDateUsers != null && !summaryReportByDateUsers.isEmpty()) {
      HpcReportCriteria criteria = new HpcReportCriteria();
      criteria.setType(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE);
      Calendar today = Calendar.getInstance();
      Calendar oneWeekbefore = Calendar.getInstance();
      oneWeekbefore.add(Calendar.DATE, -7);
      criteria.setFromDate(oneWeekbefore);
      criteria.setToDate(today);
      eventService.generateReportsEvents(summaryReportByDateUsers, criteria);
    }
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void refreshMetadataViews() throws HpcException {
    metadataService.refreshViews();
  }

  @Override
  @HpcExecuteAsSystemAccount
  public void refreshReportViews() throws HpcException {
    reportsService.refreshViews();
  }

  @Override
  public void closeConnection() {
    dataManagementService.closeConnection();
  }

  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

  /**
   * add data transfer upload event.
   *
   * @param userId The user ID.
   * @param path The data object path.
   * @param dataTransferStatus The data transfer upload status.
   * @param sourceLocation (Optional) The data transfer source location.
   * @param dataTransferCompleted (Optional) The time the data upload completed.
   * @param dataTransferType The type of data transfer last used to upload (Globus, S3, etc).
   * @param configurationId The data management configuration ID.
   * @param sourceDataTransferType The type of source the file was uploaded from (S3 or Globus)
   */
  private void addDataTransferUploadEvent(String userId, String path,
      HpcDataTransferUploadStatus dataTransferStatus, HpcFileLocation sourceLocation,
      Calendar dataTransferCompleted, HpcDataTransferType dataTransferType, String configurationId,
      HpcDataTransferType sourceDataTransferType) {
    setFileContainerName(sourceDataTransferType, configurationId, sourceLocation);
    try {
      switch (dataTransferStatus) {
        case ARCHIVED:
          eventService.addDataTransferUploadArchivedEvent(userId, path, sourceLocation,
              dataTransferCompleted);
          break;

        case FAILED:
          eventService.addDataTransferUploadFailedEvent(userId, path, sourceLocation,
              dataTransferCompleted, dataTransferType.value() + " failure");
          break;

        case URL_GENERATED:
          // This status is sent when the upload URL expired. (i.e. user did not upload file)
          eventService.addDataTransferUploadURLExpiredEvent(userId, path);
          break;

        default:
          logger.error("Unexpected data transfer status: {}", dataTransferStatus);
      }

    } catch (HpcException e) {
      logger.error("Failed to add a data transfer upload event", e);
    }
  }

  /**
   * add bulk data object registration event.
   *
   * @param registrationTask The bulk registration task.
   * @param result The bulk registration result.
   * @param message A failure message.
   * @param completed The completion time.
   */
  private void addBulkDataObjectRegistrationEvent(
      HpcBulkDataObjectRegistrationTask registrationTask, boolean result, String message,
      Calendar completed) {

    // Format the task ID. If the caller provided a UI URL, then use it to construct a URL link to
    // view this task on UI.
    String taskId = registrationTask.getId();
    if (!StringUtils.isEmpty(registrationTask.getUiURL())) {
      taskId = "<a href=\"" + registrationTask.getUiURL().replaceAll("\\{task_id\\}", taskId)
          + "\">" + taskId + "</a>";
    }

    try {
      if (result) {
        // Update the source's file container name on all registration items (so that it will be
        // displayed in the notification).
        registrationTask.getItems().forEach(item -> {
          String configurationId =
              dataManagementService.findDataManagementConfigurationId(item.getTask().getPath());
          setFileContainerName(HpcDataTransferType.GLOBUS, configurationId,
              item.getRequest().getGlobusUploadSource() != null
                  ? item.getRequest().getGlobusUploadSource().getSourceLocation()
                  : item.getRequest().getS3UploadSource().getSourceLocation());
        });

        eventService.addBulkDataObjectRegistrationCompletedEvent(registrationTask.getUserId(),
            taskId, registrationTask.getItems(), completed);
      } else {
        eventService.addBulkDataObjectRegistrationFailedEvent(registrationTask.getUserId(), taskId,
            completed, message);
      }

    } catch (HpcException e) {
      logger.error("Failed to add a data transfer upload event", e);
    }
  }

  /**
   * add data transfer download event.
   *
   * @param userId The user ID.
   * @param path The collection or data objection path.
   * @param downloadTaskType The download task type.
   * @param downloadTaskId The download task ID.
   * @param dataTransferType The data transfer type,
   * @param configurationId The data management configuration ID..
   * @param result The download result.
   * @param message A failure message.
   * @param destinationLocation The download destination location.
   * @param dataTransferCompleted The download completion time.
   */
  private void addDataTransferDownloadEvent(String userId, String path,
      HpcDownloadTaskType downloadTaskType, String downloadTaskId,
      HpcDataTransferType dataTransferType, String configurationId, boolean result, String message,
      HpcFileLocation destinationLocation, Calendar dataTransferCompleted) {
    setFileContainerName(dataTransferType, configurationId, destinationLocation);
    try {
      if (result) {
        eventService.addDataTransferDownloadCompletedEvent(userId, path, downloadTaskType,
            downloadTaskId, destinationLocation, dataTransferCompleted);
      } else {
        eventService.addDataTransferDownloadFailedEvent(userId, path, downloadTaskType,
            downloadTaskId, destinationLocation, dataTransferCompleted, message);
      }

    } catch (HpcException e) {
      logger.error("Failed to add a data transfer download event", e);
    }
  }

  /**
   * Download a collection. Traverse the collection tree and submit download request to all files in
   * the tree.
   *
   * @param collection The collection to download.
   * @param globusDownloadDestination The user requested Glopbus download destination.
   * @param s3DownloadDestination The user requested S3 download destination.
   * @param userId The user ID who requested the collection download.
   * @return The download task items (each item represent a data-object download under the
   *         collection).
   * @throws HpcException on service failure.
   */
  private List<HpcCollectionDownloadTaskItem> downloadCollection(HpcCollection collection,
      HpcGlobusDownloadDestination globusDownloadDestination,
      HpcS3DownloadDestination s3DownloadDestination, String userId) throws HpcException {
    List<HpcCollectionDownloadTaskItem> downloadItems = new ArrayList<>();

    // Iterate through the data objects in the collection and download them.
    for (HpcCollectionListingEntry dataObjectEntry : collection.getDataObjects()) {
      downloadItems.add(downloadDataObject(dataObjectEntry.getPath(), globusDownloadDestination,
          s3DownloadDestination, userId));
    }

    // Iterate through the sub-collections and download them.
    for (HpcCollectionListingEntry subCollectionEntry : collection.getSubCollections()) {
      String subCollectionPath = subCollectionEntry.getPath();
      HpcCollection subCollection = dataManagementService.getCollection(subCollectionPath, true);
      if (subCollection != null) {
        // Download this sub-collection.
        downloadItems.addAll(downloadCollection(subCollection,
            calculateGlobusDownloadDestination(globusDownloadDestination, subCollectionPath),
            calculateS3DownloadDestination(s3DownloadDestination, subCollectionPath), userId));
      }
    }

    return downloadItems;
  }

  /**
   * Download a list of data objects.
   *
   * @param dataObjectPaths The list of data object path to download.
   * @param globusDownloadDestination The user requested Glopbus download destination.
   * @param s3DownloadDestination The user requested S3 download destination.
   * @param userId The user ID who requested the collection download.
   * @return The download task items (each item represent a data-object download from the requested
   *         list).
   */
  private List<HpcCollectionDownloadTaskItem> downloadDataObjects(List<String> dataObjectPaths,
      HpcGlobusDownloadDestination globusDownloadDestination,
      HpcS3DownloadDestination s3DownloadDestination, String userId) {
    List<HpcCollectionDownloadTaskItem> downloadItems = new ArrayList<>();

    // Iterate through the data objects in the collection and download them.
    for (String dataObjectPath : dataObjectPaths) {
      downloadItems.add(downloadDataObject(dataObjectPath, globusDownloadDestination,
          s3DownloadDestination, userId));
    }

    return downloadItems;
  }

  /**
   * Download a data object.
   *
   * @param path The data object path.
   * @param globusDownloadDestination The user requested Glopbus download destination.
   * @param s3DownloadDestination The user requested S3 download destination.
   * @param userId The user ID who requested the collection download.
   * @return The download task item.
   */
  private HpcCollectionDownloadTaskItem downloadDataObject(String path,
      HpcGlobusDownloadDestination globusDownloadDestination,
      HpcS3DownloadDestination s3DownloadDestination, String userId) {
    HpcDownloadRequestDTO dataObjectDownloadRequest = new HpcDownloadRequestDTO();
    dataObjectDownloadRequest.setGlobusDownloadDestination(
        calculateGlobusDownloadDestination(globusDownloadDestination, path));
    dataObjectDownloadRequest
        .setS3DownloadDestination(calculateS3DownloadDestination(s3DownloadDestination, path));

    // Instantiate a download item for this data object.
    HpcCollectionDownloadTaskItem downloadItem = new HpcCollectionDownloadTaskItem();
    downloadItem.setPath(path);

    // Download this data object.
    try {
      HpcDataObjectDownloadResponseDTO dataObjectDownloadResponse = dataManagementBusService
          .downloadDataObject(path, dataObjectDownloadRequest, userId, false);

      downloadItem.setDataObjectDownloadTaskId(dataObjectDownloadResponse.getTaskId());
      downloadItem.setDestinationLocation(dataObjectDownloadResponse.getDestinationLocation());

    } catch (HpcException e) {
      // Data object download failed.
      logger.error("Failed to download data object in a collection", e);

      downloadItem.setResult(false);
      downloadItem.setDestinationLocation(
          globusDownloadDestination != null ? globusDownloadDestination.getDestinationLocation()
              : s3DownloadDestination.getDestinationLocation());
      downloadItem.setMessage(e.getMessage());
    }

    return downloadItem;
  }

  /**
   * Calculate a Globus download destination path for a collection entry under a collection.
   *
   * @param collectionDestination The Globus collection destination.
   * @param collectionListingEntryPath The entry path under the collection to calculate the
   *        destination location for.
   * @return A calculated destination location.
   */
  private HpcGlobusDownloadDestination calculateGlobusDownloadDestination(
      HpcGlobusDownloadDestination collectionDestination, String collectionListingEntryPath) {
    if (collectionDestination == null) {
      return null;
    }

    HpcGlobusDownloadDestination calcGlobusDestination = new HpcGlobusDownloadDestination();
    HpcFileLocation calcDestination = new HpcFileLocation();
    calcDestination
        .setFileContainerId(collectionDestination.getDestinationLocation().getFileContainerId());
    calcDestination.setFileId(collectionDestination.getDestinationLocation().getFileId()
        + collectionListingEntryPath.substring(collectionListingEntryPath.lastIndexOf('/')));
    calcGlobusDestination.setDestinationLocation(calcDestination);
    calcGlobusDestination.setDestinationOverwrite(collectionDestination.getDestinationOverwrite());

    return calcGlobusDestination;
  }

  /**
   * Calculate a S3 download destination path for a collection entry under a collection.
   *
   * @param collectionDestination The S3 collection destination.
   * @param collectionListingEntryPath The entry path under the collection to calculate the
   *        destination location for.
   * @return A calculated destination location.
   */
  private HpcS3DownloadDestination calculateS3DownloadDestination(
      HpcS3DownloadDestination collectionDestination, String collectionListingEntryPath) {
    if (collectionDestination == null) {
      return null;
    }

    HpcS3DownloadDestination calcS3Destination = new HpcS3DownloadDestination();
    HpcFileLocation calcDestination = new HpcFileLocation();
    calcDestination
        .setFileContainerId(collectionDestination.getDestinationLocation().getFileContainerId());
    calcDestination.setFileId(collectionDestination.getDestinationLocation().getFileId()
        + collectionListingEntryPath.substring(collectionListingEntryPath.lastIndexOf('/')));
    calcS3Destination.setDestinationLocation(calcDestination);
    calcS3Destination.setAccount(collectionDestination.getAccount());

    return calcS3Destination;
  }

  /**
   * Complete a collection download task. 1. Update task info in DB with results info. 2. Send an
   * event.
   *
   * @param downloadTask The download task to complete.
   * @param result The result of the task (true is successful, false is failed).
   * @param message (Optional) If the task failed, a message describing the failure.
   * @throws HpcException on service failure.
   */
  private void completeCollectionDownloadTask(HpcCollectionDownloadTask downloadTask,
      boolean result, String message) throws HpcException {
    Calendar completed = Calendar.getInstance();
    dataTransferService.completeCollectionDownloadTask(downloadTask, result, message, completed);

    // Set the payload with either the collection path or the list of data object paths.
    String path = "";
    if (downloadTask.getType().equals(HpcDownloadTaskType.COLLECTION)) {
      path = downloadTask.getPath();
    } else if (downloadTask.getType().equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
      path = StringUtils.join(downloadTask.getDataObjectPaths(), ", ");
    }

    // Send download completed/failed event.
    addDataTransferDownloadEvent(downloadTask.getUserId(), path, downloadTask.getType(),
        downloadTask.getId(),
        downloadTask.getS3DownloadDestination() != null ? HpcDataTransferType.S_3
            : HpcDataTransferType.GLOBUS,
        downloadTask.getConfigurationId(), result, message,
        downloadTask.getS3DownloadDestination() != null
            ? downloadTask.getS3DownloadDestination().getDestinationLocation()
            : downloadTask.getGlobusDownloadDestination().getDestinationLocation(),
        completed);
  }

  /**
   * Complete a data object download task that is in-progress (Globus transfer is in-progress). 1.
   * Check the status of Globus transfer. 2. If completed (succeeded or failed), record the result.
   *
   * @param downloadTask The download task to complete.
   * @throws HpcException on service failure.
   */
  private void completeDataObjectDownloadTaskInProgress(HpcDataObjectDownloadTask downloadTask)
      throws HpcException {
    if (downloadTask.getDataTransferType().equals(HpcDataTransferType.S_3)) {
      // Checking transfer status is done for active Globus downloads only.
      return;
    }

    // Get the data transfer download status.
    HpcDataTransferDownloadReport dataTransferDownloadReport =
        dataTransferService.getDataTransferDownloadStatus(downloadTask.getDataTransferType(),
            downloadTask.getDataTransferRequestId(), downloadTask.getConfigurationId());

    // Check the status of the data transfer.
    HpcDataTransferDownloadStatus dataTransferDownloadStatus =
        dataTransferDownloadReport.getStatus();
    if (!dataTransferDownloadStatus.equals(HpcDataTransferDownloadStatus.IN_PROGRESS)) {
      // This download task is no longer in-progress - complete it.
      boolean result = dataTransferDownloadStatus.equals(HpcDataTransferDownloadStatus.COMPLETED);
      String message = result ? null
          : downloadTask.getDataTransferType() + " transfer failed ["
              + dataTransferDownloadReport.getMessage() + "].";
      Calendar completed = Calendar.getInstance();
      dataTransferService.completeDataObjectDownloadTask(downloadTask, result, message, completed,
          dataTransferDownloadReport.getBytesTransferred());

      // Send a download completion event (if requested to).
      if (downloadTask.getCompletionEvent()) {
        addDataTransferDownloadEvent(downloadTask.getUserId(), downloadTask.getPath(),
            HpcDownloadTaskType.DATA_OBJECT, downloadTask.getId(),
            downloadTask.getDataTransferType(), downloadTask.getConfigurationId(), result, message,
            downloadTask.getGlobusDownloadDestination().getDestinationLocation(), completed);
      }
    } else {
      // Download is still in progress. Update the progress (percent complete).
      dataTransferService.updateDataObjectDownloadTask(downloadTask,
          dataTransferDownloadReport.getBytesTransferred());
    }
  }

  /**
   * Set the file container name.
   *
   * @param dataTransferType The data transfer type.
   * @param configurationId The data management configuration ID.
   * @param fileLocation The file location.
   */
  private void setFileContainerName(HpcDataTransferType dataTransferType, String configurationId,
      HpcFileLocation fileLocation) {
    if (fileLocation == null) {
      return;
    }

    try {
      // Get the file container ID name.
      fileLocation.setFileContainerName(dataTransferService.getFileContainerName(dataTransferType,
          configurationId, fileLocation.getFileContainerId()));

    } catch (HpcException e) {
      logger.error("Failed to get file container name: " + fileLocation.getFileContainerId(), e);
    }
  }

  /**
   * Delete a data object (from the data management system).
   *
   * @param path The data object path.
   */
  private void deleteDataObject(String path) {
    // Update the data transfer status. This is needed in case the actual deletion
    // failed.
    HpcSystemGeneratedMetadata systemGeneratedMetadata = null;
    try {
      metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, null,
          HpcDataTransferUploadStatus.FAILED, null, null, null, null);

      systemGeneratedMetadata = metadataService.getDataObjectSystemGeneratedMetadata(path);

    } catch (HpcException e) {
      logger.error("Failed to update system metadata: " + path, HpcErrorType.UNEXPECTED_ERROR, e);
    }

    // Delete the data object.
    try {
      dataManagementService.delete(path, true);

    } catch (HpcException e) {
      logger.error("Failed to delete data object: " + path, HpcErrorType.UNEXPECTED_ERROR, e);
    }

    // Send an an event if needed.
    if (systemGeneratedMetadata != null
        && systemGeneratedMetadata.getRegistrationCompletionEvent()) {
      addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path,
          systemGeneratedMetadata.getDataTransferStatus(),
          systemGeneratedMetadata.getSourceLocation(),
          systemGeneratedMetadata.getDataTransferCompleted(),
          systemGeneratedMetadata.getDataTransferType(),
          systemGeneratedMetadata.getConfigurationId(),
          systemGeneratedMetadata.getDataTransferType());
    }
  }

  /**
   * Register a data object.
   *
   * @param registrationItem The data object registration item (one in a list).
   * @param userId The registrar user-id.
   */
  private void registerDataObject(HpcBulkDataObjectRegistrationItem registrationItem,
      String userId) {
    HpcDataObjectRegistrationRequest registrationRequest = registrationItem.getRequest();
    HpcDataObjectRegistrationTaskItem registrationTask = registrationItem.getTask();

    // Get the user name.
    HpcUser user = null;
    try {
      user = securityService.getUser(userId);
    } catch (HpcException e) {
      logger.error("Failed to get user: " + userId);
    }
    String userName = user != null
        ? user.getNciAccount().getFirstName() + " " + user.getNciAccount().getLastName()
        : "UNKNOWN";

    // Map request to a DTO.
    HpcDataObjectRegistrationRequestDTO registrationDTO = new HpcDataObjectRegistrationRequestDTO();
    registrationDTO.setCallerObjectId(registrationRequest.getCallerObjectId());
    registrationDTO.setCreateParentCollections(registrationRequest.getCreateParentCollections());
    registrationDTO.setGlobusUploadSource(registrationRequest.getGlobusUploadSource());
    registrationDTO.setS3UploadSource(registrationRequest.getS3UploadSource());
    registrationDTO.getMetadataEntries().addAll(registrationRequest.getMetadataEntries());
    registrationDTO.setParentCollectionsBulkMetadataEntries(
        registrationRequest.getParentCollectionsBulkMetadataEntries());

    try {
      // Determine the data management configuration to use based on the path.
      String configurationId =
          dataManagementService.findDataManagementConfigurationId(registrationTask.getPath());
      if (StringUtils.isEmpty(configurationId)) {
        throw new HpcException("Failed to determine data management configuration.",
            HpcErrorType.INVALID_REQUEST_INPUT);
      }

      dataManagementBusService.registerDataObject(registrationTask.getPath(), registrationDTO, null,
          userId, userName, configurationId, false);

    } catch (HpcException e) {
      // Data object registration failed. Update the task accordingly.
      registrationTask.setResult(false);
      registrationTask.setMessage(e.getMessage());
      registrationTask.setCompleted(Calendar.getInstance());
    }
  }

  /**
   * Complete a bulk data object registration task. 1. Update task info in DB with results info. 2.
   * Send an event.
   *
   * @param registrationTask The registration task to complete.
   * @param result The result of the task (true is successful, false is failed).
   * @param message (Optional) If the task failed, a message describing the failure.
   */
  private void completeBulkDataObjectRegistrationTask(
      HpcBulkDataObjectRegistrationTask registrationTask, boolean result, String message) {
    Calendar completed = Calendar.getInstance();

    try {
      dataManagementService.completeBulkDataObjectRegistrationTask(registrationTask, result,
          message, completed);

    } catch (HpcException e) {
      logger.error("Failed to complete data object list registration request", e);
    }

    // Send an event.
    addBulkDataObjectRegistrationEvent(registrationTask, result, message, completed);
  }

  /**
   * Check and update status of a data object registration item
   *
   * @param registrationItem The registration item to check.
   */
  private void updateRegistrationItemStatus(HpcBulkDataObjectRegistrationItem registrationItem) {
    HpcDataObjectRegistrationTaskItem registrationTask = registrationItem.getTask();
    try {
      if (registrationTask.getResult() == null) {
        // This registration item in progress - check its status.

        // If the data object doesn't exist, it means the upload failed and it was
        // removed.
        if (dataManagementService.getDataObject(registrationTask.getPath()) == null) {
          registrationTask.setResult(false);
          registrationTask.setMessage("Data object upload failed");
          registrationTask.setCompleted(Calendar.getInstance());
          registrationTask.setPercentComplete(0);
        }

        // Get the System generated metadata.
        HpcSystemGeneratedMetadata metadata =
            metadataService.getDataObjectSystemGeneratedMetadata(registrationTask.getPath());
        registrationTask.setSize(metadata.getSourceSize());

        // Check the upload status.
        if (metadata.getDataTransferStatus().equals(HpcDataTransferUploadStatus.ARCHIVED)) {
          // Registration completed successfully for this item.
          registrationTask.setResult(true);
          registrationTask.setCompleted(metadata.getDataTransferCompleted());
          registrationTask.setPercentComplete(100);

          // Calculate the effective transfer speed.
          registrationTask.setEffectiveTransferSpeed(Math.toIntExact(metadata.getSourceSize() * 1000
              / (metadata.getDataTransferCompleted().getTimeInMillis()
                  - metadata.getDataTransferStarted().getTimeInMillis())));
        } else {
          // Registration still in progress. Update % complete.
          registrationTask.setPercentComplete(
              dataTransferService.calculateDataObjectUploadPercentComplete(metadata));
        }
      }

    } catch (HpcException e) {
      logger.error("Failed to check data object registration item status", e);
      registrationTask.setResult(false);
      registrationTask.setMessage(e.getMessage());
      registrationTask.setPercentComplete(null);
    }
  }

  /**
   * Check if an upload URL expired.
   *
   * @param urlCreated The data/time the URL was generated
   * @param configurationId The data management configuration ID. This is to get the expiration
   *        config.
   * @return True if the upload URL expired, or false otherwise.
   */
  private boolean generatedURLExpired(Calendar urlCreated, String configurationId) {
    if (urlCreated == null || StringUtils.isEmpty(configurationId)) {
      return true;
    }

    // Get the URL expiration period (in hours) from the configuration
    HpcDataManagementConfiguration dataManagementConfiguration =
        dataManagementService.getDataManagementConfiguration(configurationId);
    if (dataManagementConfiguration == null) {
      return true;
    }

    // Calculate the expiration time.
    Date expiration = new Date();
    expiration.setTime(urlCreated.getTimeInMillis() + 1000 * 60 * 60
        * dataManagementConfiguration.getArchiveS3Configuration().getUploadRequestURLExpiration());

    // Return true if the current time is passed the expiration time.
    return expiration.before(new Date());
  }

  /**
   * Package a source location into a Globus upload source object.
   *
   * @param sourceLocation The source location to package.
   * @return The packaged Globus upload source.
   */
  private HpcGlobusUploadSource toGlobusUploadSource(HpcFileLocation sourceLocation) {
    HpcGlobusUploadSource globusUploadSource = new HpcGlobusUploadSource();
    globusUploadSource.setSourceLocation(sourceLocation);
    return globusUploadSource;
  }

  /**
   * Package a source location into a S3 upload source object.
   *
   * @param sourceLocation The source location to package.
   * @param sourceURL The source URL to stream from.
   * @param sourceSize The source file size.
   * @return The packaged S3 upload source.
   */
  private HpcS3UploadSource toS3UploadSource(HpcFileLocation sourceLocation, String sourceURL,
      Long sourceSize) {
    HpcS3UploadSource s3UploadSource = new HpcS3UploadSource();
    s3UploadSource.setSourceLocation(sourceLocation);
    s3UploadSource.setSourceURL(sourceURL);
    s3UploadSource.setSourceSize(sourceSize);
    return s3UploadSource;
  }

  /**
   * Check if an upload from S3 (either via URL upload or streaming) has completed.
   *
   * @param path The path of the data object to check if an upload from S3 completed.
   * @param systemGeneratedMetadata The system generated metadata for the data object.
   * @return true if the uploaded completed, or false otherwise.
   * @throws HpcException If failed to check/update upload status.
   */
  private boolean updateS3UploadStatus(String path,
      HpcSystemGeneratedMetadata systemGeneratedMetadata) throws HpcException {
    // Lookup the archive for this data object.
    HpcPathAttributes archivePathAttributes = dataTransferService.getPathAttributes(
        systemGeneratedMetadata.getDataTransferType(), systemGeneratedMetadata.getArchiveLocation(),
        true, systemGeneratedMetadata.getConfigurationId());
    if (archivePathAttributes.getExists() && archivePathAttributes.getIsFile()) {
      // The data object is found in archive. i.e. upload was completed successfully.

      // Update the archive (Cleversafe) data object's system-metadata.
      String checksum = dataTransferService.addSystemGeneratedMetadataToDataObject(
          systemGeneratedMetadata.getArchiveLocation(),
          systemGeneratedMetadata.getConfigurationId(), systemGeneratedMetadata.getObjectId(),
          systemGeneratedMetadata.getRegistrarId());

      // Update the data management (iRODS) data object's system-metadata.
      Calendar dataTransferCompleted = Calendar.getInstance();
      metadataService.updateDataObjectSystemGeneratedMetadata(path, null, null, checksum,
          HpcDataTransferUploadStatus.ARCHIVED, null, null, dataTransferCompleted,
          archivePathAttributes.getSize());

      // Add an event if needed.
      if (systemGeneratedMetadata.getRegistrationCompletionEvent()) {
        addDataTransferUploadEvent(systemGeneratedMetadata.getRegistrarId(), path,
            HpcDataTransferUploadStatus.ARCHIVED, systemGeneratedMetadata.getSourceLocation(),
            dataTransferCompleted, systemGeneratedMetadata.getDataTransferType(),
            systemGeneratedMetadata.getConfigurationId(), HpcDataTransferType.S_3);
      }

      return true;
    }

    return false;
  }
}
