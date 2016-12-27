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

import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.exception.HpcException;

import java.util.Arrays;

import javax.ws.rs.core.Response;

import org.junit.Test;

/**
 * <p>
 * HPC Data Management Rest Service Tests.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcRegisterCollectionTest extends HpcRestServiceTest
{   
    //---------------------------------------------------------------------//
    // Unit Tests
    //---------------------------------------------------------------------//
    
    @Test
    public void testNullPath() throws HpcException 
    {
    	Response response = getDataManagementClient().registerCollection("", Arrays.asList());	
    	assertEquals(response.getStatus(), 200);
    	HpcExceptionDTO exceptionDTO = (HpcExceptionDTO) response.getEntity();
    	logger.info(exceptionDTO.toString());
    }
    
    /*
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
    }*/
}

 
