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

import java.util.Calendar;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferReport;
import gov.nih.nci.hpc.domain.dataset.HpcDataTransferRequest;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.dataset.HpcFileSet;
import gov.nih.nci.hpc.domain.dataset.HpcFileType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcFileMetadata;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccount;
import gov.nih.nci.hpc.domain.user.HpcDataTransferAccountType;
import gov.nih.nci.hpc.exception.HpcException;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Binary;

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
			   document.put(TRANSFER_STATUS_REPORT_COMPLETIONTIME, completionTime);
		}
		//if(dataEncrip != null) {
			   document.put(TRANSFER_STATUS_REPORT_DATAENCRIPTION, dataEncrip);
		//}
		if(deadLine != null) {
			   document.put(TRANSFER_STATUS_REPORT_DEADLINE, deadLine);
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

		
		return hpcDataTransferReport;
	}
	
	@Override
	public Class<HpcDataTransferReport> getEncoderClass() 
	{
		return HpcDataTransferReport.class;
	}
}

 