/**
 * HpcReviewBusService.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.review.HpcReviewDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Review Business Service Interface.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public interface HpcReviewBusService {
	/**
	 * Get review entry matching the criteria.
	 *
	 * @param projectStatus
	 *            project status
	 * @param dataCurator
	 *            data curator
	 * @return The review DTO.
	 * @throws HpcException
	 *             on service failure.
	 */
	public HpcReviewDTO getReview(String projectStatus, String dataCurator) throws HpcException;

	/**
	 * Send the annual review notifications.
	 *
	 * @throws HpcException on service failure.
	 */
	public void sendAnnualReview() throws HpcException;
	
	/**
	 * Send the annual review reminder notifications.
	 *
	 * @throws HpcException on service failure.
	 */
	public void sendAnnualReviewReminder() throws HpcException;
	
	/**
	 * Send Review Reminder Notification
	 *
	 * @param nciUserId
	 *            The user Id to send the notification to
	 * @throws HpcException
	 *             on service failure.
	 */
	public void sendReminder(String nciUserId) throws HpcException;

}
