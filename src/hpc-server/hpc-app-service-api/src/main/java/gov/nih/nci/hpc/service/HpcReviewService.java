/**
 * HpcReviewService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.review.HpcReviewEntry;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * HPC Review Application Service Interface.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */
public interface HpcReviewService {
	/**
	 * Get review entry matching the criteria.
	 *
	 * @param projectStatus
	 *            project status
	 * @param dataCurator
	 *            data curator
	 * @return A list of review entries.
	 * @throws HpcException
	 *             on service failure.
	 */
	public List<HpcReviewEntry> getReview(String projectStatus, String dataCurator) throws HpcException;

	/**
	 * Get count of review matching the criteria.
	 *
	 * @param projectStatus
	 *            project status
	 * @param dataCurator
	 *            data curator
	 * @return The count of review entries matching the query.
	 * @throws HpcException
	 *             on service failure.
	 */
	public int getReviewCount(String projectStatus, String dataCurator) throws HpcException;

	/**
	 * Add a review notification sent.
	 *
	 * @param userId
	 *            The user ID.
	 * @throws HpcException
	 *             on service failure.
	 */
	public void addReviewSentNotification(String userId) throws HpcException;

	/**
	 * Add a review reminder notification sent.
	 *
	 * @param userId
	 *            The user ID.
	 * @throws HpcException
	 *             on service failure.
	 */
	public void addReviewReminderSentNotification(String userId) throws HpcException;

	/**
	 * Get a list of data curators to send Annual Review notification
	 * 
	 * @return list of data curators
	 */
	public List<String> getCuratorsForAnnualReview() throws HpcException;
	
	/**
	 * Get a list of data curators to send Annual Review Reminder notification
	 * 
	 * @return list of data curators
	 */
	public List<String> getCuratorsForAnnualReviewReminder() throws HpcException;
}
