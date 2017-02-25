/**
 * HpcRestServiceAspect.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
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

@Aspect
public class HpcRestServiceAspect
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
     * A joint point for all rest services API.
     */
    @Pointcut("execution(* gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService.getCollection(..))")
    private void restService() {}
    
    //---------------------------------------------------------------------//
    // Advices.
    //---------------------------------------------------------------------//
    
	@Before("gov.nih.nci.hpc.ws.rs.impl.HpcRestServiceAspect.restService()")
    public void logBeforeV2(JoinPoint joinPoint) 
    {
        logger.error("ERAN: " + joinPoint.getSignature().toShortString());
        logger.error("ERAN: " + joinPoint.getSignature().toString());
        logger.error("ERAN: " + joinPoint.getSignature().toLongString());
    }
    
}

 