/**
 * HpcReportBusServiceImpl.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.bus.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import gov.nih.nci.hpc.bus.HpcReportBusService;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.domain.report.HpcReportEntry;
import gov.nih.nci.hpc.domain.report.HpcReportEntryAttribute;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.dto.report.HpcReportDTO;
import gov.nih.nci.hpc.dto.report.HpcReportEntryDTO;
import gov.nih.nci.hpc.dto.report.HpcReportRequestDTO;
import gov.nih.nci.hpc.dto.report.HpcReportsDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcReportService;
import gov.nih.nci.hpc.service.HpcSecurityService;

/**
 * HPC Report Business Service Implementation.
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */
public class HpcReportBusServiceImpl implements HpcReportBusService {
  // ---------------------------------------------------------------------//
  // Instance members
  // ---------------------------------------------------------------------//

  // The Report Application service instance.
  @Autowired
  private HpcReportService reportService = null;

  @Autowired
  private HpcSecurityService securityService = null;

  // ---------------------------------------------------------------------//
  // Constructors
  // ---------------------------------------------------------------------//

  /** Constructor for Spring Dependency Injection. */
  private HpcReportBusServiceImpl() {}

  // ---------------------------------------------------------------------//
  // Methods
  // ---------------------------------------------------------------------//

  // ---------------------------------------------------------------------//
  // HpcReportBusService Interface Implementation
  // ---------------------------------------------------------------------//

  @Override
  public HpcReportsDTO generateReport(HpcReportRequestDTO criteriaDTO) throws HpcException {
    HpcRequestInvoker invoker = securityService.getRequestInvoker();
    if (invoker == null) {
      throw new HpcException("Null request invoker", HpcErrorType.UNEXPECTED_ERROR);
    }

    if ((invoker.getUserRole().equals(HpcUserRole.GROUP_ADMIN)
        || invoker.getUserRole().equals(HpcUserRole.USER)) && criteriaDTO.getDoc() != null) {
      for (String doc : criteriaDTO.getDoc()) {
        if (!doc.equals(invoker.getNciAccount().getDoc()))
          throw new HpcException("Unauthorized access to DOC report for: " + criteriaDTO.getDoc(),
              HpcErrorType.UNAUTHORIZED_REQUEST);
      }
    }

    if (criteriaDTO == null)
      throw new HpcException("Invalid criteria to generate report",
          HpcErrorType.INVALID_REQUEST_INPUT);

    if (criteriaDTO.getType() == null)
      throw new HpcException("Report type is missing", HpcErrorType.INVALID_REQUEST_INPUT);

    if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY)
        && (criteriaDTO.getFromDate() != null || criteriaDTO.getToDate() != null
            || (criteriaDTO.getDoc() != null && !criteriaDTO.getDoc().isEmpty())
            || (criteriaDTO.getUser() != null && !criteriaDTO.getUser().isEmpty())))
      throw new HpcException(
          "Invalid request for USAGE_SUMMARY report. User, Doc, FromDate, ToDate are not allowed.",
          HpcErrorType.INVALID_REQUEST_INPUT);

    if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DATE_RANGE)) {
      if (criteriaDTO.getFromDate() == null || criteriaDTO.getToDate() == null)
        throw new HpcException("Date range is missing for USAGE_SUMMARY_BY_DATE_RANGE report",
            HpcErrorType.INVALID_REQUEST_INPUT);

      if (criteriaDTO.getDoc() != null && !criteriaDTO.getDoc().isEmpty())
        throw new HpcException(
            "Invalid request for USAGE_SUMMARY_BY_DATE_RANGE report. Doc is not allowed",
            HpcErrorType.INVALID_REQUEST_INPUT);
    }
    if (criteriaDTO.getFromDate() != null && !isDateValid(criteriaDTO.getFromDate(), "mm/dd/yyyy"))
      throw new HpcException("Invalid fromDate format. Valid format is mm/dd/yyyy",
          HpcErrorType.INVALID_REQUEST_INPUT);

    if (criteriaDTO.getToDate() != null && !isDateValid(criteriaDTO.getToDate(), "mm/dd/yyyy"))
      throw new HpcException("Invalid toDate format. Valid format is mm/dd/yyyy",
          HpcErrorType.INVALID_REQUEST_INPUT);

    if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE)) {
      if (criteriaDTO.getDoc() == null || criteriaDTO.getDoc().isEmpty())
        throw new HpcException("DOC is missing for USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE report",
            HpcErrorType.INVALID_REQUEST_INPUT);

      if (criteriaDTO.getFromDate() == null || criteriaDTO.getToDate() == null)
        throw new HpcException(
            "Date range is missing for USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE report",
            HpcErrorType.INVALID_REQUEST_INPUT);

      if (criteriaDTO.getUser() != null && !criteriaDTO.getUser().isEmpty())
        throw new HpcException("User is not allowed for USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE report",
            HpcErrorType.INVALID_REQUEST_INPUT);
    }

    if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_USER_BY_DATE_RANGE)) {
      if (criteriaDTO.getUser() == null || criteriaDTO.getUser().isEmpty())
        throw new HpcException(
            "UserId value is missing for USAGE_SUMMARY_BY_USER_BY_DATE_RANGE report",
            HpcErrorType.INVALID_REQUEST_INPUT);

      if (criteriaDTO.getFromDate() == null || criteriaDTO.getToDate() == null)
        throw new HpcException(
            "date range is missing for USAGE_SUMMARY_BY_USER_BY_DATE_RANGE report",
            HpcErrorType.INVALID_REQUEST_INPUT);

      if (criteriaDTO.getDoc() != null && !criteriaDTO.getDoc().isEmpty())
        throw new HpcException("DOC is not allowed for USAGE_SUMMARY_BY_USER_BY_DATE_RANGE report",
            HpcErrorType.INVALID_REQUEST_INPUT);
    }

    if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH)) {
      if (criteriaDTO.getPath() == null || criteriaDTO.getPath().isEmpty())
        throw new HpcException("Path value is missing for USAGE_SUMMARY_BY_PATH report",
            HpcErrorType.INVALID_REQUEST_INPUT);
    }

    if (criteriaDTO.getType().equals(HpcReportType.USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE)) {
      if (criteriaDTO.getPath() == null || criteriaDTO.getPath().isEmpty())
        throw new HpcException(
            "Path value is missing for USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE report",
            HpcErrorType.INVALID_REQUEST_INPUT);

      if (criteriaDTO.getFromDate() == null || criteriaDTO.getToDate() == null)
        throw new HpcException(
            "date range is missing for USAGE_SUMMARY_BY_PATH_BY_DATE_RANGE report",
            HpcErrorType.INVALID_REQUEST_INPUT);

      if (criteriaDTO.getDoc() != null && !criteriaDTO.getDoc().isEmpty())
        throw new HpcException("DOC is not allowed for USAGE_SUMMARY_BY_USER_BY_DATE_RANGE report",
            HpcErrorType.INVALID_REQUEST_INPUT);
    }

    HpcReportCriteria criteria = new HpcReportCriteria();
    criteria.getDocs().addAll(criteriaDTO.getDoc());
    criteria.setType(criteriaDTO.getType());
    criteria.setPath(criteriaDTO.getPath());
    criteria.setIsMachineReadable(true);
    if (criteriaDTO.getReportColumns() != null && !criteriaDTO.getReportColumns().isEmpty()) {
      for(HpcReportEntryAttribute reportColumn : criteriaDTO.getReportColumns()) {
        criteria.getAttributes().add(reportColumn);
      }
    }
    Calendar fromcal = null;
    Calendar tocal = null;
    try {
      if (criteriaDTO.getFromDate() != null && criteriaDTO.getToDate() != null) {
        SimpleDateFormat fromFormat = new SimpleDateFormat("M/dd/yyyy");
        SimpleDateFormat toFormat = new SimpleDateFormat("M/dd/yyyy");
        fromFormat.parse(criteriaDTO.getFromDate());
        fromcal = fromFormat.getCalendar();
        criteria.setFromDate(fromcal);
        toFormat.parse(criteriaDTO.getToDate());
        tocal = toFormat.getCalendar();
        criteria.setToDate(tocal);
      }
    } catch (ParseException e) {
      throw new HpcException("Failed to parse date value " + e.getMessage(),
          HpcErrorType.INVALID_REQUEST_INPUT);
    }
    criteria.getUsers().addAll(criteriaDTO.getUser());

    List<HpcReport> reports = reportService.generateReport(criteria);
    HpcReportsDTO dto = new HpcReportsDTO();
    SimpleDateFormat displayformat = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss");
    for (HpcReport report : reports) {
      HpcReportDTO dtoreport = new HpcReportDTO();
      dtoreport.setDoc(report.getDoc());
      dtoreport.setFromDate((fromcal != null ? displayformat.format(fromcal.getTime()) : null));

      dtoreport.setGeneratedOn(displayformat.format(report.getGeneratedOn().getTime()));
      dtoreport.setToDate((tocal != null ? displayformat.format(tocal.getTime()) : null));
      dtoreport.setType(report.getType().value());
      dtoreport.setUser(report.getUser());
      dtoreport.setPath(report.getPath());
      dtoreport.setDataOwner(report.getDataOwner());
      dtoreport.setDataCurator(report.getDataCurator());

      for (HpcReportEntry entry : report.getReportEntries()) {
        HpcReportEntryDTO entryDTO = new HpcReportEntryDTO();
        entryDTO.setAttribute(entry.getAttribute().value());
        entryDTO.setValue(entry.getValue());
        dtoreport.getReportEntries().add(entryDTO);
      }
      dto.getReports().add(dtoreport);
    }
    return dto;
  }

  private boolean isDateValid(String dateToValidate, String dateFromat) {

    if (dateToValidate == null) {
      return false;
    }

    SimpleDateFormat sdf = new SimpleDateFormat(dateFromat);
    sdf.setLenient(false);

    try {
      sdf.parse(dateToValidate);
    } catch (ParseException e) {
      return false;
    }

    return true;
  }
}
