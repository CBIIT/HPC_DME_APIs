/**
 * HpcReportRestServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import gov.nih.nci.hpc.bus.HpcReportBusService;
import gov.nih.nci.hpc.dto.report.HpcReportRequestDTO;
import gov.nih.nci.hpc.dto.report.HpcReportsDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcReportRestService;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Notification REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public class HpcReportRestServiceImpl extends HpcRestServiceImpl implements HpcReportRestService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Report Business Service instance.
	@Autowired
	private HpcReportBusService reportBusService = null;

	// The Logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 * @throws HpcException
	 *             Constructor is disabled.
	 */
	private HpcReportRestServiceImpl() throws HpcException {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcNotificationRestService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public Response generateReport(HpcReportRequestDTO reportDTO) {
		logger.info("Invoking RS: POST /report: " + reportDTO);
		HpcReportsDTO report = null;
		try {
			 report = reportBusService.generateReport(reportDTO);

		} catch (HpcException e) {
			logger.error("RS: POST /notification/{nciUserId} failed:", e);
			return errorResponse(e);
		}

		return okResponse(report , true);
	}
}
