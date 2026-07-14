/**
 * HpcGlobusTransferTaskDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.List;

import gov.nih.nci.hpc.domain.model.HpcGlobusTransferTask;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Globus Transfer Task DAO Interface.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public interface HpcGlobusTransferTaskDAO {
	/**
	 * Insert a new globus transfer request.
	 *
	 * @param request The request to be inserted.
	 * @throws HpcException on database error.
	 */
	public void insertRequest(HpcGlobusTransferTask request) throws HpcException;

	/**
	 * Delete the globus request from the table.
	 *
	 * @param dataTransferRequestId The dataTransferRequestId to be deleted.
	 * @throws HpcException on database error.
	 */
	void deleteRequest(String dataTransferRequestId) throws HpcException;

	/**
	 * Get Globus accounts used
	 *
	 * @return The list of globus accounts used ordered by least used
	 * @throws HpcException on database error.
	 */
	List<String> getGlobusAccountsUsed() throws HpcException;

	/**
	 * Get Globus users in the task queue
	 *
	 * @param download Whether the requests are download or upload
	 * @return The list of Globus users in the task queue
	 * @throws HpcException on database error.
	 */
	List<String> getGlobusUsersAllocated(boolean download) throws HpcException;

	/**
	 * Get the count of Globus transfer requests from a user
	 *
	 * @param userId   The user ID
	 * @param download Whether the requests are download or upload
	 * @return The count of Globus transfer requests from a user
	 * @throws HpcException on database error.
	 */
	int getGlobusRequestCountByUser(String userId, boolean download) throws HpcException;

}
