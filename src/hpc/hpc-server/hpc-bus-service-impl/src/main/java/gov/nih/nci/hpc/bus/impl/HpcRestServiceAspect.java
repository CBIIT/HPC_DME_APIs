/**
 * HpcRestServiceAspect.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Rest Services Aspect - implement cross cutting concerns:
 * 1. Basic service execution profiler.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcRestServiceAspect
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Advices.
    //---------------------------------------------------------------------//
    
	public void profile(ProceedingJoinPoint pjp) throws Throwable
    {
        logger.error("ERAN: " + pjp.getSignature().toShortString());
        logger.error("ERAN: " + pjp.getSignature().toString());
        logger.error("ERAN: " + pjp.getSignature().toLongString());
        pjp.proceed();
        logger.error("ERAN: AFTER");
    }
	
	public void logBefore(JoinPoint joinPoint) {
		logger.error("ERAN: " + joinPoint.getSignature().toShortString());
	}
    
}

 