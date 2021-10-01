/**
 * HpcMessageTask.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.util;

import org.slf4j.Logger;

import gov.nih.nci.hpc.domain.message.HpcTaskMessage;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Message task interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

@FunctionalInterface
public interface HpcMessageTask 
{   
    /**
     * The message task implementation
     * @param taskMessage 
     *
     * @throws HpcException on task execution failure.
     */
    public void execute(HpcTaskMessage taskMessage) throws HpcException;
    
    /**
	 * Execute a message task.
	 *
	 * @param name The task name.
	 * @param task The task to execute.
     * @throws HpcException 
	 */
	public static void execute(HpcTaskMessage taskMessage, String name, HpcMessageTask task, Logger logger) throws HpcException {
		long start = System.currentTimeMillis();
		logger.info("Message task started: {} for taskID {}", name, taskMessage.getTaskId());

		try {
			task.execute(taskMessage);

		} catch (HpcException e) {
			logger.error("Message task failed: {} for taskID {} ", name, taskMessage.getTaskId(), e);

		} finally {
			long executionTime = System.currentTimeMillis() - start;
			logger.info("Message task completed: {} for taskId {} - Task execution time: {} milliseconds", name, taskMessage.getTaskId(), executionTime);
		}
	}

}

 