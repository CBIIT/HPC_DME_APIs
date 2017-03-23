/**
 * HpcDataManagementProxy.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcDataManagementAccount;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;


/**
 * <p>
 * HPC Data Management Proxy Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$ 
 */

public interface HpcDataManagementProxy 
{    
    /**
     * Authenticate the invoker w/ the data management system.
     *
     * @param dataManagementAccount The Data Management account to authenticate.
     * @param ldapAuthenticated An indicator if the user was authenticated via LDAP. 
     *                          This determines the authentication scheme to use w/ Data Management. 
     * @return An authenticated token, to be used in subsequent calls to data management.
     *         It returns null if the account is not authenticated.
     * @throws HpcException on data management system failure.
     */
    public Object authenticate(HpcIntegratedSystemAccount dataManagementAccount,
    		                   boolean ldapAuthenticated) 
    		                  throws HpcException;

    /**
     * Create HPC data management account from proxy account object. 
     * This is to cache proxy data management account for better performance.
     * 
     * @param proxyAccount The tokenized account.
     * @return A data management account
     * @throws HpcException on data management system failure.
     */
    public HpcDataManagementAccount getHpcDataManagementAccount(Object proxyAccount) throws HpcException;
    
    /**
     * Create Proxy data management account from cached HPC data management account.
     * 
     * @param datamanagementAccount Data Management Account.
     * @return Tokenized account.
     * @throws HpcException on data management system failure.
     */
    public Object getProxyManagementAccount(HpcDataManagementAccount datamanagementAccount) throws HpcException;
    
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
    public void createCollectionDirectory(Object authenticatedToken, String path) 
    		                             throws HpcException;
    
    /**
     * Create a data object's file.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data object path.
     * @throws HpcException on data management system failure.
     */
    public void createDataObjectFile(Object authenticatedToken, 
    		                         String path) 
    		                        throws HpcException;
    
    /**
     * Delete a path (data object or directory).
     *
     * @param authenticatedToken An authenticated token.
     * @param path The path to delete.
     * @return true if the object file was successfully deleted.
     */
    public boolean delete(Object authenticatedToken, String path);

    /**
     * Add metadata to a collection.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The collection path.
     * @param metadataEntries The metadata entries to add.
     * @throws HpcException on data management system failure.
     */
    public void addMetadataToCollection(Object authenticatedToken, 
    		                            String path,
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException;
    
    /**
     * Update collection's metadata.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The collection path.
     * @param metadataEntries The metadata entries to update.
     * @throws HpcException on data management system failure.
     */
    public void updateCollectionMetadata(Object authenticatedToken, 
    		                             String path,
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException;
    
    /**
     * Add metadata to a data object.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data object path.
     * @param metadataEntries The metadata entries to add.
     * @throws HpcException on data management system failure.
     */
    public void addMetadataToDataObject(Object authenticatedToken, 
    		                            String path,
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException;
    
    /**
     * Update data object's metadata.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data object path.
     * @param metadataEntries The metadata entries to update.
     * @throws HpcException on data management system failure.
     */
    public void updateDataObjectMetadata(Object authenticatedToken, 
    		                             String path,
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException;
    
    /**
     * Check if a path's parent is a directory.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The path.
     * @return true if the parent path is a directory, or false otherwise.
     * @throws HpcException on data management system failure.
     */
    public boolean isPathParentDirectory(Object authenticatedToken, 
    		                             String path)
    		                            throws HpcException;   
    
    /**
     * Get path attributes (exists, isDirectory, isFile)
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data-object/collection path.
     * @return The path attributes.
     * @throws HpcException on data management system failure.
     */
    public HpcPathAttributes getPathAttributes(Object authenticatedToken, 
    		                                   String path)
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
     * Get metadata of a collection.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The collection path.
     * @return List of metadata entries.
     * @throws HpcException on data management system failure.
     */
    public List<HpcMetadataEntry> getCollectionMetadata(Object authenticatedToken, 
   		                                                String path) 
   		                                               throws HpcException;
    
    /**
     * Get data object by its path.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data object's path.
     * @return The Data Object.
     * @throws HpcException on data management system failure.
     */
    public HpcDataObject getDataObject(Object authenticatedToken, String path) 
    		                          throws HpcException;
    
    /**
     * Get data objects by metadata query.
     *
     * @param authenticatedToken An authenticated token.
     * @param metadataQueries The metadata entries to query for.
     * @return List of data objects.
     * @throws HpcException on data management system failure.
     */
    public List<HpcDataObject> getDataObjects(Object authenticatedToken,
    		                                  List<HpcMetadataQuery> metadataQueries) 
    		                                 throws HpcException;
    
    /**
     * Get metadata of a data object.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data object path.
     * @return List of metadata entries.
     * @throws HpcException on data management system failure.
     */
    public List<HpcMetadataEntry> getDataObjectMetadata(Object authenticatedToken, 
   		                                                String path) 
   		                                               throws HpcException;   
    
    /**
     * Get the user's role.
     *
     * @param authenticatedToken An authenticated token.
     * @param username The user name of the account to get its type.
     * @return The user's role.
     * @throws HpcException on data management system failure.
     */
    public HpcUserRole getUserRole(Object authenticatedToken, String username) 
    		                      throws HpcException;   
    
    /**
     * Add a user.
     *
     * @param authenticatedToken An authenticated token.
     * @param nciAccount The NCI account of the user to be added to data management.
     * @param userRole The HPC user role to assign to the new user.
     * @throws HpcException If it failed to add a user or user already exists.
     */
    public void addUser(Object authenticatedToken,
    		            HpcNciAccount nciAccount, HpcUserRole userRole) 
    		           throws HpcException;
    
    /**
     * Delete a user.
     *
     * @param authenticatedToken An authenticated token.
     * @param nciUserId The user name.
     * @throws HpcException If it failed to delete the user.
     */
    public void deleteUser(Object authenticatedToken, String nciUserId)
                          throws HpcException;
    
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
    public void updateUser(Object authenticatedToken,
    		               String username, String firstName, String lastName,
                           HpcUserRole userRole) 
    		              throws HpcException;

    /**
     * Get Collection permissions.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The collection's path.
     * @return A list of subjects (users/groups) and their permission on the collection.
     * @throws HpcException on data management system failure.
     */
    public List<HpcSubjectPermission> getCollectionPermissions(Object authenticatedToken,
    		                                                   String path) 
    		                                                  throws HpcException; 
    
    /**
     * Set Collection permission.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The collection's path.
     * @param permissionRequest The permission request for a subject (user or group).
     * @throws HpcException on data management system failure.
     */
    public void setCollectionPermission(Object authenticatedToken,
    		                            String path,
    		                            HpcSubjectPermission permissionRequest) 
    		                           throws HpcException;   
    
    /**
     * Get Data Object permissions.
     *
     * @param authenticatedToken An authenticated token.
     * @param path data object's path.
     * @return A list of subjects (users/groups) and their permission on the data object.
     * @throws HpcException on data management system failure.
     */
    public List<HpcSubjectPermission> getDataObjectPermissions(Object authenticatedToken,
    		                                                   String path) 
    		                                                  throws HpcException; 
    
    /**
     * Set Data Object permission.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data object's path.
     * @param permissionRequest The subject (user or group) permission request.
     * @throws HpcException on data management system failure.
     */
    public void setDataObjectPermission(Object authenticatedToken,
    		                            String path,
    		                            HpcSubjectPermission permissionRequest) 
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
    public void addGroupMember(Object authenticatedToken, String groupName, 
    		                   String userId) 
    		                  throws HpcException;
    
    /**
     * Delete a member from a group.
     * 
     * @param authenticatedToken An authenticated token.
     * @param groupName The group name.
     * @param userId The member's user id to delete from the group.
     * @throws HpcException on service failure.
     */
    public void deleteGroupMember(Object authenticatedToken, String groupName, 
    		                      String userId) 
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
     * Get user groups by search criteria.
     * 
     * @param authenticatedToken An authenticated token.
     * @param groupSearchCriteria The group search criteria (In the form of SQL 'LIKE', using case insensitive matching).
     * @return A list of group names.
     * @throws HpcException on data management system failure.
     */
    public List<String> getGroups(Object authenticatedToken, String groupSearchCriteria) 
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

 