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

import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
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
     * @param dataManagementAccount The Data Management account.
     * @param path The collection path.
     * 
     * @throws HpcException
     */
    public void createDirectory(HpcIntegratedSystemAccount dataManagementAccount,
    		                    String path) 
    		                   throws HpcException;
    
    /**
     * Create a data object's file.
     *
     * @param dataManagementAccount The Data Management account.
     * @param path The data object path.
     * 
     * @throws HpcException
     */
    public void createFile(HpcIntegratedSystemAccount dataManagementAccount,
    		               String path) 
    		              throws HpcException;

    /**
     * Add metadata to a collection.
     *
     * @param dataManagementAccount The Data Management account.
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
     * @param dataManagementAccount The Data Management account.
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
     * Create and attach file (physical) location and source metadata to a data object.
     *
     * @param dataManagementAccount The Data Management account.
     * @param path The data object path.
     * @param fileLocation The physical file location.
     * @param fileSource The source location of the file.
     * 
     * @throws HpcException
     */
    public void addFileLocationsMetadataToDataObject(
    		           HpcIntegratedSystemAccount dataManagementAccount,
    		           String path, 
    		           HpcFileLocation fileLocation,
    		           HpcFileLocation fileSource) 
    		           throws HpcException; 
}

 