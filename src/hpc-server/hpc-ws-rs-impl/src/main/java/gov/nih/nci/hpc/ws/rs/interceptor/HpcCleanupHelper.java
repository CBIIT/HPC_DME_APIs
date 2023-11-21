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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import gov.nih.nci.hpc.bus.HpcSystemBusService;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.impl.HpcDataManagementRestServiceImpl;

/**
 * <p>
 * HPC Cleanup Helper
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcCleanupHelper {
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
   * Default Constructor for Spring Dependency Injection.
   * 
   */
  private HpcCleanupHelper() {}

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  /**
   * Clean up after completion of API call
   *
   * @param message The message received in the interceptor chain
   * @param fault True if this is handling a fault, or false otherwise.
   */
  public void cleanup(Message message, boolean fault) {
    // Close the connection to Data Management.
    systemBusService.closeConnection();

    // Delete file staged for sync download request.
    Object fileObj = message.getContextualProperty(
        HpcDataManagementRestServiceImpl.DATA_OBJECT_DOWNLOAD_FILE_MC_ATTRIBUTE);
    if (fileObj != null && fileObj instanceof File) {
      File file = (File) fileObj;
      if (!FileUtils.deleteQuietly(file)) {
        logger.error("Failed to delete a file: " + file.getAbsolutePath());
      }
    }

    // Complete a sync download task.
    Object taskId = message.getContextualProperty(
        HpcDataManagementRestServiceImpl.DATA_OBJECT_DOWNLOAD_TASK_ID_MC_ATTRIBUTE);
    if (taskId != null) {
      try {
        systemBusService.completeSynchronousDataObjectDownloadTask(taskId.toString(),
            fault ? HpcDownloadResult.FAILED : HpcDownloadResult.COMPLETED);
      } catch (HpcException e) {
        logger.error("Failed to complete a sync download task", e);
      }
    }
  }
}
