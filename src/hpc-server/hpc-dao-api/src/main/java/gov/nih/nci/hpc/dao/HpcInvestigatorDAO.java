/**
 * HpcInvestigatorDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.List;

import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Investigator DAO Interface.
 * </p>
 *
 * @author <a href="mailto:yuri.dinh@nih.gov">Yuri Dinh</a>
 */

public interface HpcInvestigatorDAO {
	/**
	 * Get all investigators ned id
	 * 
	 * @return list of nedIds.
	 * @throws HpcException on service failure.
	 */
	public List<String> getAllNedIds() throws HpcException;

	/**
	 * Update investigator record
	 *
	 * @param account The HpcNciAccount.
	 * @throws HpcException on service failure.
	 */
	public void updateInvestigator(HpcNciAccount account) throws HpcException;

}
