package gov.nih.nci.hpc.bus.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectRegistrationTaskItem;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcCollectionDownloadTaskStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadResult;
import gov.nih.nci.hpc.domain.datatransfer.HpcDownloadTaskType;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementSecurityService;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcEventService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcSecurityService;
import gov.nih.nci.hpc.service.HpcSystemAccountFunctionNoReturn;

class HpcSystemBusServiceImplTest {

    // Mocks the dependencies
    @Mock
    private HpcMetadataService metadataService;
    @Mock
    private HpcDataTransferService dataTransferService;
    @Mock
    private HpcDataManagementService dataManagementService;
    @Mock
    private HpcNotificationService notificationService;
    @Mock
    private HpcSecurityService securityService;
    @Mock
    private HpcDataManagementSecurityService dataManagementSecurityService;
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
    void testUpdateRegistrationItemStatusSetsBytesTransferredAndPercentComplete() throws Exception {
        HpcBulkDataObjectRegistrationItem item = new HpcBulkDataObjectRegistrationItem();
        HpcDataObjectRegistrationTaskItem task = new HpcDataObjectRegistrationTaskItem();
        task.setPath("/path/to/data");
        item.setTask(task);

        when(dataManagementService.getDataObject("/path/to/data")).thenReturn(new HpcDataObject());

        HpcSystemGeneratedMetadata metadata = new HpcSystemGeneratedMetadata();
        metadata.setObjectId("object-id");
        metadata.setSourceSize(1000L);
        metadata.setDataTransferStatus(HpcDataTransferUploadStatus.IN_PROGRESS_TO_ARCHIVE);
        when(metadataService.getDataObjectSystemGeneratedMetadata("/path/to/data")).thenReturn(metadata);
        when(dataTransferService.getDataObjectUploadBytesTransferred(metadata)).thenReturn(55L);
        when(dataTransferService.getDataObjectUploadProgress(metadata)).thenReturn(6);

        Method method = HpcSystemBusServiceImpl.class.getDeclaredMethod("updateRegistrationItemStatus",
                HpcBulkDataObjectRegistrationItem.class);
        method.setAccessible(true);
        method.invoke(service, item);

        assertEquals(55L, item.getTask().getBytesTransferred());
        assertEquals(6, item.getTask().getPercentComplete());
    }

    /*
     * Test Case: Executing a collection download task as the system account fails before the task is processed.
     * Expected: The task in-process indicator is cleared, so a later run retries it.
     */
    @Test
    void testProcessCollectionDownloadTasks_SystemAccountFailureClearsInProcess() throws HpcException {
        HpcCollectionDownloadTask downloadTask = receivedCollectionDownloadTask();
        service.collectionDownloadTaskExecutor = Runnable::run;
        doThrow(new HpcException("System Data Management Account not configured", HpcErrorType.UNEXPECTED_ERROR))
                .when(securityService).executeAsSystemAccount(any(), any(HpcSystemAccountFunctionNoReturn.class));

        service.processCollectionDownloadTasks();

        verify(dataTransferService).setCollectionDownloadTaskInProgress(downloadTask.getId(), true);
        verify(dataTransferService).setCollectionDownloadTaskInProgress(downloadTask.getId(), false);
    }

    /*
     * Test Case: Executing as the system account fails after the collection download task was processed (e.g. on
     * data management disconnect).
     * Expected: The task in-process indicator is not cleared.
     */
    @Test
    void testProcessCollectionDownloadTasks_FailureAfterProcessingDoesNotClearInProcess() throws HpcException {
        HpcCollectionDownloadTask downloadTask = receivedCollectionDownloadTask();
        service.collectionDownloadTaskExecutor = Runnable::run;
        doAnswer(invocation -> {
            ((HpcSystemAccountFunctionNoReturn) invocation.getArgument(1)).execute();
            throw new IllegalStateException("Failed to disconnect from data management");
        }).when(securityService).executeAsSystemAccount(any(), any(HpcSystemAccountFunctionNoReturn.class));

        service.processCollectionDownloadTasks();

        // The task has no collections to download, so it was processed (completed as failed).
        verify(dataTransferService).completeCollectionDownloadTask(eq(downloadTask), eq(HpcDownloadResult.FAILED),
                any(), any());
        verify(dataTransferService, never()).setCollectionDownloadTaskInProgress(downloadTask.getId(), false);
    }

    /*
     * Test Case: The collection download task executor rejects the task (e.g. on shutdown).
     * Expected: The task in-process indicator is cleared, so a later run retries it.
     */
    @Test
    void testProcessCollectionDownloadTasks_RejectedExecutionClearsInProcess() throws HpcException {
        HpcCollectionDownloadTask downloadTask = receivedCollectionDownloadTask();
        service.collectionDownloadTaskExecutor = command -> {
            throw new RejectedExecutionException("Executor is shut down");
        };

        service.processCollectionDownloadTasks();

        verify(dataTransferService).setCollectionDownloadTaskInProgress(downloadTask.getId(), true);
        verify(dataTransferService).setCollectionDownloadTaskInProgress(downloadTask.getId(), false);
        verify(securityService, never()).executeAsSystemAccount(any(), any(HpcSystemAccountFunctionNoReturn.class));
    }

    /*
     * Test Case: Processing a collection download task fails before the task is marked in-process.
     * Expected: The task in-process indicator is not changed.
     */
    @Test
    void testProcessCollectionDownloadTasks_FailureBeforeInProcessDoesNotChangeIt() throws HpcException {
        HpcCollectionDownloadTask downloadTask = receivedCollectionDownloadTask();
        when(dataTransferService.getCollectionDownloadTasksCountByUserAndPath(downloadTask.getUserId(),
                downloadTask.getPath(), true)).thenThrow(new HpcException("DB error", HpcErrorType.DATABASE_ERROR));

        service.processCollectionDownloadTasks();

        verify(dataTransferService, never()).setCollectionDownloadTaskInProgress(any(), anyBoolean());
    }

    /**
     * Mock a single received collection download task (w/o collections to download) to be processed.
     */
    private HpcCollectionDownloadTask receivedCollectionDownloadTask() throws HpcException {
        HpcCollectionDownloadTask downloadTask = new HpcCollectionDownloadTask();
        downloadTask.setId("collection-download-task-id");
        downloadTask.setUserId("user-id");
        downloadTask.setPath("/collection/path");
        downloadTask.setType(HpcDownloadTaskType.COLLECTION_LIST);
        when(dataTransferService.getCollectionDownloadTasks(HpcCollectionDownloadTaskStatus.RECEIVED, false))
                .thenReturn(List.of(downloadTask));
        return downloadTask;
    }
     
}