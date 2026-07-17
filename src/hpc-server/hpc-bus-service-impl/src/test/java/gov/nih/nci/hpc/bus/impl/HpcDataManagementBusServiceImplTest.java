package gov.nih.nci.hpc.bus.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import gov.nih.nci.hpc.domain.datamanagement.HpcDataObject;
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
import gov.nih.nci.hpc.domain.user.HpcUserRole;

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
        void testDownloadDataObjectFromExternalSource_RegistersAndDelegatesWithExternalFlag() throws Exception {
        setPrivateField("downloadArchiveLinkBasePath", "/temp/external");

        String externalPath = "/external-root/project/file.txt";
        String relativePath = "/project/file.txt";
        String permanentPath = "/base" + relativePath;
        String temporaryPath = "/temp/external" + relativePath;

        HpcDownloadRequestDTO downloadRequest = new HpcDownloadRequestDTO();

        HpcDataTransferConfiguration s3Configuration = mock(HpcDataTransferConfiguration.class);
        HpcDataManagementConfiguration dataManagementConfiguration = mock(HpcDataManagementConfiguration.class);
        gov.nih.nci.hpc.domain.datatransfer.HpcArchive archive =
            mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation archiveLocation =
            mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class);

        when(dataManagementService.getS3ArchiveConfigurationForExternalPath(externalPath)).thenReturn(s3Configuration);
        when(s3Configuration.getDataManagementConfigurationId()).thenReturn("dm-config");
        when(s3Configuration.getPosixPath()).thenReturn("/external-root");
        when(s3Configuration.getId()).thenReturn("s3-config");
        when(s3Configuration.getBaseArchiveDestination()).thenReturn(archive);
        when(archive.getFileLocation()).thenReturn(archiveLocation);
        when(archiveLocation.getFileContainerId()).thenReturn("bucket-a");
        when(dataManagementService.getDataManagementConfiguration("dm-config")).thenReturn(dataManagementConfiguration);
        when(dataManagementConfiguration.getBasePath()).thenReturn("/base");

        when(dataManagementService.getDataObject(permanentPath)).thenReturn(null);
        when(dataManagementService.getDataObject(temporaryPath)).thenReturn(null);

        HpcDataObjectRegistrationResponseDTO registrationResponse = new HpcDataObjectRegistrationResponseDTO();
        registrationResponse.setRegistered(true);
        doReturn(registrationResponse)
            .when(service)
            .registerDataObject(eq(temporaryPath), any(HpcDataObjectRegistrationRequestDTO.class), isNull());

        HpcDataObjectDownloadResponseDTO delegatedResponse = new HpcDataObjectDownloadResponseDTO();
        delegatedResponse.setTaskId("task-123");
        doReturn(delegatedResponse).when(service).downloadDataObject(temporaryPath, downloadRequest, true);

        HpcDataObjectDownloadResponseDTO response =
            service.downloadDataObjectFromExternalSource(externalPath, downloadRequest);

        assertEquals("task-123", response.getTaskId());
        verify(dataManagementService).getS3ArchiveConfigurationForExternalPath(externalPath);
        verify(dataManagementService).getDataObject(permanentPath);
        verify(dataManagementService).getDataObject(temporaryPath);

        ArgumentCaptor<HpcDataObjectRegistrationRequestDTO> requestCaptor =
            ArgumentCaptor.forClass(HpcDataObjectRegistrationRequestDTO.class);
        verify(service).registerDataObject(eq(temporaryPath), requestCaptor.capture(), isNull());
        HpcDataObjectRegistrationRequestDTO registrationRequest = requestCaptor.getValue();
        assertEquals("s3-config", registrationRequest.getS3ArchiveConfigurationId());
        assertTrue(registrationRequest.getCreateParentCollections());
        assertEquals("bucket-a",
            registrationRequest.getArchiveLinkSource().getSourceLocation().getFileContainerId());
        assertEquals("project/file.txt",
            registrationRequest.getArchiveLinkSource().getSourceLocation().getFileId());

        verify(service).downloadDataObject(temporaryPath, downloadRequest, true);
        }

        @Test
        void testDownloadDataObjectFromExternalSource_ExistingPermanentLinkRejected() throws Exception {
        setPrivateField("downloadArchiveLinkBasePath", "/temp/external");

        String externalPath = "/external-root/project/file.txt";
        String relativePath = "/project/file.txt";
        String permanentPath = "/base" + relativePath;

        HpcDataTransferConfiguration s3Configuration = mock(HpcDataTransferConfiguration.class);
        HpcDataManagementConfiguration dataManagementConfiguration = mock(HpcDataManagementConfiguration.class);
        gov.nih.nci.hpc.domain.datatransfer.HpcArchive archive =
            mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation archiveLocation =
            mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class);

        when(dataManagementService.getS3ArchiveConfigurationForExternalPath(externalPath)).thenReturn(s3Configuration);
        when(s3Configuration.getDataManagementConfigurationId()).thenReturn("dm-config");
        when(s3Configuration.getPosixPath()).thenReturn("/external-root");
        when(s3Configuration.getBaseArchiveDestination()).thenReturn(archive);
        when(archive.getFileLocation()).thenReturn(archiveLocation);
        when(archiveLocation.getFileContainerId()).thenReturn("bucket-a");
        when(dataManagementService.getDataManagementConfiguration("dm-config")).thenReturn(dataManagementConfiguration);
        when(dataManagementConfiguration.getBasePath()).thenReturn("/base");

        when(dataManagementService.getDataObject(permanentPath)).thenReturn(mock(HpcDataObject.class));

        HpcException exception = assertThrows(HpcException.class,
            () -> service.downloadDataObjectFromExternalSource(externalPath, new HpcDownloadRequestDTO()));

        assertTrue(exception.getMessage().contains("Permanent or default Archive Link"));
        verify(service, never()).downloadDataObject(anyString(), any(HpcDownloadRequestDTO.class), anyBoolean());
        }

        @Test
        void testDownloadDataObjectFromExternalSource_SkipsRegistrationWhenTemporaryLinkExists() throws Exception {
        setPrivateField("downloadArchiveLinkBasePath", "/temp/external");

        String externalPath = "/external-root/project/file.txt";
        String relativePath = "/project/file.txt";
        String permanentPath = "/base" + relativePath;
        String temporaryPath = "/temp/external" + relativePath;

        HpcDownloadRequestDTO downloadRequest = new HpcDownloadRequestDTO();
        HpcDataTransferConfiguration s3Configuration = mock(HpcDataTransferConfiguration.class);
        HpcDataManagementConfiguration dataManagementConfiguration = mock(HpcDataManagementConfiguration.class);
        gov.nih.nci.hpc.domain.datatransfer.HpcArchive archive =
            mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation archiveLocation =
            mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class);

        when(dataManagementService.getS3ArchiveConfigurationForExternalPath(externalPath)).thenReturn(s3Configuration);
        when(s3Configuration.getDataManagementConfigurationId()).thenReturn("dm-config");
        when(s3Configuration.getPosixPath()).thenReturn("/external-root");
        when(s3Configuration.getBaseArchiveDestination()).thenReturn(archive);
        when(archive.getFileLocation()).thenReturn(archiveLocation);
        when(archiveLocation.getFileContainerId()).thenReturn("bucket-a");
        when(dataManagementService.getDataManagementConfiguration("dm-config")).thenReturn(dataManagementConfiguration);
        when(dataManagementConfiguration.getBasePath()).thenReturn("/base");
        when(dataManagementService.getDataObject(permanentPath)).thenReturn(null);
        when(dataManagementService.getDataObject(temporaryPath)).thenReturn(mock(HpcDataObject.class));

        HpcDataObjectDownloadResponseDTO delegatedResponse = new HpcDataObjectDownloadResponseDTO();
        delegatedResponse.setTaskId("task-456");
        doReturn(delegatedResponse).when(service).downloadDataObject(temporaryPath, downloadRequest, true);

        HpcDataObjectDownloadResponseDTO response =
            service.downloadDataObjectFromExternalSource(externalPath, downloadRequest);

        assertEquals("task-456", response.getTaskId());
        verify(service, never()).registerDataObject(anyString(), any(HpcDataObjectRegistrationRequestDTO.class), isNull());
        verify(service).downloadDataObject(temporaryPath, downloadRequest, true);
        }

        @Test
        void testDownloadDataObjectFromExternalSource_CleansUpTemporaryLinkOnDownloadFailure() throws Exception {
        setPrivateField("downloadArchiveLinkBasePath", "/temp/external");

        String externalPath = "/external-root/project/file.txt";
        String relativePath = "/project/file.txt";
        String permanentPath = "/base" + relativePath;
        String temporaryPath = "/temp/external" + relativePath;

        HpcDownloadRequestDTO downloadRequest = new HpcDownloadRequestDTO();

        HpcDataTransferConfiguration s3Configuration = mock(HpcDataTransferConfiguration.class);
        HpcDataManagementConfiguration dataManagementConfiguration = mock(HpcDataManagementConfiguration.class);
        gov.nih.nci.hpc.domain.datatransfer.HpcArchive archive =
            mock(gov.nih.nci.hpc.domain.datatransfer.HpcArchive.class);
        gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation archiveLocation =
            mock(gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation.class);
        HpcMetadataEntries metadataEntries = mock(HpcMetadataEntries.class);
        HpcSystemGeneratedMetadata systemMetadata = mock(HpcSystemGeneratedMetadata.class);

        when(dataManagementService.getS3ArchiveConfigurationForExternalPath(externalPath)).thenReturn(s3Configuration);
        when(s3Configuration.getDataManagementConfigurationId()).thenReturn("dm-config");
        when(s3Configuration.getPosixPath()).thenReturn("/external-root");
        when(s3Configuration.getId()).thenReturn("s3-config");
        when(s3Configuration.getBaseArchiveDestination()).thenReturn(archive);
        when(archive.getFileLocation()).thenReturn(archiveLocation);
        when(archiveLocation.getFileContainerId()).thenReturn("bucket-a");
        when(dataManagementService.getDataManagementConfiguration("dm-config")).thenReturn(dataManagementConfiguration);
        when(dataManagementConfiguration.getBasePath()).thenReturn("/base");
        when(dataManagementService.getDataObject(permanentPath)).thenReturn(null);
        when(dataManagementService.getDataObject(temporaryPath)).thenReturn(mock(HpcDataObject.class));

        when(metadataService.getDataObjectMetadataEntries(temporaryPath, false)).thenReturn(metadataEntries);
        when(metadataEntries.getSelfMetadataEntries()).thenReturn(Collections.emptyList());
        when(metadataService.toSystemGeneratedMetadata(Collections.emptyList())).thenReturn(systemMetadata);
        when(systemMetadata.getConfigurationId()).thenReturn("dm-config");
        when(systemMetadata.getS3ArchiveConfigurationId()).thenReturn("s3-config");
        when(dataTransferService.deleteTemporaryArchiveLink(temporaryPath, "dm-config", "s3-config")).thenReturn(true);

        doThrow(new HpcException("delegated download failed", HpcErrorType.INVALID_REQUEST_INPUT))
            .when(service)
            .downloadDataObject(temporaryPath, downloadRequest, true);

        HpcException exception = assertThrows(HpcException.class,
            () -> service.downloadDataObjectFromExternalSource(externalPath, downloadRequest));

        assertTrue(exception.getMessage().contains("Failed to create download task for external download"));
        verify(dataTransferService).deleteTemporaryArchiveLink(temporaryPath, "dm-config", "s3-config");
        }

        private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = HpcDataManagementBusServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
        }

}