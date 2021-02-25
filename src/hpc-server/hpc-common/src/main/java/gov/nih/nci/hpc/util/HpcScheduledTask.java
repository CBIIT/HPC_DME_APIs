/**
 * HpcScheduledTask.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.util;

import org.slf4j.Logger;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Scheduled task interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

@FunctionalInterface
public interface HpcScheduledTask 
{   
    /**
     * The scheduled task implementation
     *
     * @throws HpcException on task execution failure.
     */
    public void execute() throws HpcException;
    
    /**
	 * Execute a scheduled task.
	 *
	 * @param name The task name.
	 * @param task The task to execute.
	 */
	public static void execute(String name, HpcScheduledTask task, Logger logger) {
		long start = System.currentTimeMillis();
		logger.info("Scheduled task started: {}", name);

		try {
			task.execute();

		} catch (HpcException e) {
			logger.error("Scheduled task failed: " + name, e);

		} finally {
			long executionTime = System.currentTimeMillis() - start;
			logger.info("Scheduled task completed: {} - Task execution time: {} milliseconds", name, executionTime);
		}
	}

}

 