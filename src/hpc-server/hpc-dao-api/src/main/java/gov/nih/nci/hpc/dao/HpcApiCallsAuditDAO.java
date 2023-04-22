/**
 * HpcDataManagementAuditDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.Calendar;

import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC API Calls Audit DAO Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public interface HpcApiCallsAuditDAO {
	/**
	 * Store a new audit record.
	 *
	 * @param userId            The user who initiated the API call.
	 * @param httpRequestMethod The HTTP request method.
	 * @param endpoint          The API endpoint.
	 * @param serverId          The server handled the request
	 * @param created           The time the request was created.
	 * @param completed         The time the request was completed.
	 * @throws HpcException on database error.
	 */
	public void insert(String userId, String httpRequestMethod, String endpoint, String httpResponseCode,
			String serverId, Calendar created, Calendar completed) throws HpcException;
}
