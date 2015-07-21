/**
 * HpcDataTransferAccountCodec.java
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
import java.util.Date;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * <p>
 * HPC File Location Codec. 
 * </p>
 *
 * @author <a href="mailto:mahidhar.narra@nih.gov">Mahidhar Narra</a>
 * @version $Id: HpcDataTransferReportCodec.java 300 2015-07-07 17:18:19Z narram $
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
						HpcDataTransferReport hpcDataTransferReport,
					   EncoderContext encoderContext) 
	{
		Document document = new Document();

		// Extract the data from the POJO.
		
		String command = hpcDataTransferReport.getCommand();
		String destEndpoint = hpcDataTransferReport.getDestinationEndpoint();
		String sourceEndpoint = hpcDataTransferReport.getSourceEndpoint();
		String status = hpcDataTransferReport.getStatus();
		String taskId = hpcDataTransferReport.getTaskID();
		String taskName = hpcDataTransferReport.getTaskType();
		long bytesChecksummed = hpcDataTransferReport.getBytesChecksummed();
		long bytestransferred = hpcDataTransferReport.getBytesTransferred();
		boolean checksumVerification = hpcDataTransferReport.getChecksumVerification();
		Calendar completionTime = hpcDataTransferReport.getCompletionTime();
		Calendar requestTime = hpcDataTransferReport.getRequestTime();
		boolean dataEncrip = hpcDataTransferReport.getDataEncryption();
		Calendar deadLine = hpcDataTransferReport.getDeadline();
		boolean delete = hpcDataTransferReport.getDelete();
		int directories = hpcDataTransferReport.getDirectories();
		int files = hpcDataTransferReport.getFiles();
		int expansions = hpcDataTransferReport.getExpansions();
		double effectiveMbits = hpcDataTransferReport.getEffectiveMbitsPerSec();
		int faults = hpcDataTransferReport.getFaults();
		int filesSkipped = hpcDataTransferReport.getFilesSkipped();
		int totalTasks = hpcDataTransferReport.getTotalTasks();
		int tasksSuccessful = hpcDataTransferReport.getTasksSuccessful();
		int tasksExpired = hpcDataTransferReport.getTasksExpired();
		int tasksCancelled = hpcDataTransferReport.getTasksCanceled();
		int tasksFailed = hpcDataTransferReport.getTasksFailed();
		int tasksPending = hpcDataTransferReport.getTasksPending();
		int tasksRetrying = hpcDataTransferReport.getTasksRetrying();

		
		// Set the data on the BSON document.
		if(command != null) {
		   document.put(TRANSFER_STATUS_REPORT_COMMAND, command);
		}
		if(destEndpoint != null) {
		   document.put(TRANSFER_STATUS_REPORT_DEST_ENDPOINT, destEndpoint);
		}
		if(sourceEndpoint != null) {
		   document.put(TRANSFER_STATUS_REPORT_SOURCE_ENDPOINT, sourceEndpoint);
		}
		if(status != null) {
		   document.put(TRANSFER_STATUS_REPORT_STATUS, status);
		}
		if(taskId != null) {
		   document.put(TRANSFER_STATUS_REPORT_TASKID, taskId);
		}
		//if(bytesChecksummed != null) {
		   document.put(TRANSFER_STATUS_REPORT_BYTESCHECKSUMMED, bytesChecksummed);
		//}
		//if(bytestransferred != null) {
			   document.put(TRANSFER_STATUS_REPORT_BYTESTRANSFERRED, bytestransferred);
		//}
		if(completionTime != null) {
			   document.put(TRANSFER_STATUS_REPORT_COMPLETIONTIME, completionTime.getTime());
		}
		if(requestTime != null) {
			   document.put(TRANSFER_STATUS_REPORT_REQUESTTIME, requestTime.getTime());
		}		
		
		//if(dataEncrip != null) {
			   document.put(TRANSFER_STATUS_REPORT_DATAENCRIPTION, dataEncrip);
		//}
		if(deadLine != null) {
			   document.put(TRANSFER_STATUS_REPORT_DEADLINE, deadLine.getTime());
		}		
		//if(delete != null) {
			   document.put(TRANSFER_STATUS_REPORT_DELETE, delete);
		//}		
		//if(directories != null) {
			   document.put(TRANSFER_STATUS_REPORT_DIRECTORIES, directories);
		//}		
		//if(files != null) {
			   document.put(TRANSFER_STATUS_REPORT_FILES, files);
		//}		
		//if(expansions != null) {
			   document.put(TRANSFER_STATUS_REPORT_EXPANSIONS, expansions);
		//}
		//if(effectiveMbits != null) {
			   document.put(TRANSFER_STATUS_REPORT_EFFECTIVEMBITS, effectiveMbits);
		//}
		//if(faults != null) {
			   document.put(TRANSFER_STATUS_REPORT_FAULTS, faults);
		//}
		//if(filesSkipped != null) {
			   document.put(TRANSFER_STATUS_REPORT_FILESSKIPPED, filesSkipped);
		//}		
		//if(totalTasks != null) {
			   document.put(TRANSFER_STATUS_REPORT_TOTALTASKS, totalTasks);
		//}
		//if(tasksSuccessful != null) {
			   document.put(TRANSFER_STATUS_REPORT_TASKSSUCCESSFUL, tasksSuccessful);
		//}
		//if(tasksExpired != null) {
			   document.put(TRANSFER_STATUS_REPORT_TASKSEXPIRED, tasksExpired);
		//}				
		//if(tasksCancelled != null) {
			   document.put(TRANSFER_STATUS_REPORT_TASKSCANCELLED, tasksCancelled);
		//}
		//if(tasksFailed != null) {
			   document.put(TRANSFER_STATUS_REPORT_TASKSFAILED, tasksFailed);
		//}	
		//if(tasksPending != null) {
			   document.put(TRANSFER_STATUS_REPORT_TASKSPENDING, tasksPending);
		//}
		//if(tasksRetrying != null) {
			   document.put(TRANSFER_STATUS_REPORT_TASKSRETRYING, tasksRetrying);
		//}			

	
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
		HpcDataTransferReport hpcDataTransferReport = new HpcDataTransferReport();
		hpcDataTransferReport.setCommand(document.get(TRANSFER_STATUS_REPORT_COMMAND, String.class));
		hpcDataTransferReport.setDestinationEndpoint(document.get(TRANSFER_STATUS_REPORT_DEST_ENDPOINT, String.class));
		hpcDataTransferReport.setSourceEndpoint(document.get(TRANSFER_STATUS_REPORT_SOURCE_ENDPOINT, String.class));
		hpcDataTransferReport.setStatus(document.get(TRANSFER_STATUS_REPORT_STATUS, String.class));
		hpcDataTransferReport.setTaskID(document.get(TRANSFER_STATUS_REPORT_TASKID, String.class));
		hpcDataTransferReport.setBytesChecksummed(document.get(TRANSFER_STATUS_REPORT_BYTESCHECKSUMMED, Long.class));
		hpcDataTransferReport.setBytesTransferred(document.get(TRANSFER_STATUS_REPORT_BYTESTRANSFERRED, Long.class));
		
		if(document.get(TRANSFER_STATUS_REPORT_COMPLETIONTIME, Date.class) != null)
		{
			Calendar completionTime = Calendar.getInstance();
			completionTime.setTime(document.get(TRANSFER_STATUS_REPORT_COMPLETIONTIME, Date.class));
			hpcDataTransferReport.setCompletionTime(completionTime);
		}
		if(document.get(TRANSFER_STATUS_REPORT_REQUESTTIME, Date.class) != null)
		{
			Calendar requestTime = Calendar.getInstance();
			requestTime.setTime(document.get(TRANSFER_STATUS_REPORT_REQUESTTIME, Date.class));
			hpcDataTransferReport.setRequestTime(requestTime);
		}
		if(document.get(TRANSFER_STATUS_REPORT_DEADLINE, Date.class) != null)
		{	
			Calendar deadLine = Calendar.getInstance();
			deadLine.setTime(document.get(TRANSFER_STATUS_REPORT_DEADLINE, Date.class));
			hpcDataTransferReport.setDeadline(deadLine);		
		}
		hpcDataTransferReport.setDataEncryption(document.get(TRANSFER_STATUS_REPORT_DATAENCRIPTION, Boolean.class));
		hpcDataTransferReport.setDelete(document.get(TRANSFER_STATUS_REPORT_DELETE, Boolean.class));
		hpcDataTransferReport.setDirectories(document.get(TRANSFER_STATUS_REPORT_DIRECTORIES, Integer.class));
		hpcDataTransferReport.setFiles(document.get(TRANSFER_STATUS_REPORT_FILES, Integer.class));
		hpcDataTransferReport.setExpansions(document.get(TRANSFER_STATUS_REPORT_EXPANSIONS, Integer.class));
		hpcDataTransferReport.setEffectiveMbitsPerSec(document.get(TRANSFER_STATUS_REPORT_EFFECTIVEMBITS, Double.class));
		hpcDataTransferReport.setFaults(document.get(TRANSFER_STATUS_REPORT_FAULTS, Integer.class));
		hpcDataTransferReport.setFilesSkipped(document.get(TRANSFER_STATUS_REPORT_FILESSKIPPED, Integer.class));
		hpcDataTransferReport.setTotalTasks(document.get(TRANSFER_STATUS_REPORT_TOTALTASKS, Integer.class));
		hpcDataTransferReport.setTasksSuccessful(document.get(TRANSFER_STATUS_REPORT_TASKSSUCCESSFUL, Integer.class));
		hpcDataTransferReport.setTasksCanceled(document.get(TRANSFER_STATUS_REPORT_TASKSCANCELLED, Integer.class));
		hpcDataTransferReport.setTasksFailed(document.get(TRANSFER_STATUS_REPORT_TASKSFAILED, Integer.class));
		hpcDataTransferReport.setTasksPending(document.get(TRANSFER_STATUS_REPORT_TASKSPENDING, Integer.class));
		hpcDataTransferReport.setTasksRetrying(document.get(TRANSFER_STATUS_REPORT_TASKSRETRYING, Integer.class));
		
		
		return hpcDataTransferReport;
	}
	
	@Override
	public Class<HpcDataTransferReport> getEncoderClass() 
	{
		return HpcDataTransferReport.class;
	}
}

 
