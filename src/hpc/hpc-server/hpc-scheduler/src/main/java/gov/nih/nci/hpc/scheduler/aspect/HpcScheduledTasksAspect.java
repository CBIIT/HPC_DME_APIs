/**
 * HpcScheduledTasksAspect.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.scheduler.aspect;

import gov.nih.nci.hpc.exception.HpcException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Scheduled Task Aspect - implement cross cutting concerns:
 * 1. Basic scheduled task profiler - log execution time.
 * 2. Setup service account as the request invoker and close data management connection on completion of services. 
 * 3. Exception logger - logging when exceptions are thrown scheduled tasks.
 * 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@Aspect
public class HpcScheduledTasksAspect
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Pointcuts.
    //---------------------------------------------------------------------//
	
    /** 
     * Join Point for all business services that are defined by an interface
     * in the gov.nih.nci.hpc.bus package, and implemented by a concrete class
     * in gov.nih.nci.hpc.bus.impl
     */
	@Pointcut("execution(* gov.nih.nci.hpc.scheduler.impl.*.*(..))")
	private void allScheduledTasks() 
	{
		// Intentionally left blank.
	}
	
    //---------------------------------------------------------------------//
    // Advices.
    //---------------------------------------------------------------------//
    
    /** 
     * Advice that performs the following on all scheduled task.
     * 1. Basic scheduled task profiler - log execution time.
     * 2. Setup service account as the request invoker and close data management connection on completion of services. 
     * 2. Exception logger - logging when exceptions are thrown scheduled tasks.
     * 
     * @param joinPoint The join point.
     * @return The advised object return.
     * @throws Throwable The advised object exception.
     */
	@Around("allScheduledTasks()")
	public void scheduledTaskAdvice(ProceedingJoinPoint joinPoint) throws Throwable
    {
		long start = System.currentTimeMillis();
		String scheduledTask = joinPoint.getSignature().toShortString();
		logger.info("ERAN :" + scheduledTask + " scheduled task started.");
		
		try {
			 joinPoint.proceed();
			 
		} catch(HpcException e) {
			    logger.error("ERAN :" +  scheduledTask  + " scheduled task failed:  " + e.getMessage(), e); 
			 
		} finally {
			       long executionTime = System.currentTimeMillis() - start;
			       logger.info("ERAN :" + scheduledTask + " scheduled task completed in " + 
			                    executionTime + " milliseconds.");
		}
    }
}

 