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
import gov.nih.nci.hpc.domain.model.HpcDocConfiguration;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC DOC configuration loctor. A map for supported DOC to their configuration.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
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
     * Load the DOC configurations from the DB.
     *
     * @param docBasePaths The base paths in a config-string format.
     * @throws HpcException On configuration error.
     */
    @PostConstruct
	public void initialize() throws HpcException
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
    
	/**
     * Get all the DOC base paths.
     *
     * @return A list of all DOC base paths.
     */
	public Set<String> getBasePaths() 
    {
		return basePaths;
    }
}

 