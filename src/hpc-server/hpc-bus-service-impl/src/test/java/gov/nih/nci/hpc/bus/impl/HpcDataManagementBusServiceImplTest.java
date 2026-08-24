package gov.nih.nci.hpc.bus.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationItem;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationResult;
import gov.nih.nci.hpc.domain.model.HpcBulkDataObjectRegistrationTask;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObjectRegistrationTaskItem;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcBulkDataObjectRegistrationTaskDTO;
import gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectDownloadResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.HpcDataObjectRegistrationResponseDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDataObjectRegistrationRequestDTO;
import gov.nih.nci.hpc.dto.datamanagement.v2.HpcDownloadRequestDTO;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;
import gov.nih.nci.hpc.service.HpcMetadataService;
import gov.nih.nci.hpc.service.HpcNotificationService;
import gov.nih.nci.hpc.service.HpcSecurityService;
import gov.nih.nci.hpc.service.HpcSystemAccountFunctionNoReturn;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.domain.user.HpcAuthenticationType;
import gov.nih.nci.hpc.domain.user.HpcNciAccount;
import gov.nih.nci.hpc.domain.user.HpcUserRole;
import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;

class HpcDataManagementBusServiceImplTest {

    // Mocks the dependencies
    @Mock
    private HpcMetadataService metadataService;
    @Mock
    private HpcDataTransferService dataTransferService;
    @Mock
    private HpcNotificationService notificationService;
    @Mock
    private HpcDataManagementService dataManagementService;
    @Mock
    private HpcSecurityService securityService;

    // The bus service under test.
    @Spy
    @InjectMocks
    private HpcDataManagementBusServiceImpl service;
    
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        setPrivateField("downloadArchiveLinkBasePath", "/download/archive");
        var invoker = mock(HpcRequestInvoker.class);
        var nciAccount = mock(HpcNciAccount.class);
        when(nciAccount.getUserId()).thenReturn("test-user");
        when(invoker.getNciAccount()).thenReturn(nciAccount);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }
    
    /*
     * Test Case: path input is null. 
     * Expected: exception returned from deletedDataObject
     */
    @Test
    void testDeletedDataObject_PathNull() throws HpcException {
        // If originalPath is null, should throw exception
        HpcException exception = assertThrows(HpcException.class, () -> {
          service.deleteDataObject(null, false, null);
        });
        // Verify the exception message
        assertEquals("Null / empty path", exception.getMessage());
    }
    
    /*
     * Test Case: path input is empty. 
     * Expected: exception returned from deletedDataObject
     */
    @Test
    void testDeletedDataObject_PathEmpty() throws HpcException {
        // If originalPath is empty, should throw exception
        HpcException exception = assertThrows(HpcException.class, () -> {
          service.deleteDataObject("", false, null);
        });
        // Verify the exception message
        assertEquals("Null / empty path", exception.getMessage());
    }
 
    @Test
    void testDeleteDataObject_DataObjectNotFound() throws Exception {
        when(dataTransferService.getPathAttributes(any(), any(), anyBoolean(), any(), any())).thenReturn(mock(gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes.class));
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class));
        when(dataManagementService.getDataObject(anyString())).thenReturn(null);
        Exception exception = assertThrows(HpcException.class, () -> {
            service.deleteDataObject("/path/to/data", false, null);
        });
        assertEquals("Data object doesn't exist: /path/to/data", exception.getMessage());
    }

    @Test
    void testDeleteDataObject_PermissionDenied() throws Exception {
        var dataObject = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        when(dataManagementService.getDataObject(anyString())).thenReturn(dataObject);
        var sysMeta = mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class);
        var globusConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration.class);
        
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(sysMeta);
        when(sysMeta.getLinkSourcePath()).thenReturn(null);
        when(sysMeta.getDataTransferStatus()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus.ARCHIVED);
        when(sysMeta.getS3ArchiveConfigurationId()).thenReturn("conf");
        when(sysMeta.getArchiveLocation()).thenReturn(mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class));
        when(dataManagementService.getS3ArchiveConfiguration(anyString())).thenReturn(globusConfig);
        var invoker = mock(gov.nih.nci.hpc.domain.model.HpcRequestInvoker.class);
        when(invoker.getAuthenticationType()).thenReturn(HpcAuthenticationType.TOKEN);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
        var perm = mock(gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission.class);
        when(dataManagementService.getDataObjectPermission(anyString())).thenReturn(perm);
        when(perm.getPermission()).thenReturn(gov.nih.nci.hpc.domain.datamanagement.HpcPermission.READ);
        var dataMgmConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration.class);
        var hpcArchive = mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        when(dataMgmConfig.getGlobusConfiguration()).thenReturn(globusConfig);
        when(globusConfig.getBaseArchiveDestination()).thenReturn(hpcArchive);
        when(hpcArchive.getType()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType.TEMPORARY_ARCHIVE);
        when(dataManagementService.getDataManagementConfiguration(anyString())).thenReturn(dataMgmConfig);
        
        var pathAttributes = mock(gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes.class);
        when(pathAttributes.getExists()).thenReturn(true);
        when(pathAttributes.getIsFile()).thenReturn(true);
        when(dataTransferService.getPathAttributes(any(), any(), anyBoolean(), any(), any())).thenReturn(pathAttributes);
        
        Exception exception = assertThrows(HpcException.class, () -> {
            service.deleteDataObject("/path/to/data", false, null);
        });
        assertEquals("Data object can only be deleted by its owner. Your permission: READ", exception.getMessage());
    }

    @Test
    void testDeleteDataObject_ArchiveNotFound() throws Exception {
        var dataObject = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        when(dataManagementService.getDataObject(anyString())).thenReturn(dataObject);
        var sysMeta = mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class);
        var globusConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration.class);
        
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(sysMeta);
        when(sysMeta.getLinkSourcePath()).thenReturn(null);
        when(sysMeta.getDataTransferStatus()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus.ARCHIVED);
        when(sysMeta.getConfigurationId()).thenReturn("conf");
        when(sysMeta.getS3ArchiveConfigurationId()).thenReturn("conf");
        when(sysMeta.getArchiveLocation()).thenReturn(mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class));
        when(dataManagementService.getS3ArchiveConfiguration(anyString())).thenReturn(globusConfig);
        
        var invoker = mock(gov.nih.nci.hpc.domain.model.HpcRequestInvoker.class);
        when(invoker.getAuthenticationType()).thenReturn(HpcAuthenticationType.TOKEN);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
        var perm = mock(gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission.class);
        when(dataManagementService.getDataObjectPermission(anyString())).thenReturn(perm);
        when(perm.getPermission()).thenReturn(gov.nih.nci.hpc.domain.datamanagement.HpcPermission.OWN);
        var attrs = mock(gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes.class);
        when(attrs.getExists()).thenReturn(false);
        when(attrs.getIsFile()).thenReturn(false);
        when(dataTransferService.getPathAttributes(any(), any(), anyBoolean(), any(), any())).thenReturn(attrs);
        var dataMgmConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration.class);
        var hpcArchive = mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        when(dataMgmConfig.getGlobusConfiguration()).thenReturn(globusConfig);
        when(globusConfig.getBaseArchiveDestination()).thenReturn(hpcArchive);
        when(hpcArchive.getType()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType.TEMPORARY_ARCHIVE);
        when(dataManagementService.getDataManagementConfiguration(anyString())).thenReturn(dataMgmConfig);
       
       Exception exception = assertThrows(HpcException.class, () -> {
            service.deleteDataObject("/path/to/data", false, null);
        });
        assertTrue(exception.getMessage().contains("The data object was not found in the archive."));
    }

    @Test
    void testDeleteDataObject_HardDeleteByNonAdmin() throws Exception {
        var dataObject = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        var sysMeta = mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class);
        var globusConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration.class);
        var invoker = mock(gov.nih.nci.hpc.domain.model.HpcRequestInvoker.class);
        var perm = mock(gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission.class);
        var attrs = mock(gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes.class);
        var dataMgmConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration.class);
        var hpcArchive = mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        when(dataManagementService.getDataObject(anyString())).thenReturn(dataObject);
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(sysMeta);
        when(sysMeta.getLinkSourcePath()).thenReturn(null);
        when(sysMeta.getDataTransferStatus()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus.ARCHIVED);
        when(sysMeta.getS3ArchiveConfigurationId()).thenReturn("conf");
        when(sysMeta.getArchiveLocation()).thenReturn(mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class));
        when(dataManagementService.getS3ArchiveConfiguration(anyString())).thenReturn(globusConfig);
        when(invoker.getAuthenticationType()).thenReturn(HpcAuthenticationType.TOKEN);
        when(invoker.getUserRole()).thenReturn(HpcUserRole.USER);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
        when(dataManagementService.getDataObjectPermission(anyString())).thenReturn(perm);
        when(perm.getPermission()).thenReturn(gov.nih.nci.hpc.domain.datamanagement.HpcPermission.OWN);
        when(attrs.getExists()).thenReturn(true);
        when(attrs.getIsFile()).thenReturn(true);
        when(dataTransferService.getPathAttributes(any(), any(), anyBoolean(), any(), any())).thenReturn(attrs);
        when(dataManagementService.getDataManagementConfiguration(anyString())).thenReturn(dataMgmConfig);
        when(dataMgmConfig.getGlobusConfiguration()).thenReturn(globusConfig);
        when(globusConfig.getBaseArchiveDestination()).thenReturn(hpcArchive);
        when(hpcArchive.getType()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType.ARCHIVE);
        Exception exception = assertThrows(HpcException.class, () -> {
            service.deleteDataObject("/path/to/data", true, null);
        });
        assertTrue(exception.getMessage().contains("Hard delete is permitted for system administrators only"));
    }

    @Test
    void testDeleteDataObject_HardDeleteByAdmin() throws Exception {
        var dataObject = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        var sysMeta = mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class);
        var globusConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration.class);
        var invoker = mock(gov.nih.nci.hpc.domain.model.HpcRequestInvoker.class);
        var perm = mock(gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission.class);
        var attrs = mock(gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes.class);
        var dataMgmConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration.class);
        var hpcArchive = mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        when(dataManagementService.getDataObject(anyString())).thenReturn(dataObject);
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(sysMeta);
        when(sysMeta.getLinkSourcePath()).thenReturn(null);
        when(sysMeta.getDataTransferStatus()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus.ARCHIVED);
        when(sysMeta.getS3ArchiveConfigurationId()).thenReturn("conf");
        when(sysMeta.getArchiveLocation()).thenReturn(mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class));
        when(dataManagementService.getS3ArchiveConfiguration(anyString())).thenReturn(globusConfig);
        when(invoker.getAuthenticationType()).thenReturn(HpcAuthenticationType.TOKEN);
        when(invoker.getUserRole()).thenReturn(HpcUserRole.SYSTEM_ADMIN);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
        when(dataManagementService.getDataObjectPermission(anyString())).thenReturn(perm);
        when(perm.getPermission()).thenReturn(gov.nih.nci.hpc.domain.datamanagement.HpcPermission.OWN);
        when(attrs.getExists()).thenReturn(true);
        when(attrs.getIsFile()).thenReturn(true);
        when(dataTransferService.getPathAttributes(any(), any(), anyBoolean(), any(), any())).thenReturn(attrs);
        when(dataManagementService.getDataManagementConfiguration(anyString())).thenReturn(dataMgmConfig);
        when(dataMgmConfig.getGlobusConfiguration()).thenReturn(globusConfig);
        when(globusConfig.getBaseArchiveDestination()).thenReturn(hpcArchive);
        when(hpcArchive.getType()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType.ARCHIVE);
        when(dataManagementService.getDataObjectLinks(anyString())).thenReturn(java.util.Collections.emptyList());
        doNothing().when(dataManagementService).delete(anyString(), anyBoolean());
        var resp = service.deleteDataObject("/path/to/data", true, null);
        assertEquals(true, resp.getDataManagementDeleteStatus());
    }

    @Test
    void testDeleteDataObject_SoftDelete() throws Exception {
        var dataObject = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        var sysMeta = mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class);
        var globusConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration.class);
        var invoker = mock(gov.nih.nci.hpc.domain.model.HpcRequestInvoker.class);
        var perm = mock(gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission.class);
        var attrs = mock(gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes.class);
        var dataMgmConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration.class);
        var hpcArchive = mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        when(dataManagementService.getDataObject(anyString())).thenReturn(dataObject);
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(sysMeta);
        when(sysMeta.getLinkSourcePath()).thenReturn(null);
        when(sysMeta.getDataTransferStatus()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus.ARCHIVED);
        when(sysMeta.getS3ArchiveConfigurationId()).thenReturn("conf");
        when(sysMeta.getArchiveLocation()).thenReturn(mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class));
        when(dataManagementService.getS3ArchiveConfiguration(anyString())).thenReturn(globusConfig);
        when(invoker.getAuthenticationType()).thenReturn(HpcAuthenticationType.TOKEN);
        when(invoker.getUserRole()).thenReturn(HpcUserRole.USER);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
        when(dataManagementService.getDataObjectPermission(anyString())).thenReturn(perm);
        when(perm.getPermission()).thenReturn(gov.nih.nci.hpc.domain.datamanagement.HpcPermission.OWN);
        when(attrs.getExists()).thenReturn(true);
        when(attrs.getIsFile()).thenReturn(true);
        when(dataTransferService.getPathAttributes(any(), any(), anyBoolean(), any(), any())).thenReturn(attrs);
        when(dataManagementService.getDataManagementConfiguration(anyString())).thenReturn(dataMgmConfig);
        when(dataMgmConfig.getGlobusConfiguration()).thenReturn(globusConfig);
        when(globusConfig.getBaseArchiveDestination()).thenReturn(hpcArchive);
        when(hpcArchive.getType()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType.ARCHIVE);
        when(dataManagementService.getDataObjectLinks(anyString())).thenReturn(java.util.Collections.emptyList());
        doNothing().when(securityService).executeAsSystemAccount(any(), any(HpcSystemAccountFunctionNoReturn.class));
        doNothing().when(dataManagementService).softDelete(anyString(), any());
        var resp = service.deleteDataObject("/path/to/data", false, null);
        assertEquals(true, resp.getDataManagementDeleteStatus());
    }

    @Test
    void testDeleteDataObject_ArchiveLinkExternalStorageMismatch() throws Exception {
        var dataObject = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        var sysMeta = mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class);
        var globusConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration.class);
        var invoker = mock(gov.nih.nci.hpc.domain.model.HpcRequestInvoker.class);
        var perm = mock(gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission.class);
        var attrs = mock(gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes.class);
        var dataMgmConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration.class);
        var hpcArchive = mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        when(dataManagementService.getDataObject(anyString())).thenReturn(dataObject);
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(sysMeta);
        when(sysMeta.getLinkSourcePath()).thenReturn(null);
        when(sysMeta.getDataTransferStatus()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus.ARCHIVED);
        when(sysMeta.getS3ArchiveConfigurationId()).thenReturn("conf");
        when(sysMeta.getArchiveLocation()).thenReturn(mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class));
        when(sysMeta.getDataTransferMethod()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadMethod.ARCHIVE_LINK);
        when(dataManagementService.getS3ArchiveConfiguration(anyString())).thenReturn(globusConfig);
        when(globusConfig.getExternalStorage()).thenReturn(false);
        when(invoker.getAuthenticationType()).thenReturn(HpcAuthenticationType.TOKEN);
        when(invoker.getUserRole()).thenReturn(HpcUserRole.SYSTEM_ADMIN);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
        when(dataManagementService.getDataObjectPermission(anyString())).thenReturn(perm);
        when(perm.getPermission()).thenReturn(gov.nih.nci.hpc.domain.datamanagement.HpcPermission.OWN);
        when(attrs.getExists()).thenReturn(true);
        when(attrs.getIsFile()).thenReturn(true);
        when(dataTransferService.getPathAttributes(any(), any(), anyBoolean(), any(), any())).thenReturn(attrs);
        when(dataManagementService.getDataManagementConfiguration(anyString())).thenReturn(dataMgmConfig);
        when(dataMgmConfig.getGlobusConfiguration()).thenReturn(globusConfig);
        when(globusConfig.getBaseArchiveDestination()).thenReturn(hpcArchive);
        when(hpcArchive.getType()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType.ARCHIVE);
        Exception exception = assertThrows(HpcException.class, () -> {
            service.deleteDataObject("/path/to/data", true, null);
        });
        assertTrue(exception.getMessage().contains("Inconsistent archive link metadata and external storage configuration"));
    }

    @Test
    void testDeleteDataObject_RegisteredLinkDeletesMetadataOnly() throws Exception {
        var dataObject = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        var sysMeta = mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class);
        var invoker = mock(gov.nih.nci.hpc.domain.model.HpcRequestInvoker.class);
        var perm = mock(gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission.class);
        when(dataManagementService.getDataObject(anyString())).thenReturn(dataObject);
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(sysMeta);
        when(sysMeta.getLinkSourcePath()).thenReturn("/link/source");
        when(invoker.getAuthenticationType()).thenReturn(HpcAuthenticationType.TOKEN);
        when(invoker.getUserRole()).thenReturn(HpcUserRole.SYSTEM_ADMIN);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
        when(dataManagementService.getDataObjectPermission(anyString())).thenReturn(perm);
        when(perm.getPermission()).thenReturn(gov.nih.nci.hpc.domain.datamanagement.HpcPermission.OWN);
        when(dataManagementService.getDataObjectLinks(anyString())).thenReturn(java.util.Collections.emptyList());
        doNothing().when(dataManagementService).delete(anyString(), anyBoolean());
        var resp = service.deleteDataObject("/path/to/data", true, null);
        assertEquals(true, resp.getDataManagementDeleteStatus());
    }

    @Test
    void testDeleteDataObject_GroupAdminOldFile() throws Exception {
        var dataObject = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        var sysMeta = mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class);
        var globusConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration.class);
        var invoker = mock(gov.nih.nci.hpc.domain.model.HpcRequestInvoker.class);
        var perm = mock(gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission.class);
        var attrs = mock(gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes.class);
        var dataMgmConfig = mock(gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration.class);
        var hpcArchive = mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        when(dataManagementService.getDataObject(anyString())).thenReturn(dataObject);
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(sysMeta);
        when(sysMeta.getLinkSourcePath()).thenReturn(null);
        when(sysMeta.getDataTransferStatus()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus.ARCHIVED);
        when(sysMeta.getS3ArchiveConfigurationId()).thenReturn("conf");
        when(sysMeta.getArchiveLocation()).thenReturn(mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class));
        when(dataManagementService.getS3ArchiveConfiguration(anyString())).thenReturn(globusConfig);
        when(invoker.getAuthenticationType()).thenReturn(HpcAuthenticationType.TOKEN);
        when(invoker.getUserRole()).thenReturn(HpcUserRole.GROUP_ADMIN);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
        when(dataManagementService.getDataObjectPermission(anyString())).thenReturn(perm);
        when(perm.getPermission()).thenReturn(gov.nih.nci.hpc.domain.datamanagement.HpcPermission.OWN);
        when(attrs.getExists()).thenReturn(true);
        when(attrs.getIsFile()).thenReturn(true);
        when(dataTransferService.getPathAttributes(any(), any(), anyBoolean(), any(), any())).thenReturn(attrs);
        when(dataMgmConfig.getGlobusConfiguration()).thenReturn(globusConfig);
        when(globusConfig.getBaseArchiveDestination()).thenReturn(hpcArchive);
        when(hpcArchive.getType()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType.ARCHIVE);
        Calendar fileDate = Calendar.getInstance();
        fileDate.add(Calendar.DAY_OF_YEAR, -100);
        when(sysMeta.getDataTransferCompleted()).thenReturn(fileDate);
        when(dataMgmConfig.getDeletionAllowed()).thenReturn(false);
        when(dataManagementService.getDataManagementConfiguration(any())).thenReturn(dataMgmConfig);
        
        Exception exception = assertThrows(HpcException.class, () -> {
            service.deleteDataObject("/path/to/data", false, null);
        });
        assertTrue(exception.getMessage().contains("not eligible for deletion"));
    }

    @Test
    void testDeleteDataObject_DeleteLinks() throws Exception {
        var dataObject = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        var sysMeta = mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class);
        var invoker = mock(gov.nih.nci.hpc.domain.model.HpcRequestInvoker.class);
        var perm = mock(gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission.class);
        var link = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        when(dataManagementService.getDataObject(anyString())).thenReturn(dataObject);
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(sysMeta);
        when(sysMeta.getLinkSourcePath()).thenReturn(null);
        when(sysMeta.getDataTransferStatus()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus.ARCHIVED);
        when(sysMeta.getS3ArchiveConfigurationId()).thenReturn("conf");
        when(sysMeta.getArchiveLocation()).thenReturn(mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class));
        when(dataManagementService.getS3ArchiveConfiguration(anyString())).thenReturn(mock(gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration.class));
        when(invoker.getAuthenticationType()).thenReturn(HpcAuthenticationType.TOKEN);
        when(invoker.getUserRole()).thenReturn(HpcUserRole.SYSTEM_ADMIN);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
        when(dataManagementService.getDataObjectPermission(anyString())).thenReturn(perm);
        when(perm.getPermission()).thenReturn(gov.nih.nci.hpc.domain.datamanagement.HpcPermission.OWN);
        when(dataManagementService.getDataObjectLinks(anyString())).thenReturn(java.util.Collections.singletonList(link));
        when(link.getAbsolutePath()).thenReturn("/link/path");
        doNothing().when(dataManagementService).delete(eq("/link/path"), anyBoolean());
        doNothing().when(dataManagementService).delete(anyString(), anyBoolean());
        
        var pathAttributes = mock(gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes.class);
        when(pathAttributes.getExists()).thenReturn(true);
        when(pathAttributes.getIsFile()).thenReturn(true);
        when(dataTransferService.getPathAttributes(any(), any(), anyBoolean(), any(), any())).thenReturn(pathAttributes);
        
        var resp = service.deleteDataObject("/path/to/data", true, null);
        assertEquals(true, resp.getLinksDeleteStatus());
    }

    @Test
    void testDeleteDataObject_DeleteLinksFailure() throws Exception {
        var dataObject = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        var sysMeta = mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class);
        var invoker = mock(gov.nih.nci.hpc.domain.model.HpcRequestInvoker.class);
        var perm = mock(gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission.class);
        var link = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        when(dataManagementService.getDataObject(anyString())).thenReturn(dataObject);
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(sysMeta);
        when(sysMeta.getLinkSourcePath()).thenReturn(null);
        when(sysMeta.getDataTransferStatus()).thenReturn(gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus.ARCHIVED);
        when(sysMeta.getS3ArchiveConfigurationId()).thenReturn("conf");
        when(sysMeta.getArchiveLocation()).thenReturn(mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class));
        when(dataManagementService.getS3ArchiveConfiguration(anyString())).thenReturn(mock(gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration.class));
        when(invoker.getAuthenticationType()).thenReturn(HpcAuthenticationType.TOKEN);
        when(invoker.getUserRole()).thenReturn(HpcUserRole.SYSTEM_ADMIN);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
        when(dataManagementService.getDataObjectPermission(anyString())).thenReturn(perm);
        when(perm.getPermission()).thenReturn(gov.nih.nci.hpc.domain.datamanagement.HpcPermission.OWN);
        when(dataManagementService.getDataObjectLinks(anyString())).thenReturn(java.util.Collections.singletonList(link));
        when(link.getAbsolutePath()).thenReturn("/link/path");
        
        var HpcPathAttributes = mock(gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes.class);
        when(HpcPathAttributes.getExists()).thenReturn(true);
        when(HpcPathAttributes.getIsFile()).thenReturn(true);
        when(dataTransferService.getPathAttributes(any(), any(), anyBoolean(), any(), any())).thenReturn(HpcPathAttributes);
        
        doThrow(new HpcException("Failed to delete file from datamanagement", HpcErrorType.INVALID_REQUEST_INPUT)).when(dataManagementService).delete(eq("/link/path"), anyBoolean());
        var resp = service.deleteDataObject("/path/to/data", true, null);
        assertEquals(false, resp.getLinksDeleteStatus());
        assertTrue(resp.getMessage().contains("Failed to delete file from datamanagement"));
    }

    @Test
    void testDeleteDataObject_DeleteExceptionSetsStatus() throws Exception {
        var dataObject = mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class);
        var sysMeta = mock(gov.nih.nci.hpc.domain.model.HpcSystemGeneratedMetadata.class);
        var invoker = mock(gov.nih.nci.hpc.domain.model.HpcRequestInvoker.class);
        var perm = mock(gov.nih.nci.hpc.domain.datamanagement.HpcSubjectPermission.class);
        when(dataManagementService.getDataObject(anyString())).thenReturn(dataObject);
        when(metadataService.getDataObjectMetadataEntries(anyString(), anyBoolean())).thenReturn(mock(gov.nih.nci.hpc.domain.metadata.HpcMetadataEntries.class));
        when(metadataService.toSystemGeneratedMetadata(any())).thenReturn(sysMeta);
        when(sysMeta.getLinkSourcePath()).thenReturn("/link/source");
        when(invoker.getAuthenticationType()).thenReturn(HpcAuthenticationType.TOKEN);
        when(invoker.getUserRole()).thenReturn(HpcUserRole.SYSTEM_ADMIN);
        when(securityService.getRequestInvoker()).thenReturn(invoker);
        when(dataManagementService.getDataObjectPermission(anyString())).thenReturn(perm);
        when(perm.getPermission()).thenReturn(gov.nih.nci.hpc.domain.datamanagement.HpcPermission.OWN);
        when(dataManagementService.getDataObjectLinks(anyString())).thenReturn(java.util.Collections.emptyList());
        doThrow(new HpcException("Delete failed", HpcErrorType.INVALID_REQUEST_INPUT)).when(dataManagementService).delete(anyString(), anyBoolean());
        var resp = service.deleteDataObject("/path/to/data", true, null);
        assertEquals(false, resp.getDataManagementDeleteStatus());
        assertTrue(resp.getMessage().contains("Delete failed"));
    }

    @Test
    void testCompletedBulkRegistrationResultPreservesItemSize() throws Exception {
        HpcBulkDataObjectRegistrationResult result = new HpcBulkDataObjectRegistrationResult();
        HpcBulkDataObjectRegistrationItem registrationItem = new HpcBulkDataObjectRegistrationItem();
        HpcDataObjectRegistrationTaskItem taskItem = new HpcDataObjectRegistrationTaskItem();
        taskItem.setPath("/path/to/data");
        taskItem.setResult(true);
        taskItem.setSize(123L);
        registrationItem.setTask(taskItem);
        result.getItems().add(registrationItem);

        Method method = HpcDataManagementBusServiceImpl.class.getDeclaredMethod("toBulkDataObjectRegistrationTaskDTO",
                HpcBulkDataObjectRegistrationResult.class, boolean.class);
        method.setAccessible(true);

        HpcBulkDataObjectRegistrationTaskDTO taskDTO =
                (HpcBulkDataObjectRegistrationTaskDTO) method.invoke(service, result, false);

        assertEquals(1, taskDTO.getCompletedItems().size());
        assertEquals(123L, taskDTO.getCompletedItems().get(0).getSize());
    }

    @Test
    void testInProgressBulkRegistrationTaskPreservesItemSize() throws Exception {
        HpcBulkDataObjectRegistrationTask task = new HpcBulkDataObjectRegistrationTask();
        HpcBulkDataObjectRegistrationItem registrationItem = new HpcBulkDataObjectRegistrationItem();
        HpcDataObjectRegistrationTaskItem taskItem = new HpcDataObjectRegistrationTaskItem();
        taskItem.setPath("/path/to/data");
        taskItem.setPercentComplete(50);
        taskItem.setSize(123L);
        registrationItem.setTask(taskItem);
        task.getItems().add(registrationItem);

        Method method = HpcDataManagementBusServiceImpl.class.getDeclaredMethod("toBulkDataObjectRegistrationTaskDTO",
                HpcBulkDataObjectRegistrationTask.class, boolean.class);
        method.setAccessible(true);

        HpcBulkDataObjectRegistrationTaskDTO taskDTO =
                (HpcBulkDataObjectRegistrationTaskDTO) method.invoke(service, task, false);

        assertEquals(1, taskDTO.getInProgressItems().size());
        assertEquals(123L, taskDTO.getInProgressItems().get(0).getSize());
    }
    
    @Test
    void testDownloadDataObjectFromExternalSource_NullRequest() throws Exception {
        HpcException exception = assertThrows(HpcException.class, () -> {
            service.downloadDataObjectFromExternalSource("/external/path", null);
        });

        assertEquals("Null download request", exception.getMessage());
    }

    @Test
    void testDownloadDataObjectFromExternalSource_MissingBasePath() throws Exception {
        setPrivateField("downloadArchiveLinkBasePath", "");

        HpcException exception = assertThrows(HpcException.class, () -> {
            service.downloadDataObjectFromExternalSource("/external/path", new HpcDownloadRequestDTO());
        });

        assertTrue(exception.getMessage().contains(
                "Download archive link base path is not configured as property: hpc.bus.downloadArchiveLinkBasePath"));
    }

    @Test
    void testDownloadDataObjectFromExternalSource_NoMatchingS3Configuration() throws Exception {
        String path = "/external/project/file.txt";
        when(dataManagementService.getS3ArchiveConfigurationForExternalPath(path)).thenReturn(null);

        HpcException exception = assertThrows(HpcException.class, () -> {
            service.downloadDataObjectFromExternalSource(path, new HpcDownloadRequestDTO());
        });

        assertTrue(exception.getMessage().contains(
                "No matching S3 archive configuration found for external download path: " + path));
    }

    @Test
    void testDownloadDataObjectFromExternalSource_PermanentArchiveLinkExists() throws Exception {
        String path = "/external/project/file.txt";
        String permanentArchiveLinkPath = "/base/project/file.txt";
        var s3ArchiveConfiguration = buildExternalDownloadConfiguration("dm-config", "s3-config", "/external",
                "/base", "bucket-a", "archive-object-");
        when(dataManagementService.getS3ArchiveConfigurationForExternalPath(path)).thenReturn(s3ArchiveConfiguration);
        when(dataManagementService.getDataManagementConfiguration("dm-config")).thenReturn(buildDataManagementConfiguration("/base"));
        when(dataManagementService.getDataObject(permanentArchiveLinkPath)).thenReturn(mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class));

        HpcException exception = assertThrows(HpcException.class, () -> {
            service.downloadDataObjectFromExternalSource(path, new HpcDownloadRequestDTO());
        });

        assertTrue(exception.getMessage().contains(
                "Permanent or default Archive Link for /project/file.txt already exists. The Archive Link could have been created for a Migration."));
    }

    @Test
    void testDownloadDataObjectFromExternalSource_Success() throws Exception {
        String path = "/external/project/file.txt";
        String permanentArchiveLinkPath = "/base/project/file.txt";
        String temporaryArchiveLinkPath = "/download/archive/external/project/file.txt";
        HpcDownloadRequestDTO downloadRequest = new HpcDownloadRequestDTO();

        var s3ArchiveConfiguration = buildExternalDownloadConfiguration("dm-config", "s3-config", "/external",
                "/base", "bucket-a", "archive-object-");
        when(dataManagementService.getS3ArchiveConfigurationForExternalPath(path)).thenReturn(s3ArchiveConfiguration);
        when(dataManagementService.getDataManagementConfiguration("dm-config")).thenReturn(buildDataManagementConfiguration("/base"));
        when(dataManagementService.getDataObject(permanentArchiveLinkPath)).thenReturn(null);
        when(dataManagementService.getDataObject(temporaryArchiveLinkPath)).thenReturn(null);

        var registrationResponse = new HpcDataObjectRegistrationResponseDTO();
        registrationResponse.setRegistered(true);
        doReturn(registrationResponse).when(service).registerDataObject(eq(temporaryArchiveLinkPath), any(HpcDataObjectRegistrationRequestDTO.class), isNull());

        var downloadResponse = new HpcDataObjectDownloadResponseDTO();
        downloadResponse.setTaskId("task-1");
        doReturn(downloadResponse).when(service).downloadDataObject(eq(temporaryArchiveLinkPath), any(HpcDownloadRequestDTO.class), eq(true));

        HpcDataObjectDownloadResponseDTO response = service.downloadDataObjectFromExternalSource(path, downloadRequest);

        assertEquals("task-1", response.getTaskId());
        verify(service).registerDataObject(eq(temporaryArchiveLinkPath), any(HpcDataObjectRegistrationRequestDTO.class), isNull());
        verify(service).downloadDataObject(eq(temporaryArchiveLinkPath), any(HpcDownloadRequestDTO.class), eq(true));
    }

    @Test
    void testDownloadDataObjectFromExternalSource_DownloadFailureCleansUpTemporaryLink() throws Exception {
        String path = "/external/project/file.txt";
        String permanentArchiveLinkPath = "/base/project/file.txt";
        String temporaryArchiveLinkPath = "/download/archive/external/project/file.txt";
        HpcDownloadRequestDTO downloadRequest = new HpcDownloadRequestDTO();

        var s3ArchiveConfiguration = buildExternalDownloadConfiguration("dm-config", "s3-config", "/external",
                "/base", "bucket-a", "archive-object-");
        when(dataManagementService.getS3ArchiveConfigurationForExternalPath(path)).thenReturn(s3ArchiveConfiguration);
        when(dataManagementService.getDataManagementConfiguration("dm-config")).thenReturn(buildDataManagementConfiguration("/base"));
        when(dataManagementService.getDataObject(permanentArchiveLinkPath)).thenReturn(null);
        when(dataManagementService.getDataObject(temporaryArchiveLinkPath)).thenReturn(mock(gov.nih.nci.hpc.domain.datamanagement.HpcDataObject.class));

        var metadataEntries = mock(HpcMetadataEntries.class);
        var tempMetadata = mock(HpcSystemGeneratedMetadata.class);
        when(metadataService.getDataObjectMetadataEntries(temporaryArchiveLinkPath, false)).thenReturn(metadataEntries);
        when(metadataEntries.getSelfMetadataEntries()).thenReturn(Collections.emptyList());
        when(metadataService.toSystemGeneratedMetadata(Collections.emptyList())).thenReturn(tempMetadata);
        when(tempMetadata.getConfigurationId()).thenReturn("dm-config");
        when(tempMetadata.getS3ArchiveConfigurationId()).thenReturn("s3-config");
        when(dataTransferService.deleteTemporaryArchiveLink(temporaryArchiveLinkPath, "dm-config", "s3-config")).thenReturn(true);

        var registrationResponse = new HpcDataObjectRegistrationResponseDTO();
        registrationResponse.setRegistered(true);
        doReturn(registrationResponse).when(service).registerDataObject(eq(temporaryArchiveLinkPath), any(HpcDataObjectRegistrationRequestDTO.class), isNull());
        doThrow(new HpcException("delegated download failed", HpcErrorType.INVALID_REQUEST_INPUT))
                .when(service).downloadDataObject(eq(temporaryArchiveLinkPath), any(HpcDownloadRequestDTO.class), eq(true));

        HpcException exception = assertThrows(HpcException.class, () -> {
            service.downloadDataObjectFromExternalSource(path, downloadRequest);
        });

        assertTrue(exception.getMessage().contains("Failed the Registration/Download step for external download"));
        verify(dataTransferService).deleteTemporaryArchiveLink(temporaryArchiveLinkPath, "dm-config", "s3-config");
    }

    private HpcDataTransferConfiguration buildExternalDownloadConfiguration(String configurationId,
            String s3ArchiveConfigurationId, String posixPath, String basePath, String bucket, String archiveObjectId) {
        var baseArchiveDestination = mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        var fileLocation = new gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation();
        fileLocation.setFileContainerId(bucket);
        fileLocation.setFileId(archiveObjectId);
        when(baseArchiveDestination.getFileLocation()).thenReturn(fileLocation);

        var s3ArchiveConfiguration = new HpcDataTransferConfiguration();
        s3ArchiveConfiguration.setId(s3ArchiveConfigurationId);
        s3ArchiveConfiguration.setDataManagementConfigurationId(configurationId);
        s3ArchiveConfiguration.setPosixPath(posixPath);
        s3ArchiveConfiguration.setBaseArchiveDestination(baseArchiveDestination);
        return s3ArchiveConfiguration;
    }

    private HpcDataManagementConfiguration buildDataManagementConfiguration(String basePath) {
        var configuration = new HpcDataManagementConfiguration();
        configuration.setBasePath(basePath);
        return configuration;
    }

    private void setPrivateField(String fieldName, Object value) {
        try {
            Field field = HpcDataManagementBusServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(service, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

}