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
import gov.nih.nci.hpc.domain.datamanagement.HpcEntityPermission;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Management Application Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDataManagementNewService 
{   
    /**
     * Create a collection's directory.
     *
     * @param path The collection path.
     * @return true if the directory was created, or false if it already exists.
     * 
     * @throws HpcException
     */
    public boolean createDirectory(String path) throws HpcException;
    
    /**
     * Create a data object's file.
     *
     * @param path The data object path.
     * @return true if the data object file was created, or false if it already exists.
     * 
     * @throws HpcException
     */
    public boolean createFile(String path) throws HpcException;
    
    /**
     * Delete a path (data object or directory).
     *
     * @param path The path to delete.
     * @throws HpcException
     */
    public void delete(String path) throws HpcException;
    
    /**
     * Set permission of an entity (collection or data object) for a user. 
     *
     * @param path The entity path.
     * @param permissionRequest The permission request (NCI user ID and permission).
     * @return HpcPathAttributes of the path.
     * 
     * @throws HpcException If it failed to set the entity permission.
     */
    public HpcPathAttributes setPermission(String path,
    		                               HpcEntityPermission permissionRequest) 
    		                              throws HpcException;
    
    /**
     * Assign system account as an additional owner of an entity.
     *
     * @param path The entity path.
     * @return HpcPathAttributes of the path.
     * 
     * @throws HpcException If it failed to set the entity permission.
     */
    public HpcPathAttributes assignSystemAccountPermission(String path) 
    		                                              throws HpcException;
    
    /**
     * Validate a path against a hierarchy definition.
     *
     * @param path The collection path.
     * @param doc Use validation rules of this DOC.
     * @param dataObjectRegistration If true, the service validates if data object registration is allowed 
     *                               in this collection
     * 
     * @throws HpcException If the hierarchy is invalid.
     */
    public void validateHierarchy(String path, String doc,
    		                      boolean dataObjectRegistration) 
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
     * Get data object by its path.
     *
     * @param path The data object's path.
     * @return HpcDataObject.
     * 
     * @throws HpcException
     */
    public HpcDataObject getDataObject(String path) throws HpcException;
}

 