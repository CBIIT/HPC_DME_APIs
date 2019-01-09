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

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * <p>
 * HPC Data Management Rest Service Tests.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */

public class HpcRegisterCollectionTest extends HpcRestServiceTest
{   
    //---------------------------------------------------------------------//
    // Instance members
    //---------------------------------------------------------------------//
	
	// Path attributes mocking a path that doesn't exist.
	private static HpcPathAttributes pathNotExist = new HpcPathAttributes();
	
	// Path attributes mocking a path that doesn't exist.
	private static HpcPathAttributes pathExistsAsFile = new HpcPathAttributes();
	
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
    	Response response = 
    	dataManagementClient.registerCollection("/UnitTest/Collection", 
    	                                        createCollectionRegistrationRequest(null, null, null));	
    	
    	// Assert expected result.
    	assertEquals(HTTP_STATUS_CODE_MSG, 400, response.getStatus());
    	HpcExceptionDTO exceptionDTO = response.readEntity(HpcExceptionDTO.class);
    	assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exceptionDTO.getErrorType());
    	assertEquals(EXCEPTION_MSG, "Null path or metadata entries", exceptionDTO.getMessage());
    }
    
    /**
     * Test: Invalid collection path in registration request. Path equals to DOC base path.
     * Expected: [400] Invalid collection path: /UnitTest. 
     */
    @Test
    public void testInvalidCollectionPathDOC() throws HpcException 
    {
    	// Mock Integration / DAO services.
    	when(dataManagementProxyMock.getRelativePath(eq("/UnitTest"))).thenReturn("/UnitTest");
    	
    	// Invoke the service.
    	HpcCollectionRegistrationDTO collectionRegistrationRequest = 
    	   createCollectionRegistrationRequest("ParentColllection", "text-val", null);
    	Response response = dataManagementClient.registerCollection("/UnitTest", 
    			                                                    collectionRegistrationRequest);

    	// Assert expected result.
    	assertEquals(HTTP_STATUS_CODE_MSG, 400, response.getStatus());
    	HpcExceptionDTO exceptionDTO = response.readEntity(HpcExceptionDTO.class);
    	assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exceptionDTO.getErrorType());
    	assertEquals(EXCEPTION_MSG, "Invalid collection path: /UnitTest", exceptionDTO.getMessage());
    }
    
    /**
     * Test: Invalid collection path in registration request. Path equals to root.
     * Expected: [400] Invalid collection path: /. 
     */
    @Test
    public void testInvalidCollectionPathRoot() throws HpcException 
    {
    	// Mock Integration / DAO services.
    	when(dataManagementProxyMock.getRelativePath(eq("/"))).thenReturn("/");
    	
    	// Invoke the service.
    	HpcCollectionRegistrationDTO collectionRegistrationRequest = 
    	   createCollectionRegistrationRequest("ParentColllection", "text-val", null);
    	Response response = dataManagementClient.registerCollection("/", collectionRegistrationRequest);

    	// Assert expected result.
    	assertEquals(HTTP_STATUS_CODE_MSG, 400, response.getStatus());
    	HpcExceptionDTO exceptionDTO = response.readEntity(HpcExceptionDTO.class);
    	assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exceptionDTO.getErrorType());
    	assertEquals(EXCEPTION_MSG, "Invalid collection path: /", exceptionDTO.getMessage());
    }
    
    /**
     * Test: Invalid collection path in registration request. Path exists as a file.
     * Expected: [400] Invalid collection path: /. 
     */
    @Test
    public void testInvalidCollectionPathExistsAsFile() throws HpcException 
    {
    	// Mock Integration / DAO services.
    	when(dataManagementProxyMock.getPathAttributes(anyObject(), eq("/UnitTest/file"))).thenReturn(pathExistsAsFile);
    	when(dataManagementProxyMock.getRelativePath(eq("/UnitTest/file"))).thenReturn("/UnitTest/file");
    	
    	// Invoke the service.
    	HpcCollectionRegistrationDTO collectionRegistrationRequest = 
    	   createCollectionRegistrationRequest("ParentColllection", "text-val", null);
    	Response response = dataManagementClient.registerCollection("/UnitTest/file", collectionRegistrationRequest);

    	// Assert expected result.
    	assertEquals(HTTP_STATUS_CODE_MSG, 400, response.getStatus());
    	HpcExceptionDTO exceptionDTO = response.readEntity(HpcExceptionDTO.class);
    	assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, exceptionDTO.getErrorType());
    	assertEquals(EXCEPTION_MSG, "Path already exists as a file: /UnitTest/file", exceptionDTO.getMessage());
    }
    
    //---------------------------------------------------------------------//
    // Helper Methods
    //---------------------------------------------------------------------//
    
    /**
     * Init static path attributes used in mocking.
     */
    @BeforeClass
    public static void initPathAttributes()  
    {
    	pathNotExist.setExists(false);
    	pathNotExist.setIsAccessible(true);
    	pathNotExist.setIsDirectory(false);
    	pathNotExist.setIsFile(false);
    	
    	pathExistsAsFile.setExists(true);
    	pathExistsAsFile.setIsAccessible(true);
    	pathExistsAsFile.setIsDirectory(false);
    	pathExistsAsFile.setIsFile(true);
    }
    
    /** 
     * Create collection registration request.
     * 
     * @param type The collection type
     * @param text 'text' metadata value
     * @param choice 'choice' metadata value
     * @return collection registration request DTO
     */
    private HpcCollectionRegistrationDTO createCollectionRegistrationRequest(String type, String text, String choice)
    {
		HpcCollectionRegistrationDTO collectionRegistration = new HpcCollectionRegistrationDTO();
		
		if(type != null) {
		   HpcMetadataEntry typeMetadata = new HpcMetadataEntry();
		   typeMetadata.setAttribute("collection_type");
		   typeMetadata.setValue(type);
		   collectionRegistration.getMetadataEntries().add(typeMetadata);
		}
		if(text != null) {
		   HpcMetadataEntry textMetadata = new HpcMetadataEntry();
		   textMetadata.setAttribute("text");
		   textMetadata.setValue(text);
		   collectionRegistration.getMetadataEntries().add(textMetadata);
		}
		if(choice != null) {
		   HpcMetadataEntry choiceMetadata = new HpcMetadataEntry();
		   choiceMetadata.setAttribute("choice");
		   choiceMetadata.setValue(choice);
		   collectionRegistration.getMetadataEntries().add(choiceMetadata);
		}
		return collectionRegistration;
    }
}

 
