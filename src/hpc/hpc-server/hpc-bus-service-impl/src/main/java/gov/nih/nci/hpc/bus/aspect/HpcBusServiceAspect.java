/**
 * HpcBusServiceAspect.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.aspect;

import gov.nih.nci.hpc.exception.HpcException;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Bus Services Aspect - implement cross cutting concerns:
 * 1. Basic business service profiler - log execution time.
 * 2. Exception logger - logging when exceptions are thrown by API impl.
 * 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

@Aspect
public class HpcBusServiceAspect
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
	@Pointcut("within(gov.nih.nci.hpc.bus.impl.*) && execution(* gov.nih.nci.hpc.bus.*.*(..))")
	private void allBusServices() 
	{
		// Intentionally left blank.
	}
	
    //---------------------------------------------------------------------//
    // Advices.
    //---------------------------------------------------------------------//
    
    /** 
     * Advice that logs business service execution time. 
     * 
     * @param joinPoint The join point.
     * @return The advised object return.
     * @throws Throwable The advised object exception.
     */
	@Around("allBusServices()")
	public Object profileService(ProceedingJoinPoint joinPoint) throws Throwable
    {
		long start = System.currentTimeMillis();
		logger.info(joinPoint.getSignature().toShortString() + " business service invoked.");
		
		try {
			 return joinPoint.proceed();
			 
		} finally {
			       long executionTime = System.currentTimeMillis() - start;
			       logger.debug(joinPoint.getSignature().toShortString() + " business service completed in " + 
			                    executionTime + " milliseconds.");
		}
    }
	
    /** 
     * Advice that logs business service exception. 
     * 
     * @param joinPoint The join point.
     * @param exception The exception to log.
     * @throws Throwable The advised object exception.
     */
	@AfterThrowing (pointcut = "allBusServices()", throwing = "exception")
    public void logException(JoinPoint joinPoint, HpcException exception) throws Throwable  
	{
		logger.error(joinPoint.getSignature().toShortString() + 
				     " business service error:  " + exception.getMessage(), exception); 
	}
}

 