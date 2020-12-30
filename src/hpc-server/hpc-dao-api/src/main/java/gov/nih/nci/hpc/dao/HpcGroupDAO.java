/**
 * HpcGroupDAO.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.dao;

import java.util.List;

import gov.nih.nci.hpc.domain.model.HpcGroup;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Group DAO Interface. Note: This DAO was created to workaround a
 * limitation with the Jargon API. There is no way to search for groups in a
 * case insensitive way using the Jargon API. This DAO provides this capability
 * by querying the iRODS table r_user_main directly. Once Jargon API is enhanced
 * to support group case insensitive search, the implementation should switch
 * back and this DAO needs to be retired.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcGroupDAO {
	/**
	 * Get groups by search criteria.
	 *
	 * @param groupPattern The group pattern to search for (using case insensitive
	 *                     matching). SQL LIKE wildcards ('%', '_') are supported.
	 * @return A list of groups names matching the criteria.
	 * @throws HpcException on service failure.
	 */
	public List<String> getGroups(String groupPattern) throws HpcException;

	/**
	 * Get list of groups that a user belongs to.
	 * 
	 * @param userId The userId.
	 * @return list of groups.
	 * @throws HpcException on service failure.
	 */
	public List<String> getUserGroups(String userId) throws HpcException;

	/**
	 * Add a new group to the system
	 *
	 * @param group The group domain object.
	 * @throws HpcException on service failure.
	 */
	public void upsertGroup(HpcGroup group) throws HpcException;

	/**
	 * Update a group in the system
	 *
	 * @param group The group domain object.
	 * @throws HpcException on service failure.
	 */
	public void updateGroup(HpcGroup group) throws HpcException;

	/**
	 * Delete the group from the system
	 * 
	 * @param name The group to be deleted.
	 * @throws HpcException on service failure.
	 */
	public void deleteGroup(String name) throws HpcException;

	/**
	 * Get a group
	 * 
	 * @param name The name of the group.
	 * @return The group domain object
	 * @throws HpcException on service failure.
	 */
	public HpcGroup getGroup(String name) throws HpcException;

}
