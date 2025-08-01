/**
 * HpcDataTransferProgressListener.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration;

import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * HPC Data Transfer Completion Listener Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcDataTransferProgressListener {
	/**
	 * Called when data transfer request (upload or download) completed.
	 * 
	 * @param bytesTransferred If known, total bytes transferred provided, otherwise
	 *                         null.
	 */
	public void transferCompleted(Long bytesTransferred);

	/**
	 * Called when data transfer request (upload or download) failed.
	 * 
	 * @param message An (optional) error message describing the transfer failure.
	 */
	public void transferFailed(String message);

	/**
	 * (Optional) Called when data transfer (upload or download) progressed.
	 * 
	 * @param bytesTransferred total bytes transferred so far.
	 */
	public default void transferProgressed(long bytesTransferred) {
	}

	/**
	 * (Optional) Setter for a CompletableFuture which allows a listener to cancel a
	 * transfer if needed.
	 * 
	 * @param completableFuture The CompletableFuture instance that can be used to
	 *                          cancel the transfer.
	 */
	public default void setCompletableFuture(CompletableFuture<?> completableFuture) {
	}
}
