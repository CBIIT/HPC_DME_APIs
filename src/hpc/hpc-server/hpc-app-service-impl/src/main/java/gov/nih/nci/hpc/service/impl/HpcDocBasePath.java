/**
 * HpcDocBasePath.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataManagementProxy;

import java.util.Arrays;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * HPC DOC base path. A map for supported DOC to their data management base path
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDocBasePath extends HashMap<String, String> 
{        
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	private static final long serialVersionUID = -5080812833698352085L;
	
    // The Data Management Proxy instance.
	@Autowired
    private HpcDataManagementProxy dataManagementProxy = null;
	
	//---------------------------------------------------------------------//
    // Constructors
    //---------------------------------------------------------------------//

	/**
     * Constructor for Spring Dependency Injection.
     *
     * @param docBasePaths A whitespace separated list of valid DOC values and their base path.
     * @throws HpcException On configuration error
     */
    private HpcDocBasePath(String docBasePaths) throws HpcException
    {
    	super();
    	for(String docBasePath : Arrays.asList(docBasePaths.split("\\s+"))) {
    		String[] splitDocBasePath= docBasePath.split("=");
    		if(splitDocBasePath.length != 2) {
    		   throw new HpcException("Invalid DOC base path configuration: " + docBasePaths,
			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    		}
    		
    		put(splitDocBasePath[0], dataManagementProxy.getAbsolutePath(splitDocBasePath[1]));
    	}
    }
    
    /**
     * Default constructor disabled.
     *
     * @throws HpcException Constructor is disabled.
     */
    private HpcDocBasePath() throws HpcException
    {
    	throw new HpcException("Constructor disabled",
    			               HpcErrorType.SPRING_CONFIGURATION_ERROR);
    }
}

 