/**
 * HpcDocConfigurationLocator.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.dao.HpcDocConfigurationDAO;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.model.HpcDocConfiguration;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC DOC configuration locator. A map for supported DOC to their configuration.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcDocConfigurationLocator extends HashMap<String, HpcDocConfiguration> 
{        
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	private static final long serialVersionUID = -2233828633688868458L;

	// The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
    // The DOC Configuration DAO instance.
	@Autowired
    private HpcDocConfigurationDAO docConfigurationDAO = null;
	
	// A set of all DOC base path (to allow quick search)
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
    private HpcDocConfigurationLocator()
    {
    	super();
    }
    
	//---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//

	/**
     * Get all the DOC base paths.
     *
     * @return A list of all DOC base paths.
     */
	public Set<String> getBasePaths() 
    {
		return basePaths;
    }
	
	/**
     * Get Archive connection URL for a specific DOC and data transfer type.
     * Note: At this time only S3 URL is configurable, but not Globus.
     *
     * @param doc The DOC.
     * @param dataTransferType The data transfer type.
     * @return The archive URL for the requested DOC and data-transfer.
     */
	public String getArchiveURL(String doc, HpcDataTransferType dataTransferType) 
    {
		HpcDocConfiguration docConfiguration = get(doc);
		if(docConfiguration != null && dataTransferType.equals(HpcDataTransferType.S_3)) {
		   return docConfiguration.getS3URL();
		}
		return null;
    }
	
	/**
     * Get base archive destination for a specific DOC and data transfer type.
     * Note: At this time only S3 base archive destination is configurable, but not Globus.
     *
     * @param doc The DOC.
     * @param dataTransferType The data transfer type.
     * @return The base archive destination  for the requested DOC and data-transfer.
     */
	public HpcArchive getBaseArchiveDestination(String doc, HpcDataTransferType dataTransferType) 
    {
		HpcDocConfiguration docConfiguration = get(doc);
		if(docConfiguration != null && dataTransferType.equals(HpcDataTransferType.S_3)) {
		   return docConfiguration.getS3BaseArchiveDestination();
		}
		return null;
    }
	
	/**
     * Load the DOC configurations from the DB. Called by spring as init-method.
     *
     * @throws HpcException On configuration error.
     */
	public void reload() throws HpcException
    {
    	clear();
    	basePaths.clear();
    	
    	for(HpcDocConfiguration docConfiguration : docConfigurationDAO.getDocConfigurations()) {
    		// Ensure the base path is in the form of a relative path.
    		docConfiguration.setBasePath(dataManagementProxy.getRelativePath(docConfiguration.getBasePath()));
    		
    		put(docConfiguration.getDoc(), docConfiguration);
    		basePaths.add(docConfiguration.getBasePath());
    	}
    	
    	logger.info("DOC Configurations: " + toString());
    }
}

 