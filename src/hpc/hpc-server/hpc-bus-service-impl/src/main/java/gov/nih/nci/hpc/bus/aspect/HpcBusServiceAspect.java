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
	private void busServices() 
	{
		// Intentionally left blank.
	}
	
    /** 
     * Join Point for all system business services that are implemented in 
     * gov.nih.nci.hpc.bus.impl.HpcSystemBusServiceImpl and annotated with @SystemBusServiceImpl
     */
	@Pointcut("within(gov.nih.nci.hpc.bus.impl.HpcSystemBusServiceImpl) && annotation(SystemBusServiceImpl)")
	private void systemBusServices() 
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
	@Around("busServices()")
	public Object profileService(ProceedingJoinPoint joinPoint) throws Throwable
    {
		long start = System.currentTimeMillis();
		String businessService = joinPoint.getSignature().toShortString();
		logger.info(businessService + " business service invoked.");
		
		try {
			 return joinPoint.proceed();
			 
		} finally {
			       long executionTime = System.currentTimeMillis() - start;
			       logger.debug(businessService + " business service completed in " + 
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
	@AfterThrowing (pointcut = "busServices()", throwing = "exception")
    public void logException(JoinPoint joinPoint, HpcException exception) throws Throwable  
	{
		logger.error(joinPoint.getSignature().toShortString() + 
				     " business service error:  " + exception.getMessage(), exception); 
	}
	
    /** 
     * Advice that set up the system account as the request invoker
     * 
     * @param joinPoint The join point.
     * @return The advised object return.
     * @throws Throwable The advised object exception.
     */
	@Around("systemBusServices()")
	public Object setSystemRequestInvoker(ProceedingJoinPoint joinPoint) throws Throwable
    {
		logger.info("ERAN: set system request invoker");
		
		try {
			 return joinPoint.proceed();
			 
		} finally {
			       logger.info("ERAN: unset system request invoker"); 
		}
    }
}

 