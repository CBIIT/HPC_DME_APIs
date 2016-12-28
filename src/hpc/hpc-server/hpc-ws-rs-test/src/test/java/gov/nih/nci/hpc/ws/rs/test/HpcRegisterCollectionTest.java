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

import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    
    /**
     * Test: Empty collection path in registration request.
     * Expected: [400] Null path or metadata entries. 
     */
    @Test
    public void testEmptyCollectionPath()  
    {
    	Response response = dataManagementClient.registerCollection("", Arrays.asList());	
    	assertEquals(HTTP_STATUS_CODE_MSG, 400, response.getStatus());
    	HpcExceptionDTO exceptionDTO = response.readEntity(HpcExceptionDTO.class);
    	assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exceptionDTO.getErrorType());
    	assertEquals(EXCEPTION_MSG, "Null path or metadata entries", exceptionDTO.getMessage());
    }
    
    /**
     * Test: Empty metadata entries in registration request.
     * Expected: [400] Null path or metadata entries. 
     */
    @Test
    public void testEmptyMetadataEntries()  
    {
    	Response response = dataManagementClient.registerCollection("/UnitTest/Collection", Arrays.asList());	
    	assertEquals(HTTP_STATUS_CODE_MSG, 400, response.getStatus());
    	HpcExceptionDTO exceptionDTO = response.readEntity(HpcExceptionDTO.class);
    	assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exceptionDTO.getErrorType());
    	assertEquals(EXCEPTION_MSG, "Null path or metadata entries", exceptionDTO.getMessage());
    }
    
    /**
     * Test: Invalid metadata entry in registration request.
     * Expected: [400] . 
     */
    @Test
    public void testInvalidMandatoryMetadataEntry()  
    {
    	HpcMetadataEntry entry = new HpcMetadataEntry();
    	entry.setAttribute("attribute");
    	
    	List<HpcMetadataEntry> metadataEntries = new ArrayList<>();
    	metadataEntries.add(entry);
    	
    	Response response = dataManagementClient.registerCollection("/UnitTest/Collection", metadataEntries);	
    	assertEquals(HTTP_STATUS_CODE_MSG, 400, response.getStatus());
    	HpcExceptionDTO exceptionDTO = response.readEntity(HpcExceptionDTO.class);
    	assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exceptionDTO.getErrorType());
    	assertEquals(EXCEPTION_MSG, "Invalid metadata entries", exceptionDTO.getMessage());
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
    */

}

 
