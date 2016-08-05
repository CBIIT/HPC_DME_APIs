/**
 * HpcReportService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Report Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public interface HpcReportService {
	/**
	 * Generate HPC report
	 *
	 * @param criteria
	 *            to generate the report
	 * @return HpcReport
	 * 
	 * @throws HpcException
	 */
	public HpcReport generateReport(HpcReportCriteria criteria) throws HpcException;

}
