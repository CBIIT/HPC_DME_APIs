/**
 * HpcDataManagementConfigurationLocator.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.dao.HpcDataManagementConfigurationDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

/**
 * <p>
 * HPC Data Management configuration locator. 
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDataManagementConfigurationLocator extends HashMap<String, HpcDataManagementConfiguration> 
{        
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	private static final long serialVersionUID = -2233828633688868458L;

	// The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
    // The Data Management Configuration DAO instance.
	@Autowired
    private HpcDataManagementConfigurationDAO dataManagementConfigurationDAO = null;
	
	// A set of all supported base paths (to allow quick search).
	private Set<String> basePaths = new HashSet<>();
	
    // The logger instance.
	private final Logger logger = 
			             LoggerFactory.getLogger(this.getClass().getName());
	
	//---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

    /**
     * Default constructor for Spring Dependency Injection.
     *
     */
    private HpcDataManagementConfigurationLocator()
    {
    	super();
    }
    
	//---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//

	/**
     * Get all supported base paths.
     *
     * @return A list of all supported base paths.
     */
	public Set<String> getBasePaths() 
    {
		return basePaths;
    }
	
	/**
     * Get Archive connection URL for a specific configuration and data transfer type.
     * Note: At this time only S3 URL is configurable, but not Globus.
     *
     * @param configurationId The data management configuration ID.
     * @param dataTransferType The data transfer type.
     * @return The archive URL for the requested DOC and data-transfer.
     */
	public String getArchiveURL(String configurationId, HpcDataTransferType dataTransferType) 
    {
		HpcDataManagementConfiguration dataManagementConfiguration = get(configurationId);
		if(dataManagementConfiguration != null && dataTransferType.equals(HpcDataTransferType.S_3)) {
		   return dataManagementConfiguration.getS3URL();
		}
		return null;
    }
	
	/**
     * Get base archive destination for a specific configuration and data transfer type.
     * Note: At this time only S3 base archive destination is configurable, but not Globus.
     *
     * @param configurationId The data management configuration ID.
     * @param dataTransferType The data transfer type.
     * @return The base archive destination  for the requested DOC and data-transfer.
     */
	public HpcArchive getBaseArchiveDestination(String configurationId, 
			                                    HpcDataTransferType dataTransferType) 
    {
		HpcDataManagementConfiguration dataManagementConfiguration = get(configurationId);
		if(dataManagementConfiguration != null && dataTransferType.equals(HpcDataTransferType.S_3)) {
		   return dataManagementConfiguration.getS3BaseArchiveDestination();
		}
		return null;
    }
	
	/**
     * Load the data management configurations from the DB. Called by spring as init-method.
     *
     * @throws HpcException On configuration error.
     */
	public void reload() throws HpcException
    {
		clear();
    	basePaths.clear();
    	
    	for(HpcDataManagementConfiguration dataManagementConfiguration : 
    		dataManagementConfigurationDAO.getDataManagementConfigurations()) {
    		// Ensure the base path is in the form of a relative path.
    		dataManagementConfiguration.setBasePath(dataManagementProxy.getRelativePath(dataManagementConfiguration.getBasePath()));
    		
    		// Ensure base path is unique (i.e. no 2 configurations share the same base path).
    		if(!basePaths.add(dataManagementConfiguration.getBasePath())) {
    		   throw new HpcException("Duplicate base-path in data management configurations:" + 
    				                  dataManagementConfiguration.getBasePath(), 
    				                  HpcErrorType.UNEXPECTED_ERROR);	
    		}
    		
    		put(dataManagementConfiguration.getId(), dataManagementConfiguration);
    	}
    	
    	logger.info("Data Management Configurations: " + toString());
    }
}

 