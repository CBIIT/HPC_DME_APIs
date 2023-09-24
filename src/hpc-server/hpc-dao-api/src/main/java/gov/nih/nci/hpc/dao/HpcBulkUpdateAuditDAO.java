/**
 * HpcBulkUpdateAuditDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.Calendar;

import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQuery;
import gov.nih.nci.hpc.domain.metadata.HpcCompoundMetadataQueryType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Bulk Update Audit DAO Interface.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public interface HpcBulkUpdateAuditDAO {
	/**
	 * Store a new bulk update audit record.
	 *
	 * @param userId            The userId of the user performing the updates.
	 * @param query             The query being used for bulk metadata update.
	 * @param queryType         The query type, data object or collection.
	 * @param metadataName      The name of the metadata being updated.
	 * @param metadataValue     The value of the metadata being updated.
	 * @param created           The time the request was created.
	 * @throws HpcException on database error.
	 */
	public void insert(String userId, HpcCompoundMetadataQuery query, HpcCompoundMetadataQueryType queryType,
			String metadataName, String metadataValue, Calendar created) throws HpcException;
}
