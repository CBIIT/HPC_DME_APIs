package gov.nih.nci.hpc.jms;

import gov.nih.nci.hpc.domain.message.HpcMessageQueue;
import gov.nih.nci.hpc.domain.message.HpcTaskMessage;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Jms Queue Sender Interface.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */


public interface HpcJmsQueueSender {
	
	/**
	 * Send HpcTaskMessage message
	 * 
	 * @param message The message.
	 * @param queue The queue.
	 * @throws HpcException on service failure.
	 */
	public void send(HpcTaskMessage message, HpcMessageQueue queue) throws HpcException;
	
	/**
	 * Send HpcTaskMessage message with delay
	 * 
	 * @param message The message.
	 * @param queue The queue.
	 * @param delay True if delivery needs to be delayed.
	 * @throws HpcException on service failure.
	 */
	public void send(HpcTaskMessage message, HpcMessageQueue queue, Boolean delay) throws HpcException;

}
