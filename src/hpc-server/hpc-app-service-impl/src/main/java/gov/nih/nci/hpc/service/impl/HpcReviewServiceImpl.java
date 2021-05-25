/**
 * HpcReviewServiceImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.nih.nci.hpc.dao.HpcReviewDAO;
import gov.nih.nci.hpc.domain.notification.HpcEventType;
import gov.nih.nci.hpc.domain.notification.HpcNotificationReview;
import gov.nih.nci.hpc.domain.review.HpcReviewEntry;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;
import gov.nih.nci.hpc.service.HpcReviewService;

import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * HPC Review Application Service Implementation.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcReviewServiceImpl implements HpcReviewService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Review DAO.
	@Autowired
	private HpcReviewDAO reviewDAO = null;

	// The Data Management Proxy instance.
	@Autowired
	private HpcDataManagementProxy dataManagementProxy = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcReviewServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcReviewService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public List<HpcReviewEntry> getReview(String projectStatus, String dataCurator) throws HpcException {

		List<HpcReviewEntry> reviewEntries = reviewDAO.getReview(projectStatus, dataCurator);
		for (HpcReviewEntry reviewEntry : reviewEntries) {
			reviewEntry.setPath(toRelativePath(reviewEntry.getPath()));
		}
		return reviewEntries;
	}

	@Override
	public int getReviewCount(String projectStatus, String dataCurator) throws HpcException {

		return reviewDAO.getReviewCount(projectStatus, dataCurator);
	}

	@Override
	public void addReviewSentNotification(String userId) throws HpcException {
		if (userId == null) {
			return;
		}

		HpcNotificationReview reviewNotification = new HpcNotificationReview();
		reviewNotification.setUserId(userId);
		reviewNotification.setEventType(HpcEventType.REVIEW_SENT);
		reviewNotification.setDelivered(Calendar.getInstance());

		try {
			reviewDAO.addReviewNotification(reviewNotification);

		} catch (HpcException e) {
			logger.error("Failed to insert a review notification record", e);
		}
	}

	@Override
	public void addReviewReminderSentNotification(String userId) throws HpcException {
		if (userId == null) {
			return;
		}

		HpcNotificationReview reviewNotification = new HpcNotificationReview();
		reviewNotification.setUserId(userId);
		reviewNotification.setEventType(HpcEventType.REVIEW_REMINDER_SENT);
		reviewNotification.setDelivered(Calendar.getInstance());

		try {
			reviewDAO.addReviewNotification(reviewNotification);

		} catch (HpcException e) {
			logger.error("Failed to insert a review notification record", e);
		}
	}
	
	@Override
	public List<String> getCuratorsForAnnualReview() throws HpcException {
		return reviewDAO.getCuratorsForAnnualReview();
	}

	@Override
	public List<String> getCuratorsForAnnualReviewReminder() throws HpcException {
		return reviewDAO.getCuratorsForAnnualReviewReminder();
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Convert an absolute path, to relative path.
	 *
	 * @param path
	 *            The absolute paths.
	 * @return The relative paths.
	 */
	public String toRelativePath(String path) {
		return dataManagementProxy.getRelativePath(path);
	}

}
