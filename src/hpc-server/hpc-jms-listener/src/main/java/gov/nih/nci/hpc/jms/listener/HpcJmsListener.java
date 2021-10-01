package gov.nih.nci.hpc.jms.listener;

import static gov.nih.nci.hpc.util.HpcMessageTask.execute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcSystemBusService;
import gov.nih.nci.hpc.domain.message.HpcTaskMessage;
import gov.nih.nci.hpc.exception.HpcException;

public class HpcJmsListener {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//
	// The Security Business Service instance.
	@Autowired
	private HpcSystemBusService systemBusService = null;


	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/**
	 * Start Data Object Download Tasks that are in RECEIVED state for Globus
	 * transfer.
	 * @throws HpcException 
	 */
	public void startGlobusDataObjectDownloadTasks(HpcTaskMessage taskMessage) throws HpcException {
		logger.info("Received startGoogleDriveDataObjectDownloadTasks with taskId {}", taskMessage.getTaskId());
		execute(taskMessage, "startGlobusDataObjectDownloadTasks()", systemBusService::startGlobusDataObjectDownloadTasks, logger);
	}

	/**
	 * Start Data Object Download Tasks that are in RECEIVED state for S3 transfer.
	 * @throws HpcException 
	 */
	public void startS3DataObjectDownloadTasks(HpcTaskMessage taskMessage) throws HpcException {
		logger.info("Received startGoogleDriveDataObjectDownloadTasks with taskId {}", taskMessage.getTaskId());
		execute(taskMessage, "startS3DataObjectDownloadTasks()", systemBusService::startS3DataObjectDownloadTasks, logger);
	}

	/**
	 * Start Data Object Download Tasks that are in RECEIVED state for GOOGLE_DRIVE
	 * transfer.
	 * @throws HpcException 
	 */
	public void startGoogleDriveDataObjectDownloadTasks(HpcTaskMessage taskMessage) throws HpcException {
		logger.info("Received startGoogleDriveDataObjectDownloadTasks with taskId {}", taskMessage.getTaskId());
		execute(taskMessage, "startGoogleDriveDataObjectDownloadTasks()", systemBusService::startGoogleDriveDataObjectDownloadTasks, logger);
	}
	
	/** Process collection download tasks. 
	 * @throws HpcException */
	public void processCollectionDownloadTasks(HpcTaskMessage taskMessage) throws HpcException {
		logger.info("Received processCollectionDownloadTasks with taskId {} ", taskMessage.getTaskId());
		execute(taskMessage, "processCollectionDownloadTasks()", systemBusService::processCollectionDownloadTasks, logger);
		
    }

	
	
}
