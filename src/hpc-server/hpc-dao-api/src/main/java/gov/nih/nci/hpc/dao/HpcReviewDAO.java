/**
 * HpcReviewDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.notification.HpcNotificationReview;
import gov.nih.nci.hpc.domain.review.HpcReviewEntry;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Review DAO Interface.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 * @version $Id$
 */

public interface HpcReviewDAO {

	/**
	 * Get review entries for the specified criteria.
	 *
	 * @param projectStatus
	 *            project status
	 * @param dataCurator
	 *            data curator
	 * @return List of review entries.
	 * @throws HpcException
	 *             on database error.
	 */
	public List<HpcReviewEntry> getReview(String projectStatus, String dataCurator) throws HpcException;

	/**
	 * Get count of catalog entries for the specified criteria.
	 *
	 * @param projectStatus
	 *            Project status
	 * @param dataCurator
	 *            Data curator
	 * @return Count of meta data review entries.
	 * @throws HpcException
	 *             on database error.
	 */
	public int getReviewCount(String projectStatus, String dataCurator) throws HpcException;

	/**
	 * Insert review notification record.
	 *
	 * @param reviewNotification
	 *            The review notfication record to insert.
	 * @throws HpcException
	 *             on database failure.
	 */
	public void addReviewNotification(HpcNotificationReview reviewNotification) throws HpcException;

	/**
	 * Get a list of data curators to send Annual Review notification
	 * 
	 * @return list of data curators
	 * @throws HpcException on database failure.
	 */
	public List<String> getCuratorsForAnnualReview() throws HpcException;
	
	/**
	 * Get a list of data curators to send Annual Review Reminder notification
	 * 
	 * @return list of data curators
	 * @throws HpcException on database failure.
	 */
	public List<String> getCuratorsForAnnualReviewReminder() throws HpcException;
}
