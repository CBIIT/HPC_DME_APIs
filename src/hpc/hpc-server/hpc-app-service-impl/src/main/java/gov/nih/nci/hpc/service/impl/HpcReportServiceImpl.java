/**
 * HpcReportServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.dao.HpcReportsDAO;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.error.HpcRequestRejectReason;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;
import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.domain.report.HpcReportType;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcReportService;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC Report Application Service Implementation.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public class HpcReportServiceImpl implements HpcReportService {
	// ---------------------------------------------------------------------//
	// Constants
	// ---------------------------------------------------------------------//

	// Event payload entries attributes.
	private static final String DATA_TRANSFER_REQUEST_ID_ATTRIBUTE = "DATA_TRANSFER_REQUEST_ID";
	private static final String DATA_OBJECT_PATH_ATTRIBUTE = "DATA_OBJECT_PATH";

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The Event DAO instance.
	@Autowired
	private HpcReportsDAO reportsDAO = null;

	// The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Constructor for Spring Dependency Injection.
	 *
	 */
	private HpcReportServiceImpl() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	// ---------------------------------------------------------------------//
	// HpcEventService Interface Implementation
	// ---------------------------------------------------------------------//

	@Override
	public List<HpcReport> generateReport(HpcReportCriteria criteria) throws HpcException {
    	HpcRequestInvoker invoker = HpcRequestContext.getRequestInvoker();
    	if(invoker == null) {
	       throw new HpcException("Unknown user",
			                      HpcRequestRejectReason.NOT_AUTHORIZED);
    	}
    	if(!invoker.getUserRole().equals(HpcUserRole.SYSTEM_ADMIN))
    	{
	    	if(criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC) ||
	    			criteria.getType().equals(HpcReportType.USAGE_SUMMARY_BY_DOC_BY_DATE_RANGE))
	    	{
	    		String doc = invoker.getNciAccount().getDOC();
	    		List<String> docs = criteria.getDocs();
	    		for(String criteriaDOC : docs)
	    		{
	    			if(!criteriaDOC.endsWith(doc))
	    				throw new HpcException("Not authorized to generate report on the division: "+criteriaDOC, HpcErrorType.UNAUTHORIZED_REQUEST);
	    		}
	    	}
    	}
		return reportsDAO.generatReport(criteria);
	}

}
	