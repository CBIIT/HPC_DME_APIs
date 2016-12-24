/**
 * HpcDataManagementRestServiceTest.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.test;

import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * HPC Data Management Rest Service Tests.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcDataManagementRestServiceTest extends HpcRestServiceTest
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//

    // The logger instance.
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
    //---------------------------------------------------------------------//
    // Unit Tests
    //---------------------------------------------------------------------//
    
    @Test
    public void testRegistration() 
    {
    	logger.info("*** ERAN test 1 ***");
    	HpcDataManagementRestService dataManagementClient = getDataManagementClient();
    	Response response = dataManagementClient.getCollection("/eran");
    	logger.error("ERAN: " + response);
    	assertEquals(response.getStatus(), 200);
    	
    }
    
    @Test
    public void testRegistration1() 
    {
    	logger.info("*** ERAN test 2 ***");
    }
}

 
