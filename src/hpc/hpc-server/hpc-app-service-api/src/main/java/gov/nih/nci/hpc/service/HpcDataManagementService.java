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
import gov.nih.nci.hpc.domain.datamanagement.HpcDataHierarchy;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcEntityPermission;
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
     * @return true if the directory was created, or false if it already exists.
     * @throws HpcException on service failure.
     */
    public boolean createDirectory(String path) throws HpcException;
    
    /**
     * Create a data object's file.
     *
     * @param path The data object path.
     * @return true if the data object file was created, or false if it already exists.
     * @throws HpcException on service failure.
     */
    public boolean createFile(String path) throws HpcException;
    
    /**
     * Delete a path (data object or directory).
     *
     * @param path The path to delete.
     * @throws HpcException on service failure.
     */
    public void delete(String path) throws HpcException;
    
    /**
     * Set permission of an entity (collection or data object) for a user. 
     *
     * @param path The entity path.
     * @param permissionRequest The permission request (NCI user ID and permission).
     * @throws HpcException on service failure.
     */
    public void setPermission(String path, HpcEntityPermission permissionRequest) 
    		                 throws HpcException;
    
    /**
     * Assign system account as an additional owner of an entity.
     *
     * @param path The entity path.
     * @throws HpcException on service failure.
     */
    public void assignSystemAccountPermission(String path) throws HpcException;
    
    /**
     * Validate a path against a hierarchy definition.
     *
     * @param path The collection path.
     * @param doc Use validation rules of this DOC.
     * @param dataObjectRegistration If true, the service validates if data object registration is allowed 
     *                               in this collection
     * @throws HpcException If the hierarchy is invalid.
     */
    public void validateHierarchy(String path, String doc,
    		                      boolean dataObjectRegistration) 
    		                     throws HpcException;
    
    /**
     * Get collection by its path.
     *
     * @param path The collection's path.
     * @param list An indicator to list sub-collections and data-objects.
     * @return A collection.
     * @throws HpcException on service failure.
     */
    public HpcCollection getCollection(String path, boolean list) throws HpcException;
    
    /**
     * Get data object by its path.
     *
     * @param path The data object's path.
     * @return A data object.
     * @throws HpcException on service failure.
     */
    public HpcDataObject getDataObject(String path) throws HpcException;
    
    /**
     * Get data hierarchy of a DOC.
     * 
     * @param doc The DOC.
     * @return The DOC's data hierarchy.
     * @throws HpcException on service failure.
     */
    public HpcDataHierarchy getDataHierarchy(String doc) throws HpcException;
    
    /**
     * Get data objects that have their data transfer in-progress.
     *
     * @return A list of data objects.
     * @throws HpcException on service failure.
     */
    public List<HpcDataObject> getDataObjectsInProgress() throws HpcException;
    
    /**
     * Get data objects that have their data stored in temporary archive.
     *
     * @return A list of data objects.
     * @throws HpcException on service failure.
     */
    public List<HpcDataObject> getDataObjectsInTemporaryArchive() throws HpcException;
    
    /**
     * Close connection to Data Management system for the current service call.
     */
    public void closeConnection();
}

 