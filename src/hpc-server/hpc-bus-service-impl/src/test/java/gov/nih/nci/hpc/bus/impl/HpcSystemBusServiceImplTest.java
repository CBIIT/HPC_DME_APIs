package gov.nih.nci.hpc.bus.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcNotificationService;

class HpcSystemBusServiceImplTest {

    // Mocks the dependencies
    @Mock
    private HpcMetadataService metadataService;
    @Mock
    private HpcDataTransferService dataTransferService;
    @Mock
    private HpcNotificationService notificationService;

    // The bus service under test.
    @InjectMocks
    private HpcSystemBusServiceImpl service;
    
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }
    
    /*
     * Test Case: originalPath input is null. 
     * Expected: true returned from canRemoveDeletedDataObject
     */
    @Test
    void testCanRemoveDeletedDataObject_OriginalPathNull() throws HpcException {
        // If originalPath is null, should return true
        
        assertTrue(service.canRemoveDeletedDataObject("somePath", null));
    }
    
    /*
     * Test Case: The archive path exist and different from originalPath. 
     * Expected: true returned from canRemoveDeletedDataObject
     */
    @Test
    void testCanRemoveDeletedDataObject_WithDiffArchivePathFromOriginalPath() throws HpcException {
      
        // Test case where a record exist in original path but the archive file is different, should return true
        HpcSystemGeneratedMetadata somePathMetadata = new HpcSystemGeneratedMetadata();
        HpcFileLocation someArchiveLocation = new HpcFileLocation();
        someArchiveLocation.setFileId("someArchivePath");
        somePathMetadata.setArchiveLocation(someArchiveLocation);
        doReturn(somePathMetadata).when(metadataService).getDataObjectSystemGeneratedMetadata("somePath");
        
        HpcSystemGeneratedMetadata originalPathMetadata = new HpcSystemGeneratedMetadata();
        HpcFileLocation originalArchiveLocation = new HpcFileLocation();
        originalArchiveLocation.setFileId("originalArchivePath");
        originalPathMetadata.setArchiveLocation(originalArchiveLocation);
        doReturn(originalPathMetadata).when(metadataService).getDataObjectSystemGeneratedMetadata("originalPath");
        
        
        HpcPathAttributes somePathAttributes = new HpcPathAttributes();
        somePathAttributes.setExists(true);
        somePathAttributes.setIsFile(true);
        doReturn(somePathAttributes).when(dataTransferService).getPathAttributes(any(),any(),anyBoolean(),any(),any());
        
        doNothing().when(notificationService).sendNotification(any());
        
        assertTrue(service.canRemoveDeletedDataObject("somePath", "originalPath"));
    }
    
    /*
     * Test Case: The archive path exist and is the same am originalPath. 
     * Expected: false returned from canRemoveDeletedDataObject
     */
    @Test
    void testCanRemoveDeletedDataObject_WithSameArchivePathAsOriginalPath() throws HpcException {
      
        // Test case where a record exist in original path and the archive file points to the same, should return false
        HpcSystemGeneratedMetadata somePathMetadata = new HpcSystemGeneratedMetadata();
        HpcFileLocation someArchiveLocation = new HpcFileLocation();
        someArchiveLocation.setFileId("someArchivePath");
        somePathMetadata.setArchiveLocation(someArchiveLocation);
        doReturn(somePathMetadata).when(metadataService).getDataObjectSystemGeneratedMetadata("somePath");
        
        HpcSystemGeneratedMetadata originalPathMetadata = new HpcSystemGeneratedMetadata();
        HpcFileLocation originalArchiveLocation = new HpcFileLocation();
        originalArchiveLocation.setFileId("someArchivePath");
        originalPathMetadata.setArchiveLocation(originalArchiveLocation);
        doReturn(originalPathMetadata).when(metadataService).getDataObjectSystemGeneratedMetadata("originalPath");
        
        
        HpcPathAttributes somePathAttributes = new HpcPathAttributes();
        somePathAttributes.setExists(true);
        somePathAttributes.setIsFile(true);
        doReturn(somePathAttributes).when(dataTransferService).getPathAttributes(any(),any(),anyBoolean(),any(),any());
        
        doNothing().when(notificationService).sendNotification(any());
        
        assertFalse(service.canRemoveDeletedDataObject("somePath", "originalPath"));
    }
    
    /*
     * Test Case: The record exist for originalPath but file is missing (orphaned record)
     * Expected: false returned from canRemoveDeletedDataObject
     */
    @Test
    void testCanRemoveDeletedDataObject_WithOrphanedArchivePath() throws HpcException {
      
        // Test case where a record exist in original path but the archive file is missing (orphaned record)
        HpcSystemGeneratedMetadata somePathMetadata = new HpcSystemGeneratedMetadata();
        HpcFileLocation someArchiveLocation = new HpcFileLocation();
        someArchiveLocation.setFileId("someArchivePath");
        somePathMetadata.setArchiveLocation(someArchiveLocation);
        doReturn(somePathMetadata).when(metadataService).getDataObjectSystemGeneratedMetadata("somePath");
        
        HpcSystemGeneratedMetadata originalPathMetadata = new HpcSystemGeneratedMetadata();
        HpcFileLocation originalArchiveLocation = new HpcFileLocation();
        originalArchiveLocation.setFileId("originalArchivePath");
        originalPathMetadata.setArchiveLocation(originalArchiveLocation);
        doReturn(originalPathMetadata).when(metadataService).getDataObjectSystemGeneratedMetadata("originalPath");
        
        
        HpcPathAttributes somePathAttributes = new HpcPathAttributes();
        somePathAttributes.setExists(false);
        somePathAttributes.setIsFile(false);
        doReturn(somePathAttributes).when(dataTransferService).getPathAttributes(any(),any(),anyBoolean(),any(),any());
        
        doNothing().when(notificationService).sendNotification(any());
        
        assertFalse(service.canRemoveDeletedDataObject("somePath", "originalPath"));
    }
    
}