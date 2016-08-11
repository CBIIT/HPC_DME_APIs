/**
 * HpcEventServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.dao.HpcEventDAO;
import gov.nih.nci.hpc.dao.HpcReportsDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.notification.HpcEvent;
import gov.nih.nci.hpc.domain.notification.HpcEventPayloadEntry;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.domain.report.HpcReportEntry;
import gov.nih.nci.hpc.domain.report.HpcReportEntryAttribute;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcEventService;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Event Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id: HpcNotificationServiceImpl.java 1368 2016-07-30 15:00:37Z rosenbergea $
 */

public class HpcEventServiceImpl implements HpcEventService
{
    //---------------------------------------------------------------------//
    // Constants
    //---------------------------------------------------------------------//
	
    // Event payload entries attributes.
	private static final String DATA_TRANSFER_REQUEST_ID_ATTRIBUTE = 
			                    "DATA_TRANSFER_REQUEST_ID";
	private static final String DATA_OBJECT_PATH_ATTRIBUTE = 
                                "DATA_OBJECT_PATH";
	
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The Event DAO instance.
	@Autowired
    private HpcEventDAO eventDAO = null;
	
	@Autowired
    private HpcReportsDAO reportsDAO = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Constructor for Spring Dependency Injection.
     *
     */
    private HpcEventServiceImpl()
    {
    }
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//

    //---------------------------------------------------------------------//
    // HpcEventService Interface Implementation
    //---------------------------------------------------------------------//
    
    @Override
    public List<HpcEvent> getEvents() throws HpcException
    {
    	return eventDAO.getEvents();
    }
    
    @Override
    public void archiveEvent(HpcEvent event)
    {
    	if(event == null) {
    	   return;
    	}
    	
    	// Delete the event from the active table and insert to the history.
    	try {
    	     eventDAO.deleteEvent(event.getId());
    	     eventDAO.insertEventHistory(event);
    	     
    	} catch(HpcException e) {
    		    logger.error("Failed to archive event", e);
    	}
    }
    
    @Override
    public void addDataTransferDownloadCompletedEvent(
    		       String userId, String dataTransferRequestId) throws HpcException
    {
    	addDataTransferEvent(userId, HpcEventType.DATA_TRANSFER_DOWNLOAD_COMPLETED,
    			             dataTransferRequestId, null);
    }
    
    @Override
    public void addDataTransferDownloadFailedEvent(
    		       String userId, String dataTransferRequestId) throws HpcException
    {
    	addDataTransferEvent(userId, HpcEventType.DATA_TRANSFER_DOWNLOAD_FAILED,
	                         dataTransferRequestId, null);
    }
    
    @Override
    public void addDataTransferUploadInTemporaryArchiveEvent(String userId, String path) 
    		                                                throws HpcException
    {
    	addDataTransferEvent(userId, HpcEventType.DATA_TRANSFER_UPLOAD_IN_TEMPORARY_ARCHIVE,
                             null, path);
    }
    
    @Override
    public void addDataTransferUploadArchivedEvent(String userId, String path) 
    		                                      throws HpcException
    {
    	addDataTransferEvent(userId, HpcEventType.DATA_TRANSFER_UPLOAD_ARCHIVED,
                             null, path);
    }
    
    @Override
    public void addDataTransferUploadFailedEvent(String userId, String path) 
    		                                    throws HpcException
    {
    	addDataTransferEvent(userId, HpcEventType.DATA_TRANSFER_UPLOAD_FAILED,
                             null, path);
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//  
    
    /**
     * Add an event.
     * 
     * @param notificationEvent The event to add.
     * 
     * @throws HpcException if validation failed.
     */
    private void addEvent(HpcEvent event) throws HpcException
    {
    	// Input validation.
    	if(event == null || event.getUserIds() == null || event.getUserIds().isEmpty() ||
    	   event.getType() == null) {
    	   throw new HpcException("Invalid event",
    			                  HpcErrorType.INVALID_REQUEST_INPUT);
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
     */
    private HpcEventPayloadEntry toPayloadEntry(String attribute, String value)
    {
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
     * @param dataTransferRequestId (Optional) The data transfer request ID.
     * @param path (Optional) The data object path.
     */
    private void addDataTransferEvent(String userId, HpcEventType eventType, 
    		                          String dataTransferRequestId, String path) throws HpcException
	{
		// Input Validation.
		if(userId == null || eventType == null) {
		   throw new HpcException("Invalid data transfer event input", 
				                  HpcErrorType.INVALID_REQUEST_INPUT);
		}
		
		// Construct the event.
		HpcEvent event = new HpcEvent();
		event.getUserIds().add(userId);
		event.setType(eventType);
		if(dataTransferRequestId != null) {
		   event.getPayloadEntries().add(toPayloadEntry(DATA_TRANSFER_REQUEST_ID_ATTRIBUTE, 
			                                            dataTransferRequestId));
		}
		if(path != null) {
		   event.getPayloadEntries().add(toPayloadEntry(DATA_OBJECT_PATH_ATTRIBUTE, path));
		}
	
		// Persist to DB.
		addEvent(event);
	}

    public void generateReportsEvents(List<String> userIds, HpcReportCriteria criteria) throws HpcException
    {
		HpcEvent event = new HpcEvent();
		event.getUserIds().addAll(userIds);
		HpcEventType type = getEventType(criteria.getType());
		if(type == null)
			throw new HpcException("Invalid report type", HpcErrorType.INVALID_REQUEST_INPUT);
		event.setType(type);
		HpcReport report = reportsDAO.generatReport(criteria);
		if(report.getDoc() != null)
			event.getPayloadEntries().add(toPayloadEntry(HpcReportEntryAttribute.DOC.name(), 
                   report.getDoc()));
		Format formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm");
		
		if(report.getFromDate() != null)
		{
			event.getPayloadEntries().add(toPayloadEntry(HpcReportEntryAttribute.FROM_DATE.name(), 
					formatter.format(report.getFromDate().getTime())));
		}
		if(report.getToDate() != null)
		{
			try
			{
			event.getPayloadEntries().add(toPayloadEntry(HpcReportEntryAttribute.TO_DATE.name(), 
					formatter.format(report.getToDate().getTime())));
			}
			catch(Exception e)
			{
				
			}
		}
		if(report.getGeneratedOn() != null)
			event.getPayloadEntries().add(toPayloadEntry(HpcReportEntryAttribute.REPORT_GENERATED_ON.name(), 
					formatter.format(report.getGeneratedOn().getTime())));
		
		if(report.getType() != null)
			event.getPayloadEntries().add(toPayloadEntry(HpcReportEntryAttribute.TYPE.name(), 
                   report.getType().name()));
		if(report.getUser() != null)
			event.getPayloadEntries().add(toPayloadEntry(HpcReportEntryAttribute.USER_ID.name(), 
                   report.getUser()));
		List<HpcReportEntry> entries = report.getReportEntries();
		for(HpcReportEntry entry : entries)
		{
			event.getPayloadEntries().add(toPayloadEntry(entry.getAttribute().name(), 
	                   entry.getValue()));
		}
		// Persist to DB.
		addEvent(event);
    	
    }
    
    private HpcEventType getEventType(HpcReportType reportType)
    {
    	if(reportType == null)
    		return null;
    	else if(reportType.equals(HpcReportType.USAGE_SUMMARY))
    		return HpcEventType.USAGE_SUMMARY_REPORT;
    	else if(reportType.equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE))
    		return HpcEventType.USAGE_SUMMARY_BY_WEEKLY_REPORT;
    	else if(reportType.equals(HpcReportType.USAGE_SUMMARY_BY_DOC))
    		return HpcEventType.USAGE_SUMMARY_BY_DOC_REPORT;
    	else if(reportType.equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
    		return HpcEventType.USAGE_SUMMARY_BY_DOC_BY_WEEKLY_REPORT;
    	else if(reportType.equals(HpcReportType.USAGE_SUMMARY_BY_USER))
    		return HpcEventType.USAGE_SUMMARY_BY_USER_REPORT;
    	else if(reportType.equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE))
    		return HpcEventType.USAGE_SUMMARY_BY_USER_BY_WEEKLY_REPORT;
    	else
    		return null;
    }
}

