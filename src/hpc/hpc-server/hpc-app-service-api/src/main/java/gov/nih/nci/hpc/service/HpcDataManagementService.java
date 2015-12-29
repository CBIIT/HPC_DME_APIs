/**
 * HpcDataManagementService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcUserPermission;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataQuery;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.List;

/**
 * <p>
 * HPC Data Management Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDataManagementService 
{         
    /**
     * Create a collection's directory.
     *
     * @param path The collection path.
     * 
     * @throws HpcException
     */
    public void createDirectory(String path) throws HpcException;
    
    /**
     * Create a data object's file.
     *
     * @param path The data object path.
     * @param createParentPathDirectory If set to true, create the directory for the file.
     * 
     * @throws HpcException
     */
    public void createFile(String path, boolean createParentPathDirectory) 
    		              throws HpcException;

    /**
     * Add metadata to a collection.
     *
     * @param path The collection path.
     * @param metadataEntries The metadata entries to add.
     * 
     * @throws HpcException
     */
    public void addMetadataToCollection(String path, 
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException; 
    
    /**
     * Add metadata to a data object.
     *
     * @param path The data object path.
     * @param metadataEntries The metadata entries to add.
     * 
     * @throws HpcException
     */
    public void addMetadataToDataObject(String path, 
    		                            List<HpcMetadataEntry> metadataEntries) 
    		                           throws HpcException; 
    
    /**
     * Create and attach file (physical) location and source metadata to a data object.
     *
     * @param path The data object path.
     * @param fileLocation The physical file location.
     * @param fileSource The source location of the file.
     * 
     * @throws HpcException
     */
    public void addFileLocationsMetadataToDataObject(String path, 
    		                                         HpcFileLocation fileLocation,
    		                                         HpcFileLocation fileSource) 
    		                                        throws HpcException; 
    
    /**
     * Get collection by its path.
     *
     * @param path The collection's path.
     * @return HpcCollection.
     * 
     * @throws HpcException
     */
    public HpcCollection getCollection(String path) throws HpcException;
    
    /**
     * Get collections by metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @return HpcCollection list.
     * 
     * @throws HpcException
     */
    public List<HpcCollection> getCollections(
    		    List<HpcMetadataQuery> metadataQueries) throws HpcException;
    
    /**
     * Get metadata of a collection.
     *
     * @param path The collection path.
     * @return HpcMetadataEntry collection.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getCollectionMetadata(String path) throws HpcException;
    
    /**
     * Get data object by its path.
     *
     * @param path The data object's path.
     * @return HpcDataObject.
     * 
     * @throws HpcException
     */
    public HpcDataObject getDataObject(String path) throws HpcException;
    
    /**
     * Get data objects by metadata query.
     *
     * @param metadataQueries The metadata queries.
     * @return HpcDataObject list.
     * 
     * @throws HpcException
     */
    public List<HpcDataObject> getDataObjects(
    		    List<HpcMetadataQuery> metadataQueries) throws HpcException;
    
    /**
     * Get metadata of a data object.
     *
     * @param path The collection path.
     * @return HpcMetadataEntry collection.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getDataObjectMetadata(String path) throws HpcException;
    
    /**
     * Get the user type of this service invoker.
     *
     * @return The user's type
     * 
     * @throws HpcException
     */
    public String getUserType() throws HpcException;  
    
    /**
     * Close connection to Data Management system for the current service call.
     */
    public void closeConnection();
    
    /**
     * Set permission of an entity (collection or data object) for a user. 
     *
     * @param path The entity path.
     * @param permissionRequest The permission request.
     * @return HpcMetadataEntry collection.
     * 
     * @throws HpcException
     */
    public void setPermission(String path, HpcUserPermission permissionRequest) 
    		                 throws HpcException;
}

 