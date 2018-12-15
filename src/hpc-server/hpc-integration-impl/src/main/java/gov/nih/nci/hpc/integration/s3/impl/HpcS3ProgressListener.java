/**
 * HpcS3ProgressListener.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.s3.impl;

import java.net.HttpURLConnection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;

/**
 * HPC S3 Progress Listener.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcS3ProgressListener implements ProgressListener {
  //---------------------------------------------------------------------//
  // Constants
  //---------------------------------------------------------------------//

  // The transfer progress logging rate (in bytes). Log transfer progress every 10MB.
  private static long TRANSFER_LOGGING_RATE = 1024 * 1024 * 10;
  private static long MB = 1024 * 1024;

  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  // HPC progress listener
  HpcDataTransferProgressListener progressListener = null;

  // Bytes transferred and logged.
  AtomicLong bytesTransferred = new AtomicLong(0);
  long bytesTransferredLogged = 0;

  // Transfer source and/or destination (for logging purposed)
  String transferSourceDestination = null;

  // Keep track if we reported a failure.
  AtomicBoolean transferFailedReported = new AtomicBoolean(false);
  
  // The source URL connection (when we transfer from URL).
  HttpURLConnection sourceConnection = null;

  // Logger
  private final Logger logger = LoggerFactory.getLogger(getClass().getName());

  //---------------------------------------------------------------------//
  // Constructors
  //---------------------------------------------------------------------//

  /**
   * Constructor.
   *
   * @param progressListener The HPC progress listener.
   * @param transferSourceDestination The transfer source and destination (for logging progress).
   * @throws HpcException if no progress listener provided.
   */
  public HpcS3ProgressListener(
      HpcDataTransferProgressListener progressListener, String transferSourceDestination)
      throws HpcException {
    if (progressListener == null) {
      throw new HpcException("Null progress listener", HpcErrorType.UNEXPECTED_ERROR);
    }
    this.progressListener = progressListener;
    this.transferSourceDestination = transferSourceDestination;
  }

  /**
   * Default Constructor.
   *
   * @throws HpcException Constructor is disabled.
   */
  @SuppressWarnings("unused")
  private HpcS3ProgressListener() throws HpcException {
    throw new HpcException("Constructor Disabled", HpcErrorType.UNEXPECTED_ERROR);
  }

  //---------------------------------------------------------------------//
  // Methods
  //---------------------------------------------------------------------//

  @Override
  public void progressChanged(ProgressEvent event) {
    if (event.getBytesTransferred() > 0) {
      bytesTransferred.getAndAdd(event.getBytesTransferred());
      if (bytesTransferredLogged == 0) {
        bytesTransferredLogged = bytesTransferred.get();
        logger.info(
            "S3 transfer [{}] started. {} bytes transferred so far",
            transferSourceDestination,
            bytesTransferredLogged);
      } else if (bytesTransferred.get() - bytesTransferredLogged >= TRANSFER_LOGGING_RATE) {
        bytesTransferredLogged = bytesTransferred.get();
        logger.info(
            "S3 transfer [{}] in progress. {}MB transferred so far",
            transferSourceDestination,
            bytesTransferredLogged / MB);
      }
    }

    switch (event.getEventType()) {
      case TRANSFER_COMPLETED_EVENT:
        logger.info(
            "S3 transfer [{}] completed. {} bytes transferred",
            transferSourceDestination,
            bytesTransferred);
        progressListener.transferCompleted(bytesTransferred.get());
        
        // If we transfer from a URL. Disconnect the connection.
        if(sourceConnection != null) {
          sourceConnection.disconnect();
        }
        break;

      case TRANSFER_FAILED_EVENT:
      case TRANSFER_CANCELED_EVENT:
        boolean invokeTransferFailed = transferFailedReported.getAndSet(true);
        logger.info(
            "S3 transfer [{}] failed. {}MB transferred. progress event = {}",
            transferSourceDestination,
            bytesTransferred.get() / MB,
            event.getEventType());
        if (invokeTransferFailed) {
          progressListener.transferFailed("S3 event - " + event.toString());
          
          // If we transfer from a URL. Disconnect the connection.
          if(sourceConnection != null) {
            sourceConnection.disconnect();
          }
        }
        break;

      default:
        break;
    }
  }
  
  /**
   * Set the source URL connection. The sole purpose of this method is to ensure the URL connection 
   * stays open for the duration of the download. Once it's done, we'll close that connection
   *
   * @param sourceConnection The source URL connection.
   */
  public void setSourceConnection(HttpURLConnection sourceConnection) {
    this.sourceConnection = sourceConnection;
  }
}
