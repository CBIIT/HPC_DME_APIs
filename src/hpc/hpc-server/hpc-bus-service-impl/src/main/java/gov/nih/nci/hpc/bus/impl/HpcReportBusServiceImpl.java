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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import gov.nih.nci.hpc.dto.report.HpcReportsDTO;
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
	public HpcReportsDTO generateReport(HpcReportRequestDTO criteriaDTO) throws HpcException {
		if (criteriaDTO == null)
			throw new HpcException("Invalid criteria to generate report", HpcErrorType.INVALID_REQUEST_INPUT);

		if (criteriaDTO.getType() == null)
			throw new HpcException("Report type is missing", HpcErrorType.INVALID_REQUEST_INPUT);

		if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE)
				&& (criteriaDTO.getFromDate() == null || criteriaDTO.getToDate() == null))
			throw new HpcException("Date range is missing for USAGE_SUMMARY_BY_DATE_RANGE report",
					HpcErrorType.INVALID_REQUEST_INPUT);

		if(criteriaDTO.getFromDate() != null && !isDateValid(criteriaDTO.getFromDate(), "mm/dd/yyyy"))
			throw new HpcException("Invalid fromDate format. Valid format is mm/dd/yyyy",
					HpcErrorType.INVALID_REQUEST_INPUT);
		
		if(criteriaDTO.getToDate() != null && !isDateValid(criteriaDTO.getToDate(), "mm/dd/yyyy"))
			throw new HpcException("Invalid toDate format. Valid format is mm/dd/yyyy",
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
		Calendar fromcal = null;
		Calendar tocal = null; 
		try {
			if(criteriaDTO.getFromDate() != null && criteriaDTO.getToDate() != null)
			{
				SimpleDateFormat sdf = new SimpleDateFormat("mm/dd/yyyy");
				sdf.parse(criteriaDTO.getFromDate());
				fromcal = sdf.getCalendar();
				criteria.setFromDate(fromcal);
				sdf.parse(criteriaDTO.getToDate());
				tocal = sdf.getCalendar();
				criteria.setToDate(tocal);
			}
		} catch (ParseException e) {
			throw new HpcException(
					"Failed to parse date value "+e.getMessage(),
					HpcErrorType.INVALID_REQUEST_INPUT);
		}
		criteria.setUser(criteriaDTO.getUser());
		List<HpcReport> reports = reportService.generateReport(criteria);
		HpcReportsDTO dto = new HpcReportsDTO();
		for(HpcReport report : reports)
		{
			HpcReport dtoreport = new HpcReport();
			dtoreport.setDoc(report.getDoc());
			dtoreport.setFromDate(fromcal);
			dtoreport.setGeneratedOn(report.getGeneratedOn());
			dtoreport.setToDate(tocal);
			dtoreport.setType(report.getType());
			dtoreport.setUser(report.getUser());
			dtoreport.getReportEntries().addAll(report.getReportEntries());
			dto.getReports().add(dtoreport);
		}
		return dto;
	}

	private boolean isDateValid(String dateToValidate, String dateFromat){

		if(dateToValidate == null){
			return false;
		}

		SimpleDateFormat sdf = new SimpleDateFormat(dateFromat);
		sdf.setLenient(false);

		try {
			Date date = sdf.parse(dateToValidate);
		} catch (ParseException e) {

			e.printStackTrace();
			return false;
		}

		return true;
	}

}
