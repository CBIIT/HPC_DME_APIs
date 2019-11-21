/**
 * HpcDataManagementProxy.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration;

import java.util.List;
import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.user.HpcAuthenticationType;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * HPC Data Management Proxy Interface.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public interface HpcDataManagementProxy {
  /**
   * Authenticate the invoker w/ the data management system.
   *
   * @param dataManagementAccount The Data Management account to authenticate.
   * @param authenticationType The authentication type.
   * @return An authenticated token, to be used in subsequent calls to data management. It returns
   *     null if the account is not authenticated.
   * @throws HpcException on data management system failure.
   */
  public Object authenticate(
      HpcIntegratedSystemAccount dataManagementAccount,
      HpcAuthenticationType authenticationType)
      throws HpcException;

  /**
   * Close iRODS connection of an account.
   *
   * @param authenticatedToken An authenticated token.
   */
  public void disconnect(Object authenticatedToken);

  /**
   * Create a collection's directory.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The collection path.
   * @throws HpcException on data management system failure.
   */
  public void createCollectionDirectory(Object authenticatedToken, String path) throws HpcException;

  /**
   * Sets permission inheritance on a collection
   *
   * @param authenticatedToken An authenticated token.
   * @param path The collection path.
   * @param permissionInheritace If true, any collections / data objects created under this
   *     collection will inherit the same permissions as this collection.
   * @throws HpcException on data management system failure.
   */
  public void setCollectionPermissionInheritace(
      Object authenticatedToken, String path, boolean permissionInheritace) throws HpcException;

  /**
   * Create a data object's file.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The data object path.
   * @throws HpcException on data management system failure.
   */
  public void createDataObjectFile(Object authenticatedToken, String path) throws HpcException;

  /**
   * Delete a path (data object or directory).
   *
   * @param authenticatedToken An authenticated token.
   * @param path The path to delete.
   * @return true if the data object or directory was successfully deleted.
   * @throws HpcException on data management system failure.
   */
  public boolean delete(Object authenticatedToken, String path) throws HpcException;

  /**
   * Move a path (data object or directory).
   *
   * @param authenticatedToken An authenticated token.
   * @param sourcePath The move source path.
   * @param destinationPath The move destination path.
   * @return true if the data object object or directory was successfully moved.
   * @throws HpcException on data management system failure.
   */
  public boolean move(Object authenticatedToken, String sourcePath, String destinationPath)
      throws HpcException;

  /**
   * Add metadata to a collection.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The collection path.
   * @param metadataEntries The metadata entries to add.
   * @throws HpcException on data management system failure.
   */
  public void addMetadataToCollection(
      Object authenticatedToken, String path, List<HpcMetadataEntry> metadataEntries)
      throws HpcException;

  /**
   * Update collection's metadata.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The collection path.
   * @param metadataEntries The metadata entries to update.
   * @throws HpcException on data management system failure.
   */
  public void updateCollectionMetadata(
      Object authenticatedToken, String path, List<HpcMetadataEntry> metadataEntries)
      throws HpcException;

  /**
   * Add metadata to a data object.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The data object path.
   * @param metadataEntries The metadata entries to add.
   * @throws HpcException on data management system failure.
   */
  public void addMetadataToDataObject(
      Object authenticatedToken, String path, List<HpcMetadataEntry> metadataEntries)
      throws HpcException;

  /**
   * Update data object's metadata.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The data object path.
   * @param metadataEntries The metadata entries to update.
   * @throws HpcException on data management system failure.
   */
  public void updateDataObjectMetadata(
      Object authenticatedToken, String path, List<HpcMetadataEntry> metadataEntries)
      throws HpcException;

  /**
   * Check if a path's parent is a directory.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The path.
   * @return true if the parent path is a directory, or false otherwise.
   * @throws HpcException on data management system failure.
   */
  public boolean isPathParentDirectory(Object authenticatedToken, String path) throws HpcException;

  /**
   * Determine if path refers to collection or data file.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The path.
   * @return Indication of either collection or data file via boolean; true corresponds to
   *     collection while false corresponds to data file
   * @throws HpcException on data management system failure.
   */
  public boolean interrogatePathRef(Object authenticatedToken, String path) throws HpcException;

  /**
   * Get path attributes (exists, isDirectory, isFile)
   *
   * @param authenticatedToken An authenticated token.
   * @param path The data-object/collection path.
   * @return The path attributes.
   * @throws HpcException on data management system failure.
   */
  public HpcPathAttributes getPathAttributes(Object authenticatedToken, String path)
      throws HpcException;

  /**
   * Get collection by its path.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The collection's path.
   * @param list An indicator to list sub-collections and data-objects.
   * @return The Collection.
   * @throws HpcException on data management system failure.
   */
  public HpcCollection getCollection(Object authenticatedToken, String path, boolean list)
      throws HpcException;

  /**
   * Get collection children by its path. No collection metadata will be returned
   *
   * @param authenticatedToken An authenticated token.
   * @param path The collection's path.
   * @return The Collection.
   * @throws HpcException on data management system failure.
   */
  public HpcCollection getCollectionChildren(Object authenticatedToken, String path)
      throws HpcException;

  /**
   * Get metadata of a collection.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The collection path.
   * @return List of metadata entries.
   * @throws HpcException on data management system failure.
   */
  public List<HpcMetadataEntry> getCollectionMetadata(Object authenticatedToken, String path)
      throws HpcException;

  /**
   * Get data object by its path.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The data object's path.
   * @return The Data Object.
   * @throws HpcException on data management system failure.
   */
  public HpcDataObject getDataObject(Object authenticatedToken, String path) throws HpcException;

  /**
   * Get data objects by metadata query.
   *
   * @param authenticatedToken An authenticated token.
   * @param metadataQueries The metadata entries to query for.
   * @return List of data objects.
   * @throws HpcException on data management system failure.
   */
  public List<HpcDataObject> getDataObjects(
      Object authenticatedToken, List<HpcMetadataQuery> metadataQueries) throws HpcException;

  /**
   * Get metadata of a data object.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The data object path.
   * @return List of metadata entries.
   * @throws HpcException on data management system failure.
   */
  public List<HpcMetadataEntry> getDataObjectMetadata(Object authenticatedToken, String path)
      throws HpcException;

  /**
   * Add a user.
   *
   * @param authenticatedToken An authenticated token.
   * @param nciAccount The NCI account of the user to be added to data management.
   * @param userRole The HPC user role to assign to the new user.
   * @throws HpcException If it failed to add a user or user already exists.
   */
  public void addUser(Object authenticatedToken, HpcNciAccount nciAccount, HpcUserRole userRole)
      throws HpcException;

  /**
   * Delete a user.
   *
   * @param authenticatedToken An authenticated token.
   * @param nciUserId The user name.
   * @throws HpcException If it failed to delete the user.
   */
  public void deleteUser(Object authenticatedToken, String nciUserId) throws HpcException;

  /**
   * Update a user.
   *
   * @param authenticatedToken An authenticated token.
   * @param username The user name of the account to update
   * @param firstName The user first name.
   * @param lastName The user last name.
   * @param userRole The HPC user role to update for the user.
   * @throws HpcException If it failed to update user, or the user doesn't exist.
   */
  public void updateUser(
      Object authenticatedToken,
      String username,
      String firstName,
      String lastName,
      HpcUserRole userRole)
      throws HpcException;

  /**
   * Check if a user exists
   *
   * @param authenticatedToken An authenticated token.
   * @param username The user name of the account to check if it exists.
   * @return True if the user exists. and false otherwise.
   * @throws HpcException on data management system failure.
   */
  public boolean userExists(Object authenticatedToken, String username) throws HpcException;

  /**
   * Get the user's role.
   *
   * @param authenticatedToken An authenticated token.
   * @param username The user name of the account to get its type.
   * @return The user's role.
   * @throws HpcException on data management system failure.
   */
  public HpcUserRole getUserRole(Object authenticatedToken, String username) throws HpcException;

  /**
   * Get Collection permissions.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The collection's path.
   * @return A list of subjects (users/groups) and their permission on the collection.
   * @throws HpcException on data management system failure.
   */
  public List<HpcSubjectPermission> getCollectionPermissions(Object authenticatedToken, String path)
      throws HpcException;

  /**
   * Get Collection permission for a userId.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The collection's path.
   * @param userId UserId.
   * @return permission on the collection.
   * @throws HpcException on data management system failure.
   */
  public HpcSubjectPermission getCollectionPermission(
      Object authenticatedToken, String path, String userId) throws HpcException;

  /**
   * Acquire permission on a given collection for a given user.
   *
   * <p>Similar to <code>getCollectionPermission</code> method of this class but internally ensures
   * that no permission is represented as a level of "NONE" even if iRODS Jargon API returns null to
   * indicate no permission.
   *
   * @param authenticatedToken An authenticated token.
   * @param path Path of given collection.
   * @param userId UserId of given user.
   * @return <code>HpcSubjectPermission</code> instance representing given user's permission on
   *     given collection
   * @throws HpcException on data management system failure.
   */
  public HpcSubjectPermission acquireCollectionPermission(
      Object authenticatedToken, String path, String userId) throws HpcException;

  
  /**
   * Acquire permission on a given DataObject for a given user.
   *
   * <p>Similar to <code>getDataObjectPermission</code> method of this class but internally ensures
   * that no permission is represented as a level of "NONE" even if iRODS Jargon API returns null to
   * indicate no permission.
   *
   * @param authenticatedToken An authenticated token.
   * @param path Path of given DataObject.
   * @param userId UserId of given user.
   * @return <code>HpcSubjectPermission</code> instance representing given user's permission on
   *     given DataObject
   * @throws HpcException on data management system failure.
   */
  public HpcSubjectPermission acquireDataObjectPermission(
      Object authenticatedToken, String path, String userId) throws HpcException;
  
  
  /**
   * Set Collection permission.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The collection's path.
   * @param permissionRequest The permission request for a subject (user or group).
   * @throws HpcException on data management system failure.
   */
  public void setCollectionPermission(
      Object authenticatedToken, String path, HpcSubjectPermission permissionRequest)
      throws HpcException;

  /**
   * Get Data Object permissions.
   *
   * @param authenticatedToken An authenticated token.
   * @param path data object's path.
   * @return A list of subjects (users/groups) and their permission on the data object.
   * @throws HpcException on data management system failure.
   */
  public List<HpcSubjectPermission> getDataObjectPermissions(Object authenticatedToken, String path)
      throws HpcException;

  /**
   * Get Data object permission for a userId.
   *
   * @param authenticatedToken An authenticated token.
   * @param path data object's path.
   * @param userId UserId.
   * @return permission on the data object.
   * @throws HpcException on data management system failure.
   */
  public HpcSubjectPermission getDataObjectPermission(
      Object authenticatedToken, String path, String userId) throws HpcException;

  /**
   * Set Data Object permission.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The data object's path.
   * @param permissionRequest The subject (user or group) permission request.
   * @throws HpcException on data management system failure.
   */
  public void setDataObjectPermission(
      Object authenticatedToken, String path, HpcSubjectPermission permissionRequest)
      throws HpcException;

  /**
   * Add a group.
   *
   * @param authenticatedToken An authenticated token.
   * @param groupName The group name.
   * @throws HpcException on data management system failure.
   */
  public void addGroup(Object authenticatedToken, String groupName) throws HpcException;

  /**
   * Delete a group.
   *
   * @param authenticatedToken An authenticated token.
   * @param groupName The group name.
   * @throws HpcException on data management system failure.
   */
  public void deleteGroup(Object authenticatedToken, String groupName) throws HpcException;

  /**
   * Check if a group exists
   *
   * @param authenticatedToken An authenticated token.
   * @param groupName The group name.
   * @return True if the group exists. and false otherwise
   * @throws HpcException on data management system failure.
   */
  public boolean groupExists(Object authenticatedToken, String groupName) throws HpcException;

  /**
   * Add a member to a group.
   *
   * @param authenticatedToken An authenticated token.
   * @param groupName The group name.
   * @param userId The member's user id to add to the group.
   * @throws HpcException on service failure.
   */
  public void addGroupMember(Object authenticatedToken, String groupName, String userId)
      throws HpcException;

  /**
   * Delete a member from a group.
   *
   * @param authenticatedToken An authenticated token.
   * @param groupName The group name.
   * @param userId The member's user id to delete from the group.
   * @throws HpcException on service failure.
   */
  public void deleteGroupMember(Object authenticatedToken, String groupName, String userId)
      throws HpcException;

  /**
   * Get group members.
   *
   * @param authenticatedToken An authenticated token.
   * @param groupName The group name.
   * @return A list of the group members user id.
   * @throws HpcException on service failure.
   */
  public List<String> getGroupMembers(Object authenticatedToken, String groupName)
      throws HpcException;

  /**
   * Count the number of data objects (files) under a collection path.
   *
   * @param authenticatedToken An authenticated token.
   * @param path The collection path.
   * @return The count of data objects under a given path.
   * @throws HpcException 
   */
  public int countDataObjectsUnderPath(Object authenticatedToken, String path) 
      throws HpcException;
  
  /**
   * Get absolute path (append the iRODs base path, to the user provided path).
   *
   * @param relativePath The relative Path.
   * @return The absolute path.
   */
  public String getAbsolutePath(String relativePath);

  /**
   * Get relative path of an (iRODS) absolute path.
   *
   * @param absolutePath The absolute Path.
   * @return The relative path.
   */
  public String getRelativePath(String absolutePath);
}
