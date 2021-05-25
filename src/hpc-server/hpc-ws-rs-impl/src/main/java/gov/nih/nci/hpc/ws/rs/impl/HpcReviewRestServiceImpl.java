/**
 * HpcReviewRestServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.ws.rs.impl;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import gov.nih.nci.hpc.bus.HpcReviewBusService;
import gov.nih.nci.hpc.dto.review.HpcReviewDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcReviewRestService;

/**
 * HPC Review REST Service Implementation.
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */
public class HpcReviewRestServiceImpl extends HpcRestServiceImpl implements HpcReviewRestService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Data Review Business Service instance.
	@Autowired
	private HpcReviewBusService reviewBusService = null;

	// ---------------------------------------------------------------------//
	// constructors
	// ---------------------------------------------------------------------//

	/** Constructor for Spring Dependency Injection. */
	private HpcReviewRestServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcDataTieringRestService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public Response queryReview(String projectStatus, String dataCurator) {
		HpcReviewDTO reviews = null;
		try {
			reviews = reviewBusService.getReview(projectStatus, dataCurator);

		} catch (HpcException e) {
			return errorResponse(e);
		}

		return okResponse(!CollectionUtils.isEmpty(reviews.getReviewEntries()) ? reviews : null, true);
	}

	@Override
	public Response sendReminder(String nciUserId) {
		try {
			reviewBusService.sendReminder(nciUserId);

		} catch (HpcException e) {
			return errorResponse(e);
		}

		return okResponse(null, false);
	}
}
