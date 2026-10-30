/**
 * HpcDataTransferProxyImplTest.java
 *
 * <p>
 * Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>
 * Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.integration.globus.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gov.nih.nci.hpc.domain.datatransfer.HpcArchive;
import gov.nih.nci.hpc.domain.datatransfer.HpcArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.datatransfer.HpcSetArchiveObjectMetadataResponse;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * Globus data transfer proxy tests - POSIX archive file operations.
 */
public class HpcDataTransferProxyImplTest {

	private static final String ARCHIVE_CONTENT = "hello world";
	private static final String ARCHIVE_CONTENT_MD5 = "5eb63bbbe01eeed093cb22bb8f5acdc3";
	private static final String FILE_ID = "/base/collection/file name; $(x).txt";

	@TempDir
	Path tempDir;

	private HpcDataTransferProxyImpl globusProxy = null;
	private HpcArchive baseArchive = null;
	private Path archiveFile = null;

	@BeforeEach
	public void setUp() throws Exception {
		Constructor<HpcDataTransferProxyImpl> constructor = HpcDataTransferProxyImpl.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		globusProxy = constructor.newInstance();

		Path archiveDirectory = tempDir.resolve("posix archive");
		HpcFileLocation baseLocation = new HpcFileLocation();
		baseLocation.setFileContainerId("endpoint");
		baseLocation.setFileId("/base");
		baseArchive = new HpcArchive();
		baseArchive.setFileLocation(baseLocation);
		baseArchive.setType(HpcArchiveType.ARCHIVE);
		baseArchive.setDirectory(archiveDirectory.toString());

		archiveFile = archiveDirectory.resolve("collection/file name; $(x).txt");
		Files.createDirectories(archiveFile.getParent());
		Files.writeString(archiveFile, ARCHIVE_CONTENT);
	}

	@Test
	public void testSetDataObjectMetadataCalculatesChecksum() throws HpcException {
		HpcSetArchiveObjectMetadataResponse response = globusProxy.setDataObjectMetadata(null, fileLocation(FILE_ID),
				baseArchive, Collections.emptyList(), null);

		assertEquals(ARCHIVE_CONTENT_MD5, response.getChecksum());
		assertTrue(response.getMetadataAdded());
	}

	@Test
	public void testSetDataObjectMetadataMissingFile() {
		HpcException e = assertThrows(HpcException.class, () -> globusProxy.setDataObjectMetadata(null,
				fileLocation("/base/collection/missing"), baseArchive, Collections.emptyList(), null));
		assertEquals(HpcErrorType.UNEXPECTED_ERROR, e.getErrorType());
	}

	@Test
	public void testSynchronousDownloadCopiesFile() throws Exception {
		Path destination = Files.createDirectories(tempDir.resolve("download dir")).resolve("out file.txt");
		Files.writeString(destination, "existing content that is longer");

		HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
		downloadRequest.setArchiveLocation(fileLocation(FILE_ID));
		downloadRequest.setFileDestination(destination.toFile());

		globusProxy.downloadDataObject(null, downloadRequest, baseArchive, null, false);

		assertEquals(ARCHIVE_CONTENT, Files.readString(destination));
		assertEquals(ARCHIVE_CONTENT, Files.readString(archiveFile));
	}

	@Test
	public void testSynchronousDownloadMissingFile() {
		HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
		downloadRequest.setArchiveLocation(fileLocation("/base/collection/missing"));
		downloadRequest.setFileDestination(new File(tempDir.toFile(), "out"));

		HpcException e = assertThrows(HpcException.class,
				() -> globusProxy.downloadDataObject(null, downloadRequest, baseArchive, null, false));
		assertEquals(HpcErrorType.DATA_TRANSFER_ERROR, e.getErrorType());
	}

	@Test
	public void testDeleteDataObjectDeletesFileAndMetadata() throws Exception {
		Path metadataFile = archiveFile.resolveSibling("." + archiveFile.getFileName());
		Files.writeString(metadataFile, "attribute=value");

		globusProxy.deleteDataObject(null, fileLocation(FILE_ID), baseArchive);

		assertFalse(Files.exists(archiveFile));
		assertFalse(Files.exists(metadataFile));
	}

	@Test
	public void testDeleteDataObjectWithoutMetadataFile() throws Exception {
		globusProxy.deleteDataObject(null, fileLocation(FILE_ID), baseArchive);

		assertFalse(Files.exists(archiveFile));
	}

	private HpcFileLocation fileLocation(String fileId) {
		HpcFileLocation fileLocation = new HpcFileLocation();
		fileLocation.setFileContainerId("endpoint");
		fileLocation.setFileId(fileId);
		return fileLocation;
	}
}
