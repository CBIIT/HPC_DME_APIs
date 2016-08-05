/**
 * HpcReportBusServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.bus.HpcReportBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.dto.report.HpcReportDTO;
import gov.nih.nci.hpc.dto.report.HpcReportRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcReportService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * <p>
 * HPC Report Business Service Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id:$
 */

public class HpcReportBusServiceImpl implements HpcReportBusService {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// Application service instances.

	@Autowired
	private HpcReportService reportService = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 * 
	 */
	private HpcReportBusServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcNotificationBusService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public HpcReportDTO generateReport(HpcReportRequestDTO criteriaDTO) throws HpcException {
		if (criteriaDTO == null)
			throw new HpcException("Invalid criteria to generate report", HpcErrorType.INVALID_REQUEST_INPUT);

		if (criteriaDTO.getType() == null)
			throw new HpcException("Report type is missing", HpcErrorType.INVALID_REQUEST_INPUT);

		if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE)
				&& (criteriaDTO.getFromDate() == null || criteriaDTO.getToDate() == null))
			throw new HpcException("Date range is missing for USAGE_SUMMARY_BY_DATE_RANGE report",
					HpcErrorType.INVALID_REQUEST_INPUT);

		if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC)
				&& (criteriaDTO.getDoc() == null || criteriaDTO.getDoc().isEmpty()))
			throw new HpcException("DOC value is missing for USAGE_SUMMARY_BY_DOC report",
					HpcErrorType.INVALID_REQUEST_INPUT);

		if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE) && (criteriaDTO.getDoc() == null
				|| criteriaDTO.getDoc().isEmpty() || criteriaDTO.getFromDate() == null || criteriaDTO.getToDate() == null))
			throw new HpcException("DOC value or date range is missing for USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE report",
					HpcErrorType.INVALID_REQUEST_INPUT);

		if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER)
				&& (criteriaDTO.getUser() == null || criteriaDTO.getUser().isEmpty()))
			throw new HpcException("UserId value is missing for USAGE_SUMMARY_BY_USER report",
					HpcErrorType.INVALID_REQUEST_INPUT);

		if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE) && (criteriaDTO.getUser() == null
				|| criteriaDTO.getUser().isEmpty() || criteriaDTO.getFromDate() == null || criteriaDTO.getToDate() == null))
			throw new HpcException(
					"UserId value or date range is missing for USAGE_SUMMARY_BY_USER_BY_DATE_RANGE report",
					HpcErrorType.INVALID_REQUEST_INPUT);

		HpcReportCriteria criteria = new HpcReportCriteria(); 
		criteria.setDoc(criteriaDTO.getDoc());
		criteria.setType(criteriaDTO.getType());
		criteria.setFromDate(criteriaDTO.getFromDate());
		criteria.setToDate(criteriaDTO.getToDate());
		criteria.setUser(criteriaDTO.getUser());
		HpcReport report = reportService.generateReport(criteria);
		HpcReportDTO dto = new HpcReportDTO();
		dto.setDoc(report.getDoc());
		dto.setFromDate(report.getFromDate());
		dto.setGeneratedOn(report.getGeneratedOn());
		dto.setToDate(report.getToDate());
		dto.setType(report.getType());
		dto.setUser(report.getUser());
		dto.getReportEntries().addAll(report.getReportEntries());
		return dto;
	}

}
