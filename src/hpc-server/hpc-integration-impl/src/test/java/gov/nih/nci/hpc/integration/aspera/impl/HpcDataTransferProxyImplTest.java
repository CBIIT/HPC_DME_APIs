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
package gov.nih.nci.hpc.integration.aspera.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaAccount;
import gov.nih.nci.hpc.domain.datatransfer.HpcAsperaDownloadDestination;
import gov.nih.nci.hpc.domain.datatransfer.HpcDataObjectDownloadRequest;
import gov.nih.nci.hpc.domain.datatransfer.HpcFileLocation;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;
import gov.nih.nci.hpc.integration.HpcDataTransferProgressListener;

/**
 * Aspera data transfer proxy tests. A fake 'ascp' script records the arguments
 * it was called with.
 */
public class HpcDataTransferProxyImplTest {

	private static final String ARCHIVE_CONTENT = "archive content";

	@TempDir
	Path tempDir;

	private HpcDataTransferProxyImpl asperaProxy = null;
	private Path archiveDirectory = null;
	private Path archiveFile = null;
	private Path ascpOutput = null;

	private final AtomicReference<Long> completed = new AtomicReference<>();
	private final AtomicReference<String> failed = new AtomicReference<>();
	private final HpcDataTransferProgressListener progressListener = new HpcDataTransferProgressListener() {
		@Override
		public void transferCompleted(Long bytesTransferred) {
			completed.set(bytesTransferred);
		}

		@Override
		public void transferFailed(String message) {
			failed.set(message);
		}
	};

	@BeforeEach
	public void setUp() throws Exception {
		Constructor<HpcDataTransferProxyImpl> constructor = HpcDataTransferProxyImpl.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		asperaProxy = constructor.newInstance();
		asperaProxy.asperaExecutor = Runnable::run;
		setField("privateKeyFile", "/keys/aspera key");

		archiveDirectory = Files.createDirectories(tempDir.resolve("archive dir"));
		archiveFile = Files.writeString(archiveDirectory.resolve("archive-file"), ARCHIVE_CONTENT);
		ascpOutput = tempDir.resolve("ascp-output");
	}

	@Test
	public void testDownloadPassesDestinationAsArguments() throws Exception {
		setAscp("for a in \"$@\"; do printf 'ARG=%s\\n' \"$a\" >> '" + ascpOutput + "'; done\n"
				+ "printf 'PASS=%s\\n' \"$ASPERA_SCP_PASS\" >> '" + ascpOutput + "'\n"
				+ "printf 'CONTENT=%s\\n' \"$(cat \"$8\")\" >> '" + ascpOutput + "'\n");
		String fileName = "my file; $(id) `id` | & x.txt";

		asperaProxy.downloadDataObject(null, downloadRequest("/remote dir", "dest dir/" + fileName), null,
				progressListener, false);

		List<String> expected = Arrays.asList("ARG=-i", "ARG=/keys/aspera key", "ARG=-Q", "ARG=-l", "ARG=1000m",
				"ARG=-k", "ARG=0", "ARG=" + fileName, "ARG=user@aspera.example.com:/remote dir", "PASS=secret",
				"CONTENT=" + ARCHIVE_CONTENT);
		assertEquals(expected, Files.readAllLines(ascpOutput));
		assertEquals(Long.valueOf(ARCHIVE_CONTENT.length()), completed.get());
		assertNull(failed.get());
		assertStagingDirectoryRemoved();
	}

	@Test
	public void testDownloadAscpFailure() throws Exception {
		setAscp("echo 'ascp failed' >&2\nexit 1\n");

		asperaProxy.downloadDataObject(null, downloadRequest("/remote", "file.txt"), null, progressListener, false);

		assertNull(completed.get());
		assertTrue(failed.get().contains("ascp failed"));
		assertStagingDirectoryRemoved();
	}

	@Test
	public void testDownloadRejectsFileNameStartingWithDash() throws Exception {
		setAscp("touch '" + ascpOutput + "'\n");

		HpcException e = assertThrows(HpcException.class, () -> asperaProxy.downloadDataObject(null,
				downloadRequest("/remote", "dest/-rf"), null, progressListener, false));

		assertEquals(HpcErrorType.INVALID_REQUEST_INPUT, e.getErrorType());
		assertFalse(Files.exists(ascpOutput));
		assertNull(completed.get());
		assertNull(failed.get());
	}

	private HpcDataObjectDownloadRequest downloadRequest(String fileContainerId, String fileId) {
		HpcAsperaAccount account = new HpcAsperaAccount();
		account.setUser("user");
		account.setHost("aspera.example.com");
		account.setPassword("secret");

		HpcFileLocation destinationLocation = new HpcFileLocation();
		destinationLocation.setFileContainerId(fileContainerId);
		destinationLocation.setFileId(fileId);

		HpcAsperaDownloadDestination destination = new HpcAsperaDownloadDestination();
		destination.setAccount(account);
		destination.setDestinationLocation(destinationLocation);

		HpcDataObjectDownloadRequest downloadRequest = new HpcDataObjectDownloadRequest();
		downloadRequest.setPath("/collection/data-object");
		downloadRequest.setArchiveLocationFilePath(archiveFile.toString());
		downloadRequest.setAsperaDestination(destination);
		downloadRequest.setSize(ARCHIVE_CONTENT.length());
		return downloadRequest;
	}

	private void setAscp(String script) throws Exception {
		Path ascp = tempDir.resolve("ascp");
		Files.write(ascp, ("#!/bin/sh\n" + script).getBytes(StandardCharsets.UTF_8));
		Files.setPosixFilePermissions(ascp, PosixFilePermissions.fromString("rwx------"));
		setField("ascp", ascp.toString());
	}

	private void setField(String name, Object value) throws Exception {
		Field field = HpcDataTransferProxyImpl.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(asperaProxy, value);
	}

	private void assertStagingDirectoryRemoved() throws Exception {
		try (Stream<Path> files = Files.list(archiveDirectory)) {
			assertEquals(Arrays.asList(archiveFile), files.collect(Collectors.toList()));
		}
		assertEquals(ARCHIVE_CONTENT, Files.readString(archiveFile));
	}
}
