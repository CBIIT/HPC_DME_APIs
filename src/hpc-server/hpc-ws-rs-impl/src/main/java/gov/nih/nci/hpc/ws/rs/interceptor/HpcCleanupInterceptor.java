/**
 * HpcCleanupInterceptor.java
 *
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.interceptor;

import java.io.File;
import org.apache.commons.io.FileUtils;
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
 * HPC Cleanup Interceptor.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcCleanupInterceptor extends AbstractPhaseInterceptor<Message> {
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
  private HpcCleanupInterceptor() {
    super(Phase.SEND_ENDING);
  }

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  /**
   * Delete the file staged for synchronous download.
   *
   * @param fileObj The file to delete
   * @param logger Logger to log error if deletion failed.
   */
  static public void deleteSynchronousDownloadFile(Object fileObj, Logger logger) {
    if (fileObj != null && fileObj instanceof File) {
      File file = (File) fileObj;
      if (!FileUtils.deleteQuietly(file)) {
        logger.error("Failed to delete a file: " + file.getAbsolutePath());
      }
    }
  }

  // ---------------------------------------------------------------------//
  // AbstractPhaseInterceptor<Message> Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public void handleMessage(Message message) {
    // Close the connection to Data Management.
    systemBusService.closeConnection();

    // Clean up files returned by the data object download service.
    deleteSynchronousDownloadFile(message.getContextualProperty(
        HpcDataManagementRestServiceImpl.DATA_OBJECT_DOWNLOAD_FILE_MC_ATTRIBUTE), logger);
  }
}
