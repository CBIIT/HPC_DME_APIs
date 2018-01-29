package gov.nih.nci.hpc.integration;

/**
 * Interface defining response objects returned by acceptsTransfers method of HpcDataTransferProxy
 * interface.
 */
public interface HpcTransferAcceptanceResponse {

  /**
   * Returns boolean indicating whether HpcDataTransferProxy instance can accept a transfer request.
   *
   * @return true if proxy can accept a transfer request, false otherwise.
   */
  default boolean canAcceptTransfer() {
    return true;
  }

  /**
   * Returns size of data transfer queue that HpcDataTransferProxy instance is managing.
   *
   * @return int conveying length of queue
   */
  default int getQueueSize() {
    return 0;
  }

}
