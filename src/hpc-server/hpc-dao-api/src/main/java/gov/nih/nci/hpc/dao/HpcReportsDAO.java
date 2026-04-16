/**
 * HpcEventDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import gov.nih.nci.hpc.domain.report.HpcReport;
import gov.nih.nci.hpc.domain.report.HpcReportCriteria;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Reports DAO Interface.
 * </p>
 *
 * @author <a href="mailto:prasad.konka@nih.gov">Prasad Konka</a>
 * @version $Id$
 */

public interface HpcReportsDAO 
{    
    /**
     * Generate report based on given criteria
     *
     * @param criteria The report criteria.
     * @return <code>List&lt;HpcReport&gt;</code>
     * @throws HpcException on database error.
     */
    public List<HpcReport> generatReport(HpcReportCriteria criteria) throws HpcException;
    
    /**
     * Refresh all materialized views.
     * @throws HpcException on database error.
     */
    public void refreshViews() throws HpcException;

    /**
     * Get the size of the specified collection from the reports table
     * capturing collection sizes.
     *
     * @param collectionPath path of the collection
     * @return size of the specified collection
     */
	public Long getCollectionSize(String collectionPath) throws HpcException;

}

 