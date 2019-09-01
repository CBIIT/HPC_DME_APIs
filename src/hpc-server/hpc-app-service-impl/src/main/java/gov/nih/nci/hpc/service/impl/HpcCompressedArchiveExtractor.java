/**
 * HpcCompressedArchiveExtractor.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import gov.nih.nci.hpc.domain.datatransfer.HpcCompressedArchiveType;
import gov.nih.nci.hpc.domain.datatransfer.HpcPatternType;
import gov.nih.nci.hpc.domain.error.HpcErrorType;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * Extract files from compressed archives (ZIP, TAR, TGZ).
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcCompressedArchiveExtractor {
	// ---------------------------------------------------------------------//
	// Instance members
	// ---------------------------------------------------------------------//

	// The pattern convenient class to support string pattern matching
	@Autowired
	private HpcPattern pattern = null;

	// ---------------------------------------------------------------------//
	// Constructors
	// ---------------------------------------------------------------------//

	/**
	 * Default Constructor for Spring Dependency Injection.
	 */
	private HpcCompressedArchiveExtractor() {
	}

	// ---------------------------------------------------------------------//
	// Methods
	// ---------------------------------------------------------------------//

	/**
	 * Extract files from compressed archive file into another compressed archive
	 * file.
	 *
	 * @param fromCompressedArchiveFile The compressed archive file to extract files
	 *                                  from.
	 * @param toCompressedArchiveFile   The compressed archive file to extract files
	 *                                  to.
	 * @param compressedArchiveType     The compressed archive type (ZIP, TAR, TGZ).
	 * @param includePatterns           The patterns to match for extraction.
	 * @param patternType               The type of the patterns
	 * @return The number of files extracted
	 * @throws HpcException on service failure.
	 */
	public int extract(File fromCompressedArchiveFile, File toCompressedArchiveFile,
			HpcCompressedArchiveType compressedArchiveType, List<String> includePatterns, HpcPatternType patternType)
			throws HpcException {
		// Compile include patterns.
		List<Pattern> compiledIncludePatterns = pattern.compile(includePatterns, patternType, false);

		// Open archive-input and archive-output streams from the provided files, based
		// on the
		// compressedArchiveType.
		try (FileInputStream fileInputStream = new FileInputStream(fromCompressedArchiveFile);
				BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
				FileOutputStream fileOutputStream = new FileOutputStream(toCompressedArchiveFile);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);) {
			switch (compressedArchiveType) {
			case TGZ:
				try (GzipCompressorInputStream gzipCompressorInputStream = new GzipCompressorInputStream(
						bufferedInputStream);
						ArchiveInputStream archiveInputStream = new TarArchiveInputStream(gzipCompressorInputStream);
						GzipCompressorOutputStream gzipCompressorOutputStream = new GzipCompressorOutputStream(
								bufferedOutputStream);
						ArchiveOutputStream archiveOutputStream = new TarArchiveOutputStream(
								gzipCompressorOutputStream);) {
					return extract(archiveInputStream, archiveOutputStream, compressedArchiveType,
							compiledIncludePatterns);
				}

			case TAR:
				try (ArchiveInputStream archiveInputStream = new TarArchiveInputStream(bufferedInputStream);
						ArchiveOutputStream archiveOutputStream = new TarArchiveOutputStream(bufferedOutputStream)) {
					return extract(archiveInputStream, archiveOutputStream, compressedArchiveType,
							compiledIncludePatterns);
				}

			case ZIP:
				try (ArchiveInputStream archiveInputStream = new ZipArchiveInputStream(bufferedInputStream);
						ArchiveOutputStream archiveOutputStream = new ZipArchiveOutputStream(bufferedOutputStream)) {
					return extract(archiveInputStream, archiveOutputStream, compressedArchiveType,
							compiledIncludePatterns);
				}
			default:
				throw new HpcException("Unsupported comressed archive type: " + compressedArchiveType,
						HpcErrorType.UNEXPECTED_ERROR);

			}

		} catch (IOException e) {
			throw new HpcException("Failed to open compressed archive: " + e.getMessage(),
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}

	}

	// ---------------------------------------------------------------------//
	// Helper Methods
	// ---------------------------------------------------------------------//

	/**
	 * Extract files from compressed archive file into another compressed archive
	 * file.
	 *
	 * @param fromArchiveInputStream  The compressed archive extract files from.
	 * @param toArchiveOutPutStream   The compressed archive to extract files to.
	 * @param compressedArchiveType   The compressed archive type (ZIP, TAR, TGZ).
	 * @param compiledIncludePatterns The patterns to match for extraction.
	 * @return The number of files extracted
	 * @throws HpcException on service failure.
	 */
	private int extract(ArchiveInputStream fromArchiveInputStream, ArchiveOutputStream toArchiveOutPutStream,
			HpcCompressedArchiveType compressedArchiveType, List<Pattern> compiledIncludePatterns) throws HpcException {
		int counter = 0;
		ArchiveEntry fromArchiveEntry = null;
		try {
			while ((fromArchiveEntry = fromArchiveInputStream.getNextEntry()) != null) {
				if (!fromArchiveInputStream.canReadEntryData(fromArchiveEntry)) {
					continue;
				}

				if (!fromArchiveEntry.isDirectory()
						&& pattern.matches(compiledIncludePatterns, fromArchiveEntry.getName())) {
					toArchiveOutPutStream.putArchiveEntry(createArchiveEntry(fromArchiveEntry, compressedArchiveType));
					IOUtils.copyLarge(fromArchiveInputStream, toArchiveOutPutStream);
					toArchiveOutPutStream.closeArchiveEntry();

					counter++;
				}
			}

		} catch (IOException e) {
			throw new HpcException("Failed to parse compressed archive: " + e.getMessage(),
					HpcErrorType.INVALID_REQUEST_INPUT, e);
		}

		return counter;
	}

	/**
	 * Create an archive entry from another and compressedArchiveType.
	 *
	 * @param fromArchiveEntry      The archive entry to copy name and size from.
	 * @param compressedArchiveType The compressed archive type.
	 * @return An archive entry
	 * @throws HpcException on unsupported compressed archive type.
	 */
	private ArchiveEntry createArchiveEntry(ArchiveEntry fromArchiveEntry,
			HpcCompressedArchiveType compressedArchiveType) throws HpcException {
		switch (compressedArchiveType) {
		case TGZ:
		case TAR:
			TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(fromArchiveEntry.getName());
			tarArchiveEntry.setSize(fromArchiveEntry.getSize());
			return tarArchiveEntry;

		case ZIP:
			return new ZipArchiveEntry(fromArchiveEntry.getName());

		default:
			throw new HpcException("Unsupported comressed archive type: " + compressedArchiveType,
					HpcErrorType.UNEXPECTED_ERROR);

		}
	}
}
