/**
 * HpcEventServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import gov.nih.nci.hpc.dao.HpcEventDAO;
import gov.nih.nci.hpc.dao.HpcNotificationDAO;
import gov.nih.nci.hpc.dao.HpcReportsDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationItem;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.domain.report.HpcReportEntry;
import gov.nih.nci.hpc.domain.report.HpcReportEntryAttribute;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcEventService;

/**
 * HPC Event Application Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcEventServiceImpl implements HpcEventService {
  // ---------------------------------------------------------------------//
  // Constants
  // ---------------------------------------------------------------------//

  // Event payload entries attributes.
  public static final String DOWNLOAD_TASK_ID_PAYLOAD_ATTRIBUTE = "DOWNLOAD_TASK_ID";
  public static final String DOWNLOAD_TASK_TYPE_PAYLOAD_ATTRIBUTE = "DOWNLOAD_TASK_TYPE";
  public static final String DATA_OBJECT_PATH_PAYLOAD_ATTRIBUTE = "DATA_OBJECT_PATH";
  public static final String REGISTRATION_TASK_ID_PAYLOAD_ATTRIBUTE = "REGISTRATION_TASK_ID";
  public static final String COLLECTION_PATH_PAYLOAD_ATTRIBUTE = "COLLECTION_PATH";
  public static final String COLLECTION_UPDATE_PAYLOAD_ATTRIBUTE = "UPDATE";
  public static final String COLLECTION_UPDATE_DESCRIPTION_PAYLOAD_ATTRIBUTE = "UPDATE_DESCRIPTION";
  public static final String SOURCE_LOCATION_PAYLOAD_ATTRIBUTE = "SOURCE_LOCATION";
  public static final String DESTINATION_LOCATION_PAYLOAD_ATTRIBUTE = "DESTINATION_LOCATION";
  public static final String DATA_TRANSFER_COMPLETED_PAYLOAD_ATTRIBUTE = "DATA_TRANSFER_COMPLETED";
  public static final String ERROR_MESSAGE_PAYLOAD_ATTRIBUTE = "ERROR_MESSAGE";
  public static final String REGISTRATION_ITEMS_PAYLOAD_ATTRIBUTE = "REGISTRATION_ITEMS";

  // Event payload entries values.
  public static final String COLLECTION_METADATA_UPDATE_PAYLOAD_VALUE = "METADATA";
  public static final String COLLECTION_METADATA_UPDATE_DESCRIPTION_PAYLOAD_VALUE =
      "Collection metadata updated";
  public static final String COLLECTION_REGISTRATION_PAYLOAD_VALUE = "COLLECTION_REGISTRATION";
  public static final String COLLECTION_REGISTRATION_DESCRIPTION_PAYLOAD_VALUE =
      "Sub collection registerd";
  public static final String DATA_OBJECT_REGISTRATION_PAYLOAD_VALUE = "DATA_OBJECT_REGISTRATION";
  public static final String DATA_OBJECT_REGISTRATION_DESCRIPTION_PAYLOAD_VALUE =
      "Data object registered";

  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The Event DAO instance.
  @Autowired
  private HpcEventDAO eventDAO = null;

  @Autowired
  private HpcReportsDAO reportsDAO = null;

  @Autowired
  private HpcNotificationDAO notificationDAO = null;

  @Autowired
  private HpcDataManagementProxy dataManagementProxy = null;

  // An indicator whether a collection update notification should be sent to the invoker.
  // By default the invoker is not notified for changes they initiated, but this is handy for
  // testing.
  @Value("${hpc.service.event.invokerCollectionUpdateNotification}")
  boolean invokerCollectionUpdateNotification = false;

  // Date formatter to format event payload of type Calendar.
  private DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcEventServiceImpl() {}

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // HpcEventService Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public List<HpcEvent> getEvents() throws HpcException {
    return eventDAO.getEvents();
  }

  @Override
  public void archiveEvent(HpcEvent event) {
    if (event == null) {
      return;
    }

    // Delete the event from the active table and insert to the history.
    try {
      eventDAO.deleteEvent(event.getId());
      eventDAO.insertEventHistory(event);

    } catch (HpcException e) {
      logger.error("Failed to archive event", e);
    }
  }

  @Override
  public HpcEvent getArchivedEvent(int eventId) throws HpcException {
    return eventDAO.getEventHistory(eventId);
  }

  @Override
  public void addDataTransferDownloadCompletedEvent(String userId, String path,
      HpcDownloadTaskType downloadTaskType, String downloadTaskId,
      HpcFileLocation destinationLocation, Calendar dataTransferCompleted) throws HpcException {
    addDataTransferEvent(userId, HpcEventType.DATA_TRANSFER_DOWNLOAD_COMPLETED, downloadTaskType,
        downloadTaskId, path, null, dataTransferCompleted, null, destinationLocation, null, null);
  }

  @Override
  public void addDataTransferDownloadFailedEvent(String userId, String path,
      HpcDownloadTaskType downloadTaskType, String downloadTaskId,
      HpcFileLocation destinationLocation, Calendar dataTransferCompleted, String errorMessage)
      throws HpcException {
    addDataTransferEvent(userId, HpcEventType.DATA_TRANSFER_DOWNLOAD_FAILED, downloadTaskType,
        downloadTaskId, path, null, dataTransferCompleted, null, destinationLocation, errorMessage,
        null);
  }

  @Override
  public void addDataTransferUploadInTemporaryArchiveEvent(String userId, String path)
      throws HpcException {
    addDataTransferEvent(userId, HpcEventType.DATA_TRANSFER_UPLOAD_IN_TEMPORARY_ARCHIVE, null, null,
        path, null, null, null, null, null, null);
  }

  @Override
  public void addDataTransferUploadArchivedEvent(String userId, String path,
      HpcFileLocation sourceLocation, Calendar dataTransferCompleted) throws HpcException {
    addDataTransferEvent(userId, HpcEventType.DATA_TRANSFER_UPLOAD_ARCHIVED, null, null, path, null,
        dataTransferCompleted, sourceLocation, null, null, null);
  }

  @Override
  public void addDataTransferUploadFailedEvent(String userId, String path,
      HpcFileLocation sourceLocation, Calendar dataTransferCompleted, String errorMessage)
      throws HpcException {
    addDataTransferEvent(userId, HpcEventType.DATA_TRANSFER_UPLOAD_FAILED, null, null, path, null,
        dataTransferCompleted, sourceLocation, null, errorMessage, null);
  }

  @Override
  public void addDataTransferUploadURLExpiredEvent(String userId, String path) throws HpcException {
    addDataTransferEvent(userId, HpcEventType.DATA_TRANSFER_UPLOAD_URL_EXPIRED, null, null, path,
        null, null, null, null, null, null);
  }

  @Override
  public void addBulkDataObjectRegistrationCompletedEvent(String userId, String registrationTaskId,
      List<HpcBulkDataObjectRegistrationItem> registrationItems, Calendar completed)
      throws HpcException {
    addDataTransferEvent(userId, HpcEventType.BULK_DATA_OBJECT_REGISTRATION_COMPLETED, null, null,
        null, registrationTaskId, completed, null, null, null, registrationItems);
  }

  @Override
  public void addBulkDataObjectRegistrationFailedEvent(String userId, String registrationTaskId,
      Calendar completed, String errorMessage) throws HpcException {
    addDataTransferEvent(userId, HpcEventType.BULK_DATA_OBJECT_REGISTRATION_FAILED, null, null,
        null, registrationTaskId, completed, null, null, errorMessage, null);
  }
  
  @Override
  public void generateReportsEvents(List<String> userIds, HpcReportCriteria criteria)
      throws HpcException {
    HpcEvent event = new HpcEvent();
    event.getUserIds().addAll(userIds);
    HpcEventType type = getEventType(criteria.getType());
    if (type == null)
      throw new HpcException("Invalid report type", HpcErrorType.INVALID_REQUEST_INPUT);
    event.setType(type);
    HpcReport report = new HpcReport();
    List<HpcReport> reports = reportsDAO.generatReport(criteria);
    if (reports != null)
      report = reports.get(0);

    if (report.getDoc() != null)
      event.getPayloadEntries()
          .add(toPayloadEntry(HpcReportEntryAttribute.DOC.name(), report.getDoc()));
    Format formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm");

    if (report.getFromDate() != null) {
      event.getPayloadEntries().add(toPayloadEntry(HpcReportEntryAttribute.FROM_DATE.name(),
          formatter.format(report.getFromDate().getTime())));
    }
    if (report.getToDate() != null) {
      try {
        event.getPayloadEntries().add(toPayloadEntry(HpcReportEntryAttribute.TO_DATE.name(),
            formatter.format(report.getToDate().getTime())));
      } catch (Exception e) {

      }
    }
    if (report.getGeneratedOn() != null)
      event.getPayloadEntries()
          .add(toPayloadEntry(HpcReportEntryAttribute.REPORT_GENERATED_ON.name(),
              formatter.format(report.getGeneratedOn().getTime())));

    if (report.getType() != null)
      event.getPayloadEntries()
          .add(toPayloadEntry(HpcReportEntryAttribute.TYPE.name(), report.getType().name()));
    if (report.getUser() != null)
      event.getPayloadEntries()
          .add(toPayloadEntry(HpcReportEntryAttribute.USER_ID.name(), report.getUser()));
    List<HpcReportEntry> entries = report.getReportEntries();
    for (HpcReportEntry entry : entries) {
      event.getPayloadEntries().add(toPayloadEntry(entry.getAttribute().name(), entry.getValue()));
    }
    // Persist to DB.
    addEvent(event);
  }

  @Override
  public void addCollectionUpdatedEvent(String path, String userId) throws HpcException {
    addCollectionUpdatedEvent(path, COLLECTION_METADATA_UPDATE_PAYLOAD_VALUE,
        COLLECTION_METADATA_UPDATE_DESCRIPTION_PAYLOAD_VALUE, userId);
  }

  @Override
  public void addCollectionRegistrationEvent(String path, String userId) throws HpcException {
    addCollectionUpdatedEvent(path, COLLECTION_REGISTRATION_PAYLOAD_VALUE,
        COLLECTION_REGISTRATION_DESCRIPTION_PAYLOAD_VALUE, userId);
  }

  @Override
  public void addDataObjectRegistrationEvent(String path, String userId) throws HpcException {
    addCollectionUpdatedEvent(path, DATA_OBJECT_REGISTRATION_PAYLOAD_VALUE,
        DATA_OBJECT_REGISTRATION_DESCRIPTION_PAYLOAD_VALUE, userId);
  }

  @Override
  public void addTierRequestCompletedEvent(String userId, String registrationTaskId,
      List<HpcBulkDataObjectRegistrationItem> registrationItems, Calendar completed)
      throws HpcException {
    addDataTransferEvent(userId, HpcEventType.TIER_REQUEST_COMPLETED, null, null,
        null, registrationTaskId, completed, null, null, null, registrationItems);
  }

  @Override
  public void addTierRequestFailedEvent(String userId, String registrationTaskId,
      Calendar completed, String errorMessage) throws HpcException {
    addDataTransferEvent(userId, HpcEventType.TIER_REQUEST_FAILED, null, null,
        null, registrationTaskId, completed, null, null, errorMessage, null);
  }
  
  @Override
  public void addRestoreRequestCompletedEvent(String userId, String restoreTaskId,
      String path, Calendar completed)
      throws HpcException {
    addDataTransferEvent(userId, HpcEventType.RESTORE_REQUEST_COMPLETED, null, restoreTaskId,
    	path, null, completed, null, null, null, null);
  }

  @Override
  public void addRestoreRequestFailedEvent(String userId, String restoreTaskId,
	  String path, Calendar completed, String errorMessage) throws HpcException {
    addDataTransferEvent(userId, HpcEventType.RESTORE_REQUEST_FAILED, null, restoreTaskId,
    	path, null, completed, null, null, errorMessage, null);
  }
  
  // ---------------------------------------------------------------------//
  // Helper Methods
  // ---------------------------------------------------------------------//

  /**
   * Add an event.
   *
   * @param event The event to add.
   * @throws HpcException if validation failed.
   */
  private void addEvent(HpcEvent event) throws HpcException {
    // Input validation.
    if (event == null || event.getUserIds() == null || event.getUserIds().isEmpty()
        || event.getType() == null) {
      throw new HpcException("Invalid event", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Set the created timestamp.
    event.setCreated(Calendar.getInstance());

    // Persist to DB.
    eventDAO.insertEvent(event);
  }

  /**
   * Instantiate a payload entry object.
   *
   * @param attribute The payload entry attribute.
   * @param value The payload entry value.
   * @return The event payload entry.
   */
  private HpcEventPayloadEntry toPayloadEntry(String attribute, String value) {
    // Construct the event.
    HpcEventPayloadEntry payloadEntry = new HpcEventPayloadEntry();
    payloadEntry.setAttribute(attribute);
    payloadEntry.setValue(value);

    return payloadEntry;
  }

  /**
   * Add a data transfer event.
   *
   * @param userId The user ID.
   * @param eventType The event type.
   * @param downloadTaskType (Optional) The download task type.
   * @param downloadTaskId (Optional) The download task ID.
   * @param path (Optional) The data object path.
   * @param registrationTaskId (Optional) The data registration task ID.
   * @param dataTransferCompleted (Optional) The time the data upload completed.
   * @param sourceLocation (Optional) The data transfer source location.
   * @param destinationLocation (Optional) The data transfer destination location.
   * @param errorMessage (Optional) An error message.
   * @param registrationItems Bulk registration items.
   * @throws HpcException on service failure.
   */
  private void addDataTransferEvent(String userId, HpcEventType eventType,
      HpcDownloadTaskType downloadTaskType, String downloadTaskId, String path,
      String registrationTaskId, Calendar dataTransferCompleted, HpcFileLocation sourceLocation,
      HpcFileLocation destinationLocation, String errorMessage,
      List<HpcBulkDataObjectRegistrationItem> registrationItems) throws HpcException {
    // Input Validation.
    if (userId == null || eventType == null) {
      throw new HpcException("Invalid data transfer event input",
          HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Construct the event.
    HpcEvent event = new HpcEvent();
    event.getUserIds().add(userId);
    event.setType(eventType);
    if (downloadTaskId != null) {
      event.getPayloadEntries()
          .add(toPayloadEntry(DOWNLOAD_TASK_ID_PAYLOAD_ATTRIBUTE, downloadTaskId));
    }
    if (downloadTaskType != null) {
      String downloadTypeStr = "";
      if (downloadTaskType.equals(HpcDownloadTaskType.COLLECTION)) {
        downloadTypeStr = "Collection";
      } else if (downloadTaskType.equals(HpcDownloadTaskType.DATA_OBJECT)) {
        downloadTypeStr = "File";
      } else if (downloadTaskType.equals(HpcDownloadTaskType.DATA_OBJECT_LIST)) {
        downloadTypeStr = "Files";
      }

      event.getPayloadEntries()
          .add(toPayloadEntry(DOWNLOAD_TASK_TYPE_PAYLOAD_ATTRIBUTE, downloadTypeStr));
    }
    if (path != null) {
      event.getPayloadEntries().add(toPayloadEntry(DATA_OBJECT_PATH_PAYLOAD_ATTRIBUTE,
          dataManagementProxy.getRelativePath(path)));
    }
    if (registrationTaskId != null) {
      event.getPayloadEntries()
          .add(toPayloadEntry(REGISTRATION_TASK_ID_PAYLOAD_ATTRIBUTE, registrationTaskId));
    }
    if (dataTransferCompleted != null) {
      event.getPayloadEntries().add(toPayloadEntry(DATA_TRANSFER_COMPLETED_PAYLOAD_ATTRIBUTE,
          dateFormat.format(dataTransferCompleted.getTime())));
    }
    if (sourceLocation != null) {
      event.getPayloadEntries()
          .add(toPayloadEntry(SOURCE_LOCATION_PAYLOAD_ATTRIBUTE, toString(sourceLocation)));
    }
    if (destinationLocation != null) {
      event.getPayloadEntries().add(
          toPayloadEntry(DESTINATION_LOCATION_PAYLOAD_ATTRIBUTE, toString(destinationLocation)));
    }
    if (errorMessage != null) {
      event.getPayloadEntries().add(toPayloadEntry(ERROR_MESSAGE_PAYLOAD_ATTRIBUTE, errorMessage));
    }
    if (registrationItems != null) {
      event.getPayloadEntries()
          .add(toPayloadEntry(REGISTRATION_ITEMS_PAYLOAD_ATTRIBUTE, toString(registrationItems)));
    }

    // Persist to DB.
    addEvent(event);
  }

  private HpcEventType getEventType(HpcReportType reportType) {
    if (reportType == null)
      return null;
    else if (reportType.equals(HpcReportType.USAGE_SUMMARY))
      return HpcEventType.USAGE_SUMMARY_REPORT;
    else if (reportType.equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
      return HpcEventType.USAGE_SUMMARY_BY_WEEKLY_REPORT;
    else if (reportType.equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
      return HpcEventType.USAGE_SUMMARY_BY_DOC_REPORT;
    else if (reportType.equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
      return HpcEventType.USAGE_SUMMARY_BY_DOC_BY_WEEKLY_REPORT;
    else if (reportType.equals(HpcReportType.USAGE_SUMMARY_BY_USER))
      return HpcEventType.USAGE_SUMMARY_BY_USER_REPORT;
    else if (reportType.equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
      return HpcEventType.USAGE_SUMMARY_BY_USER_BY_WEEKLY_REPORT;
    else
      return null;
  }

  /**
   * Add a collection updated event.
   *
   * @param path The collection path.
   * @param updatePayloadValue The value to set on COLLECTION_UPDATE_PAYLOAD_ATTRIBUTE event
   *        payload.
   * @param updateDescriptionPayloadValue The value to set on
   *        COLLECTION_UPDATE_DESCRIPTION_PAYLOAD_ATTRIBUTE event payload.
   * @param userId The user ID who initiated the action resulted in collection update event.
   * @throws HpcException on service failure.
   */
  private void addCollectionUpdatedEvent(String path, String updatePayloadValue,
      String updateDescriptionPayloadValue, String userId) throws HpcException {
    // Input Validation.
    if (path == null || path.isEmpty()) {
      throw new HpcException("Null or empty collection path", HpcErrorType.INVALID_REQUEST_INPUT);
    }

    // Construct an event.
    HpcEvent event = new HpcEvent();
    event.setType(HpcEventType.COLLECTION_UPDATED);
    event.getPayloadEntries().addAll(
        toCollectionUpdatedPayloadEntries(path, updatePayloadValue, updateDescriptionPayloadValue));
    event.getUserIds().addAll(getCollectionUpdatedEventSubscribedUsers(path, updatePayloadValue,
        updateDescriptionPayloadValue, userId));

    // Add the event if found subscriber(s).
    if (!event.getUserIds().isEmpty()) {
      addEvent(event);
    }
  }

  /**
   * Get a list of users subscribed to a collection updated event.
   *
   * @param path The collection path.
   * @param updatePayloadValue The value to set on COLLECTION_UPDATE_PAYLOAD_ATTRIBUTE event
   *        payload.
   * @param updateDescriptionPayloadValue The value to set on
   *        COLLECTION_UPDATE_DESCRIPTION_PAYLOAD_ATTRIBUTE event payload.
   * @param userId The user ID who initiated the action resulted in collection update event.
   * @return A list of user Ids subscribed to this event. This includes users registered to the
   *         collection path itself and anywhere in the hierarchy up to the root.
   * @throws HpcException on service failure.
   */
  private HashSet<String> getCollectionUpdatedEventSubscribedUsers(String path,
      String updatePayloadValue, String updateDescriptionPayloadValue, String userId)
      throws HpcException {

    HashSet<String> userIds = new HashSet<>();

    // Get the users subscribed for this event.
    userIds.addAll(notificationDAO.getSubscribedUsers(HpcEventType.COLLECTION_UPDATED,
        toCollectionUpdatedPayloadEntries(path, updatePayloadValue,
            updateDescriptionPayloadValue)));

    // Add the user subscribed to the parent collection (if path is not root).
    int parentCollectionIndex = path.lastIndexOf('/');
    if (!path.equals("/") && parentCollectionIndex >= 0) {
      userIds.addAll(getCollectionUpdatedEventSubscribedUsers(
          parentCollectionIndex > 0 ? path.substring(0, parentCollectionIndex) : "/",
          updatePayloadValue, updateDescriptionPayloadValue, userId));
    }

    // If configured to - Exclude the user triggered the collection update event from the list.
    if (!invokerCollectionUpdateNotification) {
      userIds.remove(userId);
    }

    return userIds;
  }

  /**
   * Generate collection updated payload entries.
   *
   * @param path The collection path.
   * @param updatePayloadValue The value to set on COLLECTION_UPDATE_PAYLOAD_ATTRIBUTE event
   *        payload.
   * @param updateDescriptionPayloadValue The value to set on
   *        COLLECTION_UPDATE_DESCRIPTION_PAYLOAD_ATTRIBUTE event payload.
   * @return A list of payload entries.
   */
  private List<HpcEventPayloadEntry> toCollectionUpdatedPayloadEntries(String path,
      String updatePayloadValue, String updateDescriptionPayloadValue) {
    List<HpcEventPayloadEntry> payloadEntries = new ArrayList<>();
    payloadEntries.add(toPayloadEntry(COLLECTION_PATH_PAYLOAD_ATTRIBUTE,
        dataManagementProxy.getRelativePath(path)));
    payloadEntries.add(toPayloadEntry(COLLECTION_UPDATE_PAYLOAD_ATTRIBUTE, updatePayloadValue));
    payloadEntries.add(toPayloadEntry(COLLECTION_UPDATE_DESCRIPTION_PAYLOAD_ATTRIBUTE,
        updateDescriptionPayloadValue));
    return payloadEntries;
  }

  /**
   * Generate a string from HpcFileLocation object.
   *
   * @param fileLocation The object to generate a string for.
   * @return A string representation of the file location.
   */
  private String toString(HpcFileLocation fileLocation) {
    StringBuilder fileLocationStr = new StringBuilder();
    if (!StringUtils.isEmpty(fileLocation.getFileContainerName())) {
      fileLocationStr.append(fileLocation.getFileContainerName() + ":");
    } else if (!StringUtils.isEmpty(fileLocation.getFileContainerId())) {
      fileLocationStr.append(fileLocation.getFileContainerId() + ":");
    }
    if (!StringUtils.isEmpty(fileLocation.getFileId())) {
      fileLocationStr.append(fileLocation.getFileId());
    }

    return fileLocationStr.toString();
  }

  /**
   * Generate a string from a list of registration items.
   *
   * @param registrationItems The bulk registration items to generate a string for.
   * @return A string representation of the registration items.
   */
  private String toString(List<HpcBulkDataObjectRegistrationItem> registrationItems) {
    StringBuilder registrationItemsStr = new StringBuilder();
    registrationItems.forEach(registrationItem -> {
      String source = null;
      if (registrationItem.getRequest().getLinkSourcePath() != null || 
    		  registrationItem.getRequest().getGlobusUploadSource() != null || 
    		  registrationItem.getRequest().getS3UploadSource() != null || 
    		  registrationItem.getRequest().getGoogleDriveUploadSource() != null) {
	      if (registrationItem.getRequest().getLinkSourcePath() != null) {
	        source = registrationItem.getRequest().getLinkSourcePath();
	      } else {
	        HpcFileLocation sourceLocation = null;
	        if (registrationItem.getRequest().getGlobusUploadSource() != null) {
	          sourceLocation =
	              registrationItem.getRequest().getGlobusUploadSource().getSourceLocation();
	        } else if (registrationItem.getRequest().getS3UploadSource() != null) {
	          sourceLocation = registrationItem.getRequest().getS3UploadSource().getSourceLocation();
	        } else {
	          sourceLocation =
	              registrationItem.getRequest().getGoogleDriveUploadSource().getSourceLocation();
	        }
	        source = toString(sourceLocation);
	      }
      }
      if (source != null)
    	  registrationItemsStr.append("\n\t" + source + " -> " + registrationItem.getTask().getPath());
      else
    	  registrationItemsStr.append("\n\t" + registrationItem.getTask().getPath());
    });

    return registrationItemsStr.toString();
  }
}
