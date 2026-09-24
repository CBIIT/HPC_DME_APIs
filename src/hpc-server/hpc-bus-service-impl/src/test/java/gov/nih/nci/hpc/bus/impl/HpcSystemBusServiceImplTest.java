package gov.nih.nci.hpc.bus.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.bus.HpcDataManagementBusService;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationItemDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;
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
    @Mock
    private HpcDataManagementBusService dataManagementBusService;
    @Mock
    private HpcEventService eventService;

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

        @Test
        void testProcessExternalDownloadTask_ReturnsDownloadItemsForEachRegistration() throws Exception {
        HpcCollectionDownloadTask downloadTask = externalDownloadTask();
        HpcBulkDataObjectRegistrationResponseDTO registrationResponse = new HpcBulkDataObjectRegistrationResponseDTO();
        HpcDataObjectRegistrationItemDTO registrationItem = new HpcDataObjectRegistrationItemDTO();
        registrationItem.setPath("/external/file.txt");
        registrationResponse.getDataObjectRegistrationItems().add(registrationItem);

        HpcDataObjectDownloadResponseDTO downloadResponse = new HpcDataObjectDownloadResponseDTO();
        downloadResponse.setTaskId("data-task-1");
        when(dataManagementBusService.getFilesFromExternalSource(downloadTask)).thenReturn(registrationResponse);
        when(dataManagementBusService.downloadDataObject(eq("/external/file.txt"), any(), isNull(),
            eq("test-user"), isNull(), eq(false), eq("collection-task-1"), eq(true)))
            .thenReturn(downloadResponse);

        List<HpcCollectionDownloadTaskItem> downloadItems = invokeProcessExternalDownloadTask(downloadTask);

        assertEquals(1, downloadItems.size());
        assertEquals("/external/file.txt", downloadItems.get(0).getPath());
        assertEquals("data-task-1", downloadItems.get(0).getDataObjectDownloadTaskId());
        verify(dataManagementBusService).getFilesFromExternalSource(downloadTask);
        verify(dataManagementBusService).downloadDataObject(eq("/external/file.txt"), any(), isNull(),
            eq("test-user"), isNull(), eq(false), eq("collection-task-1"), eq(true));
        }

        @Test
        void testProcessExternalDownloadTask_FailureCompletesTaskAndReturnsEmptyList() throws Exception {
        HpcCollectionDownloadTask downloadTask = externalDownloadTask();
        when(dataManagementBusService.getFilesFromExternalSource(downloadTask))
            .thenThrow(new HpcException("scan failed", gov.nih.nci.hpc.domain.error.HpcErrorType.INVALID_REQUEST_INPUT));

        List<HpcCollectionDownloadTaskItem> downloadItems = invokeProcessExternalDownloadTask(downloadTask);

        assertTrue(downloadItems.isEmpty());
        verify(dataTransferService).completeCollectionDownloadTask(eq(downloadTask), eq(HpcDownloadResult.FAILED),
            eq("scan failed"), any(java.util.Calendar.class));
        verify(dataManagementBusService, never()).downloadDataObject(anyString(), any(), any(), anyString(), any(),
            anyBoolean(), anyString(), anyBoolean());
        }

        @Test
        void testProcessExternalDownloadTask_CompletionFailureIsHandled() throws Exception {
        HpcCollectionDownloadTask downloadTask = externalDownloadTask();
        when(dataManagementBusService.getFilesFromExternalSource(downloadTask))
            .thenThrow(new HpcException("scan failed", gov.nih.nci.hpc.domain.error.HpcErrorType.INVALID_REQUEST_INPUT));
        doThrow(new HpcException("completion failed", gov.nih.nci.hpc.domain.error.HpcErrorType.UNEXPECTED_ERROR))
            .when(dataTransferService).completeCollectionDownloadTask(eq(downloadTask), eq(HpcDownloadResult.FAILED),
                eq("scan failed"), any(java.util.Calendar.class));

        assertTrue(invokeProcessExternalDownloadTask(downloadTask).isEmpty());
        verify(dataTransferService).completeCollectionDownloadTask(eq(downloadTask), eq(HpcDownloadResult.FAILED),
            eq("scan failed"), any(java.util.Calendar.class));
        }

        private HpcCollectionDownloadTask externalDownloadTask() {
        HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
        downloadTask.setId("collection-task-1");
        downloadTask.setUserId("test-user");
        downloadTask.setPath("/external");
        downloadTask.setType(HpcDownloadTaskType.COLLECTION);
        downloadTask.setExternalArchiveFlag(true);
        downloadTask.setAppendPathToDownloadDestination(false);
        downloadTask.setAppendCollectionNameToDownloadDestination(false);
        return downloadTask;
        }

        @SuppressWarnings("unchecked")
        private List<HpcCollectionDownloadTaskItem> invokeProcessExternalDownloadTask(
            HpcCollectionDownloadTask downloadTask) throws Exception {
        Method method = HpcSystemBusServiceImpl.class.getDeclaredMethod("processExternalDownloadTask",
            HpcCollectionDownloadTask.class);
        method.setAccessible(true);
        try {
            return (List<HpcCollectionDownloadTaskItem>) method.invoke(service, downloadTask);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
        }
    
}