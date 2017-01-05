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

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntry;
import gov.nih.nci.hpc.dto.datamanagement.HpcCollectionRegistrationDTO;
import gov.nih.nci.hpc.dto.error.HpcExceptionDTO;
import gov.nih.nci.hpc.exception.HpcException;

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
    	// Invoke the service.
    	Response response = dataManagementClient.registerCollection("", new HpcCollectionRegistrationDTO());	
    	
    	// Assert expected result.
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
    	// Invoke the service.
    	Response response = dataManagementClient.registerCollection("/UnitTest/Collection", 
    			                                                    new HpcCollectionRegistrationDTO());	
    	
    	// Assert expected result.
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
    public void testInvalidMetadataEntry() throws HpcException 
    {
    	// Mock Integration / DAO services.
    	HpcPathAttributes pathAttributes = new HpcPathAttributes();
    	pathAttributes.setExists(true);
    	pathAttributes.setIsAccessible(true);
    	pathAttributes.setIsDirectory(true);
    	pathAttributes.setIsFile(false);
    	when(dataManagementProxyMock.getPathAttributes(anyObject(), eq("/UnitTest/Collection"))).thenReturn(pathAttributes);
    	
    	// Prepare Test Input.
    	HpcCollectionRegistrationDTO collectionRegistration = new HpcCollectionRegistrationDTO();
    	HpcMetadataEntry entry = new HpcMetadataEntry();
    	entry.setAttribute("attribute");
    	collectionRegistration.getMetadataEntries().add(entry);
    	
    	// Invoke the service.
    	Response response = dataManagementClient.registerCollection("/UnitTest/Collection", 
    			                                                    collectionRegistration);

    	// Assert expected result.
    	assertEquals(HTTP_STATUS_CODE_MSG, 400, response.getStatus());
    	HpcExceptionDTO exceptionDTO = response.readEntity(HpcExceptionDTO.class);
    	assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exceptionDTO.getErrorType());
    	assertEquals(EXCEPTION_MSG, "Invalid path or metadata entry", exceptionDTO.getMessage());
    }
}

 
