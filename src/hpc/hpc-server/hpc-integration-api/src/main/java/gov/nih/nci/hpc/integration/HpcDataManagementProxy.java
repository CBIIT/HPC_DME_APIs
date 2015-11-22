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

import gov.nih.nci.hpc.domain.dataset.HpcCollection;
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
     * Check if a path exists (as a directory or file)
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The data-object/collection path.
     * 
     * @throws HpcException
     */
    public boolean exists(HpcIntegratedSystemAccount dataManagementAccount, 
    		              String path)
    		             throws HpcException;  
    
    /**
     * Get collections by metadata query.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param metadataQueries The metadata entries to query for.
     * @return HpcDataset collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcCollection> getCollections(
    		    HpcIntegratedSystemAccount dataManagementAccount,
    		    List<HpcMetadataQuery> metadataQueries) throws HpcException;
    
    /**
     * Get metadata of a collection.
     *
     * @param dataManagementAccount The Data Management System account.
     * @param path The collection path.
     * @return HpcMetadataEntry collection, or null if no results found.
     * 
     * @throws HpcException
     */
    public List<HpcMetadataEntry> getCollectionMetadata(
   		                          HpcIntegratedSystemAccount dataManagementAccount, 
   		                          String path) throws HpcException;
}

 