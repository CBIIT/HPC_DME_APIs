/**
 * HpcCleanupInterceptor.java
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
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Cleanup Interceptor.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcCleanupInterceptor extends AbstractPhaseInterceptor<Message> {
  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The Cleanup helper instance
  @Autowired
  private HpcCleanupHelper cleanupHelper = null;

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /**
   * Default Constructor.
   * 
   */
  private HpcCleanupInterceptor() {
    super(Phase.SEND_ENDING);
  }

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // AbstractPhaseInterceptor<Message> Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public void handleMessage(Message message) {
    cleanupHelper.cleanup(message, false);
  }
}
