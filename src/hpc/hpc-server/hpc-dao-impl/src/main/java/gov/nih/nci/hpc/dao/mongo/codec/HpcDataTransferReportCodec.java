/**
 * HpcDataTransferReportCodec.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao.mongo.codec;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;

import java.util.Calendar;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC Data Transfer Report Codec. 
 * </p>
 *
 * @author <a href="mailto:mahidhar.narra@nih.gov">Mahidhar Narra</a>
 * @version $Id$
 */

public class HpcDataTransferReportCodec extends HpcCodec<HpcDataTransferReport>
{ 

    //---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//
	
    /**
     * Default Constructor.
     * 
     * 
     */
    private HpcDataTransferReportCodec()
    {
    }   

    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // Codec<HpcDataTransferRequest> Interface Implementation
    //---------------------------------------------------------------------//  
    
	@Override
	public void encode(BsonWriter writer, 
					   HpcDataTransferReport dataTransferReport,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();

		// Extract the data from the POJO.
		String taskId = dataTransferReport.getTaskID();
		String taskType = dataTransferReport.getTaskType();
		String status = dataTransferReport.getStatus();
		Calendar requestTime = dataTransferReport.getRequestTime();
		Calendar deadline = dataTransferReport.getDeadline();
		Calendar completionTime = dataTransferReport.getCompletionTime();
		int totalTasks = dataTransferReport.getTotalTasks();
		int tasksSuccessful = dataTransferReport.getTasksSuccessful();
		int tasksExpired = dataTransferReport.getTasksExpired();
		int tasksCanceled = dataTransferReport.getTasksCanceled();
		int tasksFailed = dataTransferReport.getTasksFailed();
		int tasksPending = dataTransferReport.getTasksPending();
		int tasksRetrying = dataTransferReport.getTasksRetrying();
		String command = dataTransferReport.getCommand();
		String sourceEndpoint = dataTransferReport.getSourceEndpoint();
		String destinationEndpoint = dataTransferReport.getDestinationEndpoint();
		boolean dataEncryption = dataTransferReport.getDataEncryption();
		boolean checksumVerification = dataTransferReport.getChecksumVerification();
		boolean delete = dataTransferReport.getDelete();
		int files = dataTransferReport.getFiles();
		int filesSkipped = dataTransferReport.getFilesSkipped();
        int directories = dataTransferReport.getDirectories();
		int expansions = dataTransferReport.getExpansions();
		long bytesTransferred = dataTransferReport.getBytesTransferred();
		long bytesChecksummed = dataTransferReport.getBytesChecksummed();
		double effectiveMbitsPerSec = dataTransferReport.getEffectiveMbitsPerSec();
		int faults = dataTransferReport.getFaults();
		
		// Set the data on the BSON document.
		if(taskId != null) {
		   document.put(DATA_TRANSFER_REPORT_TASK_ID_KEY, taskId);
		}
		if(taskType != null) {
		   document.put(DATA_TRANSFER_REPORT_TASK_TYPE_KEY, taskType);
		}
		if(status != null) {
		   document.put(DATA_TRANSFER_REPORT_STATUS_KEY, status);
		}
		if(requestTime != null) {
		   document.put(DATA_TRANSFER_REPORT_REQUEST_TIME_KEY, 
				        requestTime.getTime());
		}	
		if(deadline != null) {
		   document.put(DATA_TRANSFER_REPORT_DEADLINE_KEY, 
					    deadline.getTime());
		}
		if(completionTime != null) {
		   document.put(DATA_TRANSFER_REPORT_COMPLETION_TIME_KEY, 
					    completionTime.getTime());
		}	
		document.put(DATA_TRANSFER_REPORT_TOTAL_TASKS_KEY, totalTasks);
		document.put(DATA_TRANSFER_REPORT_TASKS_SUCCESSFUL_KEY, tasksSuccessful);
		document.put(DATA_TRANSFER_REPORT_TASKS_EXPIRED_KEY, tasksExpired);
		document.put(DATA_TRANSFER_REPORT_TASKS_CANCELED_KEY, tasksCanceled);
		document.put(DATA_TRANSFER_REPORT_TASKS_FAILED_KEY, tasksFailed);
		document.put(DATA_TRANSFER_REPORT_TASKS_PENDING_KEY, tasksPending);
		document.put(DATA_TRANSFER_REPORT_TASKS_RETRYING_KEY, tasksRetrying);
		if(command != null) {
		   document.put(DATA_TRANSFER_REPORT_COMMAND_KEY, command);
		}
		if(sourceEndpoint != null) {
		   document.put(DATA_TRANSFER_REPORT_SOURCE_ENDPOINT_KEY, sourceEndpoint);
		}
		if(destinationEndpoint != null) {
		   document.put(DATA_TRANSFER_REPORT_DESTINATION_ENDPOINT_KEY, destinationEndpoint);
		}
		document.put(DATA_TRANSFER_REPORT_DATA_ENCRYPTION_KEY, dataEncryption);
		document.put(DATA_TRANSFER_REPORT_CHECKSUM_VERIFICATION_KEY, checksumVerification);
		document.put(DATA_TRANSFER_REPORT_DELETE_KEY, delete);
		document.put(DATA_TRANSFER_REPORT_FILES_KEY, files);
		document.put(DATA_TRANSFER_REPORT_FILES_SKIPPED_KEY, filesSkipped);
		document.put(DATA_TRANSFER_REPORT_DIRECTORIES_KEY, directories);
		document.put(DATA_TRANSFER_REPORT_EXPANSIONS_KEY, expansions);
		document.put(DATA_TRANSFER_REPORT_BYTES_TRANSFERRED_KEY, bytesTransferred);
		document.put(DATA_TRANSFER_REPORT_BYTES_CHECKSUMMED_KEY, bytesChecksummed);
		document.put(DATA_TRANSFER_REPORT_EFFECTIVE_MBITS_PER_SEC_KEY, effectiveMbitsPerSec);
		document.put(DATA_TRANSFER_REPORT_FAULTS_KEY, faults);
		
		getRegistry().get(Document.class).encode(writer, document, 
				                                 encoderContext);
	}
 
	@Override
	public HpcDataTransferReport decode(BsonReader reader, 
			                             DecoderContext decoderContext) 
	{
		// Get the BSON Document.
		Document document = 
	             getRegistry().get(Document.class).decode(reader, 
	            		                                  decoderContext);
		
		// Map the document to HpcDataTransferReport instance.
		HpcDataTransferReport dataTransferReport = new HpcDataTransferReport();
		dataTransferReport.setTaskID(document.getString(DATA_TRANSFER_REPORT_TASK_ID_KEY));
		dataTransferReport.setTaskType(document.getString(DATA_TRANSFER_REPORT_TASK_TYPE_KEY));
		dataTransferReport.setStatus(document.getString(DATA_TRANSFER_REPORT_STATUS_KEY));
		if(document.getDate(DATA_TRANSFER_REPORT_REQUEST_TIME_KEY) != null) {
		   Calendar requestTime = Calendar.getInstance();
		   requestTime.setTime(document.getDate(DATA_TRANSFER_REPORT_REQUEST_TIME_KEY));
		   dataTransferReport.setRequestTime(requestTime);
		}
		if(document.getDate(DATA_TRANSFER_REPORT_DEADLINE_KEY) != null) {
		   Calendar deadline = Calendar.getInstance();
		   deadline.setTime(document.getDate(DATA_TRANSFER_REPORT_DEADLINE_KEY));
		   dataTransferReport.setDeadline(deadline);
		}
		if(document.getDate(DATA_TRANSFER_REPORT_COMPLETION_TIME_KEY) != null) {
		   Calendar completionTime = Calendar.getInstance();
		   completionTime.setTime(document.getDate(DATA_TRANSFER_REPORT_COMPLETION_TIME_KEY));
		   dataTransferReport.setCompletionTime(completionTime);
		}
		dataTransferReport.setTotalTasks(document.getInteger(
				                         DATA_TRANSFER_REPORT_TOTAL_TASKS_KEY));
		dataTransferReport.setTasksSuccessful(document.getInteger(
                                              DATA_TRANSFER_REPORT_TASKS_SUCCESSFUL_KEY));
		dataTransferReport.setTasksExpired(document.getInteger(
                                           DATA_TRANSFER_REPORT_TASKS_EXPIRED_KEY));
		dataTransferReport.setTasksCanceled(document.getInteger(
                                            DATA_TRANSFER_REPORT_TASKS_CANCELED_KEY));
		dataTransferReport.setTasksFailed(document.getInteger(
                                          DATA_TRANSFER_REPORT_TASKS_FAILED_KEY));
		dataTransferReport.setTasksPending(document.getInteger(
                                           DATA_TRANSFER_REPORT_TASKS_PENDING_KEY));
		dataTransferReport.setTasksRetrying(document.getInteger(
                                            DATA_TRANSFER_REPORT_TASKS_RETRYING_KEY));
		dataTransferReport.setCommand(document.getString(DATA_TRANSFER_REPORT_COMMAND_KEY));
		dataTransferReport.setSourceEndpoint(document.getString(
				                             DATA_TRANSFER_REPORT_SOURCE_ENDPOINT_KEY));
		dataTransferReport.setDestinationEndpoint(document.getString(
                                         DATA_TRANSFER_REPORT_DESTINATION_ENDPOINT_KEY));
		dataTransferReport.setDataEncryption(document.getBoolean(
                                             DATA_TRANSFER_REPORT_DATA_ENCRYPTION_KEY));
		dataTransferReport.setChecksumVerification(document.getBoolean(
                                      DATA_TRANSFER_REPORT_CHECKSUM_VERIFICATION_KEY));
		dataTransferReport.setDelete(document.getBoolean(
                                     DATA_TRANSFER_REPORT_DELETE_KEY));
		dataTransferReport.setFiles(document.getInteger(DATA_TRANSFER_REPORT_FILES_KEY));
		dataTransferReport.setFilesSkipped(document.getInteger(
				                           DATA_TRANSFER_REPORT_FILES_SKIPPED_KEY));
		dataTransferReport.setDirectories(document.getInteger(
				                          DATA_TRANSFER_REPORT_DIRECTORIES_KEY));
		dataTransferReport.setExpansions(document.getInteger(
				                         DATA_TRANSFER_REPORT_EXPANSIONS_KEY));
		dataTransferReport.setBytesTransferred(document.getLong(
                                   DATA_TRANSFER_REPORT_BYTES_TRANSFERRED_KEY));
		dataTransferReport.setBytesChecksummed(document.getLong(
                                   DATA_TRANSFER_REPORT_BYTES_CHECKSUMMED_KEY));
		dataTransferReport.setEffectiveMbitsPerSec(document.getDouble(
				              DATA_TRANSFER_REPORT_EFFECTIVE_MBITS_PER_SEC_KEY));
		dataTransferReport.setFaults(document.getInteger(
				                     DATA_TRANSFER_REPORT_FAULTS_KEY));
		
		return dataTransferReport;
	}
	
	@Override
	public Class<HpcDataTransferReport> getEncoderClass() 
	{
		return HpcDataTransferReport.class;
	}
}

 
