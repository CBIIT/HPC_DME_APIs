/**
 * HpcDataManagementSecurityService.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * HPC Data Management Security Application Service Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */
public interface HpcDataManagementSecurityService {
  /**
   * Add a user.
   *
   * @param nciAccount The NCI account of the user to be added to data management.
   * @param userRole The HPC user role to assign to the new user.
   * @throws HpcException on service failure.
   */
  public void addUser(HpcNciAccount nciAccount, HpcUserRole userRole) throws HpcException;

  /**
   * Check if a user exists (i.e. the user has a data management account).
   *
   * @param nciUserId The user-id to check.
   * @return True if the group exists, and false otherwise.
   * @throws HpcException on service failure.
   */
  public boolean userExists(String nciUserId) throws HpcException;

  /**
   * Delete a user.
   *
   * @param nciUserId The user-id to delete.
   * @throws HpcException on service failure.
   */
  public void deleteUser(String nciUserId) throws HpcException;

  /**
   * Update a user.
   *
   * @param nciUserId The NCI user ID of the user to update.
   * @param firstName The user first name.
   * @param lastName The user last name.
   * @param userRole The HPC user role to update for the user.
   * @throws HpcException on service failure.
   */
  public void updateUser(String nciUserId, String firstName, String lastName, HpcUserRole userRole)
      throws HpcException;

  /**
   * Get the role of a given user's name.
   *
   * @param nciUserId The user-id.
   * @return HpcUserRole The user's role.
   * @throws HpcException on service failure.
   */
  public HpcUserRole getUserRole(String nciUserId) throws HpcException;

  /**
   * Add a group.
   *
   * @param groupName The group name.
   * @throws HpcException on service failure.
   */
  public void addGroup(String groupName) throws HpcException;

  /**
   * Check if a group exists.
   *
   * @param groupName The group name.
   * @return True if the group exists, and false otherwise.
   * @throws HpcException on service failure.
   */
  public boolean groupExists(String groupName) throws HpcException;

  /**
   * Delete a group.
   *
   * @param groupName The group name.
   * @throws HpcException on service failure.
   */
  public void deleteGroup(String groupName) throws HpcException;

  /**
   * Add a member to a group.
   *
   * @param groupName The group name.
   * @param userId The member's user id to add to the group.
   * @throws HpcException on service failure.
   */
  public void addGroupMember(String groupName, String userId) throws HpcException;

  /**
   * Delete a member from a group.
   *
   * @param groupName The group name.
   * @param userId The member's user id to delete from the group.
   * @throws HpcException on service failure.
   */
  public void deleteGroupMember(String groupName, String userId) throws HpcException;

  /**
   * Get group members.
   *
   * @param groupName The group name.
   * @return A list of the group members user id.
   * @throws HpcException on service failure.
   */
  public List<String> getGroupMembers(String groupName) throws HpcException;

  /**
   * Get groups by search criteria.
   *
   * @param groupPattern The group pattern to search for (using case insensitive matching). SQL LIKE
   *     wildcards ('%', '_') are supported.
   * @return A list of groups names matching the criteria.
   * @throws HpcException on service failure.
   */
  public List<String> getGroups(String groupPattern) throws HpcException;

  
  /**
   * Get all the groups to which this user belongs
   * 
   * @param userId The userId
   * @return The list of groups to which this user belongs
   * @throws HpcException on service failure.
   */
  List<String> getUserGroups(String userId) throws HpcException;
}
