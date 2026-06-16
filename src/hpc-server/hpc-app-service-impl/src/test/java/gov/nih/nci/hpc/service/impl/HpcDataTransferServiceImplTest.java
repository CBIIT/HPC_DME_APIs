/**
 * HpcDataTransferServiceImplTest.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
import gov.nih.nci.hpc.dao.HpcGlobusTransferTaskDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadTask;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.integration.HpcTransferAcceptanceResponse;
import gov.nih.nci.hpc.service.HpcDataManagementService;
import gov.nih.nci.hpc.service.HpcDataTransferService;

/**
 * HPC Data Transfer Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@ExtendWith(MockitoExtension.class)
public class HpcDataTransferServiceImplTest {

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The app service under test.
	// @InjectMocks
	private HpcDataTransferService dataTransferService = null;

	// Mocks.
	@Mock
	private HpcDataManagementService dataManagementServiceMock = null;
	@Mock
	private HpcDataTransferProxy dataTransferProxyMock = null;
	@Mock
	private HpcDataManagementConfigurationLocator dataManagementConfigurationLocatorMock = null;
	@Mock
	private HpcSystemAccountLocator systemAccountLocatorMock = null;
	@Mock
	private HpcDataDownloadDAO dataDownloadDAOMock = null;
	@Mock
	private HpcGlobusTransferTaskDAO globusTransferDAOMock = null;
    
	// ---------------------------------------------------------------------//
	// Unit Tests
	// ---------------------------------------------------------------------//

	/**
	 * {@link HpcDataTransferService#uploadDataObject(gov.nih.nci.hpc.domain.datatransfer.HpcUploadSource, gov.nih.nci.hpc.domain.datatransfer.HpcS3UploadSource, java.io.File, boolean, String, String, String, String, String)}
	 */
	/*
	 * Test scenario: No upload source or request provided. Expected: HpcException -
	 * "No data transfer source or data attachment provided or upload URL requested"
	 */
	@Test
	public void testUploadDataObjectNoSourceOrAttachment() throws HpcException {
		HpcException ex = assertThrows(HpcException.class, () ->
				dataTransferService.uploadDataObject(null, null, null, null, null, null, false, null, null, null, null,
						"testObjectId", null, null, null, null));
		assertTrue(ex.getMessage().contains("No data transfer source or data attachment provided or upload URL requested"));
	}

	/**
	 * {@link HpcDataTransferService#uploadDataObject(gov.nih.nci.hpc.domain.datatransfer.HpcUploadSource, gov.nih.nci.hpc.domain.datatransfer.HpcS3UploadSource, java.io.File, boolean, String, String, String, String, String)}
	 */
	/*
	 * Test scenario: Invalid S3 upload source. Expected: HpcException -
	 * "Invalid S3 upload source"
	 */
	@Test
	public void testUploadDataObjectInvalidS3UploadSource() throws HpcException {
		HpcStreamingUploadSource s3UploadSource = new HpcStreamingUploadSource();
		HpcException ex = assertThrows(HpcException.class, () ->
				dataTransferService.uploadDataObject(null, s3UploadSource, null, null, null, null, false, null, null,
						null, null, "dataObjectId", null, null, null, null));
		assertTrue(ex.getMessage().contains("Invalid S3 upload source"));
	}

	/**
	 * {@link HpcDataTransferService#uploadDataObject(gov.nih.nci.hpc.domain.datatransfer.HpcUploadSource, gov.nih.nci.hpc.domain.datatransfer.HpcS3UploadSource, java.io.File, boolean, String, String, String, String, String)}
	 */
	/*
	 * Test scenario: Successful upload from AWS S3 source. Expected:
	 * HpcDataObjectUploadResponse response.
	 */
	@Test
	public void testS3UploadDataObject() throws HpcException {
		// Prepare test data.
		HpcFileLocation sourceLocation = new HpcFileLocation();
		sourceLocation.setFileContainerId("testSourceFileContainerId");
		sourceLocation.setFileId("testSourceFileId");

		HpcS3Account s3Account = new HpcS3Account();
		s3Account.setAccessKey("testAccessKey");
		s3Account.setSecretKey("testSecretKey");
		s3Account.setRegion("testRegion");

		HpcStreamingUploadSource s3UploadSource = new HpcStreamingUploadSource();
		s3UploadSource.setSourceLocation(sourceLocation);
		s3UploadSource.setAccount(s3Account);

		HpcDataManagementConfiguration dmc = new HpcDataManagementConfiguration();
		dmc.setS3UploadConfigurationId("S3_CONFIG_ID");
		dmc.setDoc("testDoc");

		// Mock setup.
		HpcPathAttributes pathAttributes = new HpcPathAttributes();
		pathAttributes.setIsAccessible(true);
		pathAttributes.setExists(true);
		pathAttributes.setIsDirectory(false);
		pathAttributes.setSize(123456789L);
		when(dataTransferProxyMock.getPathAttributes(any(), eq(sourceLocation), eq(true)))
				.thenReturn(pathAttributes);
		Mockito.lenient().when(dataManagementConfigurationLocatorMock.getArchiveDataTransferType(any()))
				.thenReturn(HpcDataTransferType.S_3);
		Mockito.lenient().when(systemAccountLocatorMock.getSystemAccount(any(), any()))
				.thenReturn(new HpcIntegratedSystemAccount());
		Mockito.lenient().when(systemAccountLocatorMock.getSystemAccount(any())).thenReturn(new HpcIntegratedSystemAccount());
		Mockito.lenient().when(dataTransferProxyMock.authenticate(any(), any(), any(), any()))
				.thenReturn("token");
		Mockito.lenient().when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(any(), any(), any()))
				.thenReturn(new HpcDataTransferConfiguration());
		when(dataManagementConfigurationLocatorMock.get(any())).thenReturn(dmc);
		Mockito.lenient().when(dataManagementServiceMock.getDataManagementConfiguration("testConfigId")).thenReturn(dmc);
		// Run the test.
		dataTransferService.uploadDataObject(null, s3UploadSource, null, null, null, null, false, null, null, null,
				"/test/path", "testUserId", "testCallerId", "testConfigId", "testObjectId", null);
	}

	/**
	 * {@link HpcDataTransferService#generateDownloadRequestURL(String, HpcFileLocation, gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType, String)}generateDownloadRequestURL()
	 */
	/*
	 * Test scenario: null dataTransferType Expected: HpcException -
	 * "Invalid generate download URL request"
	 */
	@Test
	public void testGenerateDownloadRequestURLNullDataTransferType() throws HpcException {
		HpcException ex = assertThrows(HpcException.class, () ->
				dataTransferService.generateDownloadRequestURL("", "user-id", new HpcFileLocation(), null, 2000, "", ""));
		assertTrue(ex.getMessage().contains("Invalid generate download URL request"));
	}

	/**
	 * {@link HpcDataTransferService#generateDownloadRequestURL(String, HpcFileLocation, gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType, String)}generateDownloadRequestURL()
	 */
	/*
	 * Test scenario: Invalid archiveLocation Expected: HpcException -
	 * "Invalid generate download URL request"
	 */
	@Test
	public void testGenerateDownloadRequestURLInvalidArchiveLocation() throws HpcException {
		HpcException ex = assertThrows(HpcException.class, () ->
				dataTransferService.generateDownloadRequestURL("", "user-id", new HpcFileLocation(),
						HpcDataTransferType.S_3, 1000, "", ""));
		assertTrue(ex.getMessage().contains("Invalid generate download URL request"));
	}

	/**
	 * {@link HpcDataTransferService#generateDownloadRequestURL(String, HpcFileLocation, gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType, String)}generateDownloadRequestURL()
	 */
	/*
	 * Test scenario: Successful Expected: A download URL - "https://downloadURL"
	 */
	@Test
	public void testGenerateDownloadRequestURL() throws HpcException {
		// Mock setup.
		Mockito.lenient().when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(any(), any(), any()))
				.thenReturn(new HpcDataTransferConfiguration());
		Mockito.lenient().when(systemAccountLocatorMock.getSystemAccount(any(), any()))
				.thenReturn(new HpcIntegratedSystemAccount());
		Mockito.lenient().when(systemAccountLocatorMock.getSystemAccount(any())).thenReturn(new HpcIntegratedSystemAccount());
		Mockito.lenient().when(dataTransferProxyMock.authenticate(any(), any(), any(), any()))
				.thenReturn("token");
		HpcDataManagementConfiguration testConfiguration = new HpcDataManagementConfiguration();
		testConfiguration.setDoc("testDoc");
		Mockito.lenient().when(systemAccountLocatorMock.getSystemAccount(any(), any()))
				.thenReturn(new HpcIntegratedSystemAccount());
		Mockito.lenient().when(dataManagementServiceMock.getDataManagementConfiguration("testConfigId")).thenReturn(testConfiguration);
		Mockito.lenient().when(dataTransferProxyMock.generateDownloadRequestURL(any(), any(), any(), any()))
				.thenReturn("https://downloadURL");
		HpcFileLocation archiveLocation = new HpcFileLocation();
		archiveLocation.setFileContainerId("test");
		archiveLocation.setFileId("test");

		String downloadURL = dataTransferService.generateDownloadRequestURL("", "user-id", archiveLocation,
				HpcDataTransferType.S_3, 1000, "testConfigId", "");

		// Assert expected result.
		assertEquals(downloadURL, "https://downloadURL");
	}

	/**
	 * {@link HpcDataTransferService#downloadDataObject(String, HpcFileLocation, gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination, gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination, HpcDataTransferType, String, String, boolean, long)}
	 */
	/*
	 * Test scenario: null dataTransferType Expected: HpcException -
	 * "Invalid data transfer request"
	 */
	@Test
	public void testDownloadDataObjectNullDataTransferType() throws HpcException {

		HpcException ex = assertThrows(HpcException.class, () ->
				dataTransferService.downloadDataObject("", null, null, null, null, null, null, null, null, null, null,
						null, "", "", "", false, null, 0L, HpcDataTransferUploadStatus.ARCHIVED, null, false));
		assertTrue(ex.getMessage().contains("Invalid data transfer request"));

	}

	/**
	 * {@link HpcDataTransferService#downloadDataObject(String, HpcFileLocation, gov.nih.nci.hpc.domain.datatransfer.HpcGlobusDownloadDestination, gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination, HpcDataTransferType, String, String, boolean, long)}
	 */
	/*
	 * Test scenario: Invalid archiveLocation Expected: HpcException -
	 * "Invalid data transfer request"
	 */
	@Test
	public void testDownloadDataObjectInvalidArchiveLocation() throws HpcException {

		HpcException ex = assertThrows(HpcException.class, () ->
				dataTransferService.downloadDataObject("", new HpcFileLocation(), null, null, null, null, null, null,
						null, null, null, null, "", "", "", false, null, 0L, HpcDataTransferUploadStatus.ARCHIVED, null,
						false));
		assertTrue(ex.getMessage().contains("Invalid data transfer request"));

	}

	/**
	 * {@link HpcDataTransferService#generateDownloadRequestURL(String, HpcFileLocation, gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType, String)}generateDownloadRequestURL()
	 */
	/*
	 * Test scenario: Successful download to AWS S3 destination Expected:
	 * HpcDataObjectDownloadResponse object w/ download task ID
	 */
	@Test
	public void testS3DownloadDataObject() throws HpcException {
		// Mock setup.
		HpcArchive baseDownloadSource = new HpcArchive();
		baseDownloadSource.setType(HpcArchiveType.ARCHIVE);
		HpcFileLocation baseDownloadSourceFileLocation = new HpcFileLocation();
		baseDownloadSourceFileLocation.setFileContainerId("testBaseDownloadSource");
		baseDownloadSourceFileLocation.setFileId("testBaseDownloadSource");
		baseDownloadSource.setFileLocation(baseDownloadSourceFileLocation);
		HpcDataTransferConfiguration dataTransferConfig = new HpcDataTransferConfiguration();
		dataTransferConfig.setBaseDownloadSource(baseDownloadSource);
		Mockito.lenient().when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(any(), any(), any()))
				.thenReturn(dataTransferConfig);
		Mockito.lenient().when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(any(), any(), any()))
				.thenReturn(dataTransferConfig);

		Mockito.lenient().when(systemAccountLocatorMock.getSystemAccount(any(), any()))
				.thenReturn(new HpcIntegratedSystemAccount());
		Mockito.lenient().when(systemAccountLocatorMock.getSystemAccount(any())).thenReturn(new HpcIntegratedSystemAccount());
		Mockito.lenient().when(dataTransferProxyMock.authenticate(any(), any(), any(), any()))
				.thenReturn("token");

		// Prepare test data.
		HpcFileLocation archiveLocation = new HpcFileLocation();
		archiveLocation.setFileContainerId("testArchiveFileContainerId");
		archiveLocation.setFileId("testArchiveFileId");

		HpcFileLocation destinationLocation = new HpcFileLocation();
		destinationLocation.setFileContainerId("testDestinationFileContainerId");
		destinationLocation.setFileId("testDestinationFileId");

		HpcS3Account s3Account = new HpcS3Account();
		s3Account.setAccessKey("testAccessKey");
		s3Account.setSecretKey("testSecretKey");
		s3Account.setRegion("testRegion");

		HpcS3DownloadDestination s3loadDestination = new HpcS3DownloadDestination();
		s3loadDestination.setDestinationLocation(destinationLocation);
		s3loadDestination.setAccount(s3Account);

		HpcPathAttributes pathAttributes = new HpcPathAttributes();
		pathAttributes.setIsAccessible(true);
		Mockito.lenient().when(dataTransferProxyMock.getPathAttributes(any(), eq(destinationLocation), eq(false)))
				.thenReturn(pathAttributes);

		// Run the test.
		HpcDataObjectDownloadResponse downloadResponse = dataTransferService.downloadDataObject("/test/path",
				archiveLocation, null, s3loadDestination, null, null, null, null, null, HpcDataTransferType.S_3,
				"testConfigId", "", null, "testUserId", null, false, null, 0L, HpcDataTransferUploadStatus.ARCHIVED,
				null, false);

		// Assert expected result.
		assertNull(downloadResponse.getDownloadTaskId());
		assertEquals(downloadResponse.getDestinationLocation().getFileContainerId(),
				destinationLocation.getFileContainerId());
		assertEquals(downloadResponse.getDestinationLocation().getFileId(), destinationLocation.getFileId());
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	@BeforeEach
	public void init() throws HpcException {
		Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies = new HashMap<>();
		dataTransferProxies.put(HpcDataTransferType.GLOBUS, dataTransferProxyMock);
		dataTransferProxies.put(HpcDataTransferType.S_3, dataTransferProxyMock);

		HpcDataTransferServiceImpl dataTransferServiceImpl = new HpcDataTransferServiceImpl(dataTransferProxies,
				"/test/download/directory");
		dataTransferServiceImpl.setDataManagementConfigurationLocator(dataManagementConfigurationLocatorMock);
		dataTransferServiceImpl.setSystemAccountLocator(systemAccountLocatorMock);
		dataTransferServiceImpl.setDataDownloadDAO(dataDownloadDAOMock);
		dataTransferServiceImpl.setDataManagementService(dataManagementServiceMock);
		dataTransferServiceImpl.setGlobusTransferDAO(globusTransferDAOMock);

		dataTransferService = dataTransferServiceImpl;
	}

	// ---------------------------------------------------------------------//
	// Fair-Access (HPCDATAMGM-2148) Unit Tests
	// ---------------------------------------------------------------------//

	/**
	 * Fair-access: requester is NOT in the Globus queue.
	 * Expected: user is eligible; execution proceeds past the eligibility gate
	 * (evidenced by getAuthenticatedToken being invoked).
	 */
	@Test
	public void testFairAccess_UserNotInQueue_IsEligible() throws HpcException {
		// Arrange
		Mockito.lenient().when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(any(), any(), any()))
				.thenReturn(new HpcDataTransferConfiguration());
		when(globusTransferDAOMock.getGlobusUsersAllocated()).thenReturn(Collections.emptyList());

		HpcIntegratedSystemAccount account = new HpcIntegratedSystemAccount();
		account.setUsername("test-globus-account");
		when(systemAccountLocatorMock.getSystemAccount(any(HpcDataTransferType.class), any())).thenReturn(account);
		Mockito.lenient().when(dataTransferProxyMock.authenticate(any(), any(), any(), any())).thenReturn("mock-token");
		HpcTransferAcceptanceResponse response = new HpcTransferAcceptanceResponse() {
			@Override
			public boolean canAcceptTransfer() {
				return false;
			}
		};
		when(dataTransferProxyMock.acceptsTransferRequests(any())).thenReturn(response);

		// Act
		boolean result = dataTransferService.continueDataObjectDownloadTask(buildGlobusDownloadTask("userA", "config1"));

		// Assert – eligible users proceed past the fair-access gate (method returns false
		// only because acceptsTransferRequests rejects, not because of eligibility).
		assertFalse(result);
		verify(systemAccountLocatorMock).getSystemAccount(any(HpcDataTransferType.class), any());
	}

	/**
	 * Fair-access: requester IS in the Globus queue but slots used (1) is at or
	 * below the fair-share quota (2 accounts / 2 users = 1).
	 * Expected: user is eligible; execution proceeds past the eligibility gate.
	 */
	@Test
	public void testFairAccess_UserBelowFairShare_IsEligible() throws HpcException {
		// Arrange
		Mockito.lenient().when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(any(), any(), any()))
				.thenReturn(new HpcDataTransferConfiguration());
		when(globusTransferDAOMock.getGlobusUsersAllocated()).thenReturn(Arrays.asList("userA", "userB"));
		when(globusTransferDAOMock.getGlobusRequestCountForUser("userA")).thenReturn(1); // 1 slot used
		when(dataDownloadDAOMock.getUserCountByDataTransferType(HpcDataTransferType.GLOBUS)).thenReturn(2); // 2 users
		when(systemAccountLocatorMock.getSystemAccountCount("config1")).thenReturn(2); // quota = 2/2 = 1

		HpcIntegratedSystemAccount account = new HpcIntegratedSystemAccount();
		account.setUsername("test-globus-account");
		when(systemAccountLocatorMock.getSystemAccount(any(HpcDataTransferType.class), any())).thenReturn(account);
		Mockito.lenient().when(dataTransferProxyMock.authenticate(any(), any(), any(), any())).thenReturn("mock-token");
		HpcTransferAcceptanceResponse response = new HpcTransferAcceptanceResponse() {
			@Override
			public boolean canAcceptTransfer() {
				return false;
			}
		};
		when(dataTransferProxyMock.acceptsTransferRequests(any())).thenReturn(response);

		// Act
		boolean result = dataTransferService.continueDataObjectDownloadTask(buildGlobusDownloadTask("userA", "config1"));

		// Assert – user at the exact fair-share limit (1 <= 1) is eligible.
		assertFalse(result);
		verify(systemAccountLocatorMock).getSystemAccount(any(HpcDataTransferType.class), any());
	}

	/**
	 * Fair-access: requester IS in the Globus queue with slots used (2) exactly
	 * equal to the fair-share quota (4 accounts / 2 users = 2).
	 * The algorithm uses strict greater-than, so equal is still eligible.
	 * Expected: user is eligible; execution proceeds past the eligibility gate.
	 */
	@Test
	public void testFairAccess_UserAtExactFairShareBoundary_IsEligible() throws HpcException {
		// Arrange
		Mockito.lenient().when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(any(), any(), any()))
				.thenReturn(new HpcDataTransferConfiguration());
		when(globusTransferDAOMock.getGlobusUsersAllocated()).thenReturn(Arrays.asList("userA", "userB"));
		when(globusTransferDAOMock.getGlobusRequestCountForUser("userA")).thenReturn(2); // 2 slots used
		when(dataDownloadDAOMock.getUserCountByDataTransferType(HpcDataTransferType.GLOBUS)).thenReturn(2); // 2 users
		when(systemAccountLocatorMock.getSystemAccountCount("config1")).thenReturn(4); // quota = 4/2 = 2

		HpcIntegratedSystemAccount account = new HpcIntegratedSystemAccount();
		account.setUsername("test-globus-account");
		when(systemAccountLocatorMock.getSystemAccount(any(HpcDataTransferType.class), any())).thenReturn(account);
		Mockito.lenient().when(dataTransferProxyMock.authenticate(any(), any(), any(), any())).thenReturn("mock-token");
		HpcTransferAcceptanceResponse response = new HpcTransferAcceptanceResponse() {
			@Override
			public boolean canAcceptTransfer() {
				return false;
			}
		};
		when(dataTransferProxyMock.acceptsTransferRequests(any())).thenReturn(response);

		// Act
		boolean result = dataTransferService.continueDataObjectDownloadTask(buildGlobusDownloadTask("userA", "config1"));

		// Assert – 2 == 2 is NOT greater-than, so user is still eligible.
		assertFalse(result);
		verify(systemAccountLocatorMock).getSystemAccount(any(HpcDataTransferType.class), any());
	}

	/**
	 * Fair-access: requester IS in the Globus queue and has used more slots (2)
	 * than their fair share (4 accounts / 4 users = 1).
	 * Expected: user is ineligible; continueDataObjectDownloadTask returns false
	 * immediately without reaching getAuthenticatedToken.
	 */
	@Test
	public void testFairAccess_UserExceedsFairShare_IsIneligible() throws HpcException {
		// Arrange
		Mockito.lenient().when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(any(), any(), any()))
				.thenReturn(new HpcDataTransferConfiguration());
		when(globusTransferDAOMock.getGlobusUsersAllocated())
				.thenReturn(Arrays.asList("userA", "userB", "userC", "userD"));
		when(globusTransferDAOMock.getGlobusRequestCountForUser("userA")).thenReturn(2); // 2 slots used
		when(dataDownloadDAOMock.getUserCountByDataTransferType(HpcDataTransferType.GLOBUS)).thenReturn(4); // 4 users
		when(systemAccountLocatorMock.getSystemAccountCount("config1")).thenReturn(4); // quota = 4/4 = 1

		// Act
		boolean result = dataTransferService.continueDataObjectDownloadTask(buildGlobusDownloadTask("userA", "config1"));

		// Assert – user exceeds fair share (2 > 1); must be rejected early.
		assertFalse(result);
		verify(systemAccountLocatorMock, never()).getSystemAccount(any(HpcDataTransferType.class), any());
	}

	/**
	 * Fair-access: requester IS in the Globus task table but getUserCountByDataTransferType
	 * returns 0 (race condition where the task was completed between queries).
	 * Expected: no ArithmeticException; user is treated as eligible.
	 */
	@Test
	public void testFairAccess_ZeroUsersInQueue_NoDivisionByZero() throws HpcException {
		// Arrange
		Mockito.lenient().when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(any(), any(), any()))
				.thenReturn(new HpcDataTransferConfiguration());
		when(globusTransferDAOMock.getGlobusUsersAllocated()).thenReturn(Arrays.asList("userA"));
		when(globusTransferDAOMock.getGlobusRequestCountForUser("userA")).thenReturn(1);
		when(dataDownloadDAOMock.getUserCountByDataTransferType(HpcDataTransferType.GLOBUS)).thenReturn(0); // edge case
		when(systemAccountLocatorMock.getSystemAccountCount("config1")).thenReturn(2);

		HpcIntegratedSystemAccount account = new HpcIntegratedSystemAccount();
		account.setUsername("test-globus-account");
		when(systemAccountLocatorMock.getSystemAccount(any(HpcDataTransferType.class), any())).thenReturn(account);
		Mockito.lenient().when(dataTransferProxyMock.authenticate(any(), any(), any(), any())).thenReturn("mock-token");
		HpcTransferAcceptanceResponse response = new HpcTransferAcceptanceResponse() {
			@Override
			public boolean canAcceptTransfer() {
				return false;
			}
		};
		when(dataTransferProxyMock.acceptsTransferRequests(any())).thenReturn(response);

		// Act – must not throw ArithmeticException
		boolean result = dataTransferService.continueDataObjectDownloadTask(buildGlobusDownloadTask("userA", "config1"));

		// Assert – zero-user guard treats the user as eligible; execution proceeds.
		assertFalse(result);
		verify(systemAccountLocatorMock).getSystemAccount(any(HpcDataTransferType.class), any());
	}

	/**
	 * Build a minimal Globus data-object download task for fair-access tests.
	 */
	private HpcDataObjectDownloadTask buildGlobusDownloadTask(String userId, String configId) {
		HpcDataObjectDownloadTask task = new HpcDataObjectDownloadTask();
		task.setId("task-" + userId);
		task.setUserId(userId);
		task.setConfigurationId(configId);
		task.setDataTransferType(HpcDataTransferType.GLOBUS);
		task.setDestinationType(HpcDataTransferType.GLOBUS);

		HpcFileLocation archiveLocation = new HpcFileLocation();
		archiveLocation.setFileContainerId("testContainer");
		archiveLocation.setFileId("testFile");
		task.setArchiveLocation(archiveLocation);

		HpcGlobusDownloadDestination dest = new HpcGlobusDownloadDestination();
		HpcFileLocation destLoc = new HpcFileLocation();
		destLoc.setFileContainerId("destContainer");
		destLoc.setFileId("destFile");
		dest.setDestinationLocation(destLoc);
		task.setGlobusDownloadDestination(dest);

		return task;
	}
}
