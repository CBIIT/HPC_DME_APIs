/**
 * HpcCleanupFaultInterceptor.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.interceptor;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Cleanup Fault Interceptor.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcCleanupFaultInterceptor extends AbstractPhaseInterceptor<Message> {
  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The Cleanup helper instance
  @Autowired
  private HpcCleanupHelper cleanupHelper = null;

  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
  
  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /**
   * Default Constructor.
   * 
   */
  private HpcCleanupFaultInterceptor() {
    super(Phase.PREPARE_SEND);
  }

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // AbstractPhaseInterceptor<Message> Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public void handleMessage(Message message) {
    // Intentionally left empty.
  }

  @Override
  public void handleFault(Message message) {
    logger.error("ERAN: Fault interceptor - out - " + message.toString());
    cleanupHelper.cleanup(message, true);
  }
}
