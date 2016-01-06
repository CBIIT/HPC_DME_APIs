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
	class HpcDataManagementPathAttributes {
       public boolean exists = false;
       public boolean isFile = false;
       public boolean isDirectory = false;
    }
	
    /**
     * Create a collection's directory.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The collection path.
     * 
     * @throws HpcException
     */
    public void createCollectionDirectory(HpcIntegratedSystemAccount dataManagementAccount, 
    		                              String path) 
    		                             throws HpcException;
    
    /**
     * Create a data object's file.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The data object path.
     * 
     * @throws HpcException
     */
    public void createDataObjectFile(HpcIntegratedSystemAccount dataManagementAccount, 
    		                         String path) 
    		                        throws HpcException;

    /**
     * Add metadata to a collection.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The collection path.
     * @param metadataEntries The metadata entries to add.
     * 
     * @throws HpcException
     */
    public void addMetadataToCollection(HpcIntegratedSystemAccount dataManagementAccount, 
    		                            String path,
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException;
    
    /**
     * Add metadata to a data object.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The data object path.
     * @param metadataEntries The metadata entries to add.
     * 
     * @throws HpcException
     */
    public void addMetadataToDataObject(HpcIntegratedSystemAccount dataManagementAccount, 
    		                            String path,
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException;
    /**
     * Check if a parent path is a directory.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The path.
     * 
     * @throws HpcException
     */
    public boolean isParentPathDirectory(HpcIntegratedSystemAccount dataManagementAccount, 
    		                             String path)
    		                             throws HpcException;   
    
    /**
     * Create a parent directory (if it doesn't exist already).
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The data-object path.
     * 
     * @throws HpcException
     */
    public void createParentPathDirectory(HpcIntegratedSystemAccount dataManagementAccount, 
    		                              String path)
    		                             throws HpcException;   
    
    /**
     * Get path attributes (exists, isDirectory, isFile)
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The data-object/collection path.
     * @return The path attributes.
     * 
     * @throws HpcException
     */
    public HpcDataManagementPathAttributes getPathAttributes(
    		                    HpcIntegratedSystemAccount dataManagementAccount, 
    		                    String path)
    		                    throws HpcException;  
    
    /**
     * Get collection by its path.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The collection's path.
     * @return HpcCollection.
     * 
     * @throws HpcException
     */
    public HpcCollection getCollection(HpcIntegratedSystemAccount dataManagementAccount,
    		                           String path) 
    		                          throws HpcException;
    
    /**
     * Get collections by metadata query.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param metadataQueries The metadata entries to query for.
     * @return HpcCollection list.
     * 
     * @throws HpcException
     */
    public List<HpcCollection> getCollections(
    		    HpcIntegratedSystemAccount dataManagementAccount,
    		    List<HpcMetadataQuery> metadataQueries) 
    		    throws HpcException;
    
    /**
     * Get metadata of a collection.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The collection path.
     * @return HpcMetadataEntry list.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getCollectionMetadata(
   		                          HpcIntegratedSystemAccount dataManagementAccount, 
   		                          String path) 
   		                          throws HpcException;
    
    /**
     * Get data object by its path.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The data object's path.
     * @return HpcDataObject.
     * 
     * @throws HpcException
     */
    public HpcDataObject getDataObject(HpcIntegratedSystemAccount dataManagementAccount,
    		                           String path) 
    		                          throws HpcException;
    
    /**
     * Get data objects by metadata query.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param metadataQueries The metadata entries to query for.
     * @return HpcDataObject list.
     * 
     * @throws HpcException
     */
    public List<HpcDataObject> getDataObjects(
    		    HpcIntegratedSystemAccount dataManagementAccount,
    		    List<HpcMetadataQuery> metadataQueries) 
    		    throws HpcException;
    
    /**
     * Get metadata of a data object.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The collection path.
     * @return HpcMetadataEntry list.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getDataObjectMetadata(
   		                          HpcIntegratedSystemAccount dataManagementAccount, 
   		                          String path) 
   		                          throws HpcException;   
    
    /**
     * Get the user type.
     *
     * @param dataManagementAccount The Data Management System account.
     * @return The user's type.
     * 
     * @throws HpcException
     */
    public String getUserType(HpcIntegratedSystemAccount dataManagementAccount) 
    		                 throws HpcException;   
    
    /**
     * Close iRODS connection of an account.
     *
     * @param dataManagementAccount The Data Management System account.
     */
    public void closeConnection(HpcIntegratedSystemAccount dataManagementAccount);
    
    /**
     * Set Collection permission.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The collection's path.
     * @param permissionRequest The user permission request.
     * 
     * @throws HpcException
     */
    public void setCollectionPermission(HpcIntegratedSystemAccount dataManagementAccount,
    		                            String path,
    		                            HpcUserPermission permissionRequest) 
    		                           throws HpcException;   
    
    /**
     * Set Data Object permission.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The data object's path.
     * @param permissionRequest The user permission request.
     * 
     * @throws HpcException
     */
    public void setDataObjectPermission(HpcIntegratedSystemAccount dataManagementAccount,
    		                            String path,
    		                            HpcUserPermission permissionRequest) 
    		                           throws HpcException; 
}

 