/**
 * HpcReportBusService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.report.HpcReportRequestDTO;
import gov.nih.nci.hpc.dto.report.HpcReportsDTO;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Report Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public interface HpcReportBusService 
{         
	/**
	 * Generate HPC report.
	 *
	 * @param criteria to generate the report
	 * @return Reports DTO
	 * @throws HpcException on service failure.
	 */
	public HpcReportsDTO generateReport(HpcReportRequestDTO criteria) throws HpcException;	
}

 