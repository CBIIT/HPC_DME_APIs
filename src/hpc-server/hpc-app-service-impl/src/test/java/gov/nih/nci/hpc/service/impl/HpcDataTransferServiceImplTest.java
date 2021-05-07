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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import gov.nih.nci.hpc.dao.HpcDataDownloadDAO;
import gov.nih.nci.hpc.domain.datamanagement.HpcPathAttributes;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadResponse;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataTransferUploadStatus;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3Account;
import gov.nih.nci.hpc.domain.datatransfer.HpcS3DownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcStreamingUploadSource;
import gov.nih.nci.hpc.domain.model.HpcDataManagementConfiguration;
import gov.nih.nci.hpc.domain.model.HpcDataTransferConfiguration;
import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProxy;
import gov.nih.nci.hpc.service.HpcDataTransferService;

/**
 * HPC Data Transfer Service Implementation.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class HpcDataTransferServiceImplTest {

	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The app service under test.
	// @InjectMocks
	private HpcDataTransferService dataTransferService = null;

	// Expected exception rule.
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	// Mocks.
	@Mock
	private HpcDataTransferProxy dataTransferProxyMock = null;
	@Mock
	private HpcDataManagementConfigurationLocator dataManagementConfigurationLocatorMock = null;
	@Mock
	private HpcSystemAccountLocator systemAccountLocatorMock = null;
	@Mock
	private HpcDataDownloadDAO dataDownloadDAOMock = null;

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
		expectedException.expect(HpcException.class);
		expectedException.expectMessage("No data transfer source or data attachment provided or upload URL requested");

		dataTransferService.uploadDataObject(null, null, null, null, null, false, null, null, null, "testObjectId",
				null, null, null);
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
		expectedException.expect(HpcException.class);
		expectedException.expectMessage("Invalid S3 upload source");

		HpcStreamingUploadSource s3UploadSource = new HpcStreamingUploadSource();
		dataTransferService.uploadDataObject(null, s3UploadSource, null, null, null, false, null, null, null,
				"dataObjectId", null, null, null);
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

		// Mock setup.
		HpcPathAttributes pathAttributes = new HpcPathAttributes();
		pathAttributes.setIsAccessible(true);
		pathAttributes.setExists(true);
		pathAttributes.setIsDirectory(false);
		pathAttributes.setSize(123456789L);
		when(dataTransferProxyMock.getPathAttributes(anyObject(), same(sourceLocation), eq(true)))
				.thenReturn(pathAttributes);
		when(dataManagementConfigurationLocatorMock.getArchiveDataTransferType(anyObject()))
				.thenReturn(HpcDataTransferType.S_3);
		when(systemAccountLocatorMock.getSystemAccount(anyObject(), anyObject()))
				.thenReturn(new HpcIntegratedSystemAccount());
		when(systemAccountLocatorMock.getSystemAccount(anyObject())).thenReturn(new HpcIntegratedSystemAccount());
		when(dataTransferProxyMock.authenticate(anyObject(), anyObject(), anyObject(), anyObject()))
				.thenReturn("token");
		when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(anyObject(), anyObject(), anyObject()))
				.thenReturn(new HpcDataTransferConfiguration());
		when(dataManagementConfigurationLocatorMock.get(anyObject())).thenReturn(dmc);

		// Run the test.
		dataTransferService.uploadDataObject(null, s3UploadSource, null, null, null, false, null, null, "/test/path",
				"testUserId", "testCallerId", "testConfigId", "testObjectId");
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
		expectedException.expect(HpcException.class);
		expectedException.expectMessage("Invalid generate download URL request");

		dataTransferService.generateDownloadRequestURL("", "user-id", new HpcFileLocation(), null, 2000, "", "");
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
		expectedException.expect(HpcException.class);
		expectedException.expectMessage("Invalid generate download URL request");

		dataTransferService.generateDownloadRequestURL("", "user-id", new HpcFileLocation(), HpcDataTransferType.S_3,
				1000, "", "");
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
		when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(anyObject(), anyObject(), anyObject()))
				.thenReturn(new HpcDataTransferConfiguration());
		when(systemAccountLocatorMock.getSystemAccount(anyObject(), anyObject()))
				.thenReturn(new HpcIntegratedSystemAccount());
		when(systemAccountLocatorMock.getSystemAccount(anyObject())).thenReturn(new HpcIntegratedSystemAccount());
		when(dataTransferProxyMock.authenticate(anyObject(), anyObject(), anyObject(), anyObject()))
				.thenReturn("token");
		when(dataTransferProxyMock.generateDownloadRequestURL(anyObject(), anyObject(), anyObject(), anyObject()))
				.thenReturn("https://downloadURL");

		HpcFileLocation archiveLocation = new HpcFileLocation();
		archiveLocation.setFileContainerId("test");
		archiveLocation.setFileId("test");

		String downloadURL = dataTransferService.generateDownloadRequestURL("", "user-id", archiveLocation,
				HpcDataTransferType.S_3, 1000, "", "");

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
		expectedException.expect(HpcException.class);
		expectedException.expectMessage("Invalid data transfer request");

		dataTransferService.downloadDataObject("", null, null, null, null, null, null, "", "", "", false, 0L,
				HpcDataTransferUploadStatus.ARCHIVED, null);
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
		expectedException.expect(HpcException.class);
		expectedException.expectMessage("Invalid data transfer request");

		dataTransferService.downloadDataObject("", new HpcFileLocation(), null, null, null, null,
				HpcDataTransferType.S_3, "", "", "", false, 0L, HpcDataTransferUploadStatus.ARCHIVED, null);
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
		when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(anyObject(), anyObject(), anyObject()))
				.thenReturn(dataTransferConfig);
		when(dataManagementConfigurationLocatorMock.getDataTransferConfiguration(anyObject(), anyObject(), anyObject()))
				.thenReturn(dataTransferConfig);

		when(systemAccountLocatorMock.getSystemAccount(anyObject(), anyObject()))
				.thenReturn(new HpcIntegratedSystemAccount());
		when(systemAccountLocatorMock.getSystemAccount(anyObject())).thenReturn(new HpcIntegratedSystemAccount());
		when(dataTransferProxyMock.authenticate(anyObject(), anyObject(), anyObject(), anyObject()))
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

		// Run the test.
		HpcDataObjectDownloadResponse downloadResponse = dataTransferService.downloadDataObject("/test/path",
				archiveLocation, null, s3loadDestination, null, null, HpcDataTransferType.S_3, "testConfigId", "",
				"testUserId", false, 0L, HpcDataTransferUploadStatus.ARCHIVED, null);

		// Assert expected result.
		assertNull(downloadResponse.getDownloadTaskId());
		assertEquals(downloadResponse.getDestinationLocation().getFileContainerId(),
				destinationLocation.getFileContainerId());
		assertEquals(downloadResponse.getDestinationLocation().getFileId(), destinationLocation.getFileId());
	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	@Before
	public void init() throws HpcException {
		Map<HpcDataTransferType, HpcDataTransferProxy> dataTransferProxies = new HashMap<>();
		dataTransferProxies.put(HpcDataTransferType.GLOBUS, dataTransferProxyMock);
		dataTransferProxies.put(HpcDataTransferType.S_3, dataTransferProxyMock);

		HpcDataTransferServiceImpl dataTransferServiceImpl = new HpcDataTransferServiceImpl(dataTransferProxies,
				"/test/download/directory");
		dataTransferServiceImpl.setDataManagementConfigurationLocator(dataManagementConfigurationLocatorMock);
		dataTransferServiceImpl.setSystemAccountLocator(systemAccountLocatorMock);
		dataTransferServiceImpl.setDataDownloadDAO(dataDownloadDAOMock);

		dataTransferService = dataTransferServiceImpl;
	}
}
