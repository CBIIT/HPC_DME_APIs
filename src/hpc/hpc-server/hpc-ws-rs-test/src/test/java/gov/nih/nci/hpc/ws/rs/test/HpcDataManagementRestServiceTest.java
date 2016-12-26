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

import gov.nih.nci.hpc.domain.datamanagement.HpcCollection;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.ws.rs.HpcDataManagementRestService;

import javax.ws.rs.core.Response;

import org.junit.Test;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

import org.mockito.MockitoAnnotations;
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
    public void testRegistration() throws HpcException 
    {
    	logger.info("*** ERAN test 1 ***");
    	
    	HpcPathAttributes pathAttributes = new HpcPathAttributes();
    	pathAttributes.setExists(true);
    	pathAttributes.setIsAccessible(true);
    	pathAttributes.setIsDirectory(true);
    	pathAttributes.setIsFile(false);
    	when(dataManagementProxyMock.getPathAttributes(anyObject(), anyString())).thenReturn(pathAttributes);
    	
    	HpcCollection collection = new HpcCollection();
    	collection.setCollectionId(12345);
    	collection.setAbsolutePath("/eran");
    	when(dataManagementProxyMock.getCollection(anyObject(), anyString())).thenReturn(collection);
    	
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

 
