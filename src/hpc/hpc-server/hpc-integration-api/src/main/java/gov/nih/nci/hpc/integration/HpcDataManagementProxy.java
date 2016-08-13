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
import gov.nih.nci.hpc.domain.datamanagement.HpcEntityPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.model.HpcDataManagementAccount;
import gov.nih.nci.hpc.domain.model.HpcGroup;
import gov.nih.nci.hpc.domain.user.HpcGroupResponse;
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
     * 
     * @throws HpcException
     */
    public Object authenticate(HpcIntegratedSystemAccount dataManagementAccount,
    		                   boolean ldapAuthenticated) 
    		                  throws HpcException;

    /**
     * Create HPC data management account from proxy account object. 
     * This is to cache proxy data management account for better performance
     * @param proxyAccount
     * @return HpcDataManagementAccount to tokenize 
     * @throws HpcException
     */
    public HpcDataManagementAccount getHpcDataManagementAccount(Object proxyAccount) throws HpcException;
    
    /**
     * Create Proxy data management account from cached HPC data management account 
     * @param datamanagementAccount
     * @return
     * @throws HpcException
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
     * 
     * @throws HpcException
     */
    public void createCollectionDirectory(Object authenticatedToken, String path) 
    		                             throws HpcException;
    
    /**
     * Create a data object's file.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data object path.
     * 
     * @throws HpcException
     */
    public void createDataObjectFile(Object authenticatedToken, 
    		                         String path) 
    		                        throws HpcException;
    
    /**
     * Delete a path (data object or directory).
     *
     * @param authenticatedToken An authenticated token.
     * @param path The path to delete.
     * 
     * @return true if the object file was successfully deleted.
     */
    public boolean delete(Object authenticatedToken, String path);

    /**
     * Add metadata to a collection.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The collection path.
     * @param metadataEntries The metadata entries to add.
     * 
     * @throws HpcException
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
     * 
     * @throws HpcException
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
     * 
     * @throws HpcException
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
     * 
     * @throws HpcException
     */
    public void updateDataObjectMetadata(Object authenticatedToken, 
    		                             String path,
    		                             List<HpcMetadataEntry> metadataEntries) 
    		                            throws HpcException;
    
    /**
     * Check if a parent path is a directory.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The path.
     * 
     * @throws HpcException
     */
    public boolean isParentPathDirectory(Object authenticatedToken, 
    		                             String path)
    		                            throws HpcException;   
    
    /**
     * Create a parent directory (if it doesn't exist already).
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data-object path.
     * 
     * @throws HpcException
     */
    public void createParentPathDirectory(Object authenticatedToken, 
    		                              String path)
    		                             throws HpcException;   
    
    /**
     * Get path attributes (exists, isDirectory, isFile)
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data-object/collection path.
     * @return The path attributes.
     * 
     * @throws HpcException
     */
    public HpcPathAttributes getPathAttributes(Object authenticatedToken, 
    		                                   String path)
    		                                  throws HpcException;  
    
    /**
     * Get collection by its path.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The collection's path.
     * @return HpcCollection.
     * 
     * @throws HpcException
     */
    public HpcCollection getCollection(Object authenticatedToken,
    		                           String path) 
    		                          throws HpcException;
    
    /**
     * Get collections by metadata query.
     *
     * @param authenticatedToken An authenticated token.
     * @param metadataQueries The metadata entries to query for.
     * @return HpcCollection list.
     * 
     * @throws HpcException
     */
    public List<HpcCollection> getCollections(Object authenticatedToken,
    		                                  List<HpcMetadataQuery> metadataQueries) 
    		                                 throws HpcException;
    
    /**
     * Get metadata of a collection.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The collection path.
     * @return HpcMetadataEntry list.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getCollectionMetadata(Object authenticatedToken, 
   		                                                String path) 
   		                                               throws HpcException;
    
    /**
     * Get data object by its path.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data object's path.
     * @return HpcDataObject.
     * 
     * @throws HpcException
     */
    public HpcDataObject getDataObject(Object authenticatedToken,
    		                           String path) 
    		                          throws HpcException;
    
    /**
     * Get data objects by metadata query.
     *
     * @param authenticatedToken An authenticated token.
     * @param metadataQueries The metadata entries to query for.
     * @return HpcDataObject list.
     * 
     * @throws HpcException
     */
    public List<HpcDataObject> getDataObjects(Object authenticatedToken,
    		                                  List<HpcMetadataQuery> metadataQueries) 
    		                                 throws HpcException;
    
    /**
     * Get metadata of a data object.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data object path.
     * @return HpcMetadataEntry list.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getDataObjectMetadata(Object authenticatedToken, 
   		                                                String path) 
   		                                               throws HpcException;   
    
    /**
     * Get metadata of a the parent path.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The path.
     * @return HpcMetadataEntry list.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getParentPathMetadata(Object authenticatedToken, 
   		                                                String path) 
   		                                               throws HpcException;   
    
    /**
     * Get the user's role.
     *
     * @param authenticatedToken An authenticated token.
     * @param username The user name of the account to get its type.
     * @return HpcUserRole The user's type.
     * 
     * @throws HpcException
     */
    public HpcUserRole getUserRole(Object authenticatedToken, String username) 
    		                      throws HpcException;   
    
    /**
     * Add a user.
     *
     * @param authenticatedToken An authenticated token.
     * @param nciAccount The NCI account of the user to be added to data management.
     * @param userRole The HPC user role to assign to the new user.
     * 
     * @throws HpcException If it failed to add a user or user already exists.
     */
    public void addUser(Object authenticatedToken,
    		            HpcNciAccount nciAccount, HpcUserRole userRole) 
    		           throws HpcException;
    
    /**
     * Update a user.
     *
     * @param authenticatedToken An authenticated token.
     * @param username The user name of the account to update
     * @param firstName The user first name.
     * @param lastName The user last name. 
     * @param userRole The HPC user role to update for the user.
     * 
     * @throws HpcException If it failed to update user, or the user doesn't exist.
     */
    public void updateUser(Object authenticatedToken,
    		               String username, String firstName, String lastName,
                           HpcUserRole userRole) 
    		              throws HpcException;

    /**
     * Delete a user.
     *
     * @param authenticatedToken An authenticated token.
     * @param nciUserId The user name.
     * 
     * @throws HpcException If it failed to delete the user.
     */
    public void deleteUser(Object authenticatedToken, String nciUserId)
                          throws HpcException;
    
    /**
     * Set Collection permission.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The collection's path.
     * @param permissionRequest The user permission request.
     * 
     * @throws HpcException
     */
    public void setCollectionPermission(Object authenticatedToken,
    		                            String path,
    		                            HpcEntityPermission permissionRequest) 
    		                           throws HpcException;   
    
    /**
     * Set Data Object permission.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data object's path.
     * @param permissionRequest The user permission request.
     * 
     * @throws HpcException
     */
    public void setDataObjectPermission(Object authenticatedToken,
    		                            String path,
    		                            HpcEntityPermission permissionRequest) 
    		                           throws HpcException; 
    /**
     * Create User group and assign/remove users to group
     * @param group group name
     * @param addUserId List of userIds to add to the group
     * @param removeUserId List of userIds to remove from the group
     * @throws HpcException
     */
    public HpcGroupResponse addGroup(Object authenticatedToken,
            HpcGroup hpcGroup, List<String> addUserIds, List<String> removeUserIds) 
           throws HpcException;
    
}

 