/**
 * HpcBusServiceAspect.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.aspect;

import java.util.Optional;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC Bus Services Aspect - implement cross cutting concerns: 1. Basic business service profiler -
 * log execution time. 2. Exception logger - logging when exceptions are thrown by Bus Services API
 * impl. 3. Notify System Administrator if an error occurred with an integrated system (iRODS, LDAP,
 * CLEVERSAFE, CLOUDIAN, AWS, GLOBUS, ORACLE)
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@Aspect
public class HpcBusServiceAspect {
  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The notification service instance.
  @Autowired
  private HpcNotificationService notificationService = null;

  // The security service instance.
  @Autowired
  private HpcSecurityService securityService = null;

  // LDAP authentication on/off switch.
  @Value("${hpc.bus.ldapAuthentication}")
  private Boolean ldapAuthentication = null;

  // The logger instance.
  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcBusServiceAspect() {}

  // ---------------------------------------------------------------------//
  // Pointcuts.
  // ---------------------------------------------------------------------//

  /**
   * Join Point for all business services that are defined by an interface in the
   * gov.nih.nci.hpc.bus package, and implemented by a concrete class in gov.nih.nci.hpc.bus.impl
   */
  @Pointcut("within(gov.nih.nci.hpc.bus.impl.*) && execution(* gov.nih.nci.hpc.bus.*.*(..))")
  private void busServices() {
    // Intentionally left blank.
  }

  // ---------------------------------------------------------------------//
  // Advices.
  // ---------------------------------------------------------------------//

  /**
   * Advice that logs business service execution time.
   *
   * @param joinPoint The join point.
   * @return The advised object return.
   * @throws Throwable The advised object exception.
   */
  @Around("busServices()")
  public Object profileService(ProceedingJoinPoint joinPoint) throws Throwable {
    long start = System.currentTimeMillis();
    String businessService = joinPoint.getSignature().toShortString();
    logger.info("{} business service invoked.", businessService);

    try {
      return joinPoint.proceed();

    } finally {
      long executionTime = System.currentTimeMillis() - start;
      logger.debug("{} business service completed in {} milliseconds.", businessService,
          executionTime);
    }
  }

  /**
   * Advice that logs business service exception.
   *
   * @param joinPoint The join point.
   * @param exception The exception to log.
   */
  @AfterThrowing(pointcut = "busServices()", throwing = "exception")
  public void logException(JoinPoint joinPoint, HpcException exception) {
    String businessService = joinPoint.getSignature().toShortString();
    logger.error("{} business service error: {}", businessService, exception.getMessage(),
        exception);
  }

  /**
   * Advice that alerts a system administrator of a problem with an integrated system.
   *
   * @param joinPoint The join point.
   * @param exception The exception to log.
   */
  @AfterThrowing(pointcut = "busServices()", throwing = "exception")
  public void notifySystemAdmin(JoinPoint joinPoint, HpcException exception) {
    notificationService.sendNotification(exception);
  }

  /**
   * Advice that execute a business service using system account.
   *
   * @param joinPoint The join point.
   * @return The advised object return.
   * @throws Throwable The advised object exception.
   */
  @Around("busServices() && @annotation(gov.nih.nci.hpc.bus.aspect.HpcExecuteAsSystemAccount)")
  public Object executeAsSystemAccount(ProceedingJoinPoint joinPoint) throws Throwable {
    return securityService.executeAsSystemAccount(Optional.of(ldapAuthentication), () -> {
      try {
        return joinPoint.proceed();
      } catch (HpcException e) {
        throw e;
      } catch (Throwable t) {
        throw new HpcException("Failed to execute as system account", t);
      }
    });
  }
}
