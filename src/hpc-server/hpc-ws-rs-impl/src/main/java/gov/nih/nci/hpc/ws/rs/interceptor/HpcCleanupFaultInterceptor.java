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
import gov.nih.nci.hpc.bus.HpcSystemBusService;
import gov.nih.nci.hpc.ws.rs.impl.HpcDataManagementRestServiceImpl;

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

  // The System Business Service instance.
  @Autowired
  private HpcSystemBusService systemBusService = null;

  // The logger instance.
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
    // Close the connection to Data Management.
    systemBusService.closeConnection();

    // Clean up files returned by the data object download service.
    HpcCleanupInterceptor.deleteSynchronousDownloadFile(message.getContextualProperty(
        HpcDataManagementRestServiceImpl.DATA_OBJECT_DOWNLOAD_FILE_MC_ATTRIBUTE), logger);
  }
}
