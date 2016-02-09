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
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
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
	public class HpcDataManagementPathAttributes {
       public boolean exists = false;
       public boolean isFile = false;
       public boolean isDirectory = false;
    }
	
    /**
     * Authenticate the invoker w/ iRODS.
     *
     * @param dataManagementAccount The Data Management account to authenticate.
     * @return An authenticated token, to be used in subsequent calls to data management.
     *         It returns null if the account is not authenticated.
     * 
     * @throws HpcException
     */
    public Object authenticate(HpcIntegratedSystemAccount dataManagementAccount) 
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
     * Delete a data object's file.
     *
     * @param authenticatedToken An authenticated token.
     * @param path The data object path.
     * 
     * @return true if the object file was successfully deleted.
     */
    public boolean deleteDataObjectFile(Object authenticatedToken, 
    		                            String path);

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
    public HpcDataManagementPathAttributes getPathAttributes(
    		                                      Object authenticatedToken, 
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
     * @param path The collection path.
     * @return HpcMetadataEntry list.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getDataObjectMetadata(Object authenticatedToken, 
   		                                                String path) 
   		                                               throws HpcException;   
    
    /**
     * Get the user type.
     *
     * @param authenticatedToken An authenticated token.
     * @param username The user name of the account to get its type.
     * @return The user's type.
     * 
     * @throws HpcException
     */
    public String getUserType(Object authenticatedToken, String username) 
    		                 throws HpcException;   
    
    /**
     * Add a user.
     *
     * @param authenticatedToken An authenticated token.
     * @param nciAccount The NCI account of the user to be added to data management.
     * @param userType The iRODS user type to assign to the new user.
     * 
     * @throws HpcException If it failed to add a user or user already exists.
     */
    public void addUser(Object authenticatedToken,
    		            HpcNciAccount nciAccount, String userType) 
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
    		                            HpcUserPermission permissionRequest) 
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
    		                            HpcUserPermission permissionRequest) 
    		                           throws HpcException; 
}

 